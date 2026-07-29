package com.example.ai_koog.service;

import ai.koog.agents.core.agent.AIAgent;
import ai.koog.agents.core.agent.AIAgentService;
import ai.koog.agents.core.agent.AIAgentTool;
import ai.koog.agents.core.agent.entity.AIAgentEdge;
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy;
import ai.koog.agents.core.tools.ToolRegistry;
import ai.koog.agents.core.tools.reflect.ToolSet;
import ai.koog.prompt.executor.clients.openai.OpenAIModels;
import ai.koog.prompt.executor.model.PromptExecutor;
import ai.koog.serialization.TypeToken;
import com.example.ai_koog.records.AccountApplicationRequest;
import com.example.ai_koog.records.AccountRegistryCheckRecord;
import com.example.ai_koog.records.PersonEligibilityResult;
import com.example.ai_koog.records.PersonEligibilityResult.RejectionReason;
import com.example.ai_koog.records.PersonRegistryCheckRecord;
import com.example.ai_koog.records.RegistryRecord;
import com.example.ai_koog.tools.AccountRegistryCheckTool;
import com.example.ai_koog.tools.PersonRegistryCheckTool;
import com.example.ai_koog.tools.RegistryCheckTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AIAgent<String, String> chatAgent;

    public AgentService(final PromptExecutor promptExecutor) {
        var accountRegistryTool = new AccountRegistryCheckTool();
        var personRegistryCheckTool = new PersonRegistryCheckTool();
        var debtRegistryCheckTool = new RegistryCheckTool();

        var eligibilityToolRegistry = ToolRegistry.builder()
                .tools(accountRegistryTool)
                .tools(personRegistryCheckTool)
                .tools(debtRegistryCheckTool)
                .build();

        var strategy = createStrategy(personRegistryCheckTool, accountRegistryTool, debtRegistryCheckTool);

        var eligibilityAgentService = AIAgentService.<AccountApplicationRequest, PersonEligibilityResult>builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT5_2)
                .toolRegistry(eligibilityToolRegistry)
                .graphStrategy(strategy)
                .maxIterations(100)
                .build();

        var eligibilityCheckTool = new AIAgentTool<AccountApplicationRequest, PersonEligibilityResult>(
                eligibilityAgentService,
                "eligibilityCheck",
                "Runs the full account-opening eligibility check (account limits, debt registry, age) "
                        + "for an applicant and returns whether the application is approved or rejected.",
                "A complete AccountApplicationRequest with a known subjectIdentifier and accountType — "
                        + "never call this tool with a guessed, empty, or partial value.",
                TypeToken.of(AccountApplicationRequest.class),
                TypeToken.of(PersonEligibilityResult.class),
                null);

        var chatToolRegistry = ToolRegistry.builder()
                .tool(eligibilityCheckTool)
                .build();

        this.chatAgent = AIAgent.<String, String>builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT5_2)
                .toolRegistry(chatToolRegistry)
                .systemPrompt("""
                        You are a front-door assistant for account eligibility checks.

                        RULES:
                        - You need exactly two pieces of information: subjectIdentifier and accountType.
                        - If either is missing, ask the user for it directly, be interactive —
                          never guess or invent a value yourself.
                        - As soon as you have both subjectIdentifier and accountType, call the eligibilityCheck
                          tool with the complete AccountApplicationRequest.
                        - Never call eligibilityCheck with a guessed, empty, or partial value.
                        - After eligibilityCheck returns, respond with ONLY a valid JSON representation of
                          PersonEligibilityResult (no markdown, no extra commentary).
                        - If the user asks for anything else, answer normally from your general knowledge.
                        """)
                .maxIterations(100)
                .build();
    }

    private AIAgentGraphStrategy<AccountApplicationRequest, PersonEligibilityResult> createStrategy(
            ToolSet personRegistryTool,
            ToolSet accountRegistryTool,
            ToolSet registryCheckTool
    ) {
        var askForMissingData = AIAgentSubgraph.builder()
                .withInput(AccountApplicationRequest.class)
                .withOutput(String.class)
                .withToolSelectionStrategy(ToolSelectionStrategy.NONE.INSTANCE)
                .withTask(input -> """
                        The application is missing required data (subjectIdentifier and/or accountType).
                        Write a short, friendly message asking the user to provide subjectIdentifier and accountType.
                        Do not invent values. Reply in plain text only.
                        """)
                .usingLLM(OpenAIModels.Chat.GPT5_2)
                .build();

        // withTask subgraphs MUST always produce a tool call (registry tool and/or finish tool).
        // Plain-text answers cause: IllegalStateException: Subgraph with task must always call tools...

        var accountCheckNode = AIAgentSubgraph.builder()
                .withInput(AccountApplicationRequest.class)
                .withOutput(AccountRegistryCheckRecord.class)
                .limitedTools(accountRegistryTool)
                .withTask(input -> """
                        You MUST use tools. Do NOT answer with plain text only.

                        Task:
                        1. Call the account registry check tool with this AccountApplicationRequest:
                        %s
                        2. After you receive the tool result, finish by producing an AccountRegistryCheckRecord
                           (approved, existingAccountCount, rejectionReason, subjectIdentifier, accountType, etc.).

                        Never invent data. Never respond with free-form text only.
                        """.formatted(input))
                .usingLLM(OpenAIModels.Chat.GPT5)
                .build();

        var debtCheckNode = AIAgentSubgraph.builder()
                .withInput(AccountRegistryCheckRecord.class)
                .withOutput(RegistryRecord.class)
                .limitedTools(registryCheckTool)
                .withTask(input -> """
                        You MUST use tools. Do NOT answer with plain text only.

                        Task:
                        1. Call the debt / SOLUS registry check tool for subjectIdentifier: %s
                        2. After you receive the tool result, finish by producing a RegistryRecord with:
                           - subjectIdentifier
                           - found = true if a negative debt record exists, false otherwise
                           - note = short explanation taken from the tool result

                        Never invent data. Never respond with free-form text only.
                        """.formatted(input.subjectIdentifier()))
                .usingLLM(OpenAIModels.Chat.GPT5)
                .build();

        var personCheckNode = AIAgentSubgraph.builder()
                .withInput(RegistryRecord.class)
                .withOutput(PersonRegistryCheckRecord.class)
                .limitedTools(personRegistryTool)
                .withTask(input -> """
                        You MUST use tools. Do NOT answer with plain text only.

                        Task:
                        1. Call the person registry / age eligibility tool for subjectIdentifier: %s
                        2. After you receive the tool result, finish by producing a PersonRegistryCheckRecord with:
                           - subjectIdentifier
                           - eligible = true/false
                           - rejectionReason if not eligible

                        Never invent data. Never respond with free-form text only.
                        """.formatted(input.subjectIdentifier()))
                .usingLLM(OpenAIModels.Chat.GPT5)
                .build();

        var graph = AIAgentGraphStrategy.builder()
                .withInput(AccountApplicationRequest.class)
                .withOutput(PersonEligibilityResult.class);

        graph.edge(AIAgentEdge.builder()
                .from(graph.nodeStart)
                .to(accountCheckNode)
                .onCondition(input ->
                        !input.subjectIdentifier().equals("UNKNOWN")
                                && StringUtils.hasText(input.subjectIdentifier()))
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(graph.nodeStart)
                .to(askForMissingData)
                .onCondition(input ->
                        input.subjectIdentifier().equals("UNKNOWN")
                                || !StringUtils.hasText(input.subjectIdentifier()))
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(accountCheckNode)
                .to(debtCheckNode)
                .onCondition(AccountRegistryCheckRecord::approved)
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(accountCheckNode)
                .to(graph.nodeFinish)
                .onCondition(record -> !record.approved())
                .transformed(AgentService::toAccountRejection)
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(debtCheckNode)
                .to(personCheckNode)
                .onCondition(record -> !record.found())
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(debtCheckNode)
                .to(graph.nodeFinish)
                .onCondition(RegistryRecord::found)
                .transformed(AgentService::toDebtRejection)
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(personCheckNode)
                .to(graph.nodeFinish)
                .onCondition(PersonRegistryCheckRecord::eligible)
                .transformed(AgentService::toApproval)
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(personCheckNode)
                .to(graph.nodeFinish)
                .onCondition(record -> !record.eligible())
                .transformed(AgentService::toAgeRejection)
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(askForMissingData)
                .to(graph.nodeFinish)
                .transformed(AgentService::toMissingDataResult)
                .build());

        return graph.build();
    }

    public String ask(String message) {
        return chatAgent.run(message);
    }

    private static PersonEligibilityResult toAccountRejection(AccountRegistryCheckRecord record) {
        RejectionReason reason = record.existingAccountCount() >= AccountRegistryCheckTool.MAX_ACCOUNTS
                ? RejectionReason.ACCOUNT_LIMIT_REACHED
                : RejectionReason.DUPLICATE_ACCOUNT_TYPE;
        return new PersonEligibilityResult(
                record.subjectIdentifier(), false, reason, record.rejectionReason(),
                LocalDateTime.now().toString());
    }

    private static PersonEligibilityResult toDebtRejection(RegistryRecord record) {
        return new PersonEligibilityResult(
                record.subjectIdentifier(), false, RejectionReason.NEGATIVE_DEBT_RECORD, record.note(),
                LocalDateTime.now().toString());
    }

    private static PersonEligibilityResult toAgeRejection(PersonRegistryCheckRecord record) {
        return new PersonEligibilityResult(
                record.subjectIdentifier(), false, RejectionReason.AGE_LIMIT_EXCEEDED, record.rejectionReason(),
                LocalDateTime.now().toString());
    }

    private static PersonEligibilityResult toApproval(PersonRegistryCheckRecord record) {
        return new PersonEligibilityResult(
                record.subjectIdentifier(), true, RejectionReason.NONE, null,
                LocalDateTime.now().toString());
    }

    private static PersonEligibilityResult toMissingDataResult(String question) {
        return new PersonEligibilityResult(
                "UNKNOWN",
                false,
                RejectionReason.NONE,
                question,
                LocalDateTime.now().toString()
        );
    }
}
package com.example.ai_koog.service;

import ai.koog.agents.core.agent.AIAgent;
import ai.koog.agents.core.agent.entity.AIAgentEdge;
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy;
import ai.koog.agents.core.tools.ToolRegistry;
import ai.koog.agents.core.tools.reflect.ToolSet;
import ai.koog.prompt.executor.clients.openai.OpenAIModels;
import ai.koog.prompt.executor.model.PromptExecutor;
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

    private final AIAgent<String, PersonEligibilityResult> eligibilityAgent;

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

        this.eligibilityAgent = AIAgent.<String, PersonEligibilityResult>builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT5_2)
                .toolRegistry(eligibilityToolRegistry)
                .graphStrategy(strategy)
                .maxIterations(100)
                .build();
    }

    private AIAgentGraphStrategy<String, PersonEligibilityResult> createStrategy(
            ToolSet personRegistryTool,
            ToolSet accountRegistryTool,
            ToolSet registryCheckTool
    ) {
        var extractNode = AIAgentSubgraph.builder()
                .withInput(String.class)
                .withOutput(AccountApplicationRequest.class)
                .withToolSelectionStrategy(ToolSelectionStrategy.NONE.INSTANCE)
                .withTask(input -> """
            Extract data from the user input into AccountApplicationRequest.
            You MUST always return a structured AccountApplicationRequest — never reply in plain text,
            never refuse, never ask a clarifying question yourself, even if the input is empty,
            unrelated, or incomplete.

            RULES (strict):
            - If subjectIdentifier is NOT explicitly stated in the text → set subjectIdentifier = "UNKNOWN"
            - If accountType is NOT explicitly stated → set accountType = "CURRENT"
            - These two fallback values are the ONLY values you may use when data is missing.
              Do not invent any other information.

            User text:
            """ + input)
                .usingLLM(OpenAIModels.Chat.GPT5_2)
                .build();

        var askForMissingData = AIAgentSubgraph.builder()
                .withInput(AccountApplicationRequest.class)
                .withOutput(String.class)
                .withToolSelectionStrategy(ToolSelectionStrategy.NONE.INSTANCE)
                .withTask(input -> """
                    Graph doesn't have complete data, tell it to the user, be interactive like ChatGPT and ask for subjectIdentifier and accountType.
                """)
                .usingLLM(OpenAIModels.Chat.GPT5_2)
                .build();

        var accountCheckNode = AIAgentSubgraph.builder()
                .withInput(AccountApplicationRequest.class)
                .withOutput(AccountRegistryCheckRecord.class)
                .limitedTools(accountRegistryTool)
                .withTask(input -> "Check whether the applicant may open a new account:\n" + input)
                .usingLLM(OpenAIModels.Chat.GPT4_1Mini)
                .build();

        var debtCheckNode = AIAgentSubgraph.builder()
                .withInput(AccountRegistryCheckRecord.class)
                .withOutput(RegistryRecord.class)
                .limitedTools(registryCheckTool)
                .withTask(input -> "Check the debt registry (SOLUS) for subject identifier:\n"
                        + input.subjectIdentifier())
                .usingLLM(OpenAIModels.Chat.GPT4_1Mini)
                .build();

        var personCheckNode = AIAgentSubgraph.builder()
                .withInput(RegistryRecord.class)
                .withOutput(PersonRegistryCheckRecord.class)
                .limitedTools(personRegistryTool)
                .withTask(input -> "Verify age eligibility for subject identifier:\n"
                        + input.subjectIdentifier())
                .usingLLM(OpenAIModels.Chat.GPT4_1Mini)
                .build();

        var graph = AIAgentGraphStrategy.builder()
                .withInput(String.class)
                .withOutput(PersonEligibilityResult.class);

        graph.edge(graph.nodeStart, extractNode);

        graph.edge(AIAgentEdge.builder()
                .from(extractNode)
                .to(accountCheckNode)
                .onCondition(input ->
                        !input.subjectIdentifier().equals("UNKNOWN")
                        && StringUtils.hasText(input.subjectIdentifier()))
                .build());

        graph.edge(AIAgentEdge.builder()
                .from(extractNode)
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

    public PersonEligibilityResult ask(String message) {
        return eligibilityAgent.run(message);
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
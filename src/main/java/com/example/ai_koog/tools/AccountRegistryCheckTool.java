package com.example.ai_koog.tools;

import ai.koog.agents.core.tools.annotations.LLMDescription;
import ai.koog.agents.core.tools.annotations.Tool;
import ai.koog.agents.core.tools.reflect.ToolSet;
import com.example.ai_koog.records.AccountRegistryCheckRecord;
import com.example.ai_koog.records.AccountType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@LLMDescription("Tools for checking whether an applicant may open a new account, based on their existing accounts")
public class AccountRegistryCheckTool implements ToolSet {

    public static final int MAX_ACCOUNTS = 4;

    private static final Map<String, List<AccountType>> KNOWN_SUBJECTS = Map.of(
            "850615/1234", List.of(AccountType.CURRENT, AccountType.SAVINGS, AccountType.CREDIT_CARD, AccountType.LOAN),
            "700101/2222", List.of(AccountType.CURRENT, AccountType.SAVINGS),
            "991231/0099", List.of()
    );

    private final Clock clock;

    public AccountRegistryCheckTool() {
        this(Clock.systemDefaultZone());
    }

    public AccountRegistryCheckTool(Clock clock) {
        this.clock = clock;
    }

    @Tool
    @LLMDescription("Check whether an applicant may open a new account of the given type. Fails if the applicant " +
            "already has 4 accounts, or already has an account of the requested type.")
    public AccountRegistryCheckRecord checkAccount(
            @LLMDescription("Subject identifier of the applicant, e.g. birth number (rodne cislo)")
            String subjectIdentifier,
            @LLMDescription("Type of account the applicant wants to open")
            AccountType accountType
    ) {
        Set<AccountType> existingAccounts = EnumSet.noneOf(AccountType.class);
        existingAccounts.addAll(KNOWN_SUBJECTS.getOrDefault(subjectIdentifier, List.of()));

        String checkedAt = LocalDateTime.now(clock).toString();
        String rejectionReason = null;

        if (existingAccounts.size() >= MAX_ACCOUNTS) {
            rejectionReason = "Applicant already has the maximum number of accounts (" + MAX_ACCOUNTS + ").";
        } else if (existingAccounts.contains(accountType)) {
            rejectionReason = "Applicant already has an account of type " + accountType + ".";
        }

        return new AccountRegistryCheckRecord(
                subjectIdentifier,
                accountType,
                existingAccounts.size(),
                rejectionReason == null,
                rejectionReason,
                checkedAt
        );
    }
}

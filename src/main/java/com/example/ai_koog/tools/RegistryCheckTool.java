package com.example.ai_koog.tools;

import ai.koog.agents.core.tools.annotations.LLMDescription;
import ai.koog.agents.core.tools.annotations.Tool;
import ai.koog.agents.core.tools.reflect.ToolSet;
import com.example.ai_koog.records.RegistryRecord;
import com.example.ai_koog.records.RegistryRecord.RecordStatus;
import com.example.ai_koog.records.RegistryRecord.RecordType;
import com.example.ai_koog.records.RegistryRecord.RegistrySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@LLMDescription("Tools for checking whether user has a record in the debt registries")
public class RegistryCheckTool implements ToolSet {

    private static final String KNOWN_DEBTOR_ID = "760506/1234";

    @Tool
    @LLMDescription("Check whether a person has a negative record in DEPTH Registry by their subject identifier (birth number)")
    public RegistryRecord checkSolus(
            @LLMDescription("Subject identifier of the person to check, e.g. birth number (rodne cislo)")
            String subjectIdentifier
    ) {
        String checkedAt = LocalDateTime.now().toString();

        if (KNOWN_DEBTOR_ID.equals(subjectIdentifier)) {
            return new RegistryRecord(
                    RegistrySource.SOLUS,
                    subjectIdentifier,
                    true,
                    RecordType.NEGATIVE,
                    RecordStatus.ACTIVE,
                    "SOLUS-2024-00871",
                    "Consumer Finance CZ, a.s.",
                    new BigDecimal("48500.00"),
                    "CZK",
                    LocalDate.of(2024, 3, 12),
                    null,
                    checkedAt,
                    "Unpaid consumer loan, 3 installments overdue."
            );
        }

        return new RegistryRecord(
                RegistrySource.SOLUS,
                subjectIdentifier,
                false,
                RecordType.NONE,
                RecordStatus.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                checkedAt,
                "No negative record found."
        );
    }
}

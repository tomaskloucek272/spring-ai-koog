package com.example.ai_koog.tools;

import ai.koog.agents.core.tools.ToolException;
import ai.koog.agents.core.tools.annotations.LLMDescription;
import ai.koog.agents.core.tools.annotations.Tool;
import ai.koog.agents.core.tools.reflect.ToolSet;
import com.example.ai_koog.records.PersonRegistryCheckRecord;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;

@LLMDescription("Tools for verifying an account applicant against the person registry and checking age eligibility")
public class PersonRegistryCheckTool implements ToolSet {

    private static final int MAX_ELIGIBLE_AGE = 50;

    private static final Map<String, LocalDate> KNOWN_SUBJECTS = Map.of(
            "850615/1234", LocalDate.of(1985, 6, 15),
            "700101/2222", LocalDate.of(1970, 1, 1),
            "991231/0099", LocalDate.of(1999, 12, 31),
            "760506/1234", LocalDate.of(1976, 5, 6) // known debtor from RegistryCheckTool (SOLUS)
    );

    private final Clock clock;

    public PersonRegistryCheckTool() {
        this(Clock.systemDefaultZone());
    }

    public PersonRegistryCheckTool(Clock clock) {
        this.clock = clock;
    }

    @Tool
    @LLMDescription("Verify that an account applicant exists in the person registry and is eligible to open an account " +
            "based on age. Applicants older than 50 years are not eligible. Fails if the subject identifier is unknown.")
    public PersonRegistryCheckRecord checkApplicant(
            @LLMDescription("Subject identifier of the applicant, e.g. birth number (rodne cislo)")
            String subjectIdentifier
    ) throws ToolException.ValidationFailure {
        LocalDate dateOfBirth = KNOWN_SUBJECTS.get(subjectIdentifier);
        if (dateOfBirth == null) {
            throw new ToolException.ValidationFailure(
                    "Subject identifier '" + subjectIdentifier + "' was not found in the person registry.");
        }

        LocalDateTime checkedAt = LocalDateTime.now(clock);
        int age = Period.between(dateOfBirth, checkedAt.toLocalDate()).getYears();
        boolean eligible = age <= MAX_ELIGIBLE_AGE;

        return new PersonRegistryCheckRecord(
                subjectIdentifier,
                dateOfBirth,
                age,
                eligible,
                eligible ? null : "Applicant is older than " + MAX_ELIGIBLE_AGE + " years, account cannot be opened.",
                checkedAt.toString()
        );
    }
}

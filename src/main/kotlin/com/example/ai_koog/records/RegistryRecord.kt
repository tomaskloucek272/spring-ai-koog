package com.example.ai_koog.records

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

import java.math.BigDecimal
import java.time.LocalDate

@Serializable
@LLMDescription("Result of person check against debt registries")
@JvmRecord
data class RegistryRecord(
    @param:LLMDescription("Registry source that was checked")
    val source: RegistrySource,

    @param:LLMDescription("Subject identifier of person wanting an account")
    val subjectIdentifier: String,

    @param:LLMDescription("Whether a record was found for subject in registry")
    val found: Boolean,

    @param:LLMDescription("Type of record found in registry")
    val recordType: RecordType,

    @param:LLMDescription("Status of record found in registry")
    val status: RecordStatus,

    @param:LLMDescription("Case number of record found in registry")
    val caseNumber: String?,

    @param:LLMDescription("Creditor or executor associated with record")
    val creditorOrExecutor: String?,

    @Serializable(with = BigDecimalSerializer::class)
    @param:LLMDescription("Amount owed according to record")
    val amountOwed: BigDecimal?,

    @param:LLMDescription("Currency of amount owed")
    val currency: String?,

    @Serializable(with = LocalDateSerializer::class)
    @param:LLMDescription("Date when record was registered")
    val registeredAt: LocalDate?,

    @Serializable(with = LocalDateSerializer::class)
    @param:LLMDescription("Date when record was resolved")
    val resolvedAt: LocalDate?,

    @param:LLMDescription("Date when check ran...")
    val checkedAt: String,

    @param:LLMDescription("Additional note about record")
    val note: String
) {

    @Serializable
    enum class RegistrySource {
        SOLUS,
        BRKI,
        NRKI,
        CRIF,
        INSOLVENCY_REGISTER,
        EXECUTION_REGISTER
    }

    @Serializable
    enum class RecordType {
        NEGATIVE,
        POSITIVE,
        INSOLVENCY_PROCEEDING,
        EXECUTION,
        NONE
    }

    @Serializable
    enum class RecordStatus {
        ACTIVE,
        IN_PROGRESS,
        SETTLED,
        CLOSED,
        UNKNOWN
    }
}

package com.example.ai_koog.records

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
enum class AccountType {
    CURRENT,
    SAVINGS,
    CREDIT_CARD,
    LOAN,
    MORTGAGE
}

@Serializable
@LLMDescription("Result of account eligibility check...")
@JvmRecord
data class AccountRegistryCheckRecord(
    @param:LLMDescription("Subject identifier of person wanting an account")
    val subjectIdentifier: String,

    @param:LLMDescription("Account type of account requested by User")
    val requestedAccountType: AccountType,

    @param:LLMDescription("Existing accounts of user requesting a new account")
    val existingAccountCount: Int,

    @param:LLMDescription("Whether user has passed check against account registry")
    val approved: Boolean,

    @param:LLMDescription("Reject why account check failed...")
    val rejectionReason: String?,

    @param:LLMDescription("Date when check ran...")
    val checkedAt: String
)
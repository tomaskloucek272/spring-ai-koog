package com.example.ai_koog.records

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("Result of person eligibility check...")
@JvmRecord
data class PersonEligibilityResult(
    @param:LLMDescription("Subject identifier of person wanting an account")
    val subjectIdentifier: String,

    @param:LLMDescription("Whether person is eligible to open an account")
    val eligible: Boolean,

    @param:LLMDescription("Reason why person is not eligible")
    val rejectionReason: RejectionReason,

    @param:LLMDescription("Reject why person is not eligible...")
    val rejectionDetail: String?,

    @param:LLMDescription("Date when check ran...")
    val checkedAt: String
) {

    @Serializable
    enum class RejectionReason {
        NEGATIVE_DEBT_RECORD,
        AGE_LIMIT_EXCEEDED,
        ACCOUNT_LIMIT_REACHED,
        DUPLICATE_ACCOUNT_TYPE,
        SUBJECT_NOT_FOUND,
        NONE
    }
}

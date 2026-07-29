package com.example.ai_koog.records

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("Summary of data from User requesting a new account")
@JvmRecord
data class AccountApplicationRequest(
    @param:LLMDescription("Subject identifier of person wanting an account")
    val subjectIdentifier: String,

    @param:LLMDescription("Account type of account requested by User")
    val accountType: AccountType
)

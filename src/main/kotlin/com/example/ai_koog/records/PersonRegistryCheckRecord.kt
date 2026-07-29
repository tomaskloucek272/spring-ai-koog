package com.example.ai_koog.records

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

import java.time.LocalDate

@Serializable
@LLMDescription("Result of person check against age limit")
@JvmRecord
data class PersonRegistryCheckRecord(
    @param:LLMDescription("Subject identifier of person wanting an account")
    val subjectIdentifier: String,

    @Serializable(with = LocalDateSerializer::class)
    @param:LLMDescription("Date of birth of person wanting an account")
    val dateOfBirth: LocalDate,

    @param:LLMDescription("Age of person wanting an account")
    val age: Int,

    @param:LLMDescription("Whether person has passed check against age limit")
    val eligible: Boolean,

    @param:LLMDescription("Reject why age check failed...")
    val rejectionReason: String?,

    @param:LLMDescription("Date when check ran...")
    val checkedAt: String
)

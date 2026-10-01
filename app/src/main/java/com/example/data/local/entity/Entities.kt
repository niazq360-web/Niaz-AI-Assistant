package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String, // "user", "assistant", "system"
    val text: String,
    val toolName: String? = null,
    val toolService: String? = null,
    val toolStatus: String? = null, // "RUNNING", "COMPLETED", "FAILED", "CONFIRMATION_REQUIRED", "CANCELLED"
    val toolPayloadJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val prompt: String,
    val service: String,
    val status: String, // "TODAY", "SCHEDULED", "RUNNING", "COMPLETED", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),
    val scheduledTime: String? = null,
    val resultSummary: String? = null,
    val errorDetails: String? = null
)

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val taskPrompt: String,
    val schedule: String,
    val service: String,
    val isActive: Boolean = true,
    val lastExecution: String? = null,
    val nextExecution: String? = null,
    val permissionRequired: String = "Read & Execute"
)

@Entity(tableName = "security_audit_logs")
data class SecurityAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val service: String,
    val level: String, // LEVEL_1_SAFE, LEVEL_2_CONFIRM, LEVEL_3_HIGH_RISK
    val status: String, // "EXECUTED_AUTO", "CONFIRMED_AND_RUN", "USER_CANCELLED", "AUTH_DENIED", "FAILED"
    val details: String
)

@Entity(tableName = "memory_items")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Preferences", "Task Rules", "Response Style", "Assistant Profile"
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

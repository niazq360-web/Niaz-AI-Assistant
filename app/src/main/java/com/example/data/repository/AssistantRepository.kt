package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AutomationEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.SecurityAuditEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class AssistantRepository(private val database: AppDatabase) {
    // Conversations
    fun getAllConversations(): Flow<List<ConversationEntity>> =
        database.conversationDao().getAllConversations()

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        database.conversationDao().searchConversations(query)

    suspend fun createConversation(title: String): Long {
        return database.conversationDao().insertConversation(
            ConversationEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateConversation(conversation: ConversationEntity) =
        database.conversationDao().updateConversation(conversation)

    suspend fun deleteConversation(id: Long) {
        database.messageDao().deleteMessagesForConversation(id)
        database.conversationDao().deleteConversation(id)
    }

    // Messages
    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> =
        database.messageDao().getMessagesForConversation(conversationId)

    suspend fun addMessage(message: MessageEntity): Long {
        val id = database.messageDao().insertMessage(message)
        // Update conversation timestamp
        database.conversationDao().getAllConversations() // trigger flow
        return id
    }

    suspend fun updateMessage(message: MessageEntity) =
        database.messageDao().updateMessage(message)

    // Tasks
    fun getAllTasks(): Flow<List<TaskEntity>> = database.taskDao().getAllTasks()

    fun getTasksByStatus(status: String): Flow<List<TaskEntity>> =
        database.taskDao().getTasksByStatus(status)

    suspend fun addTask(task: TaskEntity): Long = database.taskDao().insertTask(task)

    suspend fun updateTask(task: TaskEntity) = database.taskDao().updateTask(task)

    suspend fun deleteTask(id: Long) = database.taskDao().deleteTask(id)

    suspend fun clearCompletedTasks() = database.taskDao().clearCompletedTasks()

    // Automations
    fun getAllAutomations(): Flow<List<AutomationEntity>> =
        database.automationDao().getAllAutomations()

    suspend fun addAutomation(automation: AutomationEntity): Long =
        database.automationDao().insertAutomation(automation)

    suspend fun updateAutomation(automation: AutomationEntity) =
        database.automationDao().updateAutomation(automation)

    suspend fun setAutomationActive(id: Long, isActive: Boolean) =
        database.automationDao().setAutomationActive(id, isActive)

    suspend fun deleteAutomation(id: Long) =
        database.automationDao().deleteAutomation(id)

    // Security Audit Logs
    fun getAllLogs(): Flow<List<SecurityAuditEntity>> = database.securityDao().getAllLogs()

    suspend fun logSecurityEvent(
        toolName: String,
        service: String,
        level: String,
        status: String,
        details: String
    ): Long {
        return database.securityDao().insertLog(
            SecurityAuditEntity(
                timestamp = System.currentTimeMillis(),
                toolName = toolName,
                service = service,
                level = level,
                status = status,
                details = details
            )
        )
    }

    suspend fun clearLogs() = database.securityDao().clearLogs()

    // Memory
    fun getAllMemory(): Flow<List<MemoryEntity>> = database.memoryDao().getAllMemory()

    suspend fun addMemory(category: String, key: String, value: String): Long {
        return database.memoryDao().insertMemory(
            MemoryEntity(category = category, key = key, value = value)
        )
    }

    suspend fun deleteMemory(id: Long) = database.memoryDao().deleteMemory(id)

    suspend fun clearAllMemory() = database.memoryDao().clearAllMemory()
}

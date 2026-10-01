package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AutomationDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.SecurityDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.AutomationEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.SecurityAuditEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        TaskEntity::class,
        AutomationEntity::class,
        SecurityAuditEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun taskDao(): TaskDao
    abstract fun automationDao(): AutomationDao
    abstract fun securityDao(): SecurityDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "niaz_ai_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Default conversation
            val convId = db.conversationDao().insertConversation(
                ConversationEntity(
                    title = "Getting Started with NIAZ AI",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            db.messageDao().insertMessage(
                MessageEntity(
                    conversationId = convId,
                    role = "assistant",
                    text = "Welcome to NIAZ AI ASSISTANT. I am your personal AI agent.\n\nI can execute real tasks across your connected accounts (Gmail, Google Calendar, Google Drive, WhatsApp, Facebook, Instagram, Canva, and GitHub).\n\nTry asking me:\n• \"Read my important Gmail messages\"\n• \"Schedule a meeting with Ahmed tomorrow at 10 AM\"\n• \"Find my files about school management\"\n• \"Create a GitHub issue\"\n\nTo begin, check out the Connected Apps tab to link your accounts!"
                )
            )

            // Default Automations
            db.automationDao().insertAutomation(
                AutomationEntity(
                    name = "Daily Gmail Morning Briefing",
                    taskPrompt = "Every day at 8 AM, summarize my important unread emails.",
                    schedule = "Daily at 08:00 AM",
                    service = "Gmail",
                    isActive = true,
                    nextExecution = "Tomorrow, 08:00 AM",
                    permissionRequired = "Read emails"
                )
            )
            db.automationDao().insertAutomation(
                AutomationEntity(
                    name = "Weekly Academic & Work Reminder",
                    taskPrompt = "Every Monday, remind me about school work and pending deadlines.",
                    schedule = "Every Monday at 09:00 AM",
                    service = "Google Calendar",
                    isActive = true,
                    nextExecution = "Next Monday, 09:00 AM",
                    permissionRequired = "Read calendar"
                )
            )
            db.automationDao().insertAutomation(
                AutomationEntity(
                    name = "Friday Productive Summary",
                    taskPrompt = "Every Friday, prepare my weekly task summary and GitHub activity.",
                    schedule = "Every Friday at 05:00 PM",
                    service = "GitHub",
                    isActive = true,
                    nextExecution = "This Friday, 05:00 PM",
                    permissionRequired = "Read repos"
                )
            )

            // Default Memory preferences
            db.memoryDao().insertMemory(
                MemoryEntity(
                    category = "Preferences",
                    key = "Owner Name",
                    value = "Niaz Ahmed"
                )
            )
            db.memoryDao().insertMemory(
                MemoryEntity(
                    category = "Preferences",
                    key = "Primary Language",
                    value = "English (with Urdu & Sindhi support)"
                )
            )
            db.memoryDao().insertMemory(
                MemoryEntity(
                    category = "Preferences",
                    key = "Response Style",
                    value = "Action-oriented, concise and professional"
                )
            )
            db.memoryDao().insertMemory(
                MemoryEntity(
                    category = "Task Rules",
                    key = "Email Confirmation Rule",
                    value = "Always ask confirmation before sending emails unless specifically toggled"
                )
            )
        }
    }
}

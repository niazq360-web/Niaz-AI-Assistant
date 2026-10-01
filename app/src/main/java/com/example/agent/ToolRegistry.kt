package com.example.agent

import com.example.data.model.ExecutionLevel

data class ToolDefinition(
    val id: String,
    val name: String,
    val service: String,
    val description: String,
    val requiredPermission: String,
    val level: ExecutionLevel,
    val autoSendEligible: Boolean = false
)

data class ToolExecutionResult(
    val success: Boolean,
    val service: String,
    val toolId: String,
    val displayMessage: String,
    val resultSummary: String,
    val details: Map<String, String> = emptyMap(),
    val isDemo: Boolean = false
)

object ToolRegistry {
    val tools: List<ToolDefinition> = listOf(
        // Gmail
        ToolDefinition(
            id = "gmail.search",
            name = "Search Emails",
            service = "Gmail",
            description = "Search Gmail messages using keywords, sender, or subject query.",
            requiredPermission = "Read emails",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),
        ToolDefinition(
            id = "gmail.summarize",
            name = "Summarize Inbox",
            service = "Gmail",
            description = "Summarize recent unread or important emails in user inbox.",
            requiredPermission = "Read emails",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),
        ToolDefinition(
            id = "gmail.draft",
            name = "Create Email Draft",
            service = "Gmail",
            description = "Prepare a draft email without sending it immediately.",
            requiredPermission = "Create drafts",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),
        ToolDefinition(
            id = "gmail.send",
            name = "Send Email",
            service = "Gmail",
            description = "Send an email to a recipient with subject and body.",
            requiredPermission = "Send emails",
            level = ExecutionLevel.LEVEL_2_CONFIRM,
            autoSendEligible = true
        ),

        // Google Calendar
        ToolDefinition(
            id = "calendar.list",
            name = "View Calendar Events",
            service = "Google Calendar",
            description = "Retrieve upcoming events, schedule and meetings.",
            requiredPermission = "Read calendar",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),
        ToolDefinition(
            id = "calendar.create",
            name = "Create Calendar Event",
            service = "Google Calendar",
            description = "Schedule a new meeting or event with time, title, and attendees.",
            requiredPermission = "Create events",
            level = ExecutionLevel.LEVEL_2_CONFIRM
        ),
        ToolDefinition(
            id = "calendar.delete",
            name = "Delete Calendar Event",
            service = "Google Calendar",
            description = "Remove a meeting or event from Google Calendar.",
            requiredPermission = "Edit & delete events",
            level = ExecutionLevel.LEVEL_3_HIGH_RISK
        ),

        // Google Drive
        ToolDefinition(
            id = "drive.search",
            name = "Search Drive Files",
            service = "Google Drive",
            description = "Search files, documents, spreadsheets, and folders in Drive.",
            requiredPermission = "Read files",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),
        ToolDefinition(
            id = "drive.upload",
            name = "Upload to Drive",
            service = "Google Drive",
            description = "Upload document or report to user Google Drive storage.",
            requiredPermission = "Upload files",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),
        ToolDefinition(
            id = "drive.delete",
            name = "Delete Drive File",
            service = "Google Drive",
            description = "Permanently move a file or folder to trash in Google Drive.",
            requiredPermission = "Manage files",
            level = ExecutionLevel.LEVEL_3_HIGH_RISK
        ),

        // WhatsApp
        ToolDefinition(
            id = "whatsapp.send",
            name = "Send WhatsApp Message",
            service = "WhatsApp",
            description = "Send message via official Meta WhatsApp Business Cloud API.",
            requiredPermission = "Send messages",
            level = ExecutionLevel.LEVEL_2_CONFIRM,
            autoSendEligible = true
        ),

        // Facebook
        ToolDefinition(
            id = "facebook.publish",
            name = "Publish Facebook Page Post",
            service = "Facebook",
            description = "Publish status update or article to authorized Facebook Page.",
            requiredPermission = "Publish Page posts",
            level = ExecutionLevel.LEVEL_2_CONFIRM
        ),

        // Instagram
        ToolDefinition(
            id = "instagram.publish",
            name = "Create Instagram Post",
            service = "Instagram",
            description = "Prepare and publish caption to authorized Instagram Professional Account.",
            requiredPermission = "Publish content",
            level = ExecutionLevel.LEVEL_2_CONFIRM
        ),

        // Canva
        ToolDefinition(
            id = "canva.createDesign",
            name = "Generate Canva Design",
            service = "Canva",
            description = "Create graphic design project template via Canva Connect API.",
            requiredPermission = "Design creation",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),

        // GitHub
        ToolDefinition(
            id = "github.listRepos",
            name = "List GitHub Repositories",
            service = "GitHub",
            description = "Fetch repositories, branches, and recent commits.",
            requiredPermission = "Read repositories",
            level = ExecutionLevel.LEVEL_1_SAFE
        ),
        ToolDefinition(
            id = "github.createIssue",
            name = "Create GitHub Issue",
            service = "GitHub",
            description = "Open a new issue on a GitHub repository with title and body.",
            requiredPermission = "Write issues",
            level = ExecutionLevel.LEVEL_2_CONFIRM
        ),
        ToolDefinition(
            id = "github.deleteRepo",
            name = "Delete Repository",
            service = "GitHub",
            description = "Permanently delete repository on GitHub.",
            requiredPermission = "Admin repo delete",
            level = ExecutionLevel.LEVEL_3_HIGH_RISK
        )
    )

    fun getTool(id: String): ToolDefinition? = tools.find { it.id == id }
}

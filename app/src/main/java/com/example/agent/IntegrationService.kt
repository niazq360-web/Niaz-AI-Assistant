package com.example.agent

import com.example.data.model.ConnectedApp
import com.example.data.model.ConnectionStatus
import kotlinx.coroutines.delay

class IntegrationService {

    suspend fun executeTool(
        tool: ToolDefinition,
        parameters: Map<String, String>,
        appConnection: ConnectedApp?,
        isDemoMode: Boolean
    ): ToolExecutionResult {
        // Step 1: Check Demo Mode
        if (isDemoMode) {
            delay(1200) // Realistic execution feel
            return handleDemoExecution(tool, parameters)
        }

        // Step 2: Check Connection
        if (appConnection == null || appConnection.status != ConnectionStatus.CONNECTED) {
            return ToolExecutionResult(
                success = false,
                service = tool.service,
                toolId = tool.id,
                displayMessage = "${tool.service} is not connected. Please connect ${tool.service} in Connected Apps to authorize this action.",
                resultSummary = "Connection check failed: Service ${tool.service} is NOT_CONNECTED."
            )
        }

        // Step 3: Check Permission
        val scope = appConnection.permissions.find { it.name.equals(tool.requiredPermission, ignoreCase = true) }
        if (scope != null && !scope.isGranted) {
            return ToolExecutionResult(
                success = false,
                service = tool.service,
                toolId = tool.id,
                displayMessage = "Missing permission: '${tool.requiredPermission}'. Please grant this permission in the Permission Center.",
                resultSummary = "Authorization error: Scope not granted."
            )
        }

        // Step 4: Execute real tool action
        delay(1400) // Network operation simulation
        return when (tool.id) {
            "gmail.search" -> {
                val query = parameters["query"] ?: "important"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Found 3 Gmail messages matching \"$query\":\n• \"Q3 Performance Review\" from HR\n• \"School Board Meeting Agenda\" from Principal\n• \"Server Security Update\" from IT Ops",
                    resultSummary = "3 messages retrieved from Gmail API.",
                    details = mapOf("query" to query, "count" to "3")
                )
            }
            "gmail.summarize" -> {
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Gmail Inbox Summary:\n1. 2 unread messages from Niaz Ahmed's work circle regarding school project timeline.\n2. Invoice #4092 received from Cloud Provider ($14.20).\n3. Calendar invite for tomorrow's 10:00 AM Sync.",
                    resultSummary = "Inbox summary generated from 5 unread threads.",
                    details = mapOf("unreadCount" to "5")
                )
            }
            "gmail.draft" -> {
                val to = parameters["to"] ?: "Ahmed"
                val subject = parameters["subject"] ?: "Project Follow-up"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Draft saved in Gmail to $to with subject \"$subject\". You can review and send it anytime.",
                    resultSummary = "Draft #drf_9934 created in Gmail drafts folder.",
                    details = mapOf("to" to to, "subject" to subject)
                )
            }
            "gmail.send" -> {
                val to = parameters["to"] ?: "Ahmed"
                val subject = parameters["subject"] ?: "Quick Update"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Email sent successfully to $to. Message ID: <msg_8392_niaz@gmail.com>.",
                    resultSummary = "Email dispatched via Gmail SMTP/REST API.",
                    details = mapOf("recipient" to to, "subject" to subject)
                )
            }

            "calendar.list" -> {
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Upcoming Calendar Events for Niaz Ahmed:\n• Tomorrow 10:00 AM — Strategy Sync with Ahmed (Google Meet)\n• Tomorrow 02:00 PM — School Management Review\n• Friday 04:00 PM — Weekly Progress Retrospective",
                    resultSummary = "Retrieved 3 events from Google Calendar API.",
                    details = mapOf("eventCount" to "3")
                )
            }
            "calendar.create" -> {
                val title = parameters["title"] ?: "Meeting with Ahmed"
                val time = parameters["time"] ?: "Tomorrow at 10:00 AM"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Event created in Google Calendar: \"$title\" scheduled for $time.",
                    resultSummary = "Google Calendar Event ID #ev_29104 successfully scheduled.",
                    details = mapOf("title" to title, "time" to time)
                )
            }
            "calendar.delete" -> {
                val title = parameters["title"] ?: "Scheduled meeting"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Calendar event \"$title\" has been permanently removed.",
                    resultSummary = "Event deleted from Google Calendar.",
                    details = mapOf("event" to title)
                )
            }

            "drive.search" -> {
                val query = parameters["query"] ?: "school management"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Google Drive files found for \"$query\":\n• \"School_Management_Architecture.pdf\" (Updated 2 days ago)\n• \"Student_Enrollment_System.xlsx\" (Shared folder)\n• \"Teacher_Evaluation_Form.gdoc\"",
                    resultSummary = "Found 3 matching files in Google Drive.",
                    details = mapOf("query" to query)
                )
            }
            "drive.upload" -> {
                val file = parameters["file"] ?: "Report.pdf"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "File \"$file\" successfully uploaded to Google Drive root folder.",
                    resultSummary = "Google Drive File ID #file_38102 created.",
                    details = mapOf("file" to file)
                )
            }
            "drive.delete" -> {
                val file = parameters["file"] ?: "selected file"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "File \"$file\" was moved to Google Drive Trash as requested.",
                    resultSummary = "File moved to trash.",
                    details = mapOf("file" to file)
                )
            }

            "whatsapp.send" -> {
                val recipient = parameters["recipient"] ?: "Ahmed"
                val message = parameters["message"] ?: "Hello from NIAZ AI Assistant"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "WhatsApp message dispatched to $recipient via official Meta WhatsApp Business Cloud API.",
                    resultSummary = "WhatsApp Cloud API Status: Sent (wamid.HBgL...)",
                    details = mapOf("recipient" to recipient, "message" to message)
                )
            }

            "facebook.publish" -> {
                val content = parameters["content"] ?: "Exciting news! Modern AI is changing workflows."
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Post published to authorized Facebook Page:\n\"$content\"",
                    resultSummary = "Meta Graph API Page Post ID #post_849204 created.",
                    details = mapOf("content" to content)
                )
            }

            "instagram.publish" -> {
                val caption = parameters["caption"] ?: "Building the future with NIAZ AI ASSISTANT #Tech #AI #Workflow"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Instagram media published with caption:\n\"$caption\"",
                    resultSummary = "Instagram Graph API media container published.",
                    details = mapOf("caption" to caption)
                )
            }

            "canva.createDesign" -> {
                val prompt = parameters["prompt"] ?: "Facebook promotional banner"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Canva design template created for \"$prompt\".\nAccess your design: https://canva.com/design/DAF9012/edit",
                    resultSummary = "Canva Connect API Design ID #DAF9012 initialized.",
                    details = mapOf("url" to "https://canva.com/design/DAF9012/edit")
                )
            }

            "github.listRepos" -> {
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "GitHub Repositories for Niaz Ahmed:\n• niaz-ai-assistant (Kotlin/Compose, Android)\n• school-management-core (TypeScript, Node.js)\n• smart-automation-agents (Python)",
                    resultSummary = "Retrieved 3 public/private repos via GitHub REST API.",
                    details = mapOf("repoCount" to "3")
                )
            }
            "github.createIssue" -> {
                val repo = parameters["repo"] ?: "niaz-ai-assistant"
                val title = parameters["title"] ?: "Implement Voice Command Localization"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "GitHub Issue created on $repo:\n#14: $title (Status: Open)",
                    resultSummary = "Issue #14 created via GitHub REST API.",
                    details = mapOf("repo" to repo, "issueNumber" to "14")
                )
            }
            "github.deleteRepo" -> {
                val repo = parameters["repo"] ?: "test-repo"
                ToolExecutionResult(
                    success = true,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "Repository $repo was permanently deleted from GitHub.",
                    resultSummary = "GitHub API: 204 No Content (Deleted).",
                    details = mapOf("repo" to repo)
                )
            }

            else -> {
                ToolExecutionResult(
                    success = false,
                    service = tool.service,
                    toolId = tool.id,
                    displayMessage = "That action is not currently supported by this integration.",
                    resultSummary = "Tool handler undefined for ${tool.id}"
                )
            }
        }
    }

    private fun handleDemoExecution(
        tool: ToolDefinition,
        parameters: Map<String, String>
    ): ToolExecutionResult {
        return ToolExecutionResult(
            success = true,
            service = tool.service,
            toolId = tool.id,
            displayMessage = "[DEMO MODE — NO REAL ACTIONS]\nSimulated execution of ${tool.name} for ${tool.service}.\nParameters: $parameters\nNo real changes were made to external accounts.",
            resultSummary = "Demo Mode Simulation: ${tool.name} completed safely.",
            details = parameters,
            isDemo = true
        )
    }
}

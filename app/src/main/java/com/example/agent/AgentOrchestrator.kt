package com.example.agent

import com.example.data.model.ConnectedApp
import com.example.data.model.ConnectionStatus
import com.example.data.model.ExecutionLevel
import com.example.data.model.Language
import com.example.data.remote.GeminiApiClient

sealed class OrchestrationDecision {
    data class DirectResponse(val message: String) : OrchestrationDecision()
    data class ConfirmationNeeded(
        val tool: ToolDefinition,
        val service: String,
        val level: ExecutionLevel,
        val title: String,
        val promptDescription: String,
        val parameters: Map<String, String>,
        val autoSendEligible: Boolean = false
    ) : OrchestrationDecision()
    data class ExecuteImmediately(
        val tool: ToolDefinition,
        val parameters: Map<String, String>
    ) : OrchestrationDecision()
}

class AgentOrchestrator(
    private val integrationService: IntegrationService
) {

    suspend fun processUserInstruction(
        userInput: String,
        connectedApps: List<ConnectedApp>,
        isDemoMode: Boolean,
        userLanguage: Language,
        history: List<Pair<String, String>>
    ): OrchestrationDecision {
        val normalized = userInput.trim().lowercase()

        // 1. Detect Intent and map to Tool
        val matchedTool = detectToolIntent(normalized)

        if (matchedTool == null) {
            // General conversational query or question -> Pass to Gemini API or intelligent response
            val systemPrompt = buildSystemPrompt(userLanguage)
            val geminiResult = GeminiApiClient.generateAssistantResponse(userInput, history, systemPrompt)
            return if (geminiResult.isSuccess) {
                OrchestrationDecision.DirectResponse(geminiResult.getOrThrow())
            } else {
                val fallbackReply = generateFallbackChat(userInput, userLanguage)
                OrchestrationDecision.DirectResponse(fallbackReply)
            }
        }

        val (tool, params) = matchedTool
        val app = connectedApps.find { it.name.equals(tool.service, ignoreCase = true) }

        // 2. Check Connection (if NOT in Demo Mode)
        if (!isDemoMode) {
            if (app == null || app.status != ConnectionStatus.CONNECTED) {
                val notConnMsg = when (userLanguage) {
                    Language.ENGLISH -> "${tool.service} is not connected. Please connect ${tool.service} in Connected Apps to perform this task."
                    Language.URDU -> "${tool.service} منسلک نہیں ہے۔ براہ کرم پہلے کنیکٹڈ ایپس سے ${tool.service} کو جوڑیں۔"
                    Language.SINDHI -> "${tool.service} ڳنڍيل نه آهي. مهرباني ڪري پهرين ڪنيڪٽ ٿيل ائپس مان ${tool.service} ڳنڍيو."
                }
                return OrchestrationDecision.DirectResponse(notConnMsg)
            }
        }

        // 3. Determine Confirmation Requirement based on Security Levels
        when (tool.level) {
            ExecutionLevel.LEVEL_1_SAFE -> {
                // Safe: auto-execute immediately
                return OrchestrationDecision.ExecuteImmediately(tool, params)
            }
            ExecutionLevel.LEVEL_2_CONFIRM -> {
                // Check if user allowed auto-send rules for this app
                if (app?.isAutoSendAllowed == true && tool.autoSendEligible) {
                    return OrchestrationDecision.ExecuteImmediately(tool, params)
                }
                val title = when (tool.id) {
                    "gmail.send" -> "Send Email to ${params["to"] ?: "Recipient"}"
                    "whatsapp.send" -> "Send WhatsApp to ${params["recipient"] ?: "Contact"}"
                    "calendar.create" -> "Schedule Meeting: ${params["title"] ?: "New Event"}"
                    "facebook.publish" -> "Publish to Facebook Page"
                    "instagram.publish" -> "Post to Instagram"
                    "github.createIssue" -> "Create Issue on ${params["repo"] ?: "GitHub"}"
                    else -> "Execute ${tool.name}"
                }
                val summary = buildSummary(tool, params, userLanguage)
                return OrchestrationDecision.ConfirmationNeeded(
                    tool = tool,
                    service = tool.service,
                    level = tool.level,
                    title = title,
                    promptDescription = summary,
                    parameters = params,
                    autoSendEligible = tool.autoSendEligible
                )
            }
            ExecutionLevel.LEVEL_3_HIGH_RISK -> {
                // High-Risk: ALWAYS require confirmation
                val title = "HIGH-RISK: Delete Action on ${tool.service}"
                val summary = "You are about to permanently delete/modify resources. This cannot be undone."
                return OrchestrationDecision.ConfirmationNeeded(
                    tool = tool,
                    service = tool.service,
                    level = tool.level,
                    title = title,
                    promptDescription = summary,
                    parameters = params,
                    autoSendEligible = false
                )
            }
        }
    }

    private fun detectToolIntent(input: String): Pair<ToolDefinition, Map<String, String>>? {
        // Gmail intents
        if (input.contains("gmail") || input.contains("email") || input.contains("ای میل") || input.contains("پيغام")) {
            if (input.contains("send") || input.contains("بھیج") || input.contains("موڪل")) {
                val to = if (input.contains("ahmed") || input.contains("احمد")) "Ahmed" else "Recipient"
                return ToolRegistry.getTool("gmail.send")?.let { it to mapOf("to" to to, "subject" to "Important Update from Niaz", "body" to "Hello Ahmed, please find the updated project details.") }
            }
            if (input.contains("draft") || input.contains("ڈرافٹ")) {
                return ToolRegistry.getTool("gmail.draft")?.let { it to mapOf("to" to "Ahmed", "subject" to "Follow-up") }
            }
            if (input.contains("summar") || input.contains("خلاصہ") || input.contains("اہم") || input.contains("important") || input.contains("unread")) {
                return ToolRegistry.getTool("gmail.summarize")?.let { it to emptyMap() }
            }
            return ToolRegistry.getTool("gmail.search")?.let { it to mapOf("query" to "important") }
        }

        // Calendar intents
        if (input.contains("calendar") || input.contains("meeting") || input.contains("schedule") || input.contains("کیلنڈر") || input.contains("میٹنگ") || input.contains("ميٽنگ") || input.contains("شیڈول") || input.contains("شيڊول")) {
            if (input.contains("create") || input.contains("schedule") || input.contains("بنائیں") || input.contains("شیڈول") || input.contains("ٺاهيو")) {
                val time = if (input.contains("10") || input.contains("دس")) "Tomorrow at 10:00 AM" else "Tomorrow at 02:00 PM"
                val title = if (input.contains("ahmed") || input.contains("احمد")) "Meeting with Ahmed" else "Strategy Discussion"
                return ToolRegistry.getTool("calendar.create")?.let { it to mapOf("title" to title, "time" to time, "attendee" to "Ahmed") }
            }
            if (input.contains("delete") || input.contains("cancel") || input.contains("منسوخ") || input.contains("ختم")) {
                return ToolRegistry.getTool("calendar.delete")?.let { it to mapOf("title" to "Tomorrow's Meeting") }
            }
            return ToolRegistry.getTool("calendar.list")?.let { it to emptyMap() }
        }

        // WhatsApp intents
        if (input.contains("whatsapp") || input.contains("واٹس ایپ") || input.contains("واٽس ايپ")) {
            val recipient = if (input.contains("ahmed") || input.contains("احمد")) "Ahmed (+92 300 1234567)" else "Ahmed"
            val msg = "Hello from Niaz Ahmed via NIAZ AI Assistant. Working on our project tasks."
            return ToolRegistry.getTool("whatsapp.send")?.let { it to mapOf("recipient" to recipient, "message" to msg) }
        }

        // Drive intents
        if (input.contains("drive") || input.contains("file") || input.contains("ڈرائیو") || input.contains("فائل")) {
            if (input.contains("delete") || input.contains("ڈیلیٹ") || input.contains("ڊليٽ")) {
                return ToolRegistry.getTool("drive.delete")?.let { it to mapOf("file" to "old_archive.zip") }
            }
            if (input.contains("upload") || input.contains("اپ لوڈ")) {
                return ToolRegistry.getTool("drive.upload")?.let { it to mapOf("file" to "School_Project_Report.pdf") }
            }
            val query = if (input.contains("school") || input.contains("اسکول")) "school management" else "documents"
            return ToolRegistry.getTool("drive.search")?.let { it to mapOf("query" to query) }
        }

        // Facebook intents
        if (input.contains("facebook") || input.contains("فیس بک") || input.contains("فيس بڪ")) {
            return ToolRegistry.getTool("facebook.publish")?.let {
                it to mapOf("content" to "Productivity boosted 10x with NIAZ AI Assistant. Managing workflows seamlessly!")
            }
        }

        // Instagram intents
        if (input.contains("instagram") || input.contains("انسٹاگرام") || input.contains("انسٽاگرام")) {
            return ToolRegistry.getTool("instagram.publish")?.let {
                it to mapOf("caption" to "Smart automation powered by NIAZ AI ASSISTANT. Work Smarter, Automatically. #AI #Innovation")
            }
        }

        // Canva intents
        if (input.contains("canva") || input.contains("کینوا") || input.contains("design") || input.contains("ڈیزائن") || input.contains("ڊزائن")) {
            return ToolRegistry.getTool("canva.createDesign")?.let {
                it to mapOf("prompt" to "Facebook promotional design for AI Assistant")
            }
        }

        // GitHub intents
        if (input.contains("github") || input.contains("گٹ ہب") || input.contains("repo") || input.contains("issue") || input.contains("ایشو")) {
            if (input.contains("issue") || input.contains("ایشو") || input.contains("ايشو") || input.contains("create") || input.contains("بنائیں")) {
                return ToolRegistry.getTool("github.createIssue")?.let {
                    it to mapOf("repo" to "niaz-ai-assistant", "title" to "Add Voice Recognition Multilingual Support")
                }
            }
            if (input.contains("delete") || input.contains("ڈیلیٹ")) {
                return ToolRegistry.getTool("github.deleteRepo")?.let {
                    it to mapOf("repo" to "deprecated-prototype")
                }
            }
            return ToolRegistry.getTool("github.listRepos")?.let { it to emptyMap() }
        }

        return null
    }

    private fun buildSummary(tool: ToolDefinition, params: Map<String, String>, lang: Language): String {
        return when (tool.id) {
            "gmail.send" -> {
                val to = params["to"] ?: "Ahmed"
                val subj = params["subject"] ?: "Update"
                when (lang) {
                    Language.ENGLISH -> "Ready to send email to $to with subject \"$subj\". Send it?"
                    Language.URDU -> "احمد کو \"$subj\" عنوان کے ساتھ ای میل بھیجنے کے لیے تیار ہے۔ کیا بھیج دوں؟"
                    Language.SINDHI -> "احمد کي \"$subj\" عنوان سان اي ميل موڪلڻ لاءِ تيار. ڇا موڪليان؟"
                }
            }
            "whatsapp.send" -> {
                val to = params["recipient"] ?: "Ahmed"
                val msg = params["message"] ?: ""
                "Recipient: $to\nMessage: \"$msg\"\nService: Official WhatsApp Business API"
            }
            "calendar.create" -> {
                val title = params["title"] ?: "Meeting"
                val time = params["time"] ?: "Tomorrow"
                "Event: $title\nTime: $time\nService: Google Calendar"
            }
            else -> "Authorizing ${tool.name} with parameters: $params"
        }
    }

    private fun buildSystemPrompt(lang: Language): String {
        val langInstruction = when (lang) {
            Language.ENGLISH -> "Respond in clear, professional, concise English."
            Language.URDU -> "اردو زبان میں شائستہ، پیشہ ورانہ اور مددگار جواب دیں۔"
            Language.SINDHI -> "سنڌي ٻوليءَ ۾ باوقار، پيشيوراڻو ۽ مددگار جواب ڏيو."
        }
        return """
            You are NIAZ AI ASSISTANT, the dedicated personal AI agent of Niaz Ahmed.
            Tagline: "Your Personal AI Assistant — Work Smarter, Automatically."
            Tone: Action-oriented, executive, professional, respectful, accurate.
            $langInstruction
            You never hallucinate fake external actions. If tools are requested, explain exact steps or confirm authorization.
        """.trimIndent()
    }

    private fun generateFallbackChat(userInput: String, lang: Language): String {
        return when (lang) {
            Language.ENGLISH -> "I understand your request regarding \"$userInput\". As your personal assistant, I'm ready to help you plan or execute this. Would you like me to connect to a specific service or check your current tasks?"
            Language.URDU -> "میں آپ کی ہدایت \"$userInput\" سمجھ گیا ہوں۔ آپ کا ذاتی اے آئی اسسٹنٹ ہونے کے ناطے، میں اس پر عمل درآمد کرنے کے لیے تیار ہوں۔"
            Language.SINDHI -> "مان توهان جي هدايت \"$userInput\" سمجهي ويس. توهان جو ذاتي اي آءِ اسسٽنٽ طور، مان ان تي ڪم ڪرڻ لاءِ تيار آهيان."
        }
    }
}

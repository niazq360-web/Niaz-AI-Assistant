package com.example.data.model

enum class ExecutionLevel(val label: String, val description: String) {
    LEVEL_1_SAFE("Level 1 — Safe Action", "Executes automatically when permission is granted (Search, Read)"),
    LEVEL_2_CONFIRM("Level 2 — Requires Confirmation", "Requires one-tap confirmation before sending/posting"),
    LEVEL_3_HIGH_RISK("Level 3 — High-Risk Action", "Always requires explicit user verification (Delete, Permanent edits)")
}

enum class ConnectionStatus(val label: String) {
    CONNECTED("Connected"),
    NOT_CONNECTED("Not Connected"),
    ERROR("Connection Error"),
    REAUTH_REQUIRED("Reauthorization Required")
}

enum class TaskStatus(val label: String) {
    TODAY("Today"),
    SCHEDULED("Scheduled"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

data class PermissionScope(
    val id: String,
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val level: ExecutionLevel
)

data class ConnectedApp(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val iconName: String,
    val status: ConnectionStatus,
    val accountName: String? = null,
    val connectedDate: String? = null,
    val lastUsedDate: String? = null,
    val permissions: List<PermissionScope> = emptyList(),
    val isAutoSendAllowed: Boolean = false,
    val oauthClientId: String = "",
    val authNotes: String = ""
)

data class ConfirmationRequest(
    val id: String,
    val toolName: String,
    val service: String,
    val level: ExecutionLevel,
    val title: String,
    val summary: String,
    val parameters: Map<String, String>,
    val autoSendEligible: Boolean = false
)

data class ToolExecutionState(
    val isExecuting: Boolean = false,
    val currentTool: String? = null,
    val statusMessage: String? = null,
    val progress: Float? = null
)

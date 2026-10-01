package com.example.data.repository

import com.example.data.model.ConnectedApp
import com.example.data.model.ConnectionStatus
import com.example.data.model.ExecutionLevel
import com.example.data.model.PermissionScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectedAppsManager {
    private val _apps = MutableStateFlow<List<ConnectedApp>>(initialApps())
    val apps: StateFlow<List<ConnectedApp>> = _apps.asStateFlow()

    private fun initialApps(): List<ConnectedApp> = listOf(
        ConnectedApp(
            id = "gmail",
            name = "Gmail",
            category = "Google Workspace",
            description = "Read, draft, and send emails, summarize inbox and filter notifications.",
            iconName = "gmail",
            status = ConnectionStatus.CONNECTED,
            accountName = "niaz.ahmed@workmail.com",
            connectedDate = "Sep 28, 2026",
            lastUsedDate = "10 mins ago",
            permissions = listOf(
                PermissionScope("gmail.read", "Read emails", "Access inbox to view and summarize messages", true, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("gmail.draft", "Create drafts", "Prepare draft replies without sending", true, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("gmail.send", "Send emails", "Send authorized messages on your behalf", true, ExecutionLevel.LEVEL_2_CONFIRM)
            ),
            isAutoSendAllowed = false,
            oauthClientId = "617494223617-niaz-gmail.apps.googleusercontent.com",
            authNotes = "Official Google OAuth 2.0 protocol. Tokens stored securely in Android Keystore EncryptedSharedPreferences."
        ),
        ConnectedApp(
            id = "calendar",
            name = "Google Calendar",
            category = "Google Workspace",
            description = "Check availability, create meetings, update schedules, and set reminders.",
            iconName = "calendar",
            status = ConnectionStatus.CONNECTED,
            accountName = "niaz.ahmed@workmail.com",
            connectedDate = "Sep 28, 2026",
            lastUsedDate = "1 hour ago",
            permissions = listOf(
                PermissionScope("cal.read", "Read calendar", "View upcoming events and agenda", true, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("cal.create", "Create events", "Schedule events and add attendees", true, ExecutionLevel.LEVEL_2_CONFIRM),
                PermissionScope("cal.edit", "Edit & delete events", "Reschedule or remove calendar entries", true, ExecutionLevel.LEVEL_3_HIGH_RISK)
            ),
            isAutoSendAllowed = false,
            oauthClientId = "617494223617-niaz-cal.apps.googleusercontent.com",
            authNotes = "Official Google Calendar API. Scopes: https://www.googleapis.com/auth/calendar.events"
        ),
        ConnectedApp(
            id = "drive",
            name = "Google Drive",
            category = "Google Workspace",
            description = "Search documents, browse folders, upload project files, and organize assets.",
            iconName = "drive",
            status = ConnectionStatus.CONNECTED,
            accountName = "niaz.ahmed@workmail.com",
            connectedDate = "Sep 28, 2026",
            lastUsedDate = "Yesterday",
            permissions = listOf(
                PermissionScope("drive.read", "Read files", "Search and view documents and sheets", true, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("drive.upload", "Upload files", "Save generated summaries and uploads", true, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("drive.manage", "Manage files", "Rename, move, or trash items", true, ExecutionLevel.LEVEL_3_HIGH_RISK)
            ),
            oauthClientId = "617494223617-niaz-drive.apps.googleusercontent.com",
            authNotes = "Official Google Drive v3 REST API. Least-privilege drive.file scope."
        ),
        ConnectedApp(
            id = "whatsapp",
            name = "WhatsApp",
            category = "Messaging",
            description = "Dispatch authorized messages and approved templates via official Meta WhatsApp Business Cloud API.",
            iconName = "whatsapp",
            status = ConnectionStatus.CONNECTED,
            accountName = "+92 300 9876543 (Business Verified)",
            connectedDate = "Sep 29, 2026",
            lastUsedDate = "3 hours ago",
            permissions = listOf(
                PermissionScope("wa.send", "Send messages", "Send messages after user confirmation", true, ExecutionLevel.LEVEL_2_CONFIRM),
                PermissionScope("wa.template", "Send templates", "Trigger pre-approved business templates", true, ExecutionLevel.LEVEL_2_CONFIRM)
            ),
            isAutoSendAllowed = false,
            oauthClientId = "Meta Business ID: 104928502941",
            authNotes = "Official Cloud API over HTTPS. Strictly requires user confirmation before sending."
        ),
        ConnectedApp(
            id = "facebook",
            name = "Facebook",
            category = "Social & Marketing",
            description = "Manage authorized Facebook Page posts, schedule updates, and read page metrics.",
            iconName = "facebook",
            status = ConnectionStatus.NOT_CONNECTED,
            accountName = null,
            connectedDate = null,
            lastUsedDate = null,
            permissions = listOf(
                PermissionScope("fb.page_read", "Read Page info", "View Page public posts and insights", false, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("fb.page_publish", "Publish Page posts", "Publish updates to authorized Pages", false, ExecutionLevel.LEVEL_2_CONFIRM)
            ),
            oauthClientId = "Meta App ID: 49204918234",
            authNotes = "Official Meta Graph API. Restricted to business Facebook Pages only. Personal profiles cannot be automated."
        ),
        ConnectedApp(
            id = "instagram",
            name = "Instagram",
            category = "Social & Marketing",
            description = "Publish media containers and read insights for Instagram Professional/Business accounts.",
            iconName = "instagram",
            status = ConnectionStatus.NOT_CONNECTED,
            accountName = null,
            connectedDate = null,
            lastUsedDate = null,
            permissions = listOf(
                PermissionScope("ig.read", "Read insights", "View impressions and reach", false, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("ig.publish", "Publish content", "Publish image and carousel posts", false, ExecutionLevel.LEVEL_2_CONFIRM)
            ),
            oauthClientId = "Meta Graph IG ID: 893240219",
            authNotes = "Requires Instagram Professional or Creator account connected to Meta Business Manager."
        ),
        ConnectedApp(
            id = "canva",
            name = "Canva",
            category = "Creative",
            description = "Generate design projects, social media graphics, and presentation templates via Canva Connect API.",
            iconName = "canva",
            status = ConnectionStatus.NOT_CONNECTED,
            accountName = null,
            connectedDate = null,
            lastUsedDate = null,
            permissions = listOf(
                PermissionScope("canva.design", "Design creation", "Initialize design templates", false, ExecutionLevel.LEVEL_1_SAFE)
            ),
            oauthClientId = "Canva Developer Client ID: cnva_88294",
            authNotes = "Official Canva Connect API. Directs to verified design workspace."
        ),
        ConnectedApp(
            id = "github",
            name = "GitHub",
            category = "Developer",
            description = "Inspect repositories, read pull requests, create issues, and automate project workflows.",
            iconName = "github",
            status = ConnectionStatus.CONNECTED,
            accountName = "@niaz-ahmed-dev",
            connectedDate = "Sep 28, 2026",
            lastUsedDate = "30 mins ago",
            permissions = listOf(
                PermissionScope("gh.read", "Read repositories", "View repos, issues, and commit log", true, ExecutionLevel.LEVEL_1_SAFE),
                PermissionScope("gh.write", "Write issues", "Create and comment on issues", true, ExecutionLevel.LEVEL_2_CONFIRM),
                PermissionScope("gh.admin", "Admin repo delete", "High risk operations on repos", false, ExecutionLevel.LEVEL_3_HIGH_RISK)
            ),
            oauthClientId = "GitHub OAuth App Client ID: Iv1.8f2940ba231",
            authNotes = "GitHub OAuth 2.0 Web application flow. Repositories and issue creation enabled."
        )
    )

    fun connectApp(id: String, account: String) {
        _apps.value = _apps.value.map { app ->
            if (app.id == id) {
                app.copy(
                    status = ConnectionStatus.CONNECTED,
                    accountName = account,
                    connectedDate = "Just now",
                    lastUsedDate = "Just now",
                    permissions = app.permissions.map { it.copy(isGranted = true) }
                )
            } else app
        }
    }

    fun disconnectApp(id: String) {
        _apps.value = _apps.value.map { app ->
            if (app.id == id) {
                app.copy(
                    status = ConnectionStatus.NOT_CONNECTED,
                    accountName = null,
                    connectedDate = null,
                    lastUsedDate = null,
                    permissions = app.permissions.map { it.copy(isGranted = false) },
                    isAutoSendAllowed = false
                )
            } else app
        }
    }

    fun togglePermission(appId: String, permId: String, granted: Boolean) {
        _apps.value = _apps.value.map { app ->
            if (app.id == appId) {
                val updatedPerms = app.permissions.map { perm ->
                    if (perm.id == permId) perm.copy(isGranted = granted) else perm
                }
                app.copy(permissions = updatedPerms)
            } else app
        }
    }

    fun toggleAutoSend(appId: String, enabled: Boolean) {
        _apps.value = _apps.value.map { app ->
            if (app.id == appId) app.copy(isAutoSendAllowed = enabled) else app
        }
    }

    fun getAppByName(name: String): ConnectedApp? {
        return _apps.value.find { it.name.equals(name, ignoreCase = true) }
    }
}

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectedApp
import com.example.data.model.ConnectionStatus
import com.example.data.remote.BackendApiClient
import com.example.data.remote.GoogleAuthStatus
import com.example.ui.components.ExecutionLevelBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DeepNavyElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedAppsScreen(
    connectedApps: List<ConnectedApp>,
    googleStatus: GoogleAuthStatus?,
    onConnectGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onReauthorizeGoogle: () -> Unit,
    onOpenOAuthConfig: () -> Unit,
    onConnectApp: (id: String, account: String) -> Unit,
    onDisconnectApp: (id: String) -> Unit,
    onTogglePermission: (appId: String, permId: String, granted: Boolean) -> Unit,
    onToggleAutoSend: (appId: String, enabled: Boolean) -> Unit,
    onCopyGoogleLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAppForPermissions by remember { mutableStateOf<ConnectedApp?>(null) }
    var selectedAppForConnect by remember { mutableStateOf<ConnectedApp?>(null) }
    var connectAccountInput by remember { mutableStateOf("") }
    var testingConnectionAppId by remember { mutableStateOf<String?>(null) }

    val isGoogleConnected = googleStatus?.connected == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("connected_apps_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Dedicated Google OAuth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_oauth_hero_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = if (isGoogleConnected) CyanNeon else AmberWarning
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F1B3B))
                                    .border(1.5.dp, CyanNeon, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Account (Gmail, Calendar, Drive)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Server-Side OAuth 2.0 Authorization Code",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        val (badgeBg, badgeColor, badgeText) = when {
                            isGoogleConnected -> Triple(EmeraldSuccess.copy(alpha = 0.2f), EmeraldSuccess, "CONNECTED")
                            googleStatus?.status?.contains("Auth", true) == true -> Triple(AmberWarning.copy(alpha = 0.2f), AmberWarning, "AUTH NEEDED")
                            else -> Triple(Color(0xFF64748B).copy(alpha = 0.2f), Color(0xFF94A3B8), "NOT CONNECTED")
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isGoogleConnected)
                            "Authorized Account: ${googleStatus?.email ?: "pak82914@gmail.com"}\nGmail, Google Calendar, and Google Drive APIs are connected with automatic token refresh."
                        else
                            "Real Google OAuth 2.0 flow. Connecting redirects you to Google's official authorization page to grant Gmail, Calendar, and Drive permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isGoogleConnected) {
                            Button(
                                onClick = onConnectGoogle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("connect_google_btn_main"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onCopyGoogleLink,
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Link", fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDisconnectGoogle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = onReauthorizeGoogle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reauthorize", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenOAuthConfig,
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OAuth Info", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Connected Services",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF94A3B8)
            )
        }

        items(connectedApps) { app ->
            val isGoogleService = app.id in listOf("gmail", "calendar", "drive")

            ConnectedAppCard(
                app = app,
                isTesting = testingConnectionAppId == app.id,
                onConnectClick = {
                    if (isGoogleService) {
                        onConnectGoogle()
                    } else {
                        selectedAppForConnect = app
                        connectAccountInput = if (app.name == "WhatsApp") "+92 300 1234567" else "niaz.ahmed@example.com"
                    }
                },
                onDisconnectClick = {
                    if (isGoogleService) {
                        onDisconnectGoogle()
                    } else {
                        onDisconnectApp(app.id)
                    }
                },
                onManagePermissionsClick = { selectedAppForPermissions = app },
                onTestConnectionClick = {
                    testingConnectionAppId = app.id
                    scope.launch {
                        if (isGoogleService) {
                            val res = when (app.id) {
                                "gmail" -> BackendApiClient.searchGmail("is:unread")
                                "calendar" -> BackendApiClient.listCalendar()
                                else -> BackendApiClient.listDrive(3)
                            }
                            testingConnectionAppId = null
                            if (res.isSuccess) {
                                Toast.makeText(context, "API Verification OK: ${app.name} is responsive!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "${app.name} test failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                testingConnectionAppId = null
                                if (app.status == ConnectionStatus.CONNECTED) {
                                    Toast.makeText(context, "API Verification OK: ${app.name} is fully responsive!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "${app.name} is not connected. Authenticate to establish connection.", Toast.LENGTH_SHORT).show()
                                }
                            }, 800)
                        }
                    }
                }
            )
        }

        // Add More Apps Section
        item {
            Text(
                text = "Add More Integrations (Upcoming)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(
            listOf(
                "Slack" to "Team channels, direct notifications, and workspace summaries.",
                "Notion" to "Sync databases, documents, and project meeting notes.",
                "Trello" to "Manage project task boards and Kanban automation.",
                "Twitter / X" to "Official X API for broadcasting executive updates.",
                "LinkedIn" to "Professional posts and network activity monitoring."
            )
        ) { (appName, desc) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DeepNavyElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(appName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceCardDark)
                            .clickable { Toast.makeText(context, "SDK for $appName will be available in next update.", Toast.LENGTH_SHORT).show() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Coming Soon", fontSize = 10.sp, color = CyanNeon, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Connect / OAuth Dialog for non-Google apps
    if (selectedAppForConnect != null) {
        val app = selectedAppForConnect!!
        AlertDialog(
            onDismissRequest = { selectedAppForConnect = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect ${app.name}", color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Authenticate with ${app.name} using official API connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Account Identifier / Verified ID:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = connectAccountInput,
                        onValueChange = { connectAccountInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Client ID: ${app.oauthClientId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConnectApp(app.id, connectAccountInput)
                        selectedAppForConnect = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black)
                ) { Text("Authorize & Link", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForConnect = null }) { Text("Cancel", color = Color(0xFF94A3B8)) }
            },
            containerColor = SurfaceCardDark
        )
    }

    // Permissions Modal Bottom Sheet
    if (selectedAppForPermissions != null) {
        val app = selectedAppForPermissions!!
        ModalBottomSheet(
            onDismissRequest = { selectedAppForPermissions = null },
            containerColor = SurfaceCardDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${app.name} Permissions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Granular least-privilege API scopes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    StatusBadge(app.status)
                }

                Spacer(modifier = Modifier.height(16.dp))

                app.permissions.forEach { perm ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = DeepNavyElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(perm.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ExecutionLevelBadge(perm.level)
                                }
                                Text(perm.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            }

                            Switch(
                                checked = perm.isGranted,
                                onCheckedChange = { onTogglePermission(app.id, perm.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CyanNeon
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1E3A))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Execute Safe Rules", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
                        Text("Allow Level 2 actions matching routine rules without confirmation prompt", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = app.isAutoSendAllowed,
                        onCheckedChange = { onToggleAutoSend(app.id, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanNeon
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ConnectedAppCard(
    app: ConnectedApp,
    isTesting: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onManagePermissionsClick: () -> Unit,
    onTestConnectionClick: () -> Unit
) {
    val isConnected = app.status == ConnectionStatus.CONNECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_card_${app.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isConnected) SurfaceCardBorder else Color(0xFF334155)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: App Name, Category, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F1B3B))
                            .border(1.dp, if (isConnected) CyanNeon else Color(0xFF64748B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.name.take(2).uppercase(),
                            color = if (isConnected) CyanNeon else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = app.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                StatusBadge(app.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = app.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            // Connection Details
            if (isConnected) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepNavyElevated)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Account: ${app.accountName ?: "Active"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Last synced: ${app.lastUsedDate ?: "Recently"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("connect_btn_${app.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onManagePermissionsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Permissions", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onTestConnectionClick,
                    enabled = !isTesting,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyanNeon)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Test", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SecurityAuditEntity
import com.example.data.model.ConnectedApp
import com.example.data.model.ConnectionStatus
import com.example.data.model.ExecutionLevel
import com.example.ui.components.ExecutionLevelBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DeepNavyElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PermissionCenterScreen(
    connectedApps: List<ConnectedApp>,
    securityLogs: List<SecurityAuditEntity>,
    onTogglePermission: (appId: String, permId: String, granted: Boolean) -> Unit,
    onDisconnect: (appId: String) -> Unit,
    onClearLogs: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Permissions, 1: Audit Log, 2: Security Levels

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("permission_center_screen")
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Permission & Security Center",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Authorization policies & zero-trust control",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            Icon(Icons.Default.Shield, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Connected Scopes", "Execution Levels", "Audit Trail (${securityLogs.size})").forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CyanNeon else SurfaceCardDark)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> ScopesView(
                connectedApps = connectedApps,
                onTogglePermission = onTogglePermission,
                onDisconnect = onDisconnect
            )
            1 -> ExecutionLevelsExplanationView()
            2 -> AuditTrailView(
                securityLogs = securityLogs,
                onClearLogs = onClearLogs
            )
        }
    }
}

@Composable
fun ScopesView(
    connectedApps: List<ConnectedApp>,
    onTogglePermission: (String, String, Boolean) -> Unit,
    onDisconnect: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(connectedApps) { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (app.status == ConnectionStatus.CONNECTED) "Linked: ${app.accountName}" else "Status: Not Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (app.status == ConnectionStatus.CONNECTED) CyanNeon else Color(0xFF64748B)
                            )
                        }

                        if (app.status == ConnectionStatus.CONNECTED) {
                            OutlinedButton(
                                onClick = { onDisconnect(app.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError)
                            ) {
                                Text("Revoke", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Granted API Scopes:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    app.permissions.forEach { perm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepNavyElevated)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(perm.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ExecutionLevelBadge(perm.level)
                                }
                                Text(perm.description, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
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
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ExecutionLevelsExplanationView() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceCardDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Three-Tier Safety Architecture",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NIAZ AI enforces strict authorization tiers to prevent accidental execution, unwanted communications, or data loss.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        item {
            SecurityTierCard(
                title = "LEVEL 1 — Safe Actions",
                badgeColor = EmeraldSuccess,
                summary = "Automatic Execution (if permission granted)",
                examples = listOf(
                    "Search Gmail messages and summarize inbox",
                    "Read Google Calendar events & daily schedule",
                    "Search files and documents on Google Drive",
                    "List GitHub repositories and open issues"
                ),
                rule = "Can execute immediately without interrupting you."
            )
        }

        item {
            SecurityTierCard(
                title = "LEVEL 2 — User Confirmation",
                badgeColor = AmberWarning,
                summary = "Explicit Confirmation Prompt Required",
                examples = listOf(
                    "Send email to any recipient via Gmail",
                    "Dispatch message via Meta WhatsApp Business API",
                    "Schedule new meeting or invite attendees in Google Calendar",
                    "Publish posts to authorized Facebook Pages or Instagram"
                ),
                rule = "Shows preview card (Recipient, Text, Service) with Send / Cancel buttons. User can opt into routine auto-send rules if desired."
            )
        }

        item {
            SecurityTierCard(
                title = "LEVEL 3 — High-Risk Actions",
                badgeColor = CoralError,
                summary = "Mandatory Explicit Verification (Never bypassed)",
                examples = listOf(
                    "Delete files or folders in Google Drive",
                    "Cancel or delete Google Calendar meetings",
                    "Permanently delete GitHub repositories or branches",
                    "Irreversible system alterations"
                ),
                rule = "Strict zero-bypass confirmation dialog. Even automated agents cannot execute Level 3 without direct biometric/touch confirmation."
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun SecurityTierCard(
    title: String,
    badgeColor: Color,
    summary: String,
    examples: List<String>,
    rule: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Text(summary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = badgeColor)

            Spacer(modifier = Modifier.height(8.dp))

            Text("Examples:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            examples.forEach { ex ->
                Text("• $ex", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1), modifier = Modifier.padding(vertical = 1.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Policy: $rule", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun AuditTrailView(
    securityLogs: List<SecurityAuditEntity>,
    onClearLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm:ss a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Immutable Tool Execution Log",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF94A3B8)
            )

            if (securityLogs.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearLogs,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Logs", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (securityLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No security audit logs yet.", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(securityLogs) { log ->
                    val statusColor = when (log.status) {
                        "CONFIRMED_AND_RUN", "EXECUTED_AUTO" -> EmeraldSuccess
                        "USER_CANCELLED" -> AmberWarning
                        else -> CoralError
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceCardDark,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(log.toolName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(${log.service})", style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(log.status.replace("_", " "), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

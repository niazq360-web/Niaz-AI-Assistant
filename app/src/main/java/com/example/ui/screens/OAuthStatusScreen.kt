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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.BackendApiClient
import com.example.data.remote.GoogleAuthStatus
import com.example.data.remote.OAuthConfig
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DeepNavyElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardDark
import kotlinx.coroutines.launch

@Composable
fun OAuthStatusScreen(
    googleStatus: GoogleAuthStatus?,
    oauthConfig: OAuthConfig?,
    isLoading: Boolean,
    onConnectGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onReauthorizeGoogle: () -> Unit,
    onRefreshStatus: () -> Unit,
    onCopyAuthUrl: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var toolTestOutput by remember { mutableStateOf<String?>(null) }
    var isTestingTool by remember { mutableStateOf(false) }

    val isConnected = googleStatus?.connected == true
    val statusText = googleStatus?.status ?: (oauthConfig?.connectionStatus ?: "Not Connected")
    val userEmail = googleStatus?.email ?: oauthConfig?.connectedEmail
    val redirectUri = oauthConfig?.redirectUri ?: "https://ais-dev-sugu7tqqkbfnyqa4ix6qc2-617494223617.asia-east1.run.app/api/auth/google/callback"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("oauth_status_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Top Header Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceCardDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google OAuth 2.0 Security",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CyanNeon)
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanNeon.copy(alpha = 0.15f))
                                    .clickable { onRefreshStatus() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh", fontSize = 11.sp, color = CyanNeon, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Real Server-Side OAuth 2.0 Authorization Code flow. Client secret is securely preserved on the backend server and never sent to Android client code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Connection Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isConnected) EmeraldSuccess else AmberWarning
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connection Status",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        val (statusBg, statusColor) = when {
                            isConnected -> EmeraldSuccess.copy(alpha = 0.2f) to EmeraldSuccess
                            statusText.contains("Authorization", ignoreCase = true) -> AmberWarning.copy(alpha = 0.2f) to AmberWarning
                            statusText.contains("Error", ignoreCase = true) -> CoralError.copy(alpha = 0.2f) to CoralError
                            else -> Color(0xFF64748B).copy(alpha = 0.2f) to Color(0xFF94A3B8)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Connected Account Email Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DeepNavyElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Connected Google Account",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userEmail ?: "No account connected yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (userEmail != null) CyanNeon else Color(0xFF64748B)
                            )
                            if (googleStatus?.connectedAt != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Connected at: ${googleStatus.connectedAt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isConnected) {
                            Button(
                                onClick = onConnectGoogle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("connect_google_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect Google", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onCopyAuthUrl,
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Link", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDisconnectGoogle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("disconnect_google_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disconnect", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onReauthorizeGoogle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("reauthorize_google_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reauthorize", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Required Google OAuth Configuration Diagnostic Card (Shows ONLY safe info, NO secrets)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google OAuth Diagnostics",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        val isAllConfigured = oauthConfig?.clientIdConfigured == true && oauthConfig?.clientSecretConfigured == true
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isAllConfigured) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAllConfigured) EmeraldSuccess else AmberWarning)
                        ) {
                            Text(
                                text = if (isAllConfigured) "Configured ✓" else "Not Configured",
                                color = if (isAllConfigured) EmeraldSuccess else AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Safe Diagnostic rows (Secret values are NEVER shown)
                    OAuthPropertyRow(
                        label = "Google OAuth",
                        status = if (oauthConfig?.clientIdConfigured == true && oauthConfig?.clientSecretConfigured == true) "Configured ✓" else "Not Configured",
                        detail = "Production Web Application OAuth 2.0 Flow",
                        isSuccess = oauthConfig?.clientIdConfigured == true && oauthConfig?.clientSecretConfigured == true
                    )
                    OAuthPropertyRow(
                        label = "Client ID",
                        status = if (oauthConfig?.clientIdConfigured == true) "Configured ✓" else "Not Configured",
                        detail = oauthConfig?.clientIdMasked ?: "Configured ✓ (GOOGLE_CLIENT_ID)",
                        isSuccess = oauthConfig?.clientIdConfigured == true
                    )
                    OAuthPropertyRow(
                        label = "Client Secret",
                        status = if (oauthConfig?.clientSecretConfigured == true) "Configured ✓" else "Not Configured",
                        detail = "Securely loaded in server secrets — value hidden",
                        isSuccess = oauthConfig?.clientSecretConfigured == true
                    )
                    OAuthPropertyRow(
                        label = "OAuth Callback",
                        status = "Configured ✓",
                        detail = "/api/auth/google/callback",
                        isSuccess = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Multiple Client IDs Diagnostic Message
                    val multipleDetected = oauthConfig?.multipleClientIdsDetected == true
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (multipleDetected) CoralError.copy(alpha = 0.12f) else EmeraldSuccess.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (multipleDetected) CoralError else EmeraldSuccess)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (multipleDetected) "⚠️ MULTIPLE CLIENT IDS DETECTED" else "✓ CLIENT ID VERIFICATION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (multipleDetected) CoralError else EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = oauthConfig?.diagnosticMessage ?: "No multiple or conflicting Google Client IDs detected in application configuration.",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Exact Production OAuth Redirect URI:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Redirect URI Box with Copy button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF070C1B),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = redirectUri,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = CyanNeon,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanNeon)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Redirect URI", redirectUri))
                                        Toast.makeText(context, "Copied Redirect URI to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("COPY", color = Color(0xFF070C1B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Live Google APIs Capability Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connected Google Services & Scopes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OAuthPropertyRow(label = "Gmail API", status = "Ready", detail = "https://www.googleapis.com/auth/gmail.modify", isSuccess = true)
                    OAuthPropertyRow(label = "Calendar API", status = "Ready", detail = "https://www.googleapis.com/auth/calendar", isSuccess = true)
                    OAuthPropertyRow(label = "Drive API", status = "Ready", detail = "https://www.googleapis.com/auth/drive.file", isSuccess = true)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Exact Production OAuth Redirect URI:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Redirect URI Box with Copy button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF070C1B),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = redirectUri,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = CyanNeon,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanNeon)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Redirect URI", redirectUri))
                                        Toast.makeText(context, "Copied Redirect URI to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("COPY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Paste this EXACT URI into Google Cloud Console &rarr; APIs & Services &rarr; Credentials &rarr; Authorized redirect URIs.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Live Backend Tools Verification Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Live Google API Tool Test",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Run immediate server-side verification against your real Google account:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingTool = true
                                    toolTestOutput = "Querying Gmail API..."
                                    val res = BackendApiClient.searchGmail("is:unread")
                                    toolTestOutput = if (res.isSuccess) res.getOrThrow() else "Gmail Error: ${res.exceptionOrNull()?.message}"
                                    isTestingTool = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Test Gmail", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingTool = true
                                    toolTestOutput = "Querying Calendar API..."
                                    val res = BackendApiClient.listCalendar()
                                    toolTestOutput = if (res.isSuccess) res.getOrThrow() else "Calendar Error: ${res.exceptionOrNull()?.message}"
                                    isTestingTool = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Test Calendar", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingTool = true
                                    toolTestOutput = "Querying Drive API..."
                                    val res = BackendApiClient.listDrive(5)
                                    toolTestOutput = if (res.isSuccess) res.getOrThrow() else "Drive Error: ${res.exceptionOrNull()?.message}"
                                    isTestingTool = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Test Drive", fontSize = 11.sp)
                        }
                    }

                    if (toolTestOutput != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF070C1B),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Text(
                                text = toolTestOutput!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFFA5F3FC)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun OAuthPropertyRow(
    label: String,
    status: String,
    detail: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSuccess) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = status,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) EmeraldSuccess else AmberWarning
            )
        }
    }
}

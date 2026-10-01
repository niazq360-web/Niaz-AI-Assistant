package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.Language
import com.example.ui.Screen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantTopAppBar(
    currentScreen: Screen,
    currentLanguage: Language,
    isMuted: Boolean,
    onLanguageSelect: (Language) -> Unit,
    onToggleMute: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onOpenWelcome: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepNavy,
            titleContentColor = Color.White
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onNavigate(Screen.DASHBOARD) }
                    .testTag("app_branding_header")
            ) {
                // Futuristic Neural Brain 'N' icon badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F1B3B))
                        .border(1.5.dp, CyanNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        color = CyanNeon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NIAZ AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanNeon)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "AGENT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                    Text(
                        text = "Work Smarter, Automatically",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        },
        actions = {
            // Language selector button
            Box {
                IconButton(
                    onClick = { showLangMenu = true },
                    modifier = Modifier.testTag("language_toggle_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardDark)
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentLanguage.code.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false },
                    modifier = Modifier.background(SurfaceCardDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("English (Default)", color = Color.White) },
                        onClick = {
                            onLanguageSelect(Language.ENGLISH)
                            showLangMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Urdu (اردو)", color = CyanNeon) },
                        onClick = {
                            onLanguageSelect(Language.URDU)
                            showLangMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sindhi (سنڌي)", color = CyanNeon) },
                        onClick = {
                            onLanguageSelect(Language.SINDHI)
                            showLangMenu = false
                        }
                    )
                }
            }

            // Audio Mute/Unmute
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier.testTag("audio_mute_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color(0xFF94A3B8) else CyanNeon
                )
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("top_overflow_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceCardDark)
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon) },
                        text = { Text("Permission Center", color = Color.White) },
                        onClick = {
                            onNavigate(Screen.PERMISSIONS)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = CyanNeon) },
                        text = { Text("Memory & Profile", color = Color.White) },
                        onClick = {
                            onNavigate(Screen.MEMORY)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = CyanNeon) },
                        text = { Text("Settings", color = Color.White) },
                        onClick = {
                            onNavigate(Screen.SETTINGS)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanNeon) },
                        text = { Text("Welcome & Guide", color = Color.White) },
                        onClick = {
                            onOpenWelcome()
                            showMenu = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun AssistantBottomNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = DeepNavy,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onNavigate(Screen.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyanNeon,
                indicatorColor = CyanNeon,
                unselectedIconColor = Color(0xFF94A3B8),
                unselectedTextColor = Color(0xFF94A3B8)
            ),
            modifier = Modifier.testTag("nav_item_dashboard")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.CHAT,
            onClick = { onNavigate(Screen.CHAT) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Assistant") },
            label = { Text("Assistant", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyanNeon,
                indicatorColor = CyanNeon,
                unselectedIconColor = Color(0xFF94A3B8),
                unselectedTextColor = Color(0xFF94A3B8)
            ),
            modifier = Modifier.testTag("nav_item_chat")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.CONNECTED_APPS,
            onClick = { onNavigate(Screen.CONNECTED_APPS) },
            icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
            label = { Text("Connected", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyanNeon,
                indicatorColor = CyanNeon,
                unselectedIconColor = Color(0xFF94A3B8),
                unselectedTextColor = Color(0xFF94A3B8)
            ),
            modifier = Modifier.testTag("nav_item_apps")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.TASKS,
            onClick = { onNavigate(Screen.TASKS) },
            icon = { Icon(Icons.Default.Checklist, contentDescription = "Tasks") },
            label = { Text("Tasks", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyanNeon,
                indicatorColor = CyanNeon,
                unselectedIconColor = Color(0xFF94A3B8),
                unselectedTextColor = Color(0xFF94A3B8)
            ),
            modifier = Modifier.testTag("nav_item_tasks")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.AUTOMATIONS,
            onClick = { onNavigate(Screen.AUTOMATIONS) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Automations") },
            label = { Text("Automations", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyanNeon,
                indicatorColor = CyanNeon,
                unselectedIconColor = Color(0xFF94A3B8),
                unselectedTextColor = Color(0xFF94A3B8)
            ),
            modifier = Modifier.testTag("nav_item_automations")
        )
    }
}

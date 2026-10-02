package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AssistantBottomNavigationBar
import com.example.ui.components.AssistantTopAppBar
import com.example.ui.components.DemoModeBanner
import com.example.ui.components.WelcomeOnboardingDialog
import com.example.ui.screens.AutomationsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConnectedAppsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OAuthStatusScreen
import com.example.ui.screens.PermissionCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskCenterScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentScreen by viewModel.currentScreen.collectAsState()
    val language by viewModel.language.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()

    // Google Server OAuth State
    val googleStatus by viewModel.googleStatus.collectAsState()
    val oauthConfig by viewModel.oauthConfig.collectAsState()
    val isOAuthLoading by viewModel.isOAuthLoading.collectAsState()

    // Active state
    val conversations by viewModel.conversations.collectAsState()
    val activeConvId by viewModel.activeConversationId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val connectedApps by viewModel.connectedApps.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val automations by viewModel.automations.collectAsState()
    val securityLogs by viewModel.securityLogs.collectAsState()
    val memoryItems by viewModel.memoryItems.collectAsState()
    val toolExecutionState by viewModel.toolExecutionState.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val speechError by viewModel.speechError.collectAsState()

    // Auto sync on app resume (e.g. after returning from Google OAuth browser consent)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncGoogleOAuthStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher for voice mic
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    fun requestVoiceInput() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Back handling for navigation stack
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        if (!viewModel.handleBack()) {
            viewModel.navigateTo(Screen.DASHBOARD)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    AssistantTopAppBar(
                        currentScreen = currentScreen,
                        currentLanguage = language,
                        isMuted = isMuted,
                        onLanguageSelect = { viewModel.setLanguage(it) },
                        onToggleMute = { viewModel.toggleMute() },
                        onNavigate = { viewModel.navigateTo(it) },
                        onOpenWelcome = { viewModel.openOnboarding() }
                    )
                    DemoModeBanner(
                        isDemoMode = isDemoMode,
                        onDisable = { viewModel.setDemoMode(false) }
                    )
                }
            },
            bottomBar = {
                AssistantBottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        language = language,
                        connectedApps = connectedApps,
                        tasks = tasks,
                        automations = automations,
                        securityLogs = securityLogs,
                        onNavigate = { viewModel.navigateTo(it) },
                        onExecuteQuickAction = { prompt ->
                            viewModel.navigateTo(Screen.CHAT)
                            viewModel.sendMessage(prompt)
                        }
                    )

                    Screen.CHAT -> ChatScreen(
                        language = language,
                        conversations = conversations,
                        activeConversationId = activeConvId,
                        messages = messages,
                        toolExecutionState = toolExecutionState,
                        pendingConfirmation = pendingConfirmation,
                        isListening = isListening,
                        speechError = speechError,
                        onSendMessage = { text, attachment -> viewModel.sendMessage(text, attachment) },
                        onConfirmAction = { confirmed, autoSend -> viewModel.confirmPendingAction(confirmed, autoSend) },
                        onStartListening = { requestVoiceInput() },
                        onStopListening = { viewModel.stopListening() },
                        onNewConversation = { viewModel.createNewConversation() },
                        onSelectConversation = { viewModel.selectConversation(it) },
                        onRenameConversation = { id, title -> viewModel.renameConversation(id, title) },
                        onDeleteConversation = { viewModel.deleteConversation(it) }
                    )

                    Screen.CONNECTED_APPS -> ConnectedAppsScreen(
                        connectedApps = connectedApps,
                        googleStatus = googleStatus,
                        onConnectGoogle = { viewModel.connectGoogle(context) },
                        onDisconnectGoogle = { viewModel.disconnectGoogle() },
                        onReauthorizeGoogle = { viewModel.reauthorizeGoogle(context) },
                        onOpenOAuthConfig = { viewModel.navigateTo(Screen.OAUTH_CONFIG) },
                        onConnectApp = { id, account -> viewModel.appsManager.connectApp(id, account) },
                        onDisconnectApp = { id -> viewModel.appsManager.disconnectApp(id) },
                        onTogglePermission = { appId, permId, granted -> viewModel.appsManager.togglePermission(appId, permId, granted) },
                        onToggleAutoSend = { appId, enabled -> viewModel.appsManager.toggleAutoSend(appId, enabled) },
                        onCopyGoogleLink = { viewModel.copyGoogleAuthUrl(context) }
                    )

                    Screen.OAUTH_CONFIG -> OAuthStatusScreen(
                        googleStatus = googleStatus,
                        oauthConfig = oauthConfig,
                        isLoading = isOAuthLoading,
                        onConnectGoogle = { viewModel.connectGoogle(context) },
                        onDisconnectGoogle = { viewModel.disconnectGoogle() },
                        onReauthorizeGoogle = { viewModel.reauthorizeGoogle(context) },
                        onRefreshStatus = { viewModel.syncGoogleOAuthStatus() },
                        onCopyAuthUrl = { viewModel.copyGoogleAuthUrl(context) }
                    )

                    Screen.TASKS -> TaskCenterScreen(
                        tasks = tasks,
                        onRetryTask = { viewModel.retryTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onClearCompleted = { viewModel.clearCompletedTasks() }
                    )

                    Screen.AUTOMATIONS -> AutomationsScreen(
                        automations = automations,
                        onCreateAutomation = { name, prompt, sched, serv -> viewModel.createAutomation(name, prompt, sched, serv) },
                        onToggleAutomation = { id, active -> viewModel.toggleAutomation(id, active) },
                        onDeleteAutomation = { viewModel.deleteAutomation(it) },
                        onRunNow = { viewModel.runAutomationNow(it) }
                    )

                    Screen.PERMISSIONS -> PermissionCenterScreen(
                        connectedApps = connectedApps,
                        securityLogs = securityLogs,
                        onTogglePermission = { appId, permId, granted -> viewModel.appsManager.togglePermission(appId, permId, granted) },
                        onDisconnect = { viewModel.appsManager.disconnectApp(it) },
                        onClearLogs = { viewModel.clearSecurityLogs() }
                    )

                    Screen.MEMORY -> MemoryScreen(
                        memoryItems = memoryItems,
                        onAddMemory = { cat, k, v -> viewModel.addMemory(cat, k, v) },
                        onDeleteMemory = { viewModel.deleteMemory(it) },
                        onClearMemory = { viewModel.clearMemory() }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        currentLanguage = language,
                        isDemoMode = isDemoMode,
                        isDarkTheme = isDarkTheme,
                        isMuted = isMuted,
                        onLanguageSelect = { viewModel.setLanguage(it) },
                        onToggleDemoMode = { viewModel.setDemoMode(it) },
                        onToggleTheme = { viewModel.toggleDarkTheme() },
                        onToggleMute = { viewModel.toggleMute() },
                        onNavigate = { viewModel.navigateTo(it) },
                        onOpenWelcome = { viewModel.openOnboarding() }
                    )
                }
            }
        }
    }

    if (showOnboarding) {
        WelcomeOnboardingDialog(
            currentLanguage = language,
            onSelectLanguage = { viewModel.setLanguage(it) },
            onStartDemo = {
                viewModel.setDemoMode(true)
                viewModel.navigateTo(Screen.CHAT)
            },
            onConnectApps = { viewModel.navigateTo(Screen.CONNECTED_APPS) },
            onDismiss = { viewModel.dismissOnboarding() }
        )
    }
}

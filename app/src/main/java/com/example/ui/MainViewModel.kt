package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentOrchestrator
import com.example.agent.IntegrationService
import com.example.agent.OrchestrationDecision
import com.example.agent.ToolRegistry
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AutomationEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.SecurityAuditEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.model.ConfirmationRequest
import com.example.data.model.ConnectedApp
import com.example.data.model.Language
import com.example.data.model.ToolExecutionState
import com.example.data.repository.AssistantRepository
import com.example.data.repository.ConnectedAppsManager
import com.example.util.SpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    DASHBOARD,
    CHAT,
    CONNECTED_APPS,
    TASKS,
    AUTOMATIONS,
    PERMISSIONS,
    MEMORY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AssistantRepository(db)
    val appsManager = ConnectedAppsManager()
    private val integrationService = IntegrationService()
    private val orchestrator = AgentOrchestrator(integrationService)

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _navigationStack = MutableStateFlow<List<Screen>>(listOf(Screen.DASHBOARD))

    // Language & Preferences
    private val _language = MutableStateFlow(Language.ENGLISH)
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    // Tool execution & Confirmation state
    private val _toolExecutionState = MutableStateFlow(ToolExecutionState())
    val toolExecutionState: StateFlow<ToolExecutionState> = _toolExecutionState.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()

    // Speech Recognition & TTS
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var speechHelper: SpeechHelper? = null

    // Conversations & Messages
    val conversations: StateFlow<List<ConversationEntity>> = repository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    // Connected Apps
    val connectedApps: StateFlow<List<ConnectedApp>> = appsManager.apps

    // Tasks & Automations
    val tasks: StateFlow<List<TaskEntity>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automations: StateFlow<List<AutomationEntity>> = repository.getAllAutomations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityLogs: StateFlow<List<SecurityAuditEntity>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryItems: StateFlow<List<MemoryEntity>> = repository.getAllMemory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        speechHelper = SpeechHelper(
            context = application,
            onSpeechResult = { text ->
                _isListening.value = false
                sendMessage(text)
            },
            onError = { err ->
                _isListening.value = false
                _speechError.value = err
            },
            onListeningStateChanged = { listening ->
                _isListening.value = listening
            }
        )

        // Observe conversations and load first active
        viewModelScope.launch {
            conversations.collect { convList ->
                if (_activeConversationId.value == null && convList.isNotEmpty()) {
                    selectConversation(convList.first().id)
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _navigationStack.value = _navigationStack.value + screen
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        val stack = _navigationStack.value
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            _navigationStack.value = newStack
            _currentScreen.value = newStack.last()
            return true
        }
        return false
    }

    fun setLanguage(lang: Language) {
        _language.value = lang
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun dismissOnboarding() {
        _showOnboarding.value = false
    }

    fun openOnboarding() {
        _showOnboarding.value = true
    }

    fun selectConversation(id: Long) {
        _activeConversationId.value = id
        viewModelScope.launch {
            repository.getMessages(id).collect {
                _messages.value = it
            }
        }
    }

    fun createNewConversation(initialTitle: String = "New Conversation") {
        viewModelScope.launch {
            val newId = repository.createConversation(initialTitle)
            selectConversation(newId)
            repository.addMessage(
                MessageEntity(
                    conversationId = newId,
                    role = "assistant",
                    text = "Hello Niaz Ahmed! How can I assist you with your tasks today?"
                )
            )
        }
    }

    fun renameConversation(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateConversation(ConversationEntity(id = id, title = newTitle))
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            val remaining = repository.getAllConversations().first()
            if (remaining.isNotEmpty()) {
                selectConversation(remaining.first().id)
            } else {
                createNewConversation()
            }
        }
    }

    fun startListening() {
        _speechError.value = null
        speechHelper?.startListening(_language.value)
    }

    fun stopListening() {
        speechHelper?.stopListening()
    }

    fun toggleMute() {
        val muted = !_isMuted.value
        _isMuted.value = muted
        speechHelper?.isMuted = muted
    }

    fun sendMessage(userText: String, attachmentInfo: String? = null) {
        if (userText.isBlank()) return
        val convId = _activeConversationId.value ?: return

        val fullText = if (attachmentInfo != null) "$userText\n[Attached File: $attachmentInfo]" else userText

        viewModelScope.launch {
            // 1. Save user message
            repository.addMessage(
                MessageEntity(
                    conversationId = convId,
                    role = "user",
                    text = fullText
                )
            )

            // Update conversation title if first user prompt
            val existingMsgs = _messages.value
            if (existingMsgs.size <= 1) {
                val preview = if (userText.length > 28) userText.take(28) + "..." else userText
                repository.updateConversation(ConversationEntity(id = convId, title = preview))
            }

            // 2. Set Tool Execution Live Status
            _toolExecutionState.value = ToolExecutionState(
                isExecuting = true,
                statusMessage = "Analyzing instruction..."
            )

            // 3. Process via Agent Orchestrator
            val history = existingMsgs.map { it.role to it.text }
            val decision = orchestrator.processUserInstruction(
                userInput = userText,
                connectedApps = connectedApps.value,
                isDemoMode = _isDemoMode.value,
                userLanguage = _language.value,
                history = history
            )

            when (decision) {
                is OrchestrationDecision.DirectResponse -> {
                    _toolExecutionState.value = ToolExecutionState(isExecuting = false)
                    repository.addMessage(
                        MessageEntity(
                            conversationId = convId,
                            role = "assistant",
                            text = decision.message
                        )
                    )
                    speechHelper?.speak(decision.message, _language.value)
                }

                is OrchestrationDecision.ConfirmationNeeded -> {
                    _toolExecutionState.value = ToolExecutionState(
                        isExecuting = false,
                        statusMessage = "Waiting for user confirmation..."
                    )
                    val confReq = ConfirmationRequest(
                        id = System.currentTimeMillis().toString(),
                        toolName = decision.tool.name,
                        service = decision.service,
                        level = decision.level,
                        title = decision.title,
                        summary = decision.promptDescription,
                        parameters = decision.parameters,
                        autoSendEligible = decision.autoSendEligible
                    )
                    _pendingConfirmation.value = confReq

                    // Post confirmation prompt message in chat
                    val confirmMsg = "I prepared this action on **${decision.service}**.\n\n${decision.promptDescription}\n\nPlease confirm below to proceed."
                    repository.addMessage(
                        MessageEntity(
                            conversationId = convId,
                            role = "assistant",
                            text = confirmMsg,
                            toolName = decision.tool.name,
                            toolService = decision.service,
                            toolStatus = "CONFIRMATION_REQUIRED"
                        )
                    )
                }

                is OrchestrationDecision.ExecuteImmediately -> {
                    executeToolAction(decision.tool, decision.parameters, convId)
                }
            }
        }
    }

    fun confirmPendingAction(confirmed: Boolean, enableAutoSend: Boolean = false) {
        val conf = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        val convId = _activeConversationId.value ?: return

        viewModelScope.launch {
            val tool = ToolRegistry.tools.find { it.name.equals(conf.toolName, ignoreCase = true) || it.service.equals(conf.service, ignoreCase = true) }

            if (!confirmed) {
                repository.logSecurityEvent(
                    toolName = conf.toolName,
                    service = conf.service,
                    level = conf.level.name,
                    status = "USER_CANCELLED",
                    details = "User cancelled execution of ${conf.toolName}"
                )
                repository.addMessage(
                    MessageEntity(
                        conversationId = convId,
                        role = "assistant",
                        text = "Action cancelled. I will not proceed with ${conf.toolName}.",
                        toolStatus = "CANCELLED"
                    )
                )
                _toolExecutionState.value = ToolExecutionState(isExecuting = false)
                return@launch
            }

            if (enableAutoSend && conf.autoSendEligible) {
                val app = appsManager.getAppByName(conf.service)
                if (app != null) {
                    appsManager.toggleAutoSend(app.id, true)
                }
            }

            if (tool != null) {
                executeToolAction(tool, conf.parameters, convId)
            } else {
                repository.addMessage(
                    MessageEntity(
                        conversationId = convId,
                        role = "assistant",
                        text = "Tool definition could not be resolved."
                    )
                )
            }
        }
    }

    private suspend fun executeToolAction(
        tool: com.example.agent.ToolDefinition,
        parameters: Map<String, String>,
        convId: Long
    ) {
        _toolExecutionState.value = ToolExecutionState(
            isExecuting = true,
            currentTool = tool.name,
            statusMessage = "Connecting to ${tool.service} & executing ${tool.name}..."
        )

        val app = appsManager.getAppByName(tool.service)
        val result = integrationService.executeTool(
            tool = tool,
            parameters = parameters,
            appConnection = app,
            isDemoMode = _isDemoMode.value
        )

        _toolExecutionState.value = ToolExecutionState(isExecuting = false)

        // Log security audit
        repository.logSecurityEvent(
            toolName = tool.name,
            service = tool.service,
            level = tool.level.name,
            status = if (result.success) "CONFIRMED_AND_RUN" else "FAILED",
            details = result.resultSummary
        )

        // Add to Task Center
        repository.addTask(
            TaskEntity(
                title = tool.name,
                prompt = "${tool.name} on ${tool.service}",
                service = tool.service,
                status = if (result.success) "COMPLETED" else "FAILED",
                resultSummary = result.resultSummary,
                errorDetails = if (!result.success) result.displayMessage else null
            )
        )

        // Save assistant response message
        repository.addMessage(
            MessageEntity(
                conversationId = convId,
                role = "assistant",
                text = result.displayMessage,
                toolName = tool.name,
                toolService = tool.service,
                toolStatus = if (result.success) "COMPLETED" else "FAILED"
            )
        )

        speechHelper?.speak(result.displayMessage, _language.value)
    }

    // Task actions
    fun retryTask(task: TaskEntity) {
        sendMessage("Retry task: ${task.prompt}")
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
        }
    }

    // Automations
    fun createAutomation(name: String, prompt: String, schedule: String, service: String) {
        viewModelScope.launch {
            repository.addAutomation(
                AutomationEntity(
                    name = name,
                    taskPrompt = prompt,
                    schedule = schedule,
                    service = service,
                    isActive = true,
                    nextExecution = "Tomorrow, 09:00 AM",
                    permissionRequired = "Execute scheduled task"
                )
            )
        }
    }

    fun toggleAutomation(id: Long, active: Boolean) {
        viewModelScope.launch {
            repository.setAutomationActive(id, active)
        }
    }

    fun deleteAutomation(id: Long) {
        viewModelScope.launch {
            repository.deleteAutomation(id)
        }
    }

    fun runAutomationNow(auto: AutomationEntity) {
        sendMessage(auto.taskPrompt)
        viewModelScope.launch {
            repository.updateAutomation(auto.copy(lastExecution = "Just now"))
        }
    }

    // Memory
    fun addMemory(category: String, key: String, value: String) {
        viewModelScope.launch {
            repository.addMemory(category, key, value)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            repository.clearAllMemory()
        }
    }

    // Security logs
    fun clearSecurityLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper?.release()
    }
}

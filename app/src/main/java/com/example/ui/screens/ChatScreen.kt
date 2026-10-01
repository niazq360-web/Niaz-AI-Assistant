package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.model.ConfirmationRequest
import com.example.data.model.Language
import com.example.data.model.ToolExecutionState
import com.example.ui.components.ConfirmationActionCard
import com.example.ui.components.LiveToolExecutionBanner
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.DeepNavyElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    language: Language,
    conversations: List<ConversationEntity>,
    activeConversationId: Long?,
    messages: List<MessageEntity>,
    toolExecutionState: ToolExecutionState,
    pendingConfirmation: ConfirmationRequest?,
    isListening: Boolean,
    speechError: String?,
    onSendMessage: (text: String, attachmentInfo: String?) -> Unit,
    onConfirmAction: (confirmed: Boolean, enableAutoSend: Boolean) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onRenameConversation: (Long, String) -> Unit,
    onDeleteConversation: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var selectedAttachment by remember { mutableStateOf<String?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    // Dialogs
    var renameConvId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size, toolExecutionState.isExecuting) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DeepNavyElevated,
                modifier = Modifier.width(300.dp)
            ) {
                ConversationHistoryDrawer(
                    conversations = conversations,
                    activeId = activeConversationId,
                    onSelect = {
                        onSelectConversation(it)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        onNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onRename = { id, title ->
                        renameConvId = id
                        renameText = title
                    },
                    onDelete = { onDeleteConversation(it) }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chat_screen_main")
        ) {
            // Chat Subheader with Drawer Trigger & Active Chat title
            ChatSubheader(
                title = conversations.find { it.id == activeConversationId }?.title ?: "NIAZ AI Assistant",
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNewChat = onNewConversation
            )

            // Speech recognition error display
            if (speechError != null) {
                Surface(
                    color = CoralError.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = speechError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralError,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Messages List
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    EmptyChatPromptView(
                        language = language,
                        onPromptClick = { prompt -> onSendMessage(prompt, null) }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        items(messages) { msg ->
                            MessageBubble(
                                message = msg,
                                onCopy = {
                                    copyToClipboard(context, msg.text)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onRegenerate = {
                                    if (msg.role == "assistant") {
                                        onSendMessage("Regenerate previous answer", null)
                                    }
                                }
                            )
                        }

                        // Live tool banner
                        if (toolExecutionState.isExecuting) {
                            item {
                                LiveToolExecutionBanner(
                                    toolName = toolExecutionState.currentTool,
                                    statusMessage = toolExecutionState.statusMessage
                                )
                            }
                        }

                        // Confirmation Action Card
                        if (pendingConfirmation != null) {
                            item {
                                ConfirmationActionCard(
                                    request = pendingConfirmation,
                                    onConfirm = { autoSend -> onConfirmAction(true, autoSend) },
                                    onCancel = { onConfirmAction(false, false) }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }

            // Quick suggestion chips bar above input
            QuickSuggestionRow(
                language = language,
                onSelectPrompt = { inputText = it }
            )

            // Attachment indicator
            if (selectedAttachment != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCardDark)
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedAttachment ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CoralError,
                        modifier = Modifier.clickable { selectedAttachment = null }
                    )
                }
            }

            // Bottom Input Bar
            ChatInputBar(
                inputText = inputText,
                isListening = isListening,
                isExecuting = toolExecutionState.isExecuting,
                onTextChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() || selectedAttachment != null) {
                        onSendMessage(inputText, selectedAttachment)
                        inputText = ""
                        selectedAttachment = null
                    }
                },
                onAttachClick = { showAttachmentSheet = true },
                onMicClick = {
                    if (isListening) onStopListening() else onStartListening()
                }
            )
        }
    }

    // Attachment bottom sheet modal
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = SurfaceCardDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Attach Document or Asset",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    "School_Management_System.pdf" to "PDF Document • 2.4 MB",
                    "Quarterly_Financial_Report.xlsx" to "Excel Spreadsheet • 890 KB",
                    "Project_Presentation_Slides.pptx" to "Presentation • 4.1 MB",
                    "Brand_Banner_Design.png" to "Graphic Image • 1.2 MB"
                ).forEach { (filename, meta) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedAttachment = filename
                                showAttachmentSheet = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = CyanNeon)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(filename, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                            Text(meta, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Rename dialog
    if (renameConvId != null) {
        AlertDialog(
            onDismissRequest = { renameConvId = null },
            title = { Text("Rename Conversation", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        renameConvId?.let { id -> onRenameConversation(id, renameText) }
                        renameConvId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameConvId = null }) { Text("Cancel", color = Color(0xFF94A3B8)) }
            },
            containerColor = SurfaceCardDark
        )
    }
}

@Composable
fun ChatSubheader(
    title: String,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepNavyElevated,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SurfaceCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenDrawer() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Chat History",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onNewChat,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = CyanNeon
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_bubble_${message.id}"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) "You" else "NIAZ AI",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isUser) ElectricBlue else CyanNeon
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }

        Surface(
            color = if (isUser) ElectricBlue.copy(alpha = 0.25f) else SurfaceCardDark,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isUser) ElectricBlue.copy(alpha = 0.5f) else SurfaceCardBorder
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Tool status tag if executed
                if (message.toolName != null) {
                    val statusColor = when (message.toolStatus) {
                        "COMPLETED" -> EmeraldSuccess
                        "CONFIRMATION_REQUIRED" -> AmberWarning
                        "CANCELLED" -> Color(0xFF94A3B8)
                        else -> CoralError
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${message.toolService ?: "Tool"}: ${message.toolName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = Color.White
                )

                // Assistant actions (Copy / Regenerate)
                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Response",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatPromptView(
    language: Language,
    onPromptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F1B3B))
                .border(2.dp, CyanNeon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                color = CyanNeon,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NIAZ AI ASSISTANT",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = Color.White
        )

        Text(
            text = "Your Personal AI Assistant — Work Smarter, Automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CyanNeon
        )

        Spacer(modifier = Modifier.height(10.dp))

        val promptSuggestions = when (language) {
            Language.ENGLISH -> listOf(
                "Read my important Gmail messages",
                "Create a meeting tomorrow at 10 AM",
                "Send this email to Ahmed",
                "Find my files about school management",
                "Send this WhatsApp message",
                "Create a GitHub issue"
            )
            Language.URDU -> listOf(
                "میرے اہم ای میلز کا خلاصہ دیں",
                "کل صبح 10 بجے احمد کے ساتھ میٹنگ رکھیں",
                "احمد کو ای میل بھیجیں",
                "اسکول مینجمنٹ والی فائلیں تلاش کریں",
                "واٹس ایپ پر پیغام بھیجیں",
                "گٹ ہب پر ایشو بنائیں"
            )
            Language.SINDHI -> listOf(
                "منهنجا اهم جي ميل پيغام پڙهو",
                "سڀاڻي صبح 10 وڳي ميٽنگ شيڊول ڪريو",
                "احمد کي اي ميل موڪليو",
                "اسڪول مئنيجمينٽ واريون فائلون ڳوليو",
                "واٽس ايپ پيغام موڪليو",
                "گٽ هب تي نئون ايشو کوليو"
            )
        }

        promptSuggestions.forEach { prompt ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPromptClick(prompt) },
                color = SurfaceCardDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Text(
                    text = "• $prompt",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun QuickSuggestionRow(
    language: Language,
    onSelectPrompt: (String) -> Unit
) {
    val quickPills = when (language) {
        Language.ENGLISH -> listOf(
            "Summarize Gmail",
            "Schedule meeting tomorrow",
            "Send WhatsApp to Ahmed",
            "Find school files on Drive",
            "Create GitHub issue"
        )
        Language.URDU -> listOf(
            "ای میلز کا خلاصہ",
            "کل میٹنگ شیڈول کرو",
            "احمد کو واٹس ایپ کرو",
            "ڈرائیو فائلز تلاش کرو"
        )
        Language.SINDHI -> listOf(
            "اي ميل خلاصو",
            "سڀاڻي ميٽنگ رکو",
            "واٽس ايپ پيغام موڪليو",
            "ڊرائيو فائلون ڳوليو"
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickPills) { pill ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepNavyElevated)
                    .border(0.8.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onSelectPrompt(pill) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = pill,
                    fontSize = 11.sp,
                    color = CyanNeon
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    isListening: Boolean,
    isExecuting: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit
) {
    // Pulse animation for recording state
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepNavy,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SurfaceCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment Button
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("attach_file_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach Document",
                    tint = Color(0xFF94A3B8)
                )
            }

            // Input Text Field
            TextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = if (isListening) "Listening to speech..." else "Ask Niaz AI anything...",
                        color = if (isListening) CyanNeon else Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCardDark,
                    unfocusedContainerColor = SurfaceCardDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Microphone Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(if (isListening) CoralError else SurfaceCardDark)
                    .border(1.dp, if (isListening) CoralError else SurfaceCardBorder, CircleShape)
                    .clickable { onMicClick() }
                    .testTag("voice_input_mic_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Voice Input",
                    tint = if (isListening) Color.White else CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) CyanNeon else Color(0xFF1E293B))
                    .clickable(enabled = inputText.isNotBlank() && !isExecuting) { onSend() }
                    .testTag("send_message_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank()) Color.Black else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationHistoryDrawer(
    conversations: List<ConversationEntity>,
    activeId: Long?,
    onSelect: (Long) -> Unit,
    onNewChat: () -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = conversations.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("conversation_drawer")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = CyanNeon)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats...", fontSize = 12.sp, color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceCardDark,
                unfocusedContainerColor = SurfaceCardDark,
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filtered) { conv ->
                val isSelected = conv.id == activeId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(conv.id) },
                    color = if (isSelected) ElectricBlue.copy(alpha = 0.25f) else SurfaceCardDark,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) CyanNeon else SurfaceCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = conv.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        Row {
                            IconButton(
                                onClick = { onRename(conv.id, conv.title) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = { onDelete(conv.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralError, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("NIAZ AI Assistant", text)
    clipboard.setPrimaryClip(clip)
}

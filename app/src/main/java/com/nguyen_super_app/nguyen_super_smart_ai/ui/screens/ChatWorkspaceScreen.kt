package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoFilter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nguyen_super_app.nguyen_super_smart_ai.data.ai.GeminiCloudRepository
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppPreferences
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatMessageEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatSessionEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalAiModelEntity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.ai.ReasoningStreamParser
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.DocumentExportEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.ExportFormatSpec
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.GgufLifecycleManager
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.CodeBlockParser
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.SandboxExecutionResult
import com.nguyen_super_app.nguyen_super_smart_ai.engine.speech.SpeechEngineState
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.LocalizedUiStrings
import com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics.NeuralCoreHeroIllustration
import com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics.VoiceWaveformVisualizer
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.AmberWarn
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanContainer
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateSurface
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.viewmodel.PendingAttachment

/**
 * Full-screen Chat Workspace with ZERO HUB panels.
 * Navigation uses the Quick-Access Icon Bar right beside/above the chat input.
 * Reasoning blocks (<think>) are strictly collapsed unless user explicitly clicks toggle!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWorkspaceScreen(
    strings: LocalizedUiStrings,
    preferences: AppPreferences,
    currentModelLabel: String,
    localModels: List<LocalAiModelEntity>,
    sessions: List<ChatSessionEntity>,
    currentSessionId: String,
    messages: List<ChatMessageEntity>,
    streamingAiText: String,
    isGenerating: Boolean,
    reasoningExpandedMap: Map<String, Boolean>,
    pendingAttachments: List<PendingAttachment>,
    speechState: SpeechEngineState,
    sandboxResults: Map<String, SandboxExecutionResult>,
    lockedSessionIds: Set<String> = emptySet(),
    isCurrentSessionLocked: Boolean = false,
    onRequestUnlockCurrentSession: () -> Unit = {},
    onToggleReasoning: (String) -> Unit,
    onOpenAdvancedAiOptions: () -> Unit,
    onOpenLanguageSelectorSpeech: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onInputTyping: (String) -> Unit,
    onSelectGeminiModel: (String) -> Unit,
    onSelectLocalModel: (String) -> Unit,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onAttachUri: (android.net.Uri, String) -> Unit,
    onStartVoiceMic: ((String) -> Unit) -> Unit,
    onStopVoiceMic: () -> Unit,
    onQuickExport: (ExportFormatSpec) -> Unit,
    onRunCodeBlock: (String, String, String) -> Unit,
    onFixCodeWithAi: (String, String, String) -> Unit,
    onNavigateBrowser: () -> Unit,
    onNavigateStudio: () -> Unit,
    onNavigatePhotoEditor: () -> Unit,
    onNavigateHardwareHud: () -> Unit,
    onNavigateVaultSettings: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAttachUri(uri, "photo_${System.currentTimeMillis()}.jpg")
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAttachUri(uri, "doc_${System.currentTimeMillis()}.bin")
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceMic { phrase ->
                inputText = (inputText + " " + phrase).trim()
            }
        }
    }

    LaunchedEffect(messages.size, streamingAiText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        containerColor = SlateDeep,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                title = {
                    Box {
                        Surface(
                            onClick = { showModelMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = CyanContainer,
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("engine_switcher_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (preferences.useLocalGgufEngine) Icons.Rounded.Memory else Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = currentModelLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyanBright,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            GeminiCloudRepository.supportedModels.forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text("Cloud: $label") },
                                    onClick = {
                                        onSelectGeminiModel(id)
                                        showModelMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            GgufLifecycleManager.defaultCatalog.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("Offline: ${cat.name}") },
                                    onClick = {
                                        onSelectLocalModel(cat.id)
                                        showModelMenu = false
                                    }
                                )
                            }
                            localModels.filter { !it.isInTrash && it.isVisibleInList }.forEach { mdl ->
                                DropdownMenuItem(
                                    text = { Text("Đã tải (${mdl.formatId}): ${mdl.name}") },
                                    onClick = {
                                        onSelectLocalModel(mdl.id)
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Advanced AI Options Dialog Button
                    IconButton(
                        onClick = onOpenAdvancedAiOptions,
                        modifier = Modifier.testTag("chat_advanced_ai_options_button")
                    ) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = "Tùy chọn Tính năng AI nâng cao",
                            tint = if (preferences.advancedAiFeatures.enableWebSearch || preferences.advancedAiFeatures.enableSingleBatchFileGen) CyanBright else TextSecondary
                        )
                    }

                    // Quick Export Dropdown
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("chat_quick_export_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Xuất nhanh", tint = TextPrimary)
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DocumentExportEngine.supportedFormats.take(10).forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt.displayName) },
                                    onClick = {
                                        showExportMenu = false
                                        onQuickExport(fmt)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("chat_history_button")
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = strings.menuHistory, tint = TextPrimary)
                    }

                    IconButton(
                        onClick = onNewSession,
                        modifier = Modifier.testTag("chat_new_session_button")
                    ) {
                        Icon(Icons.Rounded.AddComment, contentDescription = "Hội thoại mới", tint = CyanPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtle mode + active advanced capability strip (compact, zero HUB)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (preferences.isGuestMode) strings.guestModeBadge else strings.cloudModeBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (preferences.advancedAiFeatures.enableWebSearch) {
                        Text("Web Search: ON", style = MaterialTheme.typography.labelSmall, color = EmeraldOk)
                    }
                    if (preferences.advancedAiFeatures.enableSingleBatchFileGen) {
                        Text("Batch File: ON", style = MaterialTheme.typography.labelSmall, color = CyanBright)
                    }
                }
            }

            if (isCurrentSessionLocked) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 680.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, CyanPrimary),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = CyanBright, modifier = Modifier.size(36.dp))
                            Text("Cuộc trò chuyện đang được bảo vệ", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "Nhập mật khẩu hoặc hình vẽ đã thiết lập để xem và tiếp tục cuộc trò chuyện này.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Button(
                                onClick = onRequestUnlockCurrentSession,
                                modifier = Modifier.testTag("unlock_current_chat_button")
                            ) {
                                Text("Mở khóa cuộc trò chuyện")
                            }
                        }
                    }
                }
            } else if (messages.isEmpty() && !isGenerating) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 680.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NeuralCoreHeroIllustration(sizeDp = 96.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Super Smart AI sẵn sàng", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Hỏi đáp tự nhiên trực tiếp, tải 50+ định dạng mô hình qua Trình duyệt, chạy mã Kotlin/Python/SQL hoặc chuyển đổi 75+ định dạng tệp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 760.dp)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            msg = msg,
                            isReasoningExpanded = reasoningExpandedMap[msg.id] == true,
                            onToggleReasoning = { onToggleReasoning(msg.id) },
                            sandboxResults = sandboxResults,
                            onRunCodeBlock = onRunCodeBlock,
                            onFixCodeWithAi = onFixCodeWithAi,
                            strings = strings
                        )
                    }
                    if (isGenerating) {
                        item(key = "streaming_ai_preview") {
                            val streamMsg = ChatMessageEntity(
                                id = "streaming_active",
                                sessionId = currentSessionId,
                                role = "AI",
                                content = streamingAiText.ifBlank { "<think>Đang khởi tạo luồng suy luận...</think>" },
                                engineTag = currentModelLabel,
                                timestampMillis = System.currentTimeMillis()
                            )
                            ChatMessageBubble(
                                msg = streamMsg,
                                isStreaming = true,
                                isReasoningExpanded = reasoningExpandedMap["streaming_active"] == true,
                                onToggleReasoning = { onToggleReasoning("streaming_active") },
                                sandboxResults = sandboxResults,
                                onRunCodeBlock = onRunCodeBlock,
                                onFixCodeWithAi = onFixCodeWithAi,
                                strings = strings
                            )
                        }
                    }
                }
            }

            if (speechState.isListening) {
                Surface(
                    color = SlateCard,
                    border = BorderStroke(1.dp, CyanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(speechState.statusNote, style = MaterialTheme.typography.labelMedium, color = CyanBright, modifier = Modifier.weight(1f))
                            TextButton(onClick = onStopVoiceMic) { Text("Dừng") }
                        }
                        VoiceWaveformVisualizer(rmsLevel = speechState.micRmsLevel, isListening = true)
                    }
                }
            }

            if (pendingAttachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingAttachments.forEach { att ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${att.fileName} [${att.compressionFormat.id}]") }
                        )
                    }
                }
            }

            // Quick-Access Icon Bar right beside/above the chat input (NO HUB!)
            Surface(
                color = SlateSurface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("quick_access_icon_bar"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickAccessNavIcon(
                            icon = Icons.Rounded.Public,
                            label = "Trình duyệt Model",
                            tag = "quick_nav_browser",
                            onClick = onNavigateBrowser
                        )
                        QuickAccessNavIcon(
                            icon = Icons.Rounded.Build,
                            label = "Studio Tệp",
                            tag = "quick_nav_studio",
                            onClick = onNavigateStudio
                        )
                        QuickAccessNavIcon(
                            icon = Icons.Rounded.PhotoFilter,
                            label = "Sửa ảnh",
                            tag = "quick_nav_photo_editor",
                            onClick = onNavigatePhotoEditor
                        )
                        QuickAccessNavIcon(
                            icon = Icons.Rounded.Memory,
                            label = "Phần cứng HUD",
                            tag = "quick_nav_hardware_hud",
                            onClick = onNavigateHardwareHud
                        )
                        QuickAccessNavIcon(
                            icon = Icons.Rounded.Language,
                            label = "Ngôn ngữ (${speechState.selectedLanguageCode})",
                            tag = "quick_nav_language",
                            onClick = onOpenLanguageSelectorSpeech
                        )
                        QuickAccessNavIcon(
                            icon = Icons.Rounded.Settings,
                            label = "Kho & Cài đặt",
                            tag = "quick_nav_vault_settings",
                            onClick = onNavigateVaultSettings
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Input Bar with Photo Picker, File Attach, Offline Mic & Send
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = "Chọn ảnh", tint = CyanBright)
                        }

                        IconButton(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = strings.attachFile, tint = TextSecondary)
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                onInputTyping(it)
                            },
                            placeholder = { Text(strings.chatInputPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            maxLines = 4,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_prompt_input")
                        )

                        Spacer(Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                if (speechState.isListening) {
                                    onStopVoiceMic()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        onStartVoiceMic { phrase ->
                                            inputText = (inputText + " " + phrase).trim()
                                        }
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_voice_mic_button")
                        ) {
                            Icon(
                                imageVector = if (speechState.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = strings.voiceMic,
                                tint = if (speechState.isListening) CoralError else CyanBright
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                val text = inputText
                                if (text.isNotBlank() || pendingAttachments.isNotEmpty()) {
                                    inputText = ""
                                    onSendPrompt(text)
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = CyanPrimary,
                                contentColor = SlateDeep
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = strings.send)
                        }
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = SlateCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Lịch sử hội thoại", style = MaterialTheme.typography.titleLarge)
                sessions.forEach { s ->
                    val isLocked = s.id in lockedSessionIds
                    Surface(
                        onClick = {
                            onSelectSession(s.id)
                            showHistorySheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (s.id == currentSessionId) CyanContainer else SlateElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLocked) {
                                Icon(Icons.Rounded.Lock, contentDescription = "Đã khóa", tint = AmberWarn, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${s.modelLabel} • ${s.activePersonaName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            IconButton(onClick = { onDeleteSession(s.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Xóa phiên", tint = CoralError)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuickAccessNavIcon(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SlateElevated,
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = CyanBright, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun ChatMessageBubble(
    msg: ChatMessageEntity,
    isStreaming: Boolean = false,
    isReasoningExpanded: Boolean,
    onToggleReasoning: () -> Unit,
    sandboxResults: Map<String, SandboxExecutionResult>,
    onRunCodeBlock: (String, String, String) -> Unit,
    onFixCodeWithAi: (String, String, String) -> Unit,
    strings: LocalizedUiStrings
) {
    val isUser = msg.role == "USER"
    val isError = msg.role == "SYSTEM_ERROR"

    val parsedReasoning = remember(msg.content, isStreaming) {
        if (isUser || isError) null else ReasoningStreamParser.parse(msg.content, isStreaming)
    }

    val bubbleColor = when {
        isUser -> CyanContainer
        isError -> CoralError.copy(alpha = 0.16f)
        else -> SlateCard
    }
    val borderColor = when {
        isUser -> CyanPrimary
        isError -> CoralError
        else -> SlateBorder
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bubbleColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isUser) "Bạn" else msg.engineTag,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) CyanBright else EmeraldOk
                )
                if (!msg.attachmentSummary.isNullOrBlank()) {
                    Text(msg.attachmentSummary, style = MaterialTheme.typography.labelSmall, color = AmberWarn)
                }
            }

            // STRICT REASONING SECTION: Never auto-opens; shows title + explicit toggle button ONLY!
            if (parsedReasoning != null && (parsedReasoning.hasReasoning || parsedReasoning.isCurrentlyThinking)) {
                Spacer(Modifier.height(8.dp))
                StrictReasoningCollapseCard(
                    messageId = msg.id,
                    isCurrentlyThinking = parsedReasoning.isCurrentlyThinking,
                    reasoningText = parsedReasoning.reasoningText,
                    isUserExpanded = isReasoningExpanded,
                    onToggleExpand = onToggleReasoning
                )
            }

            val visibleAnswer = parsedReasoning?.answerText ?: msg.content
            if (visibleAnswer.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                val (proseParts, codeBlocks) = remember(visibleAnswer) {
                    CodeBlockParser.parseMarkdownWithCode(visibleAnswer)
                }
                proseParts.forEach { para ->
                    Text(para, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                }
                codeBlocks.forEachIndexed { idx, block ->
                    val blockKey = "${msg.id}_$idx"
                    val execResult = sandboxResults[blockKey]
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = SlateDeep,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(block.language.uppercase(), style = MaterialTheme.typography.labelMedium, color = CyanBright)
                                TextButton(onClick = { onRunCodeBlock(blockKey, block.language, block.code) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.runSyntaxCheck)
                                }
                            }
                            Text(
                                block.code,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = TextPrimary
                            )
                            if (execResult != null) {
                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(color = SlateBorder)
                                Spacer(Modifier.height(6.dp))
                                if (execResult.succeeded) {
                                    Text(execResult.stdout, style = MaterialTheme.typography.bodySmall, color = EmeraldOk)
                                } else {
                                    Text("Lỗi: ${execResult.diagnosticError}", style = MaterialTheme.typography.bodySmall, color = CoralError)
                                    OutlinedButton(
                                        onClick = {
                                            onFixCodeWithAi(block.language, block.code, execResult.diagnosticError ?: "Syntax error")
                                        },
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Text(strings.fixErrorWithAi)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays ONLY the reasoning header title + explicit "Mở suy luận" / "Ẩn suy luận" button by default.
 * Reasoning body is shown IF AND ONLY IF reasoning exists/is active AND the user explicitly toggled it open.
 */
@Composable
fun StrictReasoningCollapseCard(
    messageId: String,
    isCurrentlyThinking: Boolean,
    reasoningText: String,
    isUserExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val titleText = if (isCurrentlyThinking) "Đang suy luận..." else "Quá trình suy luận của mô hình"
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SlateElevated,
        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reasoning_card_$messageId")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = CyanBright, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanBright,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onToggleExpand,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("reasoning_toggle_button_$messageId")
                ) {
                    Icon(
                        imageVector = if (isUserExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isUserExpanded) "Ẩn suy luận" else "Mở suy luận", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (isUserExpanded && reasoningText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = SlateBorder)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reasoningText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary,
                    modifier = Modifier.testTag("reasoning_body_$messageId")
                )
            }
        }
    }
}

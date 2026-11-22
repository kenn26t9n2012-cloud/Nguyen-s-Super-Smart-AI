package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppPreferences
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BiometricAuthMode
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatMessageEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatSessionEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.SecurityLockEntity
import com.nguyen_super_app.nguyen_super_smart_ai.security.AppLockAuthenticator
import com.nguyen_super_app.nguyen_super_smart_ai.security.AppLockManager
import com.nguyen_super_app.nguyen_super_smart_ai.security.BiometricAuthResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.FingerprintFirstOutcome
import com.nguyen_super_app.nguyen_super_smart_ai.security.PasswordLockMode
import com.nguyen_super_app.nguyen_super_smart_ai.security.PersonalFaceAuthenticator
import com.nguyen_super_app.nguyen_super_smart_ai.security.SecurityLockCrypto
import com.nguyen_super_app.nguyen_super_smart_ai.security.UnifiedLoginOption
import com.nguyen_super_app.nguyen_super_smart_ai.security.UnifiedUnlockManager
import com.nguyen_super_app.nguyen_super_smart_ai.security.UnifiedUnlockMethod
import com.nguyen_super_app.nguyen_super_smart_ai.ui.components.PatternLockGrid
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.PersonalFaceCameraDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.PersonalFaceScanMode
import kotlinx.coroutines.delay
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

fun formatAutoLockTimeoutLabel(timeoutMillis: Long): String {
    return when {
        timeoutMillis < 0L -> "Không tự động khóa"
        timeoutMillis == 0L -> "Khóa ngay khi thoát"
        timeoutMillis < 60_000L -> "Sau ${(timeoutMillis / 1000L).coerceAtLeast(1L)} giây"
        else -> "Sau ${(timeoutMillis / 60_000L).coerceAtLeast(1L)} phút"
    }
}

/**
 * Center Hub screen for:
 * 1) Đặt mật khẩu cho từng cuộc trò chuyện
 * 2) Khiên bảo vệ ứng dụng (App Lock) với công tắc Bật/Tắt có xác thực
 * 3) Số lần cho phép nhập sai
 * 4) Thời gian tự động khóa khi chạy ngầm (autoLockTimeout)
 * 5) Tùy chọn tách biệt Sinh trắc học của điện thoại
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityLockHubScreen(
    securityLocks: List<SecurityLockEntity>,
    unlimitedAttempts: Boolean,
    maxAttempts: Long,
    onOpenChatPicker: () -> Unit,
    onOpenAppLockSetupDirect: () -> Unit,
    onOpenAttemptLimitConfig: () -> Unit,
    onRemoveAppLock: () -> Unit,
    onBack: () -> Unit,
    autoLockTimeoutMillis: Long = AppPreferences.DEFAULT_AUTO_LOCK_TIMEOUT_MS,
    allowDeviceBiometrics: Boolean = false,
    enrolledPersonalFaceTargets: Set<String> = emptySet(),
    onToggleAppLockSwitch: ((enable: Boolean) -> Unit)? = null,
    onOpenAutoLockTimeout: () -> Unit = {},
    onSetAllowDeviceBiometrics: (Boolean) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val appLock = remember(securityLocks) { securityLocks.firstOrNull { it.targetType == "APP" } }
    val chatLocksCount = remember(securityLocks) { securityLocks.count { it.targetType == "CHAT" } }
    val hasAppPersonalFace = AppLockManager.APP_LOCK_ID in enrolledPersonalFaceTargets

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("security_hub_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = {
                    Text(
                        "Đặt mật khẩu cho cuộc trò chuyện/ứng dụng",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("security_hub_cancel_button")) {
                        Text("Hủy", color = CyanBright)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Option 1: Per-Chat Password Lock
            Card(
                onClick = onOpenChatPicker,
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_per_chat_lock_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Forum, contentDescription = null, tint = CyanBright, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Đặt mật khẩu cho từng cuộc trò chuyện", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (chatLocksCount > 0) "Đang bảo vệ $chatLocksCount cuộc trò chuyện • Tìm kiếm & chọn cuộc trò chuyện"
                            else "Chọn từ danh sách cuộc trò chuyện kèm thanh tìm kiếm tên/nội dung",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Option 2: Direct App Lock Shield + Password-Authenticated ON/OFF Switch
            Card(
                onClick = onOpenAppLockSetupDirect,
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, if (appLock != null) EmeraldOk.copy(alpha = 0.6f) else CyanPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_app_lock_shield_button")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = if (appLock != null) EmeraldOk else CyanPrimary, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Khiên bảo vệ ứng dụng (App Lock)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (appLock != null) {
                                    val mode = PasswordLockMode.fromId(appLock.passwordMode)
                                    val extra = if (mode == PasswordLockMode.PATTERN) " (${appLock.patternGridSize}x${appLock.patternGridSize})" else ""
                                    val faceTag = if (hasAppPersonalFace) " • Khuôn mặt cá nhân" else ""
                                    "Đang bật • Kiểu ${mode.displayLabel}$extra$faceTag • Bấm để đổi"
                                } else {
                                    "Đi thẳng vào thiết lập mật khẩu & Khuôn mặt cá nhân khóa toàn bộ ứng dụng"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (appLock != null) EmeraldOk else TextSecondary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = appLock != null,
                            onCheckedChange = { enable ->
                                if (onToggleAppLockSwitch != null) {
                                    onToggleAppLockSwitch(enable)
                                } else if (enable) {
                                    onOpenAppLockSetupDirect()
                                } else {
                                    onRemoveAppLock()
                                }
                            },
                            modifier = Modifier.testTag("hub_app_lock_switch")
                        )
                    }
                    if (appLock != null) {
                        OutlinedButton(
                            onClick = onRemoveAppLock,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("hub_remove_app_lock_button")
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = CoralError, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tắt Khiên bảo vệ ứng dụng (yêu cầu xác thực)", color = CoralError)
                        }
                    }
                }
            }

            // Option 3: Max Failed Attempts Config
            Card(
                onClick = onOpenAttemptLimitConfig,
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_attempt_limit_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Numbers, contentDescription = null, tint = AmberWarn, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Số lần cho phép nhập sai", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (unlimitedAttempts) "Hiện tại: Không giới hạn số lần nhập sai"
                            else "Hiện tại: Tối đa $maxAttempts lần nhập sai",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanBright
                        )
                    }
                }
            }

            // Option 4: Background Auto-Lock Timeout
            Card(
                onClick = onOpenAutoLockTimeout,
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_auto_lock_timeout_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = CyanBright, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thời gian tự động khóa (Auto-Lock)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatAutoLockTimeoutLabel(autoLockTimeoutMillis)} • Khóa lại cả App và các cuộc trò chuyện",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanBright
                        )
                    }
                }
            }

            // Option 5: Device Biometric Isolation Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(
                    1.dp,
                    if (allowDeviceBiometrics) CyanPrimary.copy(alpha = 0.55f) else SlateBorder
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        tint = if (allowDeviceBiometrics) CyanBright else TextSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cho phép Sinh trắc học của điện thoại", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (allowDeviceBiometrics) {
                                "Đang bật: Có thể nhấn nút Sinh trắc học trên màn hình khóa để dùng vân tay/khuôn mặt/PIN của điện thoại."
                            } else {
                                "Đang tắt (Mặc định): Mật khẩu & Khuôn mặt cá nhân trong app tách biệt hoàn toàn khỏi vân tay/khuôn mặt điện thoại."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (allowDeviceBiometrics) CyanBright else TextSecondary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = allowDeviceBiometrics,
                        onCheckedChange = onSetAllowDeviceBiometrics,
                        modifier = Modifier.testTag("hub_device_biometric_isolation_switch")
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("security_hub_bottom_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Quay lại Cài đặt")
            }
        }
    }
}

/**
 * Lists all existing chat sessions with a search bar filtering by title or message content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityChatPickerScreen(
    sessions: List<ChatSessionEntity>,
    allMessages: List<ChatMessageEntity>,
    securityLocks: List<SecurityLockEntity>,
    onSelectChatForLock: (ChatSessionEntity) -> Unit,
    onRemoveChatLock: (sessionId: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var searchQuery by remember { mutableStateOf("") }
    val lockMap = remember(securityLocks) {
        securityLocks.filter { it.targetType == "CHAT" }.associateBy { it.targetId }
    }
    val messageIndexBySession = remember(allMessages) {
        allMessages.groupBy { it.sessionId }.mapValues { (_, msgs) ->
            msgs.joinToString("\n") { it.content }
        }
    }
    val filteredSessions = remember(sessions, messageIndexBySession, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) sessions
        else sessions.filter { s ->
            s.title.contains(q, ignoreCase = true) ||
                s.modelLabel.contains(q, ignoreCase = true) ||
                (messageIndexBySession[s.id]?.contains(q, ignoreCase = true) == true)
        }
    }

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_picker_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Chọn cuộc trò chuyện", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("chat_picker_cancel_button")) {
                        Text("Hủy", color = CyanBright)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm theo tên hoặc nội dung cuộc trò chuyện...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Tìm kiếm") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_lock_search_input")
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val existingLock = lockMap[session.id]
                    val snippet = messageIndexBySession[session.id]?.lineSequence()?.firstOrNull()?.take(70)
                    Card(
                        onClick = { onSelectChatForLock(session) },
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(
                            1.dp,
                            if (existingLock != null) EmeraldOk.copy(alpha = 0.55f) else SlateBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_lock_item_${session.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (existingLock != null) Icons.Rounded.Lock else Icons.Rounded.Forum,
                                contentDescription = null,
                                tint = if (existingLock != null) EmeraldOk else CyanBright
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                if (!snippet.isNullOrBlank()) {
                                    Text(snippet, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (existingLock != null) {
                                    val m = PasswordLockMode.fromId(existingLock.passwordMode)
                                    val gridInfo = if (m == PasswordLockMode.PATTERN) " ${existingLock.patternGridSize}x${existingLock.patternGridSize}" else ""
                                    Text(
                                        "Đã khóa • Kiểu ${m.displayLabel}$gridInfo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldOk
                                    )
                                }
                            }
                            if (existingLock != null) {
                                IconButton(
                                    onClick = { onRemoveChatLock(session.id) },
                                    modifier = Modifier.testTag("remove_chat_lock_${session.id}")
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Gỡ khóa cuộc trò chuyện", tint = CoralError)
                                }
                            }
                        }
                    }
                }

                if (filteredSessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Không tìm thấy cuộc trò chuyện phù hợp.", color = TextSecondary)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Quay lại")
            }
        }
    }
}

/**
 * Multi-Mode Password Setup screen supporting all 4 types:
 * 1) Số (DIGITS)
 * 2) Chữ không số (LETTERS)
 * 3) Chữ và số (ALPHANUMERIC)
 * 4) Hình (PATTERN 3x3 .. 10x10)
 *
 * Show-password toggle is strictly shown ONLY for the 3 character-based modes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SecurityPasswordSetupScreen(
    targetType: String, // "APP" or "CHAT"
    targetId: String,
    targetLabel: String,
    existingLock: SecurityLockEntity?,
    onConfirmSave: (mode: PasswordLockMode, gridSize: Int, textSecret: String, patternNodes: List<Int>) -> Unit,
    onRemoveExistingLock: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedMode by remember(existingLock) {
        mutableStateOf(
            existingLock?.let { PasswordLockMode.fromId(it.passwordMode) } ?: PasswordLockMode.DIGITS
        )
    }
    var gridSize by remember(existingLock) {
        mutableIntStateOf(
            existingLock?.patternGridSize?.takeIf { it in 3..10 } ?: 3
        )
    }
    var textSecret by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var patternNodes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val validation = remember(selectedMode, gridSize, textSecret, patternNodes) {
        if (selectedMode == PasswordLockMode.PATTERN) {
            SecurityLockCrypto.validatePattern(gridSize, patternNodes)
        } else {
            SecurityLockCrypto.validateTextSecret(selectedMode, textSecret)
        }
    }

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("setup_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = {
                    Text(
                        if (targetType == "APP") "Khiên bảo vệ ứng dụng" else "Khóa: $targetLabel",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("setup_cancel_button")) {
                        Text("Hủy", color = CyanBright)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = SlateSurface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("security_setup_cancel_bottom_button")
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = {
                            if (!validation.isValid) {
                                errorMsg = validation.errorMessage ?: "Mật khẩu chưa hợp lệ."
                            } else {
                                onConfirmSave(selectedMode, gridSize, textSecret, patternNodes)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("security_setup_ok_button")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("OK")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Switcher
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chọn loại mật khẩu", style = MaterialTheme.typography.titleSmall, color = CyanBright)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PasswordLockMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = {
                                    selectedMode = mode
                                    textSecret = ""
                                    patternNodes = emptyList()
                                    showPassword = false
                                    errorMsg = null
                                },
                                label = { Text(mode.displayLabel) },
                                modifier = Modifier.testTag("mode_chip_${mode.name}")
                            )
                        }
                    }
                    Text(selectedMode.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            if (selectedMode == PasswordLockMode.PATTERN) {
                // Grid size selector: 3x3, 4x4, ..., 10x10
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GridOn, contentDescription = null, tint = CyanBright)
                            Spacer(Modifier.width(8.dp))
                            Text("Kích thước ma trận lưới hình (${gridSize}x${gridSize})", style = MaterialTheme.typography.titleSmall, color = CyanBright)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (SecurityLockCrypto.MIN_GRID_SIZE..SecurityLockCrypto.MAX_GRID_SIZE).forEach { size ->
                                FilterChip(
                                    selected = gridSize == size,
                                    onClick = {
                                        gridSize = size
                                        patternNodes = emptyList()
                                        errorMsg = null
                                    },
                                    label = { Text("${size}x${size}") },
                                    modifier = Modifier.testTag("grid_size_$size")
                                )
                            }
                        }

                        PatternLockGrid(
                            gridSize = gridSize,
                            selectedNodes = patternNodes,
                            onNodesChanged = {
                                patternNodes = it
                                errorMsg = null
                            }
                        )
                    }
                }
            } else {
                // Character-based password input + visual dot/character feedback + Show/Hide eye toggle
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Visual real-time input status feedback
                        VisualPasswordInputIndicator(
                            value = textSecret,
                            revealed = showPassword,
                            isValid = validation.isValid
                        )

                        OutlinedTextField(
                            value = textSecret,
                            onValueChange = { next ->
                                textSecret = when (selectedMode) {
                                    PasswordLockMode.DIGITS -> next.filter { it in '0'..'9' }
                                    PasswordLockMode.LETTERS -> next.filter { it.isLetter() }
                                    PasswordLockMode.ALPHANUMERIC -> next.filter { it.isLetterOrDigit() }
                                    PasswordLockMode.CUSTOM -> next
                                    PasswordLockMode.PATTERN -> next
                                }
                                errorMsg = null
                            },
                            label = { Text("Nhập mật khẩu (${selectedMode.displayLabel})") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (selectedMode == PasswordLockMode.DIGITS) KeyboardType.NumberPassword else KeyboardType.Text
                            ),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                if (selectedMode.supportsVisibilityToggle) {
                                    IconButton(
                                        onClick = { showPassword = !showPassword },
                                        modifier = Modifier.testTag("security_password_visibility_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = if (showPassword) "Ẩn mật khẩu" else "Hiển thị mật khẩu"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_password_input")
                        )

                        Text(
                            text = if (validation.isValid) {
                                "Hợp lệ (${textSecret.length} ký tự)"
                            } else {
                                validation.errorMessage.orEmpty()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (validation.isValid) EmeraldOk else TextSecondary
                        )
                    }
                }
            }

            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = CoralError)
            }

            if (existingLock != null && onRemoveExistingLock != null) {
                OutlinedButton(
                    onClick = onRemoveExistingLock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("security_setup_remove_lock_button")
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = CoralError)
                    Spacer(Modifier.width(6.dp))
                    Text("Gỡ bỏ mật khẩu này", color = CoralError)
                }
            }
        }
    }
}

/**
 * Configuration screen for Max Failed Attempts ("Số lần cho phép nhập sai").
 * STRICT RULE: When "Không giới hạn" is activated, ALL controls below it are disabled (enabled = false).
 * "Số lần" opens numeric keyboard allowing any arbitrary positive integer without small upper caps.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SecurityAttemptLimitScreen(
    initialUnlimited: Boolean,
    initialMaxAttempts: Long,
    onConfirmSave: (unlimited: Boolean, maxAttempts: Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var isUnlimited by remember(initialUnlimited) { mutableStateOf(initialUnlimited) }
    var customInputActive by remember(initialUnlimited) { mutableStateOf(!initialUnlimited) }
    var attemptsText by remember(initialMaxAttempts) { mutableStateOf(initialMaxAttempts.coerceAtLeast(1L).toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val numberFocusRequester = remember { FocusRequester() }

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("attempt_limit_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Số lần cho phép nhập sai", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("attempt_limit_cancel_button")) {
                        Text("Hủy", color = CyanBright)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = SlateSurface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("attempt_limit_cancel_bottom_button")
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            if (isUnlimited) {
                                onConfirmSave(true, 0L)
                            } else {
                                val parsed = attemptsText.toLongOrNull()
                                if (parsed == null || parsed <= 0L) {
                                    errorMsg = "Vui lòng nhập số lần hợp lệ (>= 1)."
                                } else {
                                    onConfirmSave(false, parsed)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("attempt_limit_ok_button")
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top control: "Không giới hạn"
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isUnlimited) CyanContainer else SlateCard),
                border = BorderStroke(1.dp, if (isUnlimited) CyanPrimary else SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AllInclusive, contentDescription = null, tint = CyanBright)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Không giới hạn", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            if (isUnlimited) "Đang bật: Mọi tùy chọn giới hạn bên dưới đều bị vô hiệu hóa."
                            else "Đang tắt: Cho phép thiết lập số lần tối đa bên dưới.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isUnlimited,
                        onCheckedChange = { checked ->
                            isUnlimited = checked
                            if (checked) {
                                customInputActive = false
                                errorMsg = null
                            } else {
                                customInputActive = true
                            }
                        },
                        modifier = Modifier.testTag("attempt_limit_unlimited_button")
                    )
                }
            }

            // All controls BELOW "Không giới hạn" are strictly disabled when isUnlimited == true
            val controlsBelowEnabled = !isUnlimited
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (controlsBelowEnabled) 1f else 0.45f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            customInputActive = true
                            try {
                                numberFocusRequester.requestFocus()
                            } catch (_: Throwable) {
                            }
                        },
                        enabled = controlsBelowEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (customInputActive && controlsBelowEnabled) CyanPrimary else SlateElevated,
                            contentColor = if (customInputActive && controlsBelowEnabled) SlateDeep else TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("attempt_limit_custom_button")
                    ) {
                        Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Số lần")
                    }

                    OutlinedTextField(
                        value = attemptsText,
                        onValueChange = { raw ->
                            attemptsText = raw.filter { it in '0'..'9' }.take(12)
                            errorMsg = null
                        },
                        enabled = controlsBelowEnabled && customInputActive,
                        label = { Text("Nhập số lần cho phép sai (tùy ý, không giới hạn trần)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(numberFocusRequester)
                            .testTag("attempt_limit_number_input")
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3L, 5L, 10L, 50L, 100L).forEach { preset ->
                            FilterChip(
                                selected = controlsBelowEnabled && attemptsText == preset.toString(),
                                onClick = {
                                    customInputActive = true
                                    attemptsText = preset.toString()
                                    errorMsg = null
                                },
                                enabled = controlsBelowEnabled,
                                label = { Text("$preset lần") },
                                modifier = Modifier.testTag("attempt_limit_preset_$preset")
                            )
                        }
                    }
                }
            }

            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = CoralError)
            }
        }
    }
}

/**
 * Configuration screen for background `autoLockTimeout`.
 * When exceeded, AppLockManager locks BOTH the App Lock and all unlocked conversations.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AutoLockTimeoutScreen(
    initialTimeoutMillis: Long,
    onConfirmSave: (timeoutMillis: Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTimeout by remember(initialTimeoutMillis) { mutableLongStateOf(initialTimeoutMillis) }
    var customMinutesText by remember(initialTimeoutMillis) {
        mutableStateOf(
            if (initialTimeoutMillis > 0L) {
                (initialTimeoutMillis / 60_000L).coerceAtLeast(1L).toString()
            } else {
                ""
            }
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        AppPreferences.AUTO_LOCK_IMMEDIATE to "Khóa ngay",
        60_000L to "1 phút",
        300_000L to "5 phút",
        900_000L to "15 phút",
        AppPreferences.AUTO_LOCK_NEVER to "Không tự khóa"
    )

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("auto_lock_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Thời gian tự động khóa", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("auto_lock_cancel_button")) {
                        Text("Hủy", color = CyanBright)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = SlateSurface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("auto_lock_cancel_bottom_button")
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = {
                            if (selectedTimeout == Long.MIN_VALUE) {
                                val mins = customMinutesText.toLongOrNull()
                                if (mins == null || mins <= 0L || mins > 1440L) {
                                    errorMsg = "Vui lòng nhập số phút hợp lệ từ 1 đến 1440."
                                } else {
                                    onConfirmSave(mins * 60_000L)
                                }
                            } else {
                                onConfirmSave(selectedTimeout)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("auto_lock_timeout_ok_button")
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = CyanBright)
                        Spacer(Modifier.width(8.dp))
                        Text("Khóa lại sau khi chạy ngầm", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text(
                        "Khi ứng dụng chạy ngầm vượt quá khoảng thời gian này, hệ thống sẽ tự động khóa lại cả Khiên ứng dụng lẫn toàn bộ các cuộc trò chuyện đã mở.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (ms, label) ->
                            FilterChip(
                                selected = selectedTimeout == ms,
                                onClick = {
                                    selectedTimeout = ms
                                    if (ms > 0L) {
                                        customMinutesText = (ms / 60_000L).toString()
                                    }
                                    errorMsg = null
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("timeout_preset_$ms")
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tùy chỉnh số phút (1 - 1440 phút)", style = MaterialTheme.typography.titleSmall, color = CyanBright)
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it in '0'..'9' }.take(4)
                            customMinutesText = clean
                            val parsed = clean.toLongOrNull()
                            selectedTimeout = if (parsed != null && parsed in 1L..1440L) {
                                parsed * 60_000L
                            } else {
                                Long.MIN_VALUE
                            }
                            errorMsg = null
                        },
                        label = { Text("Số phút tự động khóa") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timeout_custom_minutes_input")
                    )
                }
            }

            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = CoralError)
            }
        }
    }
}

/**
 * Runtime Lock Gate (used full-screen for App Lock, modal dialog for Per-Chat Lock,
 * and authentication gate before turning OFF App Lock).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordUnlockGateContent(
    lock: SecurityLockEntity,
    title: String,
    allowCancel: Boolean,
    onVerifyText: (String) -> Boolean,
    onVerifyPattern: (List<Int>) -> Boolean,
    onBiometricRecoverySuccess: () -> Unit,
    onCancel: () -> Unit,
    hasPersonalFaceEnrolled: Boolean = false,
    allowDeviceBiometric: Boolean = true,
    onVerifyPersonalFace: (FloatArray) -> Boolean = { false },
    onTriggerDeviceBiometric: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    biometricAuthMode: BiometricAuthMode? = null,
    fingerprintFirstWithPinFallback: Boolean = true,
    onTriggerFingerprintFirst: ((FragmentActivity, (FingerprintFirstOutcome) -> Unit) -> Unit)? = null,
    onTriggerPhoneFace: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    onTriggerPhoneCredential: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    unifiedUnlockManager: UnifiedUnlockManager? = null,
    biometricExpiryDelayMs: Long = UnifiedUnlockManager.DEFAULT_BIOMETRIC_EXPIRY_MS
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coordinator = unifiedUnlockManager ?: remember(context) {
        UnifiedUnlockManager(
            appLockAuthenticator = AppLockAuthenticator(context),
            personalFaceAuthenticator = PersonalFaceAuthenticator(context)
        )
    }
    val coordinatorState by coordinator.uiState.collectAsState()
    val passwordFocusRequester = remember(lock.lockId) { FocusRequester() }
    val mode = remember(lock.passwordMode) { PasswordLockMode.fromId(lock.passwordMode) }
    var textInput by remember(lock.lockId) { mutableStateOf("") }
    var showPassword by remember(lock.lockId) { mutableStateOf(false) }
    var patternNodes by remember(lock.lockId) { mutableStateOf<List<Int>>(emptyList()) }
    var errorMsg by remember(lock.lockId) { mutableStateOf<String?>(null) }
    var fallbackNotice by remember(lock.lockId) { mutableStateOf<String?>(null) }
    var showVerifyFaceDialog by remember(lock.lockId) { mutableStateOf(false) }

    val systemBioEnabled = biometricAuthMode?.allowsSystemBiometric ?: allowDeviceBiometric
    val personalFaceEnabled = hasPersonalFaceEnrolled && (biometricAuthMode?.allowsPersonalFace ?: true)
    val enabledSheetOptions = remember(
        lock.lockId,
        lock.lockedOut,
        biometricAuthMode,
        allowDeviceBiometric,
        hasPersonalFaceEnrolled
    ) {
        coordinator.getEnabledLoginOptions(
            lock = lock,
            biometricAuthMode = biometricAuthMode,
            allowDeviceBiometric = allowDeviceBiometric,
            hasPersonalFaceOverride = hasPersonalFaceEnrolled
        )
    }

    var hasAutoPromptedFingerprint by remember(lock.lockId, systemBioEnabled, fingerprintFirstWithPinFallback) {
        mutableStateOf(false)
    }

    LaunchedEffect(lock.lockId, systemBioEnabled, personalFaceEnabled, biometricExpiryDelayMs) {
        coordinator.beginUnlockSession(lock.lockId)
        if (systemBioEnabled || personalFaceEnabled) {
            if (biometricExpiryDelayMs > 0L) {
                delay(biometricExpiryDelayMs)
            }
            coordinator.notifyBiometricExpiryElapsed()
        }
    }

    LaunchedEffect(coordinatorState.focusAppInputNonce) {
        if (coordinatorState.focusAppInputNonce > 0 && mode != PasswordLockMode.PATTERN && !lock.lockedOut) {
            try {
                passwordFocusRequester.requestFocus()
            } catch (_: Throwable) {
            }
        }
    }

    val launchSystemCredential = {
        if (activity != null) {
            if (onTriggerPhoneCredential != null) {
                coordinator.notifyBiometricPromptStarted(UnifiedUnlockMethod.DEVICE_CREDENTIAL)
                onTriggerPhoneCredential(activity) { err ->
                    if (!err.isNullOrBlank()) {
                        fallbackNotice = err
                        coordinator.notifyBiometricFailedOrDismissed(err)
                    } else {
                        coordinator.markUnlocked()
                    }
                }
            } else {
                coordinator.authenticateSystemUnlock(
                    activity = activity,
                    onUnlocked = { onBiometricRecoverySuccess() },
                    onFallbackNeeded = { reason -> fallbackNotice = reason }
                )
            }
        } else {
            val msg = "Không khả dụng mã khóa hệ thống trên ngữ cảnh này."
            fallbackNotice = msg
            coordinator.notifyBiometricFailedOrDismissed(msg)
        }
    }

    val launchFingerprintFirst = {
        if (activity != null) {
            if (onTriggerFingerprintFirst != null) {
                coordinator.notifyBiometricPromptStarted(UnifiedUnlockMethod.FINGERPRINT_CRYPTO)
                onTriggerFingerprintFirst(activity) { outcome ->
                    when (outcome) {
                        is FingerprintFirstOutcome.CryptoSucceeded,
                        is FingerprintFirstOutcome.SystemFaceOrCredentialSucceeded -> {
                            fallbackNotice = null
                            errorMsg = null
                            coordinator.markUnlocked()
                            onBiometricRecoverySuccess()
                        }
                        is FingerprintFirstOutcome.SwitchMethodRequested -> {
                            fallbackNotice = outcome.reason
                            coordinator.notifyBiometricFailedOrDismissed(outcome.reason)
                        }
                        is FingerprintFirstOutcome.KeyInvalidated -> {
                            errorMsg = outcome.message
                            coordinator.notifyBiometricFailedOrDismissed(outcome.message)
                        }
                    }
                }
            } else if (onTriggerDeviceBiometric != null) {
                coordinator.notifyBiometricPromptStarted(UnifiedUnlockMethod.SYSTEM_BIOMETRIC)
                onTriggerDeviceBiometric(activity) { err ->
                    if (!err.isNullOrBlank()) {
                        errorMsg = err
                        coordinator.notifyBiometricFailedOrDismissed(err)
                    }
                }
            } else {
                coordinator.authenticateFingerprintFirst(
                    activity = activity,
                    onUnlocked = { onBiometricRecoverySuccess() },
                    onFallbackNeeded = { reason -> fallbackNotice = reason }
                )
            }
        } else {
            val msg = "Không khả dụng cảm biến vân tay trên ngữ cảnh này."
            errorMsg = msg
            coordinator.notifyBiometricFailedOrDismissed(msg)
        }
    }

    LaunchedEffect(lock.lockId, systemBioEnabled, fingerprintFirstWithPinFallback) {
        if (!hasAutoPromptedFingerprint &&
            activity != null &&
            systemBioEnabled &&
            fingerprintFirstWithPinFallback &&
            !lock.lockedOut
        ) {
            hasAutoPromptedFingerprint = true
            launchFingerprintFirst()
        }
    }

    val remainingText = if (lock.unlimitedAttempts) {
        "Không giới hạn số lần thử"
    } else {
        val rem = (lock.maxAttempts - lock.failedAttempts).coerceAtLeast(0L)
        "Còn $rem / ${lock.maxAttempts} lần thử"
    }

    val displayedNotice = fallbackNotice ?: coordinatorState.statusNotice

    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = CyanBright, modifier = Modifier.size(38.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(
            if (lock.lockedOut) {
                if (systemBioEnabled || personalFaceEnabled) {
                    "Đã vượt quá số lần cho phép nhập sai. Xác thực khuôn mặt, vân tay hoặc PIN thiết bị để khôi phục."
                } else {
                    "Đã vượt quá số lần cho phép nhập sai."
                }
            } else {
                "Kiểu khóa: ${mode.displayLabel} • $remainingText"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (lock.lockedOut) CoralError else CyanBright
        )

        if (!displayedNotice.isNullOrBlank()) {
            Surface(
                color = SlateElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unlock_fallback_notice")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayedNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarn
                    )
                    OutlinedButton(
                        onClick = { coordinator.openOptionsSheet() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_switch_method_button")
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = CyanBright)
                        Spacer(Modifier.width(8.dp))
                        Text("Chuyển đổi phương thức")
                    }
                }
            }
        }

        if (!lock.lockedOut) {
            if (mode == PasswordLockMode.PATTERN) {
                val safeGrid = lock.patternGridSize.coerceIn(3, 10)
                PatternLockGrid(
                    gridSize = safeGrid,
                    selectedNodes = patternNodes,
                    onNodesChanged = { patternNodes = it; errorMsg = null },
                    onPatternComplete = { completed ->
                        if (completed.size >= 4) {
                            if (onVerifyPattern(completed)) {
                                errorMsg = null
                                coordinator.markUnlocked()
                            } else {
                                patternNodes = emptyList()
                                errorMsg = "Hình vẽ chưa đúng."
                            }
                        }
                    }
                )
            } else {
                VisualPasswordInputIndicator(
                    value = textInput,
                    revealed = showPassword,
                    isValid = if (mode == PasswordLockMode.CUSTOM) textInput.isNotEmpty() else textInput.length >= 4
                )
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it; errorMsg = null },
                    label = { Text("Nhập mật khẩu (${mode.displayLabel})") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (mode == PasswordLockMode.DIGITS) KeyboardType.NumberPassword else KeyboardType.Text
                    ),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        if (mode.supportsVisibilityToggle) {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.testTag("unlock_visibility_toggle")
                            ) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Hiện/ẩn mật khẩu"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .testTag("unlock_password_input")
                )
            }
        }

        if (!errorMsg.isNullOrBlank()) {
            Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = CoralError)
        }

        if (!lock.lockedOut) {
            Button(
                onClick = {
                    val ok = if (mode == PasswordLockMode.PATTERN) {
                        onVerifyPattern(patternNodes)
                    } else {
                        onVerifyText(textInput)
                    }
                    if (ok) {
                        coordinator.markUnlocked()
                    } else {
                        textInput = ""
                        patternNodes = emptyList()
                        errorMsg = "Mật khẩu không chính xác."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_ok_button")
            ) {
                Text("OK")
            }
        }

        AnimatedVisibility(
            visible = (systemBioEnabled || personalFaceEnabled) &&
                coordinatorState.showOtherOptionsButton &&
                enabledSheetOptions.isNotEmpty()
        ) {
            Button(
                onClick = { coordinator.openOptionsSheet() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanContainer,
                    contentColor = CyanBright
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_other_options_button")
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tùy chọn đăng nhập khác")
            }
        }

        if (personalFaceEnabled) {
            OutlinedButton(
                onClick = { showVerifyFaceDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_personal_face_button")
            ) {
                Icon(Icons.Rounded.Face, contentDescription = null, tint = CyanBright)
                Spacer(Modifier.width(8.dp))
                Text("Mở khóa Khuôn mặt cá nhân (Trong app)")
            }
        }

        if (systemBioEnabled) {
            OutlinedButton(
                onClick = { launchFingerprintFirst() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_biometric_button")
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = CyanBright)
                Spacer(Modifier.width(8.dp))
                Text("Quét vân tay màn hình (CryptoObject)")
            }

            OutlinedButton(
                onClick = {
                    if (activity != null) {
                        if (onTriggerPhoneFace != null) {
                            coordinator.notifyBiometricPromptStarted(UnifiedUnlockMethod.SYSTEM_FACE)
                            onTriggerPhoneFace(activity) { err ->
                                if (!err.isNullOrBlank()) {
                                    fallbackNotice = err
                                    coordinator.notifyBiometricFailedOrDismissed(err)
                                } else {
                                    coordinator.markUnlocked()
                                }
                            }
                        } else {
                            coordinator.authenticatePhoneFaceOrBiometric(
                                activity = activity,
                                onUnlocked = { onBiometricRecoverySuccess() },
                                onFallbackNeeded = { reason -> fallbackNotice = reason }
                            )
                        }
                    } else {
                        val msg = "Không khả dụng sinh trắc học khuôn mặt điện thoại."
                        fallbackNotice = msg
                        coordinator.notifyBiometricFailedOrDismissed(msg)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_phone_face_button")
            ) {
                Icon(Icons.Rounded.Face, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Khuôn mặt / Sinh trắc học điện thoại")
            }

            OutlinedButton(
                onClick = { launchSystemCredential() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_phone_credential_button")
            ) {
                Icon(Icons.Rounded.Pin, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mã PIN / Hình vẽ của điện thoại")
            }
        } else if (!personalFaceEnabled) {
            Text(
                text = "Sinh trắc học điện thoại đang tách biệt khỏi khóa ứng dụng",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.testTag("unlock_biometric_isolated_note")
            )
        }

        if (allowCancel) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_cancel_button")
            ) {
                Text("Hủy / Quay lại")
            }
        }
    }

    if (coordinatorState.isOptionsSheetOpen && enabledSheetOptions.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { coordinator.closeOptionsSheet() },
            sheetState = sheetState,
            containerColor = SlateCard,
            contentColor = TextPrimary,
            modifier = Modifier.testTag("other_login_options_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = CyanBright)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tùy chọn đăng nhập khác",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Chọn nhanh phương thức xác thực thay thế đang được bật để mở khóa mà không bị gián đoạn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                HorizontalDivider(color = SlateBorder)

                enabledSheetOptions.forEach { option ->
                    Card(
                        onClick = {
                            coordinator.closeOptionsSheet()
                            when (option) {
                                UnifiedLoginOption.SYSTEM_UNLOCK -> launchSystemCredential()
                                UnifiedLoginOption.APP_CUSTOM_PIN_PATTERN -> {
                                    val msg = "Nhập mã PIN, mật khẩu hoặc vẽ mẫu hình riêng của ứng dụng."
                                    fallbackNotice = msg
                                    coordinator.switchToAppPinOrPattern(msg)
                                }
                                UnifiedLoginOption.PERSONAL_FACE -> showVerifyFaceDialog = true
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = SlateElevated),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(option.testTag)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (option) {
                                    UnifiedLoginOption.SYSTEM_UNLOCK -> Icons.Rounded.PhoneAndroid
                                    UnifiedLoginOption.APP_CUSTOM_PIN_PATTERN -> Icons.Rounded.Pin
                                    UnifiedLoginOption.PERSONAL_FACE -> Icons.Rounded.Face
                                },
                                contentDescription = null,
                                tint = CyanBright
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showVerifyFaceDialog) {
        PersonalFaceCameraDialog(
            mode = PersonalFaceScanMode.VERIFY,
            title = "Xác thực Khuôn mặt cá nhân",
            onVerifyCandidate = onVerifyPersonalFace,
            onVerifiedSuccess = {
                showVerifyFaceDialog = false
                errorMsg = null
                coordinator.markUnlocked()
            },
            onSwitchToAlternativeMethod = {
                showVerifyFaceDialog = false
                val msg = "Đã chuyển từ khuôn mặt cá nhân — chọn phương thức mở khóa thay thế."
                fallbackNotice = msg
                coordinator.notifyBiometricFailedOrDismissed(msg)
                coordinator.openOptionsSheet()
            },
            onVerificationFailedOrDismissed = { reason ->
                fallbackNotice = reason
                coordinator.notifyBiometricFailedOrDismissed(reason)
            },
            onDismiss = { showVerifyFaceDialog = false }
        )
    }
}

@Composable
fun AppPasswordLockGateScreen(
    lock: SecurityLockEntity,
    onVerifyText: (String) -> Boolean,
    onVerifyPattern: (List<Int>) -> Boolean,
    onBiometricRecoverySuccess: () -> Unit,
    hasPersonalFaceEnrolled: Boolean = false,
    allowDeviceBiometric: Boolean = true,
    onVerifyPersonalFace: (FloatArray) -> Boolean = { false },
    onTriggerDeviceBiometric: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    biometricAuthMode: BiometricAuthMode? = null,
    fingerprintFirstWithPinFallback: Boolean = true,
    onTriggerFingerprintFirst: ((FragmentActivity, (FingerprintFirstOutcome) -> Unit) -> Unit)? = null,
    onTriggerPhoneFace: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    onTriggerPhoneCredential: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    unifiedUnlockManager: UnifiedUnlockManager? = null,
    biometricExpiryDelayMs: Long = UnifiedUnlockManager.DEFAULT_BIOMETRIC_EXPIRY_MS
) {
    Surface(
        color = SlateDeep,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    PasswordUnlockGateContent(
                        lock = lock,
                        title = "Khiên bảo vệ Super Smart AI",
                        allowCancel = false,
                        onVerifyText = onVerifyText,
                        onVerifyPattern = onVerifyPattern,
                        onBiometricRecoverySuccess = onBiometricRecoverySuccess,
                        onCancel = {},
                        hasPersonalFaceEnrolled = hasPersonalFaceEnrolled,
                        allowDeviceBiometric = allowDeviceBiometric,
                        onVerifyPersonalFace = onVerifyPersonalFace,
                        onTriggerDeviceBiometric = onTriggerDeviceBiometric,
                        biometricAuthMode = biometricAuthMode,
                        fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                        onTriggerFingerprintFirst = onTriggerFingerprintFirst,
                        onTriggerPhoneFace = onTriggerPhoneFace,
                        onTriggerPhoneCredential = onTriggerPhoneCredential,
                        unifiedUnlockManager = unifiedUnlockManager,
                        biometricExpiryDelayMs = biometricExpiryDelayMs
                    )
                }
            }
        }
    }
}

@Composable
fun ChatLockUnlockDialog(
    lock: SecurityLockEntity,
    sessionTitle: String,
    onVerifyText: (String) -> Boolean,
    onVerifyPattern: (List<Int>) -> Boolean,
    onBiometricRecoverySuccess: () -> Unit,
    onDismiss: () -> Unit,
    hasPersonalFaceEnrolled: Boolean = false,
    allowDeviceBiometric: Boolean = true,
    onVerifyPersonalFace: (FloatArray) -> Boolean = { false },
    onTriggerDeviceBiometric: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    biometricAuthMode: BiometricAuthMode? = null,
    fingerprintFirstWithPinFallback: Boolean = true,
    onTriggerFingerprintFirst: ((FragmentActivity, (FingerprintFirstOutcome) -> Unit) -> Unit)? = null,
    onTriggerPhoneFace: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    onTriggerPhoneCredential: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    unifiedUnlockManager: UnifiedUnlockManager? = null,
    biometricExpiryDelayMs: Long = UnifiedUnlockManager.DEFAULT_BIOMETRIC_EXPIRY_MS
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = null,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PasswordUnlockGateContent(
                    lock = lock,
                    title = "Mở khóa: $sessionTitle",
                    allowCancel = true,
                    onVerifyText = onVerifyText,
                    onVerifyPattern = onVerifyPattern,
                    onBiometricRecoverySuccess = onBiometricRecoverySuccess,
                    onCancel = onDismiss,
                    hasPersonalFaceEnrolled = hasPersonalFaceEnrolled,
                    allowDeviceBiometric = allowDeviceBiometric,
                    onVerifyPersonalFace = onVerifyPersonalFace,
                    onTriggerDeviceBiometric = onTriggerDeviceBiometric,
                    biometricAuthMode = biometricAuthMode,
                    fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                    onTriggerFingerprintFirst = onTriggerFingerprintFirst,
                    onTriggerPhoneFace = onTriggerPhoneFace,
                    onTriggerPhoneCredential = onTriggerPhoneCredential,
                    unifiedUnlockManager = unifiedUnlockManager,
                    biometricExpiryDelayMs = biometricExpiryDelayMs
                )
            }
        },
        confirmButton = {}
    )
}

/**
 * Mandatory Password / Personal Face / Hardware Biometric authentication dialog shown
 * before turning OFF and deleting the active App Lock.
 */
@Composable
fun AppLockToggleAuthDialog(
    lock: SecurityLockEntity,
    hasPersonalFaceEnrolled: Boolean = false,
    allowDeviceBiometric: Boolean = false,
    onVerifyTextAndDisable: (String) -> Boolean,
    onVerifyPatternAndDisable: (List<Int>) -> Boolean,
    onVerifyPersonalFaceAndDisable: (FloatArray) -> Boolean = { false },
    onBiometricAuthAndDisable: () -> Unit,
    onDismiss: () -> Unit,
    biometricAuthMode: BiometricAuthMode? = null,
    fingerprintFirstWithPinFallback: Boolean = true,
    onTriggerFingerprintFirst: ((FragmentActivity, (FingerprintFirstOutcome) -> Unit) -> Unit)? = null,
    onTriggerPhoneFace: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    onTriggerPhoneCredential: ((FragmentActivity, (String?) -> Unit) -> Unit)? = null,
    unifiedUnlockManager: UnifiedUnlockManager? = null,
    biometricExpiryDelayMs: Long = UnifiedUnlockManager.DEFAULT_BIOMETRIC_EXPIRY_MS
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = null,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Xác thực mật khẩu hiện tại để tắt Khiên bảo vệ. Mật khẩu và khuôn mặt đã lưu của App Lock sẽ bị xóa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberWarn
                )
                Spacer(Modifier.height(10.dp))
                PasswordUnlockGateContent(
                    lock = lock,
                    title = "Tắt Khóa ứng dụng",
                    allowCancel = true,
                    onVerifyText = onVerifyTextAndDisable,
                    onVerifyPattern = onVerifyPatternAndDisable,
                    onBiometricRecoverySuccess = onBiometricAuthAndDisable,
                    onCancel = onDismiss,
                    hasPersonalFaceEnrolled = hasPersonalFaceEnrolled,
                    allowDeviceBiometric = allowDeviceBiometric,
                    onVerifyPersonalFace = onVerifyPersonalFaceAndDisable,
                    biometricAuthMode = biometricAuthMode,
                    fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                    onTriggerFingerprintFirst = onTriggerFingerprintFirst,
                    onTriggerPhoneFace = onTriggerPhoneFace,
                    onTriggerPhoneCredential = onTriggerPhoneCredential,
                    unifiedUnlockManager = unifiedUnlockManager,
                    biometricExpiryDelayMs = biometricExpiryDelayMs
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun VisualPasswordInputIndicator(
    value: String,
    revealed: Boolean,
    isValid: Boolean
) {
    Surface(
        color = SlateElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isValid) EmeraldOk.copy(alpha = 0.7f) else SlateBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("password_visual_feedback_bar")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (value.isEmpty()) {
                Text("Chưa nhập ký tự...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else if (revealed) {
                Text(value, style = MaterialTheme.typography.titleMedium, color = CyanBright)
            } else {
                value.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isValid) EmeraldOk else CyanBright)
                    )
                }
            }
        }
    }
}

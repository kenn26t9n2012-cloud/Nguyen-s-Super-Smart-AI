package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.PrivateFolderItem
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.UserAccountProfile
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppPreferences
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalVaultFileEntity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqArbiterSnapshot
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqRamArbiter
import com.nguyen_super_app.nguyen_super_smart_ai.engine.speech.SpeechEngineState
import com.nguyen_super_app.nguyen_super_smart_ai.engine.speech.SpeechSortOrder
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.WorldLanguagesCatalog
import com.nguyen_super_app.nguyen_super_smart_ai.security.ApiKeyValidationResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.KeyValidationState
import com.nguyen_super_app.nguyen_super_smart_ai.security.StoredSecretSummary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.auth.GoogleAuthHelper
import com.nguyen_super_app.nguyen_super_smart_ai.ui.components.LanguageSelector
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.VaultUnlockDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateSurface
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.viewmodel.VaultUnlockState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAndSettingsScreen(
    preferences: AppPreferences,
    isAuthenticatedCloud: Boolean,
    cloudProfile: UserAccountProfile?,
    cloudPrivateFolders: List<PrivateFolderItem>,
    localVaultFiles: List<LocalVaultFileEntity>,
    paqSnapshot: PaqArbiterSnapshot,
    speechState: SpeechEngineState,
    secretSummaries: List<StoredSecretSummary>,
    vaultUnlockState: VaultUnlockState,
    apiKeyValidations: Map<String, ApiKeyValidationResult>,
    onSelectAppLanguage: (String) -> Unit,
    onSelectSpeechLanguage: (String) -> Unit,
    onSetSpeechWorkers: (Int) -> Unit,
    onDownloadSpeechPack: (String) -> Unit,
    onDeleteSpeechPack: (String) -> Unit,
    onVerifyLocalPassphrase: (CharArray) -> Boolean,
    onSetLocalPassphrase: (CharArray) -> Boolean,
    onVaultUnlocked: (String) -> Unit,
    onLockVaultNow: () -> Unit,
    onValidateVaultSecret: (keyId: String) -> Unit,
    onSaveVaultSecret: (keyId: String, value: String) -> Unit,
    onRemoveVaultSecret: (keyId: String) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onBackupChatToCloud: () -> Unit,
    onDownloadCloudItem: (PrivateFolderItem) -> Unit,
    onDeleteCloudItem: (String) -> Unit,
    onDeleteLocalVaultFile: (String) -> Unit,
    onOpenAdvancedAiOptions: () -> Unit,
    onOpenSecurityLockHub: () -> Unit,
    onSignOutToGateway: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var editingKeyId by remember { mutableStateOf<String?>(null) }
    var secretInput by remember { mutableStateOf("") }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingUnlockedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var speechQuery by remember { mutableStateOf("") }
    var speechWorkers by remember(speechState.activeWorkers) { mutableFloatStateOf(speechState.activeWorkers.toFloat()) }

    fun requireVaultUnlock(action: () -> Unit) {
        if (vaultUnlockState.isUnlocked) {
            action()
        } else {
            pendingUnlockedAction = action
            showUnlockDialog = true
        }
    }

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Kho Riêng, Khóa AES & Cài đặt", style = MaterialTheme.typography.titleMedium) }
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
            // 1. Account & Biometric Lock
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tài khoản & Bảo mật thiết bị", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                    Text(
                        if (isAuthenticatedCloud) {
                            "Cloud Firestore: ${cloudProfile?.displayName ?: "Đã đăng nhập"} • Thư mục: ${cloudProfile?.folderKey ?: "users/<uid>"}"
                        } else {
                            "Đang ở Chế độ Cục bộ (Guest Mode - Room DB)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = CyanBright)
                        Spacer(Modifier.width(8.dp))
                        Text("Khóa sinh trắc học (BiometricPrompt)", modifier = Modifier.weight(1f))
                        Switch(checked = preferences.biometricLockEnabled, onCheckedChange = onSetBiometricEnabled)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenAdvancedAiOptions, modifier = Modifier.weight(1f)) {
                            Text("Tính năng AI nâng cao")
                        }
                        if (isAuthenticatedCloud) {
                            OutlinedButton(onClick = onBackupChatToCloud, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sao lưu PAQ")
                            }
                        }
                    }
                    Button(
                        onClick = onOpenSecurityLockHub,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_security_locks_button")
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mật khẩu cho cuộc trò chuyện/ứng dụng")
                    }
                    OutlinedButton(
                        onClick = {
                            GoogleAuthHelper.signOut(credentialManager, scope, onSignOutToGateway)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = CoralError)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAuthenticatedCloud) "Đăng xuất về Màn hình Đăng nhập" else "Mở Cổng Đăng nhập / Liên kết Cloud")
                    }
                }
            }

            // 2. AES-256-GCM Encrypted Secret Vault
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldOk)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keystore Vault (AES-256-GCM)", style = MaterialTheme.typography.titleMedium, color = EmeraldOk)
                            Text(
                                if (vaultUnlockState.isKeystoreHardwareBacked) {
                                    "AndroidKeyStore MasterKey + EncryptedSharedPreferences"
                                } else {
                                    "AES-256-GCM Vault (Chế độ tương thích phần mềm)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (vaultUnlockState.isUnlocked) {
                                    editingKeyId = null
                                    secretInput = ""
                                    onLockVaultNow()
                                } else {
                                    pendingUnlockedAction = null
                                    showUnlockDialog = true
                                }
                            },
                            modifier = Modifier.testTag("vault_lock_toggle_button")
                        ) {
                            Icon(
                                if (vaultUnlockState.isUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (vaultUnlockState.isUnlocked) EmeraldOk else CyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (vaultUnlockState.isUnlocked) "Khóa lại" else "Mở khóa")
                        }
                    }

                    Text(
                        if (vaultUnlockState.isUnlocked) {
                            "Đã mở khóa (${vaultUnlockState.unlockedMethod ?: "Xác thực"}) • Bạn có thể chỉnh sửa hoặc kiểm tra tính hợp lệ khóa API."
                        } else {
                            "Đang khóa bảo vệ: Yêu cầu Sinh trắc học, Mật khẩu hoặc OTP SĐT/Email trước khi giải mã hoặc kiểm tra khóa API."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (vaultUnlockState.isUnlocked) EmeraldOk else CyanBright
                    )

                    if (!vaultUnlockState.recoveryNotice.isNullOrBlank()) {
                        Text(
                            vaultUnlockState.recoveryNotice,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    secretSummaries.forEach { item ->
                        val valRes = apiKeyValidations[item.keyId]
                        Surface(
                            color = SlateElevated,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Key, contentDescription = null, tint = CyanBright, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.label, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            if (vaultUnlockState.isUnlocked || !item.isConfigured) item.maskedPreview else "•••••••• (Cần mở khóa để xem/kiểm tra)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.isConfigured) EmeraldOk else TextSecondary
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            requireVaultUnlock {
                                                editingKeyId = item.keyId
                                                secretInput = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("vault_edit_${item.keyId}")
                                    ) {
                                        Text(if (item.isConfigured) "Đổi" else "Nhập")
                                    }
                                }
                                if (item.isConfigured) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                requireVaultUnlock {
                                                    onValidateVaultSecret(item.keyId)
                                                }
                                            },
                                            enabled = valRes?.state != KeyValidationState.CHECKING,
                                            modifier = Modifier.testTag("vault_validate_${item.keyId}")
                                        ) {
                                            if (valRes?.state == KeyValidationState.CHECKING) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text("Kiểm tra hợp lệ")
                                        }
                                        IconButton(
                                            onClick = {
                                                requireVaultUnlock {
                                                    onRemoveVaultSecret(item.keyId)
                                                }
                                            },
                                            modifier = Modifier.testTag("vault_delete_${item.keyId}")
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Xóa khóa", tint = CoralError)
                                        }
                                    }
                                }
                                if (valRes != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val statusColor = when (valRes.state) {
                                            KeyValidationState.VALID -> EmeraldOk
                                            KeyValidationState.INVALID -> CoralError
                                            else -> CyanBright
                                        }
                                        Icon(
                                            if (valRes.state == KeyValidationState.VALID) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            valRes.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            modifier = Modifier.testTag("vault_validation_msg_${item.keyId}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (editingKeyId != null && vaultUnlockState.isUnlocked) {
                        OutlinedTextField(
                            value = secretInput,
                            onValueChange = { secretInput = it },
                            label = { Text("Giá trị khóa bí mật ($editingKeyId)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                onSaveVaultSecret(editingKeyId!!, secretInput)
                                editingKeyId = null
                                secretInput = ""
                            }) { Text("Lưu mã hóa") }
                            OutlinedButton(onClick = { editingKeyId = null }) { Text("Hủy") }
                        }
                    }
                }
            }

            // 3. LanguageSelector (100+ languages, Tiếng Việt prioritized) for Speech-to-Text & UI
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = CyanPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "LanguageSelector (100+ Ngôn ngữ STT & Giao diện)",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyanBright
                        )
                    }
                    Text(
                        "Ngôn ngữ STT hiện tại: ${WorldLanguagesCatalog.findByCode(preferences.speechLanguageCode).nativeName} • Giao diện: ${WorldLanguagesCatalog.findByCode(preferences.languageCode).nativeName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldOk
                    )
                    LanguageSelector(
                        selectedCode = preferences.speechLanguageCode,
                        isSpeechMode = true,
                        onSelect = { lang ->
                            onSelectSpeechLanguage(lang.code)
                            onSelectAppLanguage(lang.code)
                        }
                    )
                }
            }

            // 4. Offline Vosk / Whisper Multi-Worker Manager
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Mic, contentDescription = null, tint = CyanBright)
                        Spacer(Modifier.width(8.dp))
                        Text("Gói Giọng nói Ngoại tuyến (Vosk & Whisper.cpp)", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Số luồng DSP Multi-Worker: ${speechWorkers.toInt()}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = speechWorkers,
                        onValueChange = {
                            speechWorkers = it
                            onSetSpeechWorkers(it.toInt())
                        },
                        valueRange = 1f..6f
                    )
                    OutlinedTextField(
                        value = speechQuery,
                        onValueChange = { speechQuery = it },
                        placeholder = { Text("Lọc gói giọng nói (vi, en, whisper, vosk...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    speechState.packs.filter {
                        speechQuery.isBlank() || it.name.contains(speechQuery, ignoreCase = true) || it.languageCode.contains(speechQuery, ignoreCase = true)
                    }.sortedWith(
                        compareByDescending<com.nguyen_super_app.nguyen_super_smart_ai.engine.speech.SpeechPackModel> { it.languageCode == "vi" }
                            .thenBy { it.name }
                    ).take(8).forEach { pack ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${pack.name} (${pack.sizeMb} MB)", style = MaterialTheme.typography.bodySmall)
                                Text("${pack.engineKind.label} • ${if (pack.isInstalled) "Đã cài" else "Chưa tải"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            if (!pack.isInstalled || pack.hasUpdate) {
                                OutlinedButton(onClick = { onDownloadSpeechPack(pack.id) }) {
                                    Text(if (pack.hasUpdate) "Cập nhật" else "Tải")
                                }
                            } else if (!pack.isBundled) {
                                IconButton(onClick = { onDeleteSpeechPack(pack.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Xóa gói", tint = CoralError)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Private Folder Vault & PAQ RAM Arbiter
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = CyanBright)
                        Spacer(Modifier.width(8.dp))
                        Text("Thư mục Riêng & PAQ RAM Arbiter", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("PAQ Arbiter: ${paqSnapshot.state.name} • ${paqSnapshot.reason}", style = MaterialTheme.typography.bodySmall, color = CyanBright)
                    OutlinedButton(onClick = { PaqRamArbiter.setUserPaused(paqSnapshot.state.name != "PAUSED_BY_USER") }) {
                        Text(if (paqSnapshot.state.name == "PAUSED_BY_USER") "Tiếp tục tiến trình PAQ" else "Tạm dừng PAQ")
                    }

                    HorizontalDivider(color = SlateBorder)
                    cloudPrivateFolders.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.fileName, style = MaterialTheme.typography.bodySmall)
                                Text("Cloud [${item.compressionMethod}] • ${item.compressedSizeBytes}B", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            IconButton(onClick = { onDownloadCloudItem(item) }) {
                                Icon(Icons.Rounded.Download, contentDescription = "Tải & Giải nén", tint = CyanBright)
                            }
                            IconButton(onClick = { onDeleteCloudItem(item.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Xóa", tint = CoralError)
                            }
                        }
                    }
                    localVaultFiles.forEach { lf ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lf.fileName, style = MaterialTheme.typography.bodySmall)
                                Text("Local [${lf.compressionMethod}] • ${lf.compressedSizeBytes}B", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            IconButton(onClick = { onDeleteLocalVaultFile(lf.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Xóa", tint = CoralError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUnlockDialog) {
        VaultUnlockDialog(
            hasLocalVaultPassphrase = vaultUnlockState.hasLocalPassphrase,
            defaultEmail = cloudProfile?.recoveryEmail,
            defaultPhone = cloudProfile?.recoveryPhone,
            onVerifyLocalPassphrase = onVerifyLocalPassphrase,
            onSetLocalPassphrase = onSetLocalPassphrase,
            onUnlocked = { methodLabel ->
                onVaultUnlocked(methodLabel)
                showUnlockDialog = false
                pendingUnlockedAction?.invoke()
                pendingUnlockedAction = null
            },
            onDismiss = {
                showUnlockDialog = false
                pendingUnlockedAction = null
            }
        )
    }
}

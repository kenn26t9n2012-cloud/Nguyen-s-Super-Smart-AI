package com.nguyen_super_app.nguyen_super_smart_ai.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.nguyen_super_app.nguyen_super_smart_ai.data.ai.AiStreamChunk
import com.nguyen_super_app.nguyen_super_smart_ai.data.ai.GeminiCloudRepository
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.CloudPromptPersona
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.FirestoreRepository
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.PrivateFolderItem
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.UserAccountProfile
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AdvancedAiFeaturesConfig
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppDatabase
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppPreferences
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BiometricAuthMode
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BuildQueueJobEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatMessageEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatSessionEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ExportJobEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalAiModelEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalRepository
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalVaultFileEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.PromptTemplateEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.SecurityLockEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.UserPreferencesStore
import com.nguyen_super_app.nguyen_super_smart_ai.engine.ai.CapabilityGateDecision
import com.nguyen_super_app.nguyen_super_smart_ai.engine.ai.IntentCapabilityGate
import com.nguyen_super_app.nguyen_super_smart_ai.engine.ai.LiveWebSearchClient
import com.nguyen_super_app.nguyen_super_smart_ai.engine.browser.BrowserDownloadState
import com.nguyen_super_app.nguyen_super_smart_ai.engine.browser.ModelDownloadPipeline
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.CompressionFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.MultiFormatCompressionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqArbiterSnapshot
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqRamArbiter
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.BatchExportEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.BatchExportResult
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.DocumentExportEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.ExportFormatSpec
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.GeneratedExportPackage
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.DecodedLogoResult
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.LogoDecoderEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.MediaProcessingEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.PhotoEditParams
import com.nguyen_super_app.nguyen_super_smart_ai.engine.hardware.BenchmarkEntry
import com.nguyen_super_app.nguyen_super_smart_ai.engine.hardware.HardwareHudSnapshot
import com.nguyen_super_app.nguyen_super_smart_ai.engine.hardware.HardwareMonitoringSuite
import com.nguyen_super_app.nguyen_super_smart_ai.engine.hardware.ThermalSeverity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.AiModelFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.GgufLifecycleManager
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.GgufModelState
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.GgufQuantization
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.CodeRunnerSandbox
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.IpynbExecutionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.IpynbExecutionReport
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.PromptContextHook
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.SandboxExecutionResult
import com.nguyen_super_app.nguyen_super_smart_ai.engine.security.DigitalSignatureEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.security.SignatureAlgorithm
import com.nguyen_super_app.nguyen_super_smart_ai.engine.security.SignatureBundle
import com.nguyen_super_app.nguyen_super_smart_ai.engine.speech.OfflineSpeechEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.speech.SpeechEngineState
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.BinaryDecompileReport
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.BinaryDecompilerEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.PersistentBuildQueueManager
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.ToolchainAutoUpdater
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.ToolchainDescriptor
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.LocalizationStringsProvider
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.LocalizedUiStrings
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.fragment.app.FragmentActivity
import com.nguyen_super_app.nguyen_super_smart_ai.security.ApiKeyHealthValidator
import com.nguyen_super_app.nguyen_super_smart_ai.security.ApiKeyValidationResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.AppLockAuthenticator
import com.nguyen_super_app.nguyen_super_smart_ai.security.AppLockManager
import com.nguyen_super_app.nguyen_super_smart_ai.security.BiometricAuthResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.EncryptedSecretVault
import com.nguyen_super_app.nguyen_super_smart_ai.security.FingerprintFirstOutcome
import com.nguyen_super_app.nguyen_super_smart_ai.security.KeyValidationState
import com.nguyen_super_app.nguyen_super_smart_ai.security.LockAttemptResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.PasswordLockMode
import com.nguyen_super_app.nguyen_super_smart_ai.security.PersonalFaceAuthenticator
import com.nguyen_super_app.nguyen_super_smart_ai.security.StoredSecretSummary
import com.nguyen_super_app.nguyen_super_smart_ai.security.UnifiedUnlockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class PendingAttachment(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val mimeType: String,
    val rawBytes: ByteArray,
    val previewBitmap: Bitmap? = null,
    val compressionFormat: CompressionFormat = CompressionFormat.NONE
)

data class PhotoEditorState(
    val originalBitmap: Bitmap? = null,
    val editedBitmap: Bitmap? = null,
    val params: PhotoEditParams = PhotoEditParams(),
    val statusNote: String = "Chọn ảnh từ Photo Picker để chỉnh sửa hoặc thêm watermark."
)

data class VaultUnlockState(
    val isUnlocked: Boolean = false,
    val unlockedUntilMillis: Long = 0L,
    val unlockedMethod: String? = null,
    val hasLocalPassphrase: Boolean = false,
    val isKeystoreHardwareBacked: Boolean = false,
    val recoveryNotice: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SuperSmartViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val localRepo = LocalRepository(db)
    private val prefsStore = UserPreferencesStore(appContext)
    private val geminiRepo = GeminiCloudRepository()
    private val secretVault = EncryptedSecretVault(appContext)
    private val hardwareSuite = HardwareMonitoringSuite(appContext)
    private val appLockAuthenticator = AppLockAuthenticator(appContext)
    private val personalFaceAuth = PersonalFaceAuthenticator(appContext)
    val unifiedUnlockManager = UnifiedUnlockManager(
        appLockAuthenticator = appLockAuthenticator,
        personalFaceAuthenticator = personalFaceAuth
    )

    private val firestoreRepo: FirestoreRepository? by lazy {
        try {
            FirestoreRepository(appContext)
        } catch (e: Exception) {
            Log.w("SuperSmartVM", "Firestore unavailable: ${e.message}")
            null
        }
    }

    val ggufManager = GgufLifecycleManager(appContext)
    val speechEngine = OfflineSpeechEngine(appContext)
    val modelDownloadPipeline = ModelDownloadPipeline(
        context = appContext,
        authTokenProvider = {
            var token: String? = null
            secretVault.withDecryptedSecret(EncryptedSecretVault.KEY_HUGGINGFACE_TOKEN) { chars ->
                if (chars != null) token = String(chars)
            }
            token
        },
        onModelsRegistered = { models, sourceUrl ->
            models.forEach { (file, report) ->
                val fmt = report.format ?: AiModelFormat.GGUF
                val entity = LocalAiModelEntity(
                    id = "mdl_${UUID.randomUUID().toString().take(8)}",
                    name = file.nameWithoutExtension.take(48),
                    formatId = fmt.id,
                    fileName = file.name,
                    localPath = file.absolutePath,
                    sizeBytes = file.length(),
                    parameterBillions = 2.0f,
                    quantizationId = ggufManager.state.value.quantization.id,
                    sourceUrl = sourceUrl,
                    magicSignatureSummary = report.signatureDetails,
                    isVisibleInList = true,
                    isInTrash = false,
                    addedAtMillis = System.currentTimeMillis()
                )
                localRepo.upsertLocalAiModel(entity)
            }
        }
    )

    val preferences: StateFlow<AppPreferences> = prefsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    private val _isAuthenticatedCloud = MutableStateFlow(Firebase.auth.currentUser != null)
    val isAuthenticatedCloud: StateFlow<Boolean> = _isAuthenticatedCloud.asStateFlow()

    private val _hasEnteredWorkspace = MutableStateFlow(Firebase.auth.currentUser != null)
    val hasEnteredWorkspace: StateFlow<Boolean> = _hasEnteredWorkspace.asStateFlow()

    private val _currentSessionId = MutableStateFlow("session_default")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = localRepo.sessions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { id -> localRepo.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val promptTemplates: StateFlow<List<PromptTemplateEntity>> = localRepo.templates
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val exportJobs: StateFlow<List<ExportJobEntity>> = localRepo.exportJobs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val localVaultFiles: StateFlow<List<LocalVaultFileEntity>> = localRepo.localVaultFiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val localAiModels: StateFlow<List<LocalAiModelEntity>> = localRepo.localAiModels
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val buildQueueJobs: StateFlow<List<BuildQueueJobEntity>> = localRepo.buildQueueJobs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appLockManager = AppLockManager(
        localRepo = localRepo,
        prefsStore = prefsStore,
        firestoreRepoProvider = { firestoreRepo },
        isCloudSyncEnabled = { Firebase.auth.currentUser != null && !preferences.value.isGuestMode },
        authenticator = appLockAuthenticator,
        personalFaceAuth = personalFaceAuth,
        scope = viewModelScope
    )

    val securityLocks: StateFlow<List<SecurityLockEntity>> = appLockManager.securityLocks

    val allMessages: StateFlow<List<ChatMessageEntity>> = localRepo.allMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appLockUnlocked: StateFlow<Boolean> = appLockManager.appLockUnlocked
    val unlockedChatIds: StateFlow<Set<String>> = appLockManager.unlockedChatIds
    val autoLockTimeoutMillis: StateFlow<Long> = appLockManager.autoLockTimeoutMillis
    val allowDeviceBiometrics: StateFlow<Boolean> = appLockManager.allowDeviceBiometrics
    val biometricAuthMode: StateFlow<BiometricAuthMode> = appLockManager.biometricAuthMode
    val fingerprintFirstWithPinFallback: StateFlow<Boolean> = appLockManager.fingerprintFirstWithPinFallback
    val enrolledPersonalFaceTargets: StateFlow<Set<String>> = appLockManager.enrolledPersonalFaceTargets

    val pendingUnlockChatSession: StateFlow<ChatSessionEntity?> = combine(
        appLockManager.pendingUnlockChatId,
        sessions,
        securityLocks
    ) { pendingId, allSessions, allLocks ->
        pendingId?.let { id ->
            allSessions.firstOrNull { it.id == id }
                ?: allLocks.firstOrNull { it.targetType == AppLockManager.TARGET_CHAT && it.targetId == id }?.let { lock ->
                    ChatSessionEntity(
                        id = id,
                        title = lock.targetLabel,
                        modelLabel = currentModelDisplayLabel(),
                        activePersonaName = activePersonaTitle(),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                }
                ?: ChatSessionEntity(
                    id = id,
                    title = "Cuộc trò chuyện",
                    modelLabel = currentModelDisplayLabel(),
                    activePersonaName = activePersonaTitle(),
                    updatedAtMillis = System.currentTimeMillis()
                )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _cloudProfile = MutableStateFlow<UserAccountProfile?>(null)
    val cloudProfile: StateFlow<UserAccountProfile?> = _cloudProfile.asStateFlow()

    private val _cloudPrivateFolders = MutableStateFlow<List<PrivateFolderItem>>(emptyList())
    val cloudPrivateFolders: StateFlow<List<PrivateFolderItem>> = _cloudPrivateFolders.asStateFlow()

    private val _cloudPrompts = MutableStateFlow<List<CloudPromptPersona>>(emptyList())
    val cloudPrompts: StateFlow<List<CloudPromptPersona>> = _cloudPrompts.asStateFlow()

    val ggufState: StateFlow<GgufModelState> = ggufManager.state
    val speechState: StateFlow<SpeechEngineState> = speechEngine.state
    val paqSnapshot: StateFlow<PaqArbiterSnapshot> = PaqRamArbiter.snapshot
    val browserDownloadState: StateFlow<BrowserDownloadState> = modelDownloadPipeline.state
    val hardwareHudSnapshot: StateFlow<HardwareHudSnapshot> = hardwareSuite.hudSnapshot
    val toolchains: StateFlow<List<ToolchainDescriptor>> = ToolchainAutoUpdater.toolchains

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingAiText = MutableStateFlow("")
    val streamingAiText: StateFlow<String> = _streamingAiText.asStateFlow()

    /**
     * STRICT REASONING VISIBILITY STATE:
     * Never defaults to true and NEVER auto-opens when a message streams or finishes.
     * Mutated ONLY when user clicks the explicit "Mở suy luận" / "Ẩn suy luận" button!
     */
    private val _reasoningExpandedMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val reasoningExpandedMap: StateFlow<Map<String, Boolean>> = _reasoningExpandedMap.asStateFlow()

    private val _activePersonaId = MutableStateFlow("persona_cto")
    val activePersonaId: StateFlow<String> = _activePersonaId.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<PendingAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<PendingAttachment>> = _pendingAttachments.asStateFlow()

    private val _showCompressionPickerFor = MutableStateFlow<PendingAttachment?>(null)
    val showCompressionPickerFor: StateFlow<PendingAttachment?> = _showCompressionPickerFor.asStateFlow()

    private val _activeExportPreview = MutableStateFlow<GeneratedExportPackage?>(null)
    val activeExportPreview: StateFlow<GeneratedExportPackage?> = _activeExportPreview.asStateFlow()

    private val _batchExportResult = MutableStateFlow<BatchExportResult?>(null)
    val batchExportResult: StateFlow<BatchExportResult?> = _batchExportResult.asStateFlow()

    private val _showAdvancedAiDialog = MutableStateFlow(false)
    val showAdvancedAiDialog: StateFlow<Boolean> = _showAdvancedAiDialog.asStateFlow()

    private val _showLanguageSelectorDialog = MutableStateFlow(false)
    val showLanguageSelectorDialog: StateFlow<Boolean> = _showLanguageSelectorDialog.asStateFlow()

    private val _languageSelectorIsSpeech = MutableStateFlow(true)
    val languageSelectorIsSpeech: StateFlow<Boolean> = _languageSelectorIsSpeech.asStateFlow()

    private val _sandboxResults = MutableStateFlow<Map<String, SandboxExecutionResult>>(emptyMap())
    val sandboxResults: StateFlow<Map<String, SandboxExecutionResult>> = _sandboxResults.asStateFlow()

    private val _decompilerReport = MutableStateFlow<BinaryDecompileReport?>(null)
    val decompilerReport: StateFlow<BinaryDecompileReport?> = _decompilerReport.asStateFlow()

    private val _ipynbReport = MutableStateFlow<IpynbExecutionReport?>(null)
    val ipynbReport: StateFlow<IpynbExecutionReport?> = _ipynbReport.asStateFlow()

    private val _decodedLogoResult = MutableStateFlow<DecodedLogoResult?>(null)
    val decodedLogoResult: StateFlow<DecodedLogoResult?> = _decodedLogoResult.asStateFlow()

    private val _signatureBundle = MutableStateFlow<SignatureBundle?>(null)
    val signatureBundle: StateFlow<SignatureBundle?> = _signatureBundle.asStateFlow()

    private val _signatureVerified = MutableStateFlow<Boolean?>(null)
    val signatureVerified: StateFlow<Boolean?> = _signatureVerified.asStateFlow()

    private val _photoEditorState = MutableStateFlow(PhotoEditorState())
    val photoEditorState: StateFlow<PhotoEditorState> = _photoEditorState.asStateFlow()

    private val _secretSummaries = MutableStateFlow<List<StoredSecretSummary>>(secretVault.listSummaries())
    val secretSummaries: StateFlow<List<StoredSecretSummary>> = _secretSummaries.asStateFlow()

    private val _vaultUnlockState = MutableStateFlow(
        VaultUnlockState(
            isUnlocked = false,
            hasLocalPassphrase = secretVault.storageManager.hasLocalVaultPassphrase(),
            isKeystoreHardwareBacked = secretVault.isKeystoreHardwareBacked,
            recoveryNotice = secretVault.storageManager.lastRecoveryReason
        )
    )
    val vaultUnlockState: StateFlow<VaultUnlockState> = _vaultUnlockState.asStateFlow()

    private val _apiKeyValidations = MutableStateFlow<Map<String, ApiKeyValidationResult>>(emptyMap())
    val apiKeyValidations: StateFlow<Map<String, ApiKeyValidationResult>> = _apiKeyValidations.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val _benchmarkEntries = MutableStateFlow<List<BenchmarkEntry>>(emptyList())
    val benchmarkEntries: StateFlow<List<BenchmarkEntry>> = _benchmarkEntries.asStateFlow()

    init {
        viewModelScope.launch {
            localRepo.seedDefaultPromptsIfEmpty()
            ensureDefaultSessionExists()
            if (Firebase.auth.currentUser != null) {
                refreshCloudSession()
            }
        }
        viewModelScope.launch {
            localRepo.localAiModels.collect { list ->
                ggufManager.syncPersistedModels(list)
            }
        }
        viewModelScope.launch {
            while (true) {
                hardwareSuite.sampleNow(ggufManager.state.value)
                delay(2000L)
            }
        }
        viewModelScope.launch {
            preferences.collect { pref ->
                speechEngine.setSelectedLanguage(pref.speechLanguageCode)
            }
        }
    }

    fun uiStrings(pref: AppPreferences = preferences.value): LocalizedUiStrings =
        LocalizationStringsProvider.forLanguage(pref.languageCode)

    fun refreshHardwareHudNow() {
        val snap = hardwareSuite.sampleNow(ggufManager.state.value)
        _benchmarkEntries.value = hardwareSuite.buildBenchmarkSuite(ggufManager.state.value)
        if (snap.thermal.severity == ThermalSeverity.CRITICAL && ggufManager.state.value.estimatedVramUsedMb > 0) {
            ggufManager.suspendModel()
            showToast("Nhiệt độ thiết bị cao — đã tự động giải phóng mô hình cục bộ.")
        }
    }

    fun toggleReasoningExpanded(messageId: String) {
        val cur = _reasoningExpandedMap.value
        val next = !(cur[messageId] ?: false)
        _reasoningExpandedMap.value = cur + (messageId to next)
    }

    fun openAdvancedAiOptionsDialog(open: Boolean) {
        _showAdvancedAiDialog.value = open
    }

    fun updateAdvancedAiOptions(config: AdvancedAiFeaturesConfig) {
        viewModelScope.launch {
            prefsStore.updateAdvancedAiFeatures(config)
        }
    }

    fun openLanguageSelector(isSpeechMode: Boolean) {
        _languageSelectorIsSpeech.value = isSpeechMode
        _showLanguageSelectorDialog.value = true
    }

    fun closeLanguageSelector() {
        _showLanguageSelectorDialog.value = false
    }

    fun enterGuestMode() {
        viewModelScope.launch {
            prefsStore.setGuestMode(true)
            _isAuthenticatedCloud.value = false
            _hasEnteredWorkspace.value = true
            showToast("Đang dùng Chế độ Cục bộ (Room DB).")
        }
    }

    fun onCloudAuthenticated() {
        viewModelScope.launch {
            prefsStore.setGuestMode(false)
            _isAuthenticatedCloud.value = true
            _hasEnteredWorkspace.value = true
            refreshCloudSession()
        }
    }

    fun signOutToGateway() {
        _isAuthenticatedCloud.value = false
        _hasEnteredWorkspace.value = false
        _cloudProfile.value = null
        _cloudPrivateFolders.value = emptyList()
    }

    private fun refreshCloudSession() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val repo = firestoreRepo ?: return
        viewModelScope.launch {
            repo.ensureUserProfile(preferences.value.languageCode)
            launch { repo.observeProfile(uid).collect { _cloudProfile.value = it } }
            launch { repo.observePrivateFolders(uid).collect { _cloudPrivateFolders.value = it } }
            launch { repo.observePromptPersonas(uid).collect { _cloudPrompts.value = it } }
            launch {
                repo.observeSecurityLocks(uid).collect { cloudLocks ->
                    appLockManager.syncFromCloud(cloudLocks)
                }
            }
        }
    }

    private suspend fun ensureDefaultSessionExists() {
        localRepo.upsertSession(
            ChatSessionEntity(
                id = _currentSessionId.value,
                title = "Phiên Trợ lý AI",
                modelLabel = preferences.value.selectedGeminiModel,
                activePersonaName = "CTO & Security Architect",
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    fun createNewSession() {
        val newId = "session_${UUID.randomUUID().toString().take(8)}"
        _currentSessionId.value = newId
        viewModelScope.launch {
            localRepo.upsertSession(
                ChatSessionEntity(
                    id = newId,
                    title = "Hội thoại mới",
                    modelLabel = currentModelDisplayLabel(),
                    activePersonaName = activePersonaTitle(),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun selectSession(sessionId: String) {
        if (appLockManager.requestConversationAccess(sessionId)) {
            _currentSessionId.value = sessionId
        }
    }

    fun requestUnlockCurrentSession() {
        appLockManager.requestUnlockConversation(_currentSessionId.value)
    }

    fun dismissPendingChatUnlock() {
        appLockManager.dismissPendingConversationUnlock()
    }

    fun saveSecurityLock(
        targetType: String,
        targetId: String,
        targetLabel: String,
        mode: PasswordLockMode,
        gridSize: Int,
        textSecret: String,
        patternNodes: List<Int>
    ) {
        appLockManager.saveLock(
            targetType = targetType,
            targetId = targetId,
            targetLabel = targetLabel,
            mode = mode,
            gridSize = gridSize,
            textSecret = textSecret,
            patternNodes = patternNodes
        ) {
            showToast(
                if (targetType == AppLockManager.TARGET_APP) "Đã bật Khiên bảo vệ ứng dụng (${mode.displayLabel})."
                else "Đã đặt mật khẩu bảo vệ cho \"$targetLabel\"."
            )
        }
    }

    fun saveSecurityAttemptLimit(unlimitedAttempts: Boolean, maxAttempts: Long) {
        appLockManager.updateRetryLimit(unlimitedAttempts, maxAttempts) { safeMax ->
            showToast(
                if (unlimitedAttempts) "Đã lưu: Cho phép nhập sai không giới hạn."
                else "Đã lưu: Giới hạn tối đa $safeMax lần nhập sai."
            )
        }
    }

    fun verifySecurityLockText(lock: SecurityLockEntity, candidateText: String): Boolean {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        val result = appLockManager.verifyTextPasscode(lock, candidateText)
        if (result is LockAttemptResult.Unlocked && lock.targetType == AppLockManager.TARGET_CHAT && wasPendingTarget) {
            _currentSessionId.value = lock.targetId
        }
        return result is LockAttemptResult.Unlocked
    }

    fun verifySecurityLockPattern(lock: SecurityLockEntity, nodes: List<Int>): Boolean {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        val result = appLockManager.verifyPatternPasscode(lock, nodes)
        if (result is LockAttemptResult.Unlocked && lock.targetType == AppLockManager.TARGET_CHAT && wasPendingTarget) {
            _currentSessionId.value = lock.targetId
        }
        return result is LockAttemptResult.Unlocked
    }

    fun unlockSecurityLockViaBiometric(lock: SecurityLockEntity) {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        appLockManager.unlockViaBiometric(lock)
        if (lock.targetType == AppLockManager.TARGET_CHAT && wasPendingTarget) {
            _currentSessionId.value = lock.targetId
        }
    }

    fun authenticateDeviceBiometricForLock(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        onOutcome: (String?) -> Unit
    ) {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        appLockManager.unlockViaDeviceAuthenticator(
            activity = activity,
            lock = lock,
            title = if (lock.targetType == AppLockManager.TARGET_APP) {
                "Mở khóa Super Smart AI"
            } else {
                "Mở khóa: ${lock.targetLabel}"
            }
        ) { result ->
            when (result) {
                is BiometricAuthResult.Succeeded -> {
                    if (lock.targetType == AppLockManager.TARGET_CHAT && wasPendingTarget) {
                        _currentSessionId.value = lock.targetId
                    }
                    onOutcome(null)
                }
                is BiometricAuthResult.Failed -> onOutcome("Sinh trắc học điện thoại không khớp.")
                is BiometricAuthResult.Canceled -> onOutcome(null)
                is BiometricAuthResult.Error -> onOutcome(result.message)
                is BiometricAuthResult.Unavailable -> onOutcome(result.reason)
            }
        }
    }

    fun isFingerprintCryptoBound(): Boolean = appLockManager.isFingerprintCryptoBound()

    fun bindScreenFingerprintCrypto(
        activity: FragmentActivity,
        onFinished: (String) -> Unit
    ) {
        appLockManager.bindFingerprintCryptoForSettings(activity) { outcome ->
            when (outcome) {
                is FingerprintFirstOutcome.CryptoSucceeded,
                is FingerprintFirstOutcome.SystemFaceOrCredentialSucceeded -> {
                    val msg = "Đã liên kết & xác thực thành công vân tay màn hình (Keystore CryptoObject)."
                    showToast(msg)
                    onFinished(msg)
                }
                is FingerprintFirstOutcome.SwitchMethodRequested -> onFinished(outcome.reason)
                is FingerprintFirstOutcome.KeyInvalidated -> onFinished(outcome.message)
            }
        }
    }

    fun authenticateFingerprintFirstForLock(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        onOutcome: (FingerprintFirstOutcome) -> Unit
    ) {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        appLockManager.authenticateFingerprintFirstForLock(
            activity = activity,
            lock = lock,
            title = if (lock.targetType == AppLockManager.TARGET_APP) {
                "Quét vân tay mở khóa Super Smart AI"
            } else {
                "Quét vân tay mở khóa: ${lock.targetLabel}"
            }
        ) { result ->
            if ((result is FingerprintFirstOutcome.CryptoSucceeded ||
                    result is FingerprintFirstOutcome.SystemFaceOrCredentialSucceeded) &&
                lock.targetType == AppLockManager.TARGET_CHAT &&
                wasPendingTarget
            ) {
                _currentSessionId.value = lock.targetId
            }
            onOutcome(result)
        }
    }

    fun authenticatePhoneFaceForLock(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        onSuccess: () -> Unit = {},
        onError: (String?) -> Unit
    ) {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        appLockManager.authenticatePhoneFaceForLock(
            activity = activity,
            lock = lock,
            title = if (lock.targetType == AppLockManager.TARGET_APP) {
                "Khuôn mặt / Sinh trắc học điện thoại"
            } else {
                "Mở khóa bằng khuôn mặt: ${lock.targetLabel}"
            }
        ) { result ->
            when (result) {
                is BiometricAuthResult.Succeeded -> {
                    if (lock.targetType == AppLockManager.TARGET_CHAT && wasPendingTarget) {
                        _currentSessionId.value = lock.targetId
                    }
                    onSuccess()
                    onError(null)
                }
                is BiometricAuthResult.Failed -> onError("Khuôn mặt điện thoại không khớp.")
                is BiometricAuthResult.Canceled -> onError(null)
                is BiometricAuthResult.Error -> onError(result.message)
                is BiometricAuthResult.Unavailable -> onError(result.reason)
            }
        }
    }

    fun authenticateDeviceCredentialForLock(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        onSuccess: () -> Unit = {},
        onError: (String?) -> Unit
    ) {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        appLockManager.authenticateDeviceCredentialForLock(
            activity = activity,
            lock = lock,
            title = if (lock.targetType == AppLockManager.TARGET_APP) {
                "Mã khóa của điện thoại"
            } else {
                "Nhập PIN / Mẫu hình điện thoại: ${lock.targetLabel}"
            }
        ) { result ->
            when (result) {
                is BiometricAuthResult.Succeeded -> {
                    if (lock.targetType == AppLockManager.TARGET_CHAT && wasPendingTarget) {
                        _currentSessionId.value = lock.targetId
                    }
                    onSuccess()
                    onError(null)
                }
                is BiometricAuthResult.Failed -> onError("Mã PIN / Hình vẽ điện thoại không khớp.")
                is BiometricAuthResult.Canceled -> onError(null)
                is BiometricAuthResult.Error -> onError(result.message)
                is BiometricAuthResult.Unavailable -> onError(result.reason)
            }
        }
    }

    fun verifySecurityLockPersonalFace(lock: SecurityLockEntity, candidateVector: FloatArray): Boolean {
        val wasPendingTarget = appLockManager.pendingUnlockChatId.value == lock.targetId
        val result = appLockManager.verifyPersonalFace(lock, candidateVector)
        if (result is LockAttemptResult.Unlocked && lock.targetType == AppLockManager.TARGET_CHAT && wasPendingTarget) {
            _currentSessionId.value = lock.targetId
        }
        return result is LockAttemptResult.Unlocked
    }

    fun enrollPersonalFace(lockId: String, vector: FloatArray) {
        if (appLockManager.enrollPersonalFace(lockId, vector)) {
            showToast("Đã lưu Khuôn mặt cá nhân riêng cho ứng dụng (độc lập khỏi điện thoại).")
        } else {
            showToast("Không thể lưu đặc trưng khuôn mặt. Vui lòng quét lại.")
        }
    }

    fun removePersonalFace(lockId: String) {
        appLockManager.removePersonalFace(lockId)
        showToast("Đã xóa dữ liệu Khuôn mặt cá nhân.")
    }

    fun verifyTextAndDisableAppLock(lock: SecurityLockEntity, candidateText: String): Boolean {
        val res = appLockManager.verifyTextPasscode(lock, candidateText)
        return if (res is LockAttemptResult.Unlocked) {
            appLockManager.removeLock(AppLockManager.APP_LOCK_ID) {
                showToast("Đã tắt Khiên bảo vệ và xóa mật khẩu ứng dụng.")
            }
            true
        } else {
            false
        }
    }

    fun verifyPatternAndDisableAppLock(lock: SecurityLockEntity, nodes: List<Int>): Boolean {
        val res = appLockManager.verifyPatternPasscode(lock, nodes)
        return if (res is LockAttemptResult.Unlocked) {
            appLockManager.removeLock(AppLockManager.APP_LOCK_ID) {
                showToast("Đã tắt Khiên bảo vệ và xóa mật khẩu ứng dụng.")
            }
            true
        } else {
            false
        }
    }

    fun verifyPersonalFaceAndDisableAppLock(lock: SecurityLockEntity, vector: FloatArray): Boolean {
        val res = appLockManager.verifyPersonalFace(lock, vector)
        return if (res is LockAttemptResult.Unlocked) {
            appLockManager.removeLock(AppLockManager.APP_LOCK_ID) {
                showToast("Đã tắt Khiên bảo vệ và xóa mật khẩu ứng dụng.")
            }
            true
        } else {
            false
        }
    }

    fun disableAppLockAfterHardwareAuth(lock: SecurityLockEntity) {
        appLockManager.unlockViaBiometric(lock)
        appLockManager.removeLock(AppLockManager.APP_LOCK_ID) {
            showToast("Đã tắt Khiên bảo vệ và xóa mật khẩu ứng dụng.")
        }
    }

    fun onAppBackgrounded() {
        appLockManager.onAppBackgrounded()
    }

    fun onAppForegrounded() {
        appLockManager.onAppForegrounded()
    }

    fun setAutoLockTimeout(timeoutMillis: Long) {
        appLockManager.setAutoLockTimeout(timeoutMillis) { safe ->
            showToast("Đã lưu thời gian tự khóa: ${com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.formatAutoLockTimeoutLabel(safe)}.")
        }
    }

    fun setAllowDeviceBiometrics(allowed: Boolean) {
        appLockManager.setAllowDeviceBiometrics(allowed)
        showToast(
            if (allowed) "Đã bật liên kết Sinh trắc học điện thoại."
            else "Đã tách biệt hoàn toàn khỏi Sinh trắc học điện thoại."
        )
    }

    fun setBiometricAuthMode(mode: BiometricAuthMode) {
        appLockManager.setBiometricAuthMode(mode)
        showToast("Đã chuyển chế độ mở khóa: ${mode.displayLabel}")
    }

    fun setFingerprintFirstWithPinFallback(enabled: Boolean) {
        appLockManager.setFingerprintFirstWithPinFallback(enabled)
        showToast(
            if (enabled) "Đã bật ưu tiên quét vân tay trước (chuyển Khuôn mặt / PIN khi cần)."
            else "Đã tắt tự động bật vân tay trước."
        )
    }

    fun removeSecurityLock(lockId: String) {
        appLockManager.removeLock(lockId) {
            showToast("Đã gỡ bỏ mật khẩu bảo vệ.")
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            localRepo.deleteSession(sessionId)
            appLockManager.removeLockAndSync(AppLockManager.lockIdFor(AppLockManager.TARGET_CHAT, sessionId))
            if (_currentSessionId.value == sessionId) {
                createNewSession()
            }
        }
    }

    fun currentModelDisplayLabel(): String {
        val pref = preferences.value
        return if (pref.useLocalGgufEngine) {
            ggufState.value.activeModelName
        } else {
            pref.selectedGeminiModel
        }
    }

    private fun activePersonaTitle(): String =
        promptTemplates.value.firstOrNull { it.id == _activePersonaId.value }?.title ?: "Trợ lý Tiêu chuẩn"

    fun setSelectedGeminiModel(modelId: String) {
        viewModelScope.launch {
            prefsStore.setSelectedGeminiModel(modelId)
            prefsStore.setUseLocalGguf(false)
        }
    }

    fun switchToLocalModelEngine(modelId: String? = null) {
        viewModelScope.launch {
            if (modelId != null) {
                val custom = localAiModels.value.firstOrNull { it.id == modelId }
                if (custom != null) {
                    val fmt = AiModelFormat.entries.firstOrNull { it.id == custom.formatId } ?: AiModelFormat.GGUF
                    ggufManager.selectModel(custom.id, custom.name, fmt)
                } else {
                    val cat = GgufLifecycleManager.defaultCatalog.firstOrNull { it.id == modelId }
                    if (cat != null) ggufManager.selectModel(cat.id, cat.name, cat.defaultFormat)
                }
            }
            prefsStore.setUseLocalGguf(true)
        }
    }

    fun setAppLanguage(code: String) {
        viewModelScope.launch {
            prefsStore.setLanguageCode(code)
        }
    }

    fun setSpeechLanguage(code: String) {
        viewModelScope.launch {
            prefsStore.setSpeechLanguageCode(code)
            speechEngine.setSelectedLanguage(code)
        }
    }

    fun onChatInputTyping(text: String) {
        if (preferences.value.useLocalGgufEngine && text.length == 1) {
            val hot = hardwareHudSnapshot.value.thermal.severity == ThermalSeverity.SEVERE ||
                hardwareHudSnapshot.value.thermal.severity == ThermalSeverity.CRITICAL
            ggufManager.onUserTypingPrewarm(thermalElevated = hot)
        }
    }

    fun sendChatMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        val attachments = _pendingAttachments.value
        if (prompt.isEmpty() && attachments.isEmpty()) return

        val pref = preferences.value
        val sessId = _currentSessionId.value
        val activePersona = promptTemplates.value.firstOrNull { it.id == _activePersonaId.value }
        val attachSummary = if (attachments.isNotEmpty()) {
            attachments.joinToString(", ") { "${it.fileName} (${it.compressionFormat.id})" }
        } else null

        viewModelScope.launch {
            localRepo.upsertSession(
                ChatSessionEntity(
                    id = sessId,
                    title = prompt.take(36).ifBlank { "Phân tích tệp" },
                    modelLabel = currentModelDisplayLabel(),
                    activePersonaName = activePersona?.title ?: "Tiêu chuẩn",
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            localRepo.insertMessage(
                ChatMessageEntity(
                    id = "msg_${UUID.randomUUID()}",
                    sessionId = sessId,
                    role = "USER",
                    content = prompt.ifBlank { "[Đã đính kèm tệp để phân tích]" },
                    engineTag = "User",
                    attachmentSummary = attachSummary,
                    timestampMillis = System.currentTimeMillis()
                )
            )

            _pendingAttachments.value = emptyList()

            // Evaluate strict capability gates
            when (val decision = IntentCapabilityGate.evaluate(prompt, pref.advancedAiFeatures)) {
                is CapabilityGateDecision.RemindEnableFeature -> {
                    localRepo.insertMessage(
                        ChatMessageEntity(
                            id = "msg_${UUID.randomUUID()}",
                            sessionId = sessId,
                            role = "AI",
                            content = decision.shortReminder,
                            engineTag = "Tùy chọn AI",
                            timestampMillis = System.currentTimeMillis()
                        )
                    )
                    return@launch
                }
                is CapabilityGateDecision.ExecuteFileGeneration -> {
                    // Generate files AND persist export job naturally
                    val format = if (decision.isBatch) {
                        val batch = BatchExportEngine.exportBatch(
                            context = appContext,
                            title = prompt.take(30),
                            messages = messages.value,
                            formats = DocumentExportEngine.supportedFormats.take(5)
                        )
                        _batchExportResult.value = batch
                        localRepo.insertMessage(
                            ChatMessageEntity(
                                id = "msg_${UUID.randomUUID()}",
                                sessionId = sessId,
                                role = "AI",
                                content = "Đã tạo gói xuất hàng loạt `${batch.bundleFileName}` (${batch.zipBytes.size} bytes, ${batch.includedFormats.size} định dạng) • SHA-256: `${batch.sha256Hash.take(16)}...`. Bạn có thể lưu ngay từ Studio Tệp.",
                                engineTag = "BatchExport",
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                        return@launch
                    } else {
                        val fmt = inferRequestedExportFormat(prompt)
                        val pkg = DocumentExportEngine.exportConversation(
                            context = appContext,
                            title = "File_Theo_Yeu_Cau",
                            messages = messages.value,
                            format = fmt
                        )
                        _activeExportPreview.value = pkg
                        localRepo.insertMessage(
                            ChatMessageEntity(
                                id = "msg_${UUID.randomUUID()}",
                                sessionId = sessId,
                                role = "AI",
                                content = "Đã tạo tệp `${pkg.parts.first().fileName}` (${pkg.totalBytes} bytes). Trình xem trước & lưu tệp đã sẵn sàng.",
                                engineTag = "FileGen",
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                        return@launch
                    }
                }
                is CapabilityGateDecision.ExecuteWebSearch -> {
                    _isGenerating.value = true
                    _streamingAiText.value = ""
                    var snippetsText = ""
                    val searchRes = withContext(Dispatchers.IO) {
                        var tokenChars: CharArray? = null
                        secretVault.withDecryptedSecret(EncryptedSecretVault.KEY_WEB_SEARCH_TOKEN) { c ->
                            tokenChars = c?.copyOf()
                        }
                        LiveWebSearchClient.search(decision.searchQuery, tokenChars)
                    }
                    searchRes.onSuccess { snippets ->
                        snippetsText = if (snippets.isEmpty()) {
                            "Không tìm thấy trích đoạn trực tiếp; hãy tổng hợp kiến thức tốt nhất."
                        } else {
                            snippets.joinToString("\n") { "- ${it.title}: ${it.snippet} (${it.url})" }
                        }
                    }.onFailure { err ->
                        snippetsText = "Tìm kiếm trực tuyến gặp sự cố (${err.message})."
                    }
                    val enrichedPrompt = "Dữ liệu tìm kiếm web cho '${decision.searchQuery}':\n$snippetsText\n\nHãy trả lời trực tiếp câu hỏi: ${decision.originalPrompt}"
                    dispatchModelInference(sessId, enrichedPrompt, activePersona, attachments.mapNotNull { it.previewBitmap })
                }
                is CapabilityGateDecision.NormalChat -> {
                    dispatchModelInference(
                        sessId = sessId,
                        prompt = decision.cleanPrompt,
                        activePersona = activePersona,
                        bitmaps = attachments.mapNotNull { it.previewBitmap }
                    )
                }
            }
        }
    }

    private suspend fun dispatchModelInference(
        sessId: String,
        prompt: String,
        activePersona: PromptTemplateEntity?,
        bitmaps: List<Bitmap>
    ) {
        val pref = preferences.value
        _isGenerating.value = true
        _streamingAiText.value = ""

        val systemCtx = PromptContextHook.buildInjectedSystemContext(
            basePersona = activePersona?.content,
            preferredCodeLang = activePersona?.preferredLanguageTag,
            enableCodeHook = pref.advancedAiFeatures.enableCodeLanguageHook,
            enableReasoningTag = pref.advancedAiFeatures.enableReasoningOutputTagging
        )

        if (pref.useLocalGgufEngine) {
            val thermalHot = hardwareHudSnapshot.value.thermal.severity == ThermalSeverity.SEVERE ||
                hardwareHudSnapshot.value.thermal.severity == ThermalSeverity.CRITICAL
            val reply = ggufManager.generateLocalResponse(prompt, thermalElevated = thermalHot)
            localRepo.insertMessage(
                ChatMessageEntity(
                    id = "msg_${UUID.randomUUID()}",
                    sessionId = sessId,
                    role = "AI",
                    content = reply,
                    engineTag = "${ggufState.value.activeModelName} (${ggufState.value.activeFormat.id})",
                    timestampMillis = System.currentTimeMillis()
                )
            )
            _isGenerating.value = false
            _streamingAiText.value = ""
        } else {
            geminiRepo.streamPrompt(
                modelId = pref.selectedGeminiModel,
                systemPersona = systemCtx.ifBlank { null },
                prompt = prompt,
                attachedImages = bitmaps
            ).collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.Delta -> _streamingAiText.value = chunk.accumulatedText
                    is AiStreamChunk.Completed -> {
                        localRepo.insertMessage(
                            ChatMessageEntity(
                                id = "msg_${UUID.randomUUID()}",
                                sessionId = sessId,
                                role = "AI",
                                content = chunk.finalText,
                                engineTag = pref.selectedGeminiModel,
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                        _streamingAiText.value = ""
                        _isGenerating.value = false
                    }
                    is AiStreamChunk.Failed -> {
                        localRepo.insertMessage(
                            ChatMessageEntity(
                                id = "msg_${UUID.randomUUID()}",
                                sessionId = sessId,
                                role = "SYSTEM_ERROR",
                                content = "Lỗi kết nối ${pref.selectedGeminiModel}: ${chunk.message}\n\nBạn có thể chuyển sang mô hình cục bộ (Offline AI) ở thanh trên.",
                                engineTag = "Error",
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                        _streamingAiText.value = ""
                        _isGenerating.value = false
                    }
                }
            }
        }
    }

    private fun inferRequestedExportFormat(prompt: String): ExportFormatSpec {
        val lower = prompt.lowercase()
        return DocumentExportEngine.supportedFormats.firstOrNull { fmt ->
            lower.contains(".${fmt.id}") || lower.contains(" ${fmt.id}")
        } ?: DocumentExportEngine.supportedFormats.first() // PDF default
    }

    // Attachments & Compression (Default NONE!)
    fun queueAttachmentFromUri(uri: Uri, defaultName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = appContext.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val bmp = if (mime.startsWith("image/")) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                val pending = PendingAttachment(
                    fileName = defaultName,
                    mimeType = mime,
                    rawBytes = bytes,
                    previewBitmap = bmp,
                    compressionFormat = CompressionFormat.NONE
                )
                _pendingAttachments.value = _pendingAttachments.value + pending
                _showCompressionPickerFor.value = pending
            } catch (e: Exception) {
                showToast("Không thể đọc tệp: ${e.message}")
            }
        }
    }

    fun dismissCompressionPicker() {
        _showCompressionPickerFor.value = null
    }

    fun applyCompressionChoice(attachment: PendingAttachment, format: CompressionFormat, uploadToPrivateFolder: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = MultiFormatCompressionEngine.compress(attachment.fileName, attachment.rawBytes, format, appContext)
            _pendingAttachments.value = _pendingAttachments.value.map {
                if (it.id == attachment.id) it.copy(compressionFormat = format) else it
            }
            if (uploadToPrivateFolder) {
                val user = Firebase.auth.currentUser
                if (user != null && !preferences.value.isGuestMode && firestoreRepo != null) {
                    val folderKey = cloudProfile.value?.folderKey
                        ?: firestoreRepo!!.buildFolderKey(user.displayName ?: "User", user.uid)
                    firestoreRepo!!.uploadPrivateFolderItem(folderKey, attachment.mimeType, "MEDIA", result)
                } else {
                    val vaultDir = File(appContext.filesDir, "local_vault").apply { mkdirs() }
                    val dest = File(vaultDir, result.outputFileName)
                    dest.writeBytes(result.compressedBytes)
                    localRepo.upsertVaultFile(
                        LocalVaultFileEntity(
                            id = UUID.randomUUID().toString(),
                            folderKey = "Guest_Local_Vault",
                            fileName = result.outputFileName,
                            compressionMethod = format.id,
                            originalSizeBytes = result.originalSizeBytes,
                            compressedSizeBytes = result.compressedSizeBytes,
                            sha256Hash = result.sha256Hash,
                            chunkCount = result.chunks.size,
                            localFilePath = dest.absolutePath,
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
                showToast("Đã lưu ${result.outputFileName} (${format.id}) an toàn.")
            }
            _showCompressionPickerFor.value = null
        }
    }

    // Export & Batch Export
    fun triggerExport(format: ExportFormatSpec, maxMessagesPerPart: Int = 50, pdfPassword: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentMessages = messages.value
                if (currentMessages.isEmpty()) {
                    showToast("Chưa có tin nhắn nào trong phiên hiện tại để xuất ${format.extension.uppercase()}.")
                    return@launch
                }
                val title = sessions.value.firstOrNull { it.id == _currentSessionId.value }?.title ?: "SuperSmartAI_Chat"
                val pkg = DocumentExportEngine.exportConversation(
                    context = appContext,
                    title = title,
                    messages = currentMessages,
                    format = format,
                    maxMessagesPerPart = maxMessagesPerPart,
                    pdfPassword = pdfPassword
                )
                _activeExportPreview.value = pkg
                val first = pkg.parts.firstOrNull()
                if (first != null) {
                    localRepo.upsertExportJob(
                        ExportJobEntity(
                            id = UUID.randomUUID().toString(),
                            title = first.fileName,
                            formatId = format.id,
                            extension = format.extension,
                            status = "READY",
                            sha256Hash = first.sha256Hash,
                            sizeBytes = pkg.totalBytes,
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                showToast("Lỗi xuất tệp ${format.extension}: ${e.localizedMessage ?: "không xác định"}")
            }
        }
    }

    fun triggerBatchExport(formats: List<ExportFormatSpec>, pdfPassword: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentMessages = messages.value
                if (currentMessages.isEmpty()) {
                    showToast("Chưa có tin nhắn nào trong phiên hiện tại để xuất hàng loạt.")
                    return@launch
                }
                val title = sessions.value.firstOrNull { it.id == _currentSessionId.value }?.title ?: "SuperSmartAI_Batch"
                val batch = BatchExportEngine.exportBatch(
                    context = appContext,
                    title = title,
                    messages = currentMessages,
                    formats = formats,
                    pdfPassword = pdfPassword
                )
                _batchExportResult.value = batch
                localRepo.upsertExportJob(
                    ExportJobEntity(
                        id = UUID.randomUUID().toString(),
                        title = batch.bundleFileName,
                        formatId = "batch_zip",
                        extension = "zip",
                        status = "READY",
                        sha256Hash = batch.sha256Hash,
                        sizeBytes = batch.zipBytes.size.toLong(),
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
                showToast("Xuất hàng loạt ${batch.includedFormats.size} định dạng hoàn tất.")
            } catch (e: Exception) {
                showToast("Lỗi xuất hàng loạt: ${e.localizedMessage ?: "không xác định"}")
            }
        }
    }

    fun dismissExportPreview() {
        _activeExportPreview.value = null
    }

    fun clearBatchExportResult() {
        _batchExportResult.value = null
    }

    fun saveBytesToSafUri(uri: Uri, bytes: ByteArray, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = appContext.contentResolver.openOutputStream(uri)
                if (out == null) {
                    showToast("Không thể mở luồng ghi tới tệp $fileName.")
                    return@launch
                }
                out.use { it.write(bytes) }
                showToast("Đã lưu tệp $fileName thành công!")
            } catch (e: Exception) {
                showToast("Lỗi lưu tệp: ${e.message}")
            }
        }
    }

    // Build Queue & Toolchain Updater & Decompiler & Ipynb
    fun enqueueProjectBuild(targetKind: String, projectName: String, sourceCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val job = PersistentBuildQueueManager.enqueueBuild(appContext, targetKind, projectName, sourceCode)
            showToast("Đã xếp hàng biên dịch ${job.targetKind}: ${job.projectName}")
        }
    }

    fun runToolchainAutoUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val msg = ToolchainAutoUpdater.checkAndUpdateAll(appContext)
            showToast(msg)
        }
    }

    fun decompileSampleOrBytes(fileName: String, bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.Default) {
            _decompilerReport.value = BinaryDecompilerEngine.analyzeBytes(fileName, bytes)
        }
    }

    fun runIpynbNotebook(ipynbJson: String, title: String = "analysis.ipynb") {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _ipynbReport.value = IpynbExecutionEngine.parseAndRun(ipynbJson, title)
            } catch (e: Exception) {
                showToast("Lỗi phân tích .ipynb: ${e.message}")
            }
        }
    }

    fun decodeLogoSpec(fileName: String, rawBytes: ByteArray, formatId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _decodedLogoResult.value = LogoDecoderEngine.decodeToPng(fileName, rawBytes, 512, formatId)
        }
    }

    fun signPayload(payloadText: String, alg: SignatureAlgorithm, passphrase: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val bytes = payloadText.toByteArray(Charsets.UTF_8)
            val bundle = DigitalSignatureEngine.signAndTimestamp(bytes, alg, passphrase)
            val ok = DigitalSignatureEngine.verifyTimestampManifest(bytes, bundle.timestampJsonManifest, passphrase)
            _signatureBundle.value = bundle
            _signatureVerified.value = ok
        }
    }

    // Photo Editor
    fun loadPhotoForEditing(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                appContext.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sampleSize = 1
                val maxDim = 2048
                while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bmp = appContext.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
                if (bmp != null) {
                    val processed = MediaProcessingEngine.processBitmap(bmp, PhotoEditParams())
                    _photoEditorState.value = PhotoEditorState(
                        originalBitmap = bmp,
                        editedBitmap = processed,
                        params = PhotoEditParams(),
                        statusNote = "Đã nạp ảnh (${bmp.width}x${bmp.height}px)."
                    )
                } else {
                    showToast("Không thể giải mã ảnh đã chọn.")
                }
            } catch (e: Exception) {
                showToast("Lỗi mở ảnh: ${e.message}")
            }
        }
    }

    fun loadDefaultSamplePhoto() {
        viewModelScope.launch(Dispatchers.Default) {
            val bmp = createBitmap(512, 320)
            val c = android.graphics.Canvas(bmp)
            c.drawColor("#111A2E".toColorInt())
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = "#22D3EE".toColorInt()
                textSize = 36f
                isFakeBoldText = true
            }
            c.drawCircle(256f, 160f, 90f, android.graphics.Paint(p).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f })
            c.drawText("SUPER SMART AI", 110f, 172f, p)
            val processed = MediaProcessingEngine.processBitmap(bmp, PhotoEditParams())
            _photoEditorState.value = PhotoEditorState(bmp, processed, PhotoEditParams(), "Đã tạo ảnh mẫu kỹ thuật số.")
        }
    }

    fun updatePhotoEditParams(params: PhotoEditParams) {
        val orig = _photoEditorState.value.originalBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val out = MediaProcessingEngine.processBitmap(orig, params)
            _photoEditorState.value = _photoEditorState.value.copy(
                editedBitmap = out,
                params = params
            )
        }
    }

    fun sendEditedPhotoToChat() {
        val bmp = _photoEditorState.value.editedBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = MediaProcessingEngine.encodeBitmap(bmp, "PNG")
            val att = PendingAttachment(
                fileName = "edited_${System.currentTimeMillis()}.png",
                mimeType = "image/png",
                rawBytes = bytes,
                previewBitmap = bmp,
                compressionFormat = CompressionFormat.NONE
            )
            _pendingAttachments.value = _pendingAttachments.value + att
            showToast("Đã đính kèm ảnh đã chỉnh sửa vào khung Chat!")
        }
    }

    // Code Runner & Self-Healing
    fun runCodeBlock(blockKey: String, language: String, code: String) {
        val result = CodeRunnerSandbox.execute(language, code, appContext)
        _sandboxResults.value = _sandboxResults.value + (blockKey to result)
    }

    fun sendCodeErrorToAiFix(language: String, code: String, errorMsg: String) {
        sendChatMessage(
            "Hãy tìm nguyên nhân và sửa lỗi trong đoạn mã `$language` sau:\n```$language\n$code\n```\nLỗi thực thi: `$errorMsg`"
        )
    }

    // Model Delete Modes (Hide UI vs Move to model_trash)
    fun hideLocalModelFromList(model: LocalAiModelEntity) {
        viewModelScope.launch {
            localRepo.upsertLocalAiModel(model.copy(isVisibleInList = false))
            showToast("Đã ẩn ${model.name} khỏi danh sách (tệp gốc vẫn giữ trên thiết bị).")
        }
    }

    fun moveLocalModelToTrash(model: LocalAiModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val trashedPath = ggufManager.moveFileToTrash(model.localPath) ?: model.localPath
            localRepo.upsertLocalAiModel(model.copy(isInTrash = true, localPath = trashedPath))
            showToast("Đã chuyển ${model.name} vào Thùng rác (model_trash/).")
        }
    }

    fun restoreModelFromTrash(model: LocalAiModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val restoredPath = ggufManager.restoreFileFromTrash(model.localPath) ?: model.localPath
            localRepo.upsertLocalAiModel(model.copy(isInTrash = false, isVisibleInList = true, localPath = restoredPath))
            showToast("Đã khôi phục ${model.name}.")
        }
    }

    fun permanentlyDeleteTrashedModel(model: LocalAiModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ggufManager.permanentlyDeleteFile(model.localPath)
            localRepo.deleteLocalAiModel(model.id)
            showToast("Đã xóa vĩnh viễn ${model.name}.")
        }
    }

    // Secret Vault (AES-256-GCM + Pre-Decryption Unlock Gate + Health Validation)
    fun isVaultUnlockedNow(): Boolean {
        val st = _vaultUnlockState.value
        val active = st.isUnlocked && System.currentTimeMillis() < st.unlockedUntilMillis
        if (!active && st.isUnlocked) {
            _vaultUnlockState.value = st.copy(isUnlocked = false, unlockedMethod = null)
        }
        return active
    }

    fun markVaultUnlocked(methodLabel: String) {
        _vaultUnlockState.value = _vaultUnlockState.value.copy(
            isUnlocked = true,
            unlockedUntilMillis = System.currentTimeMillis() + 10 * 60_000L,
            unlockedMethod = methodLabel,
            hasLocalPassphrase = secretVault.storageManager.hasLocalVaultPassphrase(),
            isKeystoreHardwareBacked = secretVault.isKeystoreHardwareBacked,
            recoveryNotice = secretVault.storageManager.lastRecoveryReason
        )
        showToast("Đã mở khóa Keystore Vault bằng $methodLabel.")
    }

    fun lockVaultNow() {
        _vaultUnlockState.value = _vaultUnlockState.value.copy(
            isUnlocked = false,
            unlockedUntilMillis = 0L,
            unlockedMethod = null
        )
        showToast("Đã khóa Keystore Vault.")
    }

    fun verifyLocalVaultPassphrase(chars: CharArray): Boolean {
        return secretVault.storageManager.verifyLocalVaultPassphrase(chars)
    }

    fun setLocalVaultPassphrase(chars: CharArray): Boolean {
        val ok = secretVault.storageManager.setLocalVaultPassphrase(chars)
        if (ok) {
            _vaultUnlockState.value = _vaultUnlockState.value.copy(hasLocalPassphrase = true)
        }
        return ok
    }

    fun saveVaultSecret(keyId: String, rawSecret: String) {
        secretVault.storeSecret(keyId, rawSecret)
        _secretSummaries.value = secretVault.listSummaries()
        _apiKeyValidations.value = _apiKeyValidations.value - keyId
        _vaultUnlockState.value = _vaultUnlockState.value.copy(
            isKeystoreHardwareBacked = secretVault.isKeystoreHardwareBacked,
            recoveryNotice = secretVault.storageManager.lastRecoveryReason
        )
        showToast("Đã mã hóa AES-256-GCM và lưu khóa an toàn.")
    }

    fun removeVaultSecret(keyId: String) {
        secretVault.removeSecret(keyId)
        _secretSummaries.value = secretVault.listSummaries()
        _apiKeyValidations.value = _apiKeyValidations.value - keyId
        showToast("Đã xóa khóa khỏi Keystore Vault.")
    }

    fun validateVaultSecret(keyId: String) {
        if (!isVaultUnlockedNow()) {
            showToast("Cần mở khóa xác thực trước khi kiểm tra khóa API.")
            return
        }
        _apiKeyValidations.value = _apiKeyValidations.value + (
            keyId to ApiKeyValidationResult(
                keyId = keyId,
                state = KeyValidationState.CHECKING,
                message = "Đang kiểm tra tính hợp lệ..."
            )
        )
        viewModelScope.launch {
            val result = ApiKeyHealthValidator.validateKey(keyId, secretVault.storageManager)
            _apiKeyValidations.value = _apiKeyValidations.value + (keyId to result)
            showToast(result.message)
        }
    }

    // Templates & Hardware Tuning
    fun savePromptTemplate(title: String, category: String, content: String, kind: String, langTag: String?) {
        val cleanTitle = title.trim().ifBlank { "Mẫu mới" }
        val cleanContent = content.trim().ifBlank { return }
        val entity = PromptTemplateEntity(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            kind = kind,
            title = cleanTitle,
            category = category,
            content = cleanContent,
            preferredLanguageTag = langTag?.takeIf { it.isNotBlank() }
        )
        viewModelScope.launch {
            localRepo.upsertTemplate(entity)
            showToast("Đã lưu ${if (kind == "PERSONA") "Persona" else "Prompt"} mới.")
        }
    }

    fun setActivePersona(id: String) {
        _activePersonaId.value = id
    }

    fun updateGgufTuning(quant: GgufQuantization, layers: Int, batteryLimit: Int, idleMins: Int) {
        ggufManager.updateTuning(quant, layers, batteryLimit, idleMins)
        viewModelScope.launch {
            prefsStore.setGgufTuning(layers, batteryLimit, idleMins)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setBiometricLock(enabled) }
    }

    fun backupSessionToCloud() {
        val repo = firestoreRepo ?: return
        viewModelScope.launch {
            val transcript = messages.value.joinToString("\n") { "${it.role}: ${it.content}" }
            val res = repo.backupChatSession(
                sessionId = _currentSessionId.value,
                title = sessions.value.firstOrNull { it.id == _currentSessionId.value }?.title ?: "Chat",
                modelLabel = currentModelDisplayLabel(),
                messageCount = messages.value.size,
                transcriptText = transcript
            )
            showToast(if (res.isSuccess) "Đã sao lưu PAQ lên Firestore." else "Cần đăng nhập Cloud để sao lưu.")
        }
    }

    fun downloadAndInspectPrivateFolderItem(item: PrivateFolderItem) {
        val repo = firestoreRepo ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val res = repo.downloadPrivateFolderItemOnDemand(item, appContext)
            res.onSuccess { bytes ->
                showToast("Đã tải & giải nén ${item.fileName} (${bytes.size} bytes).")
            }.onFailure {
                showToast("Lỗi tải tệp từ Firestore: ${it.message}")
            }
        }
    }

    fun deleteCloudVaultItem(itemId: String) {
        val repo = firestoreRepo ?: return
        viewModelScope.launch { repo.deletePrivateFolderItem(itemId) }
    }

    fun deleteLocalVaultFile(id: String) {
        viewModelScope.launch { localRepo.deleteVaultFile(id) }
    }

    fun showToast(msg: String) {
        _statusToast.value = msg
    }

    fun clearToast() {
        _statusToast.value = null
    }
}

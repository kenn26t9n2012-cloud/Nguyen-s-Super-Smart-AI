package com.nguyen_super_app.nguyen_super_smart_ai

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nguyen_super_app.nguyen_super_smart_ai.firebase.configureAppCheck
import com.nguyen_super_app.nguyen_super_smart_ai.security.AppLockManager
import com.nguyen_super_app.nguyen_super_smart_ai.security.EncryptedStorageManager
import com.nguyen_super_app.nguyen_super_smart_ai.ui.components.LanguageSelectorDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.AdvancedAiOptionsDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.UnifiedCompressionPickerDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.VisualFileEditorDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.AppLockToggleAuthDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.AppPasswordLockGateScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.AuthGatewayScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.AutoLockTimeoutScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.ChatLockUnlockDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.ChatWorkspaceScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.HardwareMonitorScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.MiniBrowserScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.PasswordSettingsScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.PhotoEditorScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.SecurityAttemptLimitScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.SecurityChatPickerScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.SecurityLockHubScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.StudioWorkspaceScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.VaultAndSettingsScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SuperSmartAiTheme
import com.nguyen_super_app.nguyen_super_smart_ai.ui.viewmodel.SuperSmartViewModel
import kotlinx.serialization.Serializable

@Serializable object RouteChat
@Serializable object RouteBrowser
@Serializable object RouteHardware
@Serializable object RouteStudio
@Serializable object RoutePhotoEditor
@Serializable object RouteVaultSettings
@Serializable object RouteSecurityLockHub
@Serializable object RouteSecurityChatPicker
@Serializable data class RouteSecuritySetup(
    val targetType: String,
    val targetId: String,
    val targetLabel: String,
    val requireConfirmation: Boolean = false
)
@Serializable object RouteSecurityAttemptLimit
@Serializable object RouteSecurityAutoLockTimeout

class MainActivity : FragmentActivity() {

    private val vm: SuperSmartViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        vm.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            vm.onAppBackgrounded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)

        setContent {
            val prefs by vm.preferences.collectAsStateWithLifecycle()
            val hasEntered by vm.hasEnteredWorkspace.collectAsStateWithLifecycle()
            val isCloudAuthed by vm.isAuthenticatedCloud.collectAsStateWithLifecycle()
            val toastMsg by vm.statusToast.collectAsStateWithLifecycle()
            val showLangDialog by vm.showLanguageSelectorDialog.collectAsStateWithLifecycle()
            val langIsSpeech by vm.languageSelectorIsSpeech.collectAsStateWithLifecycle()
            val showAdvDialog by vm.showAdvancedAiDialog.collectAsStateWithLifecycle()
            val compressionTarget by vm.showCompressionPickerFor.collectAsStateWithLifecycle()
            val exportPreview by vm.activeExportPreview.collectAsStateWithLifecycle()
            val securityLocks by vm.securityLocks.collectAsStateWithLifecycle()
            val appLockUnlocked by vm.appLockUnlocked.collectAsStateWithLifecycle()
            val allowDeviceBiometrics by vm.allowDeviceBiometrics.collectAsStateWithLifecycle()
            val biometricAuthMode by vm.biometricAuthMode.collectAsStateWithLifecycle()
            val fingerprintFirstWithPinFallback by vm.fingerprintFirstWithPinFallback.collectAsStateWithLifecycle()
            val enrolledPersonalFaces by vm.enrolledPersonalFaceTargets.collectAsStateWithLifecycle()
            val appLock = remember(securityLocks) { securityLocks.firstOrNull { it.targetType == "APP" } }

            var biometricUnlocked by remember { mutableStateOf(true) }
            val snackbarHostState = remember { SnackbarHostState() }
            val storageManager = remember { EncryptedStorageManager(this) }

            LaunchedEffect(prefs.biometricLockEnabled) {
                if (prefs.biometricLockEnabled && storageManager.isBiometricAvailable()) {
                    biometricUnlocked = false
                    triggerBiometricPrompt(storageManager) { biometricUnlocked = true }
                } else {
                    biometricUnlocked = true
                }
            }

            LaunchedEffect(toastMsg) {
                if (!toastMsg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(toastMsg!!)
                    vm.clearToast()
                }
            }

            SuperSmartAiTheme {
                Scaffold(
                    containerColor = SlateDeep,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { _ ->
                    if (appLock != null && !appLockUnlocked) {
                        AppPasswordLockGateScreen(
                            lock = appLock,
                            onVerifyText = { secret -> vm.verifySecurityLockText(appLock, secret) },
                            onVerifyPattern = { nodes -> vm.verifySecurityLockPattern(appLock, nodes) },
                            onBiometricRecoverySuccess = { vm.unlockSecurityLockViaBiometric(appLock) },
                            hasPersonalFaceEnrolled = AppLockManager.APP_LOCK_ID in enrolledPersonalFaces,
                            allowDeviceBiometric = biometricAuthMode.allowsSystemBiometric,
                            onVerifyPersonalFace = { vec -> vm.verifySecurityLockPersonalFace(appLock, vec) },
                            onTriggerDeviceBiometric = { act, cb ->
                                vm.authenticateDeviceBiometricForLock(act, appLock, cb)
                            },
                            biometricAuthMode = biometricAuthMode,
                            fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                            onTriggerFingerprintFirst = { act, cb ->
                                vm.authenticateFingerprintFirstForLock(act, appLock, cb)
                            },
                            onTriggerPhoneFace = { act, cb ->
                                vm.authenticatePhoneFaceForLock(act, appLock, onError = cb)
                            },
                            onTriggerPhoneCredential = { act, cb ->
                                vm.authenticateDeviceCredentialForLock(act, appLock, onError = cb)
                            },
                            unifiedUnlockManager = vm.unifiedUnlockManager
                        )
                    } else if (!biometricUnlocked) {
                        BiometricLockGate(onUnlock = { triggerBiometricPrompt(storageManager) { biometricUnlocked = true } })
                    } else if (!hasEntered) {
                        AuthGatewayScreen(
                            strings = vm.uiStrings(prefs),
                            currentLanguageCode = prefs.languageCode,
                            onOpenLanguageSelector = { vm.openLanguageSelector(isSpeechMode = false) },
                            onAuthSuccess = { vm.onCloudAuthenticated() },
                            onSkipGuestMode = { vm.enterGuestMode() }
                        )
                    } else {
                        MainWorkspaceNavHost(vm = vm, isAuthenticatedCloud = isCloudAuthed)
                    }
                }

                if (showLangDialog) {
                    LanguageSelectorDialog(
                        selectedCode = if (langIsSpeech) prefs.speechLanguageCode else prefs.languageCode,
                        title = if (langIsSpeech) "Ngôn ngữ Giọng nói Ngoại tuyến (100+)" else "Ngôn ngữ Hiển thị Ứng dụng (100+)",
                        isSpeechMode = langIsSpeech,
                        onSelect = { lang ->
                            if (langIsSpeech) vm.setSpeechLanguage(lang.code) else vm.setAppLanguage(lang.code)
                        },
                        onDismiss = { vm.closeLanguageSelector() }
                    )
                }

                if (showAdvDialog) {
                    AdvancedAiOptionsDialog(
                        config = prefs.advancedAiFeatures,
                        onUpdate = { vm.updateAdvancedAiOptions(it) },
                        onDismiss = { vm.openAdvancedAiOptionsDialog(false) }
                    )
                }

                compressionTarget?.let { att ->
                    UnifiedCompressionPickerDialog(
                        fileName = att.fileName,
                        originalSizeBytes = att.rawBytes.size.toLong(),
                        onConfirm = { fmt, saveVault -> vm.applyCompressionChoice(att, fmt, saveVault) },
                        onDismiss = { vm.dismissCompressionPicker() }
                    )
                }

                exportPreview?.let { pkg ->
                    VisualFileEditorDialog(
                        exportPackage = pkg,
                        onRegenerate = { maxMsgs, pw ->
                            vm.triggerExport(pkg.primaryFormat, maxMsgs, pw)
                        },
                        onSavePartToUri = { uri, bytes, name -> vm.saveBytesToSafUri(uri, bytes, name) },
                        onDismiss = { vm.dismissExportPreview() }
                    )
                }
            }
        }
    }

    private fun triggerBiometricPrompt(
        storageManager: EncryptedStorageManager,
        onUnlocked: () -> Unit
    ) {
        storageManager.authenticateWithBiometric(
            activity = this,
            title = "Mở khóa Super Smart AI",
            subtitle = "Xác thực sinh trắc học hoặc mã PIN thiết bị",
            onSuccess = onUnlocked,
            onError = { err ->
                Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun MainWorkspaceNavHost(
    vm: SuperSmartViewModel,
    isAuthenticatedCloud: Boolean
) {
    val navController = rememberNavController()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val currentSessionId by vm.currentSessionId.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val streamingText by vm.streamingAiText.collectAsStateWithLifecycle()
    val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
    val reasoningMap by vm.reasoningExpandedMap.collectAsStateWithLifecycle()
    val pendingAtts by vm.pendingAttachments.collectAsStateWithLifecycle()
    val speechState by vm.speechState.collectAsStateWithLifecycle()
    val sandboxResults by vm.sandboxResults.collectAsStateWithLifecycle()
    val localModels by vm.localAiModels.collectAsStateWithLifecycle()
    val browserState by vm.browserDownloadState.collectAsStateWithLifecycle()
    val hudSnapshot by vm.hardwareHudSnapshot.collectAsStateWithLifecycle()
    val ggufState by vm.ggufState.collectAsStateWithLifecycle()
    val benchmarks by vm.benchmarkEntries.collectAsStateWithLifecycle()
    val exportJobs by vm.exportJobs.collectAsStateWithLifecycle()
    val batchResult by vm.batchExportResult.collectAsStateWithLifecycle()
    val buildJobs by vm.buildQueueJobs.collectAsStateWithLifecycle()
    val toolchains by vm.toolchains.collectAsStateWithLifecycle()
    val decompReport by vm.decompilerReport.collectAsStateWithLifecycle()
    val ipynbReport by vm.ipynbReport.collectAsStateWithLifecycle()
    val logoRes by vm.decodedLogoResult.collectAsStateWithLifecycle()
    val sigBundle by vm.signatureBundle.collectAsStateWithLifecycle()
    val sigVerified by vm.signatureVerified.collectAsStateWithLifecycle()
    val templates by vm.promptTemplates.collectAsStateWithLifecycle()
    val activePersonaId by vm.activePersonaId.collectAsStateWithLifecycle()
    val photoState by vm.photoEditorState.collectAsStateWithLifecycle()
    val cloudProfile by vm.cloudProfile.collectAsStateWithLifecycle()
    val cloudFolders by vm.cloudPrivateFolders.collectAsStateWithLifecycle()
    val localVault by vm.localVaultFiles.collectAsStateWithLifecycle()
    val paqSnap by vm.paqSnapshot.collectAsStateWithLifecycle()
    val secretSummaries by vm.secretSummaries.collectAsStateWithLifecycle()
    val vaultUnlockState by vm.vaultUnlockState.collectAsStateWithLifecycle()
    val apiKeyValidations by vm.apiKeyValidations.collectAsStateWithLifecycle()
    val securityLocks by vm.securityLocks.collectAsStateWithLifecycle()
    val allMessages by vm.allMessages.collectAsStateWithLifecycle()
    val unlockedChatIds by vm.unlockedChatIds.collectAsStateWithLifecycle()
    val pendingUnlockChat by vm.pendingUnlockChatSession.collectAsStateWithLifecycle()
    val autoLockTimeoutMillis by vm.autoLockTimeoutMillis.collectAsStateWithLifecycle()
    val allowDeviceBiometrics by vm.allowDeviceBiometrics.collectAsStateWithLifecycle()
    val biometricAuthMode by vm.biometricAuthMode.collectAsStateWithLifecycle()
    val fingerprintFirstWithPinFallback by vm.fingerprintFirstWithPinFallback.collectAsStateWithLifecycle()
    val enrolledPersonalFaces by vm.enrolledPersonalFaceTargets.collectAsStateWithLifecycle()

    val lockedSessionIds = remember(securityLocks) {
        securityLocks.filter { it.targetType == "CHAT" }.map { it.targetId }.toSet()
    }
    val isCurrentSessionLocked = currentSessionId in lockedSessionIds && currentSessionId !in unlockedChatIds

    NavHost(navController = navController, startDestination = RouteChat) {
        composable<RouteChat> {
            ChatWorkspaceScreen(
                strings = vm.uiStrings(prefs),
                preferences = prefs,
                currentModelLabel = vm.currentModelDisplayLabel(),
                localModels = localModels,
                sessions = sessions,
                currentSessionId = currentSessionId,
                messages = messages,
                streamingAiText = streamingText,
                isGenerating = isGenerating,
                reasoningExpandedMap = reasoningMap,
                pendingAttachments = pendingAtts,
                speechState = speechState,
                sandboxResults = sandboxResults,
                lockedSessionIds = lockedSessionIds,
                isCurrentSessionLocked = isCurrentSessionLocked,
                onRequestUnlockCurrentSession = { vm.requestUnlockCurrentSession() },
                onToggleReasoning = { vm.toggleReasoningExpanded(it) },
                onOpenAdvancedAiOptions = { vm.openAdvancedAiOptionsDialog(true) },
                onOpenLanguageSelectorSpeech = { vm.openLanguageSelector(isSpeechMode = true) },
                onSendPrompt = { vm.sendChatMessage(it) },
                onInputTyping = { vm.onChatInputTyping(it) },
                onSelectGeminiModel = { vm.setSelectedGeminiModel(it) },
                onSelectLocalModel = { vm.switchToLocalModelEngine(it) },
                onNewSession = { vm.createNewSession() },
                onSelectSession = { vm.selectSession(it) },
                onDeleteSession = { vm.deleteSession(it) },
                onAttachUri = { uri, name -> vm.queueAttachmentFromUri(uri, name) },
                onStartVoiceMic = { cb -> vm.speechEngine.startListening(cb) },
                onStopVoiceMic = { vm.speechEngine.stopListening() },
                onQuickExport = { fmt -> vm.triggerExport(fmt) },
                onRunCodeBlock = { k, l, c -> vm.runCodeBlock(k, l, c) },
                onFixCodeWithAi = { l, c, e -> vm.sendCodeErrorToAiFix(l, c, e) },
                onNavigateBrowser = { navController.navigate(RouteBrowser) },
                onNavigateStudio = { navController.navigate(RouteStudio) },
                onNavigatePhotoEditor = { navController.navigate(RoutePhotoEditor) },
                onNavigateHardwareHud = { navController.navigate(RouteHardware) },
                onNavigateVaultSettings = { navController.navigate(RouteVaultSettings) }
            )
        }

        composable<RouteBrowser> {
            MiniBrowserScreen(
                downloadState = browserState,
                onDownloadRequested = { url, ua, cd, mime ->
                    vm.modelDownloadPipeline.handleWebViewDownload(url, ua, cd, mime)
                },
                onDismissBanner = { vm.modelDownloadPipeline.dismissBanner() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteHardware> {
            HardwareMonitorScreen(
                hudSnapshot = hudSnapshot,
                ggufState = ggufState,
                benchmarkEntries = benchmarks,
                onRefreshHud = { vm.refreshHardwareHudNow() },
                onSelectModel = { vm.switchToLocalModelEngine(it) },
                onUpdateTuning = { q, l, b, i -> vm.updateGgufTuning(q, l, b, i) },
                onSuspendModelNow = { vm.ggufManager.suspendModel() },
                onHideModel = { vm.hideLocalModelFromList(it) },
                onTrashModel = { vm.moveLocalModelToTrash(it) },
                onRestoreModel = { vm.restoreModelFromTrash(it) },
                onPermanentDeleteModel = { vm.permanentlyDeleteTrashedModel(it) },
                onOpenBrowser = { navController.navigate(RouteBrowser) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteStudio> {
            StudioWorkspaceScreen(
                exportJobs = exportJobs,
                batchExportResult = batchResult,
                buildQueueJobs = buildJobs,
                toolchains = toolchains,
                decompilerReport = decompReport,
                ipynbReport = ipynbReport,
                decodedLogoResult = logoRes,
                signatureBundle = sigBundle,
                signatureVerified = sigVerified,
                promptTemplates = templates,
                activePersonaId = activePersonaId,
                onExportSingle = { vm.triggerExport(it) },
                onExportBatch = { vm.triggerBatchExport(it) },
                onSaveBatchZip = { uri, bytes, name -> vm.saveBytesToSafUri(uri, bytes, name) },
                onEnqueueBuild = { kind, name, src -> vm.enqueueProjectBuild(kind, name, src) },
                onUpdateToolchains = { vm.runToolchainAutoUpdate() },
                onAnalyzeBinary = { name, bytes -> vm.decompileSampleOrBytes(name, bytes) },
                onRunIpynb = { json, title -> vm.runIpynbNotebook(json, title) },
                onDecodeLogo = { name, bytes, fmt -> vm.decodeLogoSpec(name, bytes, fmt) },
                onSaveLogoPng = { uri, bytes, name -> vm.saveBytesToSafUri(uri, bytes, name) },
                onSignPayload = { text, alg, pw -> vm.signPayload(text, alg, pw) },
                onSelectPersona = { vm.setActivePersona(it) },
                onUsePromptTemplate = { content ->
                    vm.sendChatMessage(content)
                    navController.popBackStack()
                },
                onCreateTemplate = { t, c, body, k, lang -> vm.savePromptTemplate(t, c, body, k, lang) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RoutePhotoEditor> {
            PhotoEditorScreen(
                state = photoState,
                onPickPhotoUri = { vm.loadPhotoForEditing(it) },
                onCreateSamplePhoto = { vm.loadDefaultSamplePhoto() },
                onUpdateParams = { vm.updatePhotoEditParams(it) },
                onSendToChat = {
                    vm.sendEditedPhotoToChat()
                    navController.popBackStack()
                },
                onSavePngToUri = { uri, bytes, name -> vm.saveBytesToSafUri(uri, bytes, name) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteVaultSettings> {
            VaultAndSettingsScreen(
                preferences = prefs,
                isAuthenticatedCloud = isAuthenticatedCloud,
                cloudProfile = cloudProfile,
                cloudPrivateFolders = cloudFolders,
                localVaultFiles = localVault,
                paqSnapshot = paqSnap,
                speechState = speechState,
                secretSummaries = secretSummaries,
                vaultUnlockState = vaultUnlockState,
                apiKeyValidations = apiKeyValidations,
                onSelectAppLanguage = { vm.setAppLanguage(it) },
                onSelectSpeechLanguage = { vm.setSpeechLanguage(it) },
                onSetSpeechWorkers = { vm.speechEngine.setActiveWorkers(it) },
                onDownloadSpeechPack = { vm.speechEngine.downloadOrUpdatePack(it) },
                onDeleteSpeechPack = { vm.speechEngine.deletePack(it) },
                onVerifyLocalPassphrase = { vm.verifyLocalVaultPassphrase(it) },
                onSetLocalPassphrase = { vm.setLocalVaultPassphrase(it) },
                onVaultUnlocked = { vm.markVaultUnlocked(it) },
                onLockVaultNow = { vm.lockVaultNow() },
                onValidateVaultSecret = { vm.validateVaultSecret(it) },
                onSaveVaultSecret = { k, v -> vm.saveVaultSecret(k, v) },
                onRemoveVaultSecret = { vm.removeVaultSecret(it) },
                onSetBiometricEnabled = { vm.setBiometricEnabled(it) },
                onBackupChatToCloud = { vm.backupSessionToCloud() },
                onDownloadCloudItem = { vm.downloadAndInspectPrivateFolderItem(it) },
                onDeleteCloudItem = { vm.deleteCloudVaultItem(it) },
                onDeleteLocalVaultFile = { vm.deleteLocalVaultFile(it) },
                onOpenAdvancedAiOptions = { vm.openAdvancedAiOptionsDialog(true) },
                onOpenSecurityLockHub = { navController.navigate(RouteSecurityLockHub) },
                onSignOutToGateway = { vm.signOutToGateway() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteSecurityLockHub> {
            val activeAppLock = securityLocks.firstOrNull { it.targetType == AppLockManager.TARGET_APP }
            var showDisableAppLockAuthDialog by remember { mutableStateOf(false) }

            SecurityLockHubScreen(
                securityLocks = securityLocks,
                unlimitedAttempts = prefs.lockUnlimitedAttempts,
                maxAttempts = prefs.lockMaxAttempts,
                autoLockTimeoutMillis = autoLockTimeoutMillis,
                allowDeviceBiometrics = allowDeviceBiometrics,
                enrolledPersonalFaceTargets = enrolledPersonalFaces,
                onOpenChatPicker = { navController.navigate(RouteSecurityChatPicker) },
                onOpenAppLockSetupDirect = {
                    navController.navigate(
                        RouteSecuritySetup(
                            targetType = "APP",
                            targetId = "APP",
                            targetLabel = "Khiên bảo vệ ứng dụng",
                            requireConfirmation = activeAppLock == null
                        )
                    )
                },
                onToggleAppLockSwitch = { enable ->
                    if (enable) {
                        navController.navigate(
                            RouteSecuritySetup(
                                targetType = "APP",
                                targetId = "APP",
                                targetLabel = "Khiên bảo vệ ứng dụng",
                                requireConfirmation = true
                            )
                        )
                    } else if (activeAppLock != null) {
                        showDisableAppLockAuthDialog = true
                    }
                },
                onOpenAttemptLimitConfig = { navController.navigate(RouteSecurityAttemptLimit) },
                onOpenAutoLockTimeout = { navController.navigate(RouteSecurityAutoLockTimeout) },
                onSetAllowDeviceBiometrics = { vm.setAllowDeviceBiometrics(it) },
                onRemoveAppLock = {
                    if (activeAppLock != null) {
                        showDisableAppLockAuthDialog = true
                    }
                },
                onBack = { navController.popBackStack() }
            )

            if (showDisableAppLockAuthDialog && activeAppLock != null) {
                AppLockToggleAuthDialog(
                    lock = activeAppLock,
                    hasPersonalFaceEnrolled = AppLockManager.APP_LOCK_ID in enrolledPersonalFaces,
                    allowDeviceBiometric = biometricAuthMode.allowsSystemBiometric,
                    onVerifyTextAndDisable = { candidate ->
                        val ok = vm.verifyTextAndDisableAppLock(activeAppLock, candidate)
                        if (ok) showDisableAppLockAuthDialog = false
                        ok
                    },
                    onVerifyPatternAndDisable = { nodes ->
                        val ok = vm.verifyPatternAndDisableAppLock(activeAppLock, nodes)
                        if (ok) showDisableAppLockAuthDialog = false
                        ok
                    },
                    onVerifyPersonalFaceAndDisable = { vec ->
                        val ok = vm.verifyPersonalFaceAndDisableAppLock(activeAppLock, vec)
                        if (ok) showDisableAppLockAuthDialog = false
                        ok
                    },
                    onBiometricAuthAndDisable = {
                        vm.disableAppLockAfterHardwareAuth(activeAppLock)
                        showDisableAppLockAuthDialog = false
                    },
                    onDismiss = { showDisableAppLockAuthDialog = false },
                    biometricAuthMode = biometricAuthMode,
                    fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                    onTriggerFingerprintFirst = { act, cb ->
                        vm.authenticateFingerprintFirstForLock(act, activeAppLock, cb)
                    },
                    onTriggerPhoneFace = { act, cb ->
                        vm.authenticatePhoneFaceForLock(
                            activity = act,
                            lock = activeAppLock,
                            onSuccess = {
                                vm.disableAppLockAfterHardwareAuth(activeAppLock)
                                showDisableAppLockAuthDialog = false
                            },
                            onError = cb
                        )
                    },
                    onTriggerPhoneCredential = { act, cb ->
                        vm.authenticateDeviceCredentialForLock(
                            activity = act,
                            lock = activeAppLock,
                            onSuccess = {
                                vm.disableAppLockAfterHardwareAuth(activeAppLock)
                                showDisableAppLockAuthDialog = false
                            },
                            onError = cb
                        )
                    },
                    unifiedUnlockManager = vm.unifiedUnlockManager
                )
            }
        }

        composable<RouteSecurityChatPicker> {
            SecurityChatPickerScreen(
                sessions = sessions,
                allMessages = allMessages,
                securityLocks = securityLocks,
                onSelectChatForLock = { session ->
                    navController.navigate(
                        RouteSecuritySetup(
                            targetType = "CHAT",
                            targetId = session.id,
                            targetLabel = session.title
                        )
                    )
                },
                onRemoveChatLock = { sessId -> vm.removeSecurityLock("chat_$sessId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteSecuritySetup> { entry ->
            val args = entry.toRoute<RouteSecuritySetup>()
            val lockId = if (args.targetType == "APP") "app_lock" else "chat_${args.targetId}"
            val existingLock = securityLocks.firstOrNull { it.lockId == lockId }
            var showSetupDisableAppAuth by remember { mutableStateOf(false) }
            var fingerprintCryptoBound by remember { mutableStateOf(vm.isFingerprintCryptoBound()) }

            PasswordSettingsScreen(
                targetType = args.targetType,
                targetId = args.targetId,
                targetLabel = args.targetLabel,
                existingLock = existingLock,
                unlimitedAttempts = prefs.lockUnlimitedAttempts,
                maxAttempts = prefs.lockMaxAttempts,
                requireConfirmation = args.requireConfirmation,
                hasPersonalFaceEnrolled = lockId in enrolledPersonalFaces,
                onSavePersonalFace = { vec -> vm.enrollPersonalFace(lockId, vec) },
                onRemovePersonalFace = { vm.removePersonalFace(lockId) },
                biometricAuthMode = biometricAuthMode,
                onChangeBiometricAuthMode = { vm.setBiometricAuthMode(it) },
                fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                onSetFingerprintFirstWithPinFallback = { vm.setFingerprintFirstWithPinFallback(it) },
                isFingerprintCryptoBound = fingerprintCryptoBound,
                onBindScreenFingerprint = { act, cb ->
                    vm.bindScreenFingerprintCrypto(act) { msg ->
                        fingerprintCryptoBound = vm.isFingerprintCryptoBound()
                        cb(msg)
                    }
                },
                onConfirmSave = { mode, gridSize, textSecret, patternNodes ->
                    vm.saveSecurityLock(
                        targetType = args.targetType,
                        targetId = args.targetId,
                        targetLabel = args.targetLabel,
                        mode = mode,
                        gridSize = gridSize,
                        textSecret = textSecret,
                        patternNodes = patternNodes
                    )
                    navController.popBackStack()
                },
                onRemoveExistingLock = if (existingLock != null) {
                    {
                        if (args.targetType == AppLockManager.TARGET_APP) {
                            showSetupDisableAppAuth = true
                        } else {
                            vm.removeSecurityLock(lockId)
                            navController.popBackStack()
                        }
                    }
                } else null,
                onOpenAttemptLimitConfig = { navController.navigate(RouteSecurityAttemptLimit) },
                onBack = { navController.popBackStack() }
            )

            if (showSetupDisableAppAuth && existingLock != null && args.targetType == AppLockManager.TARGET_APP) {
                AppLockToggleAuthDialog(
                    lock = existingLock,
                    hasPersonalFaceEnrolled = AppLockManager.APP_LOCK_ID in enrolledPersonalFaces,
                    allowDeviceBiometric = biometricAuthMode.allowsSystemBiometric,
                    onVerifyTextAndDisable = { candidate ->
                        val ok = vm.verifyTextAndDisableAppLock(existingLock, candidate)
                        if (ok) {
                            showSetupDisableAppAuth = false
                            navController.popBackStack()
                        }
                        ok
                    },
                    onVerifyPatternAndDisable = { nodes ->
                        val ok = vm.verifyPatternAndDisableAppLock(existingLock, nodes)
                        if (ok) {
                            showSetupDisableAppAuth = false
                            navController.popBackStack()
                        }
                        ok
                    },
                    onVerifyPersonalFaceAndDisable = { vec ->
                        val ok = vm.verifyPersonalFaceAndDisableAppLock(existingLock, vec)
                        if (ok) {
                            showSetupDisableAppAuth = false
                            navController.popBackStack()
                        }
                        ok
                    },
                    onBiometricAuthAndDisable = {
                        vm.disableAppLockAfterHardwareAuth(existingLock)
                        showSetupDisableAppAuth = false
                        navController.popBackStack()
                    },
                    onDismiss = { showSetupDisableAppAuth = false },
                    biometricAuthMode = biometricAuthMode,
                    fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                    onTriggerFingerprintFirst = { act, cb ->
                        vm.authenticateFingerprintFirstForLock(act, existingLock, cb)
                    },
                    onTriggerPhoneFace = { act, cb ->
                        vm.authenticatePhoneFaceForLock(
                            activity = act,
                            lock = existingLock,
                            onSuccess = {
                                vm.disableAppLockAfterHardwareAuth(existingLock)
                                showSetupDisableAppAuth = false
                                navController.popBackStack()
                            },
                            onError = cb
                        )
                    },
                    onTriggerPhoneCredential = { act, cb ->
                        vm.authenticateDeviceCredentialForLock(
                            activity = act,
                            lock = existingLock,
                            onSuccess = {
                                vm.disableAppLockAfterHardwareAuth(existingLock)
                                showSetupDisableAppAuth = false
                                navController.popBackStack()
                            },
                            onError = cb
                        )
                    },
                    unifiedUnlockManager = vm.unifiedUnlockManager
                )
            }
        }

        composable<RouteSecurityAttemptLimit> {
            SecurityAttemptLimitScreen(
                initialUnlimited = prefs.lockUnlimitedAttempts,
                initialMaxAttempts = prefs.lockMaxAttempts,
                onConfirmSave = { unlimited, maxAttempts ->
                    vm.saveSecurityAttemptLimit(unlimited, maxAttempts)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteSecurityAutoLockTimeout> {
            AutoLockTimeoutScreen(
                initialTimeoutMillis = autoLockTimeoutMillis,
                onConfirmSave = { timeoutMs ->
                    vm.setAutoLockTimeout(timeoutMs)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }

    pendingUnlockChat?.let { targetSession ->
        val lock = securityLocks.firstOrNull { it.targetType == "CHAT" && it.targetId == targetSession.id }
        if (lock != null) {
            ChatLockUnlockDialog(
                lock = lock,
                sessionTitle = targetSession.title,
                onVerifyText = { secret -> vm.verifySecurityLockText(lock, secret) },
                onVerifyPattern = { nodes -> vm.verifySecurityLockPattern(lock, nodes) },
                onBiometricRecoverySuccess = { vm.unlockSecurityLockViaBiometric(lock) },
                hasPersonalFaceEnrolled = lock.lockId in enrolledPersonalFaces,
                allowDeviceBiometric = biometricAuthMode.allowsSystemBiometric,
                onVerifyPersonalFace = { vec -> vm.verifySecurityLockPersonalFace(lock, vec) },
                onTriggerDeviceBiometric = { act, cb ->
                    vm.authenticateDeviceBiometricForLock(act, lock, cb)
                },
                biometricAuthMode = biometricAuthMode,
                fingerprintFirstWithPinFallback = fingerprintFirstWithPinFallback,
                onTriggerFingerprintFirst = { act, cb ->
                    vm.authenticateFingerprintFirstForLock(act, lock, cb)
                },
                onTriggerPhoneFace = { act, cb ->
                    vm.authenticatePhoneFaceForLock(act, lock, onError = cb)
                },
                onTriggerPhoneCredential = { act, cb ->
                    vm.authenticateDeviceCredentialForLock(act, lock, onError = cb)
                },
                unifiedUnlockManager = vm.unifiedUnlockManager,
                onDismiss = { vm.dismissPendingChatUnlock() }
            )
        }
    }
}

@Composable
private fun BiometricLockGate(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Super Smart AI đang được bảo vệ", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onUnlock) {
                Text("Mở khóa bằng Sinh trắc học")
            }
        }
    }
}

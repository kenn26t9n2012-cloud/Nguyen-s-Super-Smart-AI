package com.nguyen_super_app.nguyen_super_smart_ai

import android.content.Context
import android.graphics.PointF
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.fragment.app.FragmentActivity
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.CloudSecurityLock
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AdvancedAiFeaturesConfig
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppDatabase
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppPreferences
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BiometricAuthMode
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatMessageEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatSessionEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalRepository
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.SecurityLockEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.UserPreferencesStore
import com.nguyen_super_app.nguyen_super_smart_ai.engine.browser.DownloadPipelinePhase
import com.nguyen_super_app.nguyen_super_smart_ai.engine.browser.ModelDownloadPipeline
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.CompressionFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.DocumentExportEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.LogoDecoderEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.AiModelFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.speech.SpeechEngineState
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.LocalizationStringsProvider
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqArbiterSnapshot
import com.nguyen_super_app.nguyen_super_smart_ai.security.ApiKeyValidationResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.AppLockAuthenticator
import com.nguyen_super_app.nguyen_super_smart_ai.security.AppLockManager
import com.nguyen_super_app.nguyen_super_smart_ai.security.EncryptedSecretVault
import com.nguyen_super_app.nguyen_super_smart_ai.security.EncryptedStorageManager
import com.nguyen_super_app.nguyen_super_smart_ai.security.FaceGeometryExtractor
import com.nguyen_super_app.nguyen_super_smart_ai.security.FaceLivenessTracker
import com.nguyen_super_app.nguyen_super_smart_ai.security.FacialLandmarksInput
import com.nguyen_super_app.nguyen_super_smart_ai.security.FingerprintFirstOutcome
import com.nguyen_super_app.nguyen_super_smart_ai.security.KeyValidationState
import com.nguyen_super_app.nguyen_super_smart_ai.security.LockAttemptResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.PasswordLockMode
import com.nguyen_super_app.nguyen_super_smart_ai.security.PersonalFaceAuthenticator
import com.nguyen_super_app.nguyen_super_smart_ai.security.PersonalFaceVerifyOutcome
import com.nguyen_super_app.nguyen_super_smart_ai.security.SecurityLockCrypto
import com.nguyen_super_app.nguyen_super_smart_ai.security.UnifiedLoginOption
import com.nguyen_super_app.nguyen_super_smart_ai.security.UnifiedUnlockManager
import org.robolectric.Robolectric
import com.nguyen_super_app.nguyen_super_smart_ai.ui.auth.GoogleAuthHelper
import com.nguyen_super_app.nguyen_super_smart_ai.ui.components.LanguageSelector
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.AdvancedAiOptionsDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.PersonalFaceCameraDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.PersonalFaceScanMode
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.UnifiedCompressionPickerDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.AppLockToggleAuthDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.AuthGatewayScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.AutoLockTimeoutScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.ChatWorkspaceScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.PasswordInputTab
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.PasswordSettingsScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.PasswordSettingsStateManager
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.PasswordUnlockGateContent
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.SecurityAttemptLimitScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.SecurityChatPickerScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.SecurityLockHubScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.SecurityPasswordSetupScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.screens.VaultAndSettingsScreen
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SuperSmartAiTheme
import com.nguyen_super_app.nguyen_super_smart_ai.ui.viewmodel.VaultUnlockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun packageAndAppName_matchUnifiedSpec() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.nguyen_super_app.nguyen_super_smart_ai", context.packageName)
        assertEquals("Super Smart AI", context.getString(R.string.app_name))
    }

    @Test
    fun roomGuestMode_persistsSessionsAndMessages() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            db.chatDao().upsertSession(
                ChatSessionEntity("s1", "Guest Session", "gemini-3.8-flash", "CTO", 1000L)
            )
            db.chatDao().insertMessage(
                ChatMessageEntity("m1", "s1", "USER", "Xin chào", "User", timestampMillis = 1001L)
            )
            val msgs = db.chatDao().observeMessages("s1").first()
            assertEquals(1, msgs.size)
            assertEquals("Xin chào", msgs.first().content)
        } finally {
            db.close()
        }
    }

    @Test
    fun encryptedSecretVault_storesAndZerosTransientCharArray() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = EncryptedSecretVault(context)
        vault.storeSecret(EncryptedSecretVault.KEY_WEB_SEARCH_TOKEN, "sk-test-secret-9988")
        assertTrue(vault.hasSecret(EncryptedSecretVault.KEY_WEB_SEARCH_TOKEN))

        var leakedRef: CharArray? = null
        vault.withDecryptedSecret(EncryptedSecretVault.KEY_WEB_SEARCH_TOKEN) { chars ->
            leakedRef = chars
            assertEquals("sk-test-secret-9988", String(chars!!))
        }
        assertTrue("Transient CharArray must be wiped with zero chars after use", leakedRef!!.all { it == '\u0000' })
    }

    @Test
    fun encryptedStorageManager_handlesApiKeyLifecycleAndWipesCharArrays() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = EncryptedStorageManager(context)
        storage.clearAllApiKeys()

        val inputChars = "AIzaSy-test-key-12345".toCharArray()
        storage.saveApiKey(EncryptedStorageManager.KEY_GEMINI_OVERRIDE, inputChars)
        assertTrue("Input CharArray must be zeroed after saveApiKey", inputChars.all { it == '\u0000' })

        assertTrue(storage.hasApiKey(EncryptedStorageManager.KEY_GEMINI_OVERRIDE))
        assertEquals("AIzaSy-test-key-12345", storage.getApiKey(EncryptedStorageManager.KEY_GEMINI_OVERRIDE))
        assertTrue(storage.listStoredApiKeyIds().contains(EncryptedStorageManager.KEY_GEMINI_OVERRIDE))

        var leakedChars: CharArray? = null
        storage.withDecryptedApiKey(EncryptedStorageManager.KEY_GEMINI_OVERRIDE) { chars ->
            leakedChars = chars
            assertEquals("AIzaSy-test-key-12345", String(chars!!))
        }
        assertTrue("Transient CharArray must be zeroed after withDecryptedApiKey", leakedChars!!.all { it == '\u0000' })

        val meta = storage.listApiKeyMetadata().first { it.keyId == EncryptedStorageManager.KEY_GEMINI_OVERRIDE }
        assertTrue(meta.isConfigured)
        assertTrue(meta.maskedPreview.contains("••••"))

        storage.deleteApiKey(EncryptedStorageManager.KEY_GEMINI_OVERRIDE)
        assertFalse(storage.hasApiKey(EncryptedStorageManager.KEY_GEMINI_OVERRIDE))
        assertEquals(null, storage.getApiKey(EncryptedStorageManager.KEY_GEMINI_OVERRIDE))

        // Root package typealias & PBKDF2 local Vault passphrase verification
        val rootStorage = com.nguyen_super_app.nguyen_super_smart_ai.EncryptedStorageManager(context)
        assertFalse(rootStorage.hasLocalVaultPassphrase())
        assertTrue(rootStorage.setLocalVaultPassphrase("Pass@2026".toCharArray()))
        assertTrue(rootStorage.hasLocalVaultPassphrase())
        assertTrue(rootStorage.verifyLocalVaultPassphrase("Pass@2026".toCharArray()))
        assertFalse(rootStorage.verifyLocalVaultPassphrase("WrongPass".toCharArray()))
    }

    @Test
    fun googleAuthHelper_normalizesVietnamAndInternationalPhones() {
        assertEquals("+84912345678", GoogleAuthHelper.normalizePhoneE164("0912 345 678"))
        assertEquals("+84987654321", GoogleAuthHelper.normalizePhoneE164("+84 987-654-321"))
        assertTrue(GoogleAuthHelper.isValidEmail("user@example.com"))
        assertFalse(GoogleAuthHelper.isValidEmail("invalid-email"))
    }

    @Test
    fun vaultSettingsScreen_requiresUnlockBeforeValidate_andDisplaysValidationBadge() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = EncryptedStorageManager(context)
        storage.clearAllApiKeys()
        storage.saveApiKey(EncryptedStorageManager.KEY_GITHUB_PAT, "ghp_test123456789")
        val summaries = EncryptedSecretVault(context).listSummaries()

        var unlockState by mutableStateOf(VaultUnlockState(isUnlocked = false, hasLocalPassphrase = false))
        var validatedKeyId: String? = null
        var validationMap by mutableStateOf<Map<String, ApiKeyValidationResult>>(emptyMap())

        composeRule.setContent {
            SuperSmartAiTheme {
                VaultAndSettingsScreen(
                    preferences = AppPreferences(isGuestMode = true),
                    isAuthenticatedCloud = false,
                    cloudProfile = null,
                    cloudPrivateFolders = emptyList(),
                    localVaultFiles = emptyList(),
                    paqSnapshot = PaqArbiterSnapshot(),
                    speechState = SpeechEngineState(),
                    secretSummaries = summaries,
                    vaultUnlockState = unlockState,
                    apiKeyValidations = validationMap,
                    onSelectAppLanguage = {},
                    onSelectSpeechLanguage = {},
                    onSetSpeechWorkers = {},
                    onDownloadSpeechPack = {},
                    onDeleteSpeechPack = {},
                    onVerifyLocalPassphrase = { storage.verifyLocalVaultPassphrase(it) },
                    onSetLocalPassphrase = { storage.setLocalVaultPassphrase(it) },
                    onVaultUnlocked = { method ->
                        unlockState = unlockState.copy(isUnlocked = true, unlockedMethod = method, hasLocalPassphrase = true)
                    },
                    onLockVaultNow = { unlockState = unlockState.copy(isUnlocked = false) },
                    onValidateVaultSecret = { keyId ->
                        validatedKeyId = keyId
                        validationMap = mapOf(keyId to ApiKeyValidationResult(keyId, KeyValidationState.VALID, "Hợp lệ (GitHub PAT)"))
                    },
                    onSaveVaultSecret = { _, _ -> },
                    onRemoveVaultSecret = {},
                    onSetBiometricEnabled = {},
                    onBackupChatToCloud = {},
                    onDownloadCloudItem = {},
                    onDeleteCloudItem = {},
                    onDeleteLocalVaultFile = {},
                    onOpenAdvancedAiOptions = {},
                    onOpenSecurityLockHub = {},
                    onSignOutToGateway = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Clicking Validate while locked MUST open VaultUnlockDialog first
        composeRule.onNodeWithTag("vault_validate_${EncryptedStorageManager.KEY_GITHUB_PAT}")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(null, validatedKeyId)

        // Switch to Password tab, create local passphrase & unlock
        composeRule.onNodeWithTag("vault_unlock_tab_PASSWORD").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_unlock_password_input").performTextInput("Vault9999")
        composeRule.onNodeWithTag("vault_unlock_password_submit").performClick()
        composeRule.waitForIdle()

        // Pending validation action fires automatically once unlocked and displays badge!
        assertEquals(EncryptedStorageManager.KEY_GITHUB_PAT, validatedKeyId)
        composeRule.onNodeWithTag("vault_validation_msg_${EncryptedStorageManager.KEY_GITHUB_PAT}")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun modelDownloadPipeline_rejectsInvalidExt_andPurgesInvalidArchives() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registered = mutableListOf<String>()
        val pipeline = ModelDownloadPipeline(
            context = context,
            onModelsRegistered = { models, _ ->
                models.forEach { registered.add(it.first.name) }
            }
        )

        // 1. Invalid extension rejected immediately
        pipeline.handleWebViewDownload("https://example.com/setup.exe", null, null, null)
        assertEquals(DownloadPipelinePhase.REJECTED_INVALID_EXTENSION, pipeline.state.value.phase)
        assertTrue(pipeline.state.value.statusMessage.contains("Đuôi mô hình không hợp lệ"))

        // 2. Archive containing 0 valid AI models must be permanently purged
        val junkZip = File(context.cacheDir, "junk_archive.zip")
        ZipOutputStream(junkZip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("readme.txt"))
            zos.write("no model here".toByteArray())
            zos.closeEntry()
        }
        val purgeState = pipeline.processLocalArchiveOrModelFile(junkZip, "https://example.com/junk.zip")
        assertEquals(DownloadPipelinePhase.PURGED_NO_VALID_MODELS, purgeState.phase)
        assertFalse("Invalid archive must be permanently deleted", junkZip.exists())
        assertTrue(registered.isEmpty())

        // 3. Archive containing a nested valid .gguf file succeeds
        val goodZip = File(context.cacheDir, "valid_bundle.zip")
        ZipOutputStream(goodZip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("nested/tiny_q4.gguf"))
            zos.write(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte(), 3, 0, 0, 0))
            zos.closeEntry()
        }
        val okState = pipeline.processLocalArchiveOrModelFile(goodZip, "https://huggingface.co/test/valid_bundle.zip")
        assertEquals(DownloadPipelinePhase.REGISTERED_SUCCESS, okState.phase)
        assertEquals(listOf(AiModelFormat.GGUF), okState.registeredModels.map { it.format })
    }

    @Test
    fun authGatewayScreen_skipGuestAndOAuthSearch_workProperly() {
        var skipped = false
        composeRule.setContent {
            SuperSmartAiTheme {
                AuthGatewayScreen(
                    strings = LocalizationStringsProvider.forLanguage("vi"),
                    currentLanguageCode = "vi",
                    onOpenLanguageSelector = {},
                    onAuthSuccess = {},
                    onSkipGuestMode = { skipped = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("oauth_search_input").performScrollTo().performTextInput("GitHub")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("oauth_chip_github.com").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("skip_guest_button").performScrollTo().performClick()
        assertTrue(skipped)
    }

    @Test
    fun compressionPicker_defaultsToNone() {
        var confirmedFormat: CompressionFormat? = null
        composeRule.setContent {
            SuperSmartAiTheme {
                UnifiedCompressionPickerDialog(
                    fileName = "photo.jpg",
                    originalSizeBytes = 2048L,
                    onConfirm = { fmt, _ -> confirmedFormat = fmt },
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("compression_confirm_button").performClick()
        assertEquals(CompressionFormat.NONE, confirmedFormat)
    }

    @Test
    fun languageSelector_pinsVietnameseFirst_andFilters100Plus() {
        var pickedCode = ""
        composeRule.setContent {
            SuperSmartAiTheme {
                LanguageSelector(
                    selectedCode = "vi",
                    onSelect = { pickedCode = it.code }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_selector_vi_featured").assertIsDisplayed()
        composeRule.onNodeWithTag("language_selector_search_input").performTextInput("Japanese")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_selector_item_ja").assertIsDisplayed().performClick()
        assertEquals("ja", pickedCode)
    }

    @Test
    fun chatWorkspace_strictlyKeepsReasoningCollapsed_untilExplicitToggle_andHasNoHubs() {
        val thinkMsg = ChatMessageEntity(
            id = "msg_reason_1",
            sessionId = "s1",
            role = "AI",
            content = "<think>Bước ẩn bí mật chỉ mở khi người dùng bấm nút</think>\nĐây là câu trả lời trực tiếp.",
            engineTag = "gemini-3.8-flash",
            timestampMillis = 1000L
        )
        val expandedMap = androidx.compose.runtime.mutableStateOf<Map<String, Boolean>>(emptyMap())

        composeRule.setContent {
            SuperSmartAiTheme {
                ChatWorkspaceScreen(
                    strings = LocalizationStringsProvider.forLanguage("vi"),
                    preferences = AppPreferences(isGuestMode = true),
                    currentModelLabel = "gemini-3.8-flash",
                    localModels = emptyList(),
                    sessions = listOf(ChatSessionEntity("s1", "Test", "gemini-3.8-flash", "CTO", 1000L)),
                    currentSessionId = "s1",
                    messages = listOf(thinkMsg),
                    streamingAiText = "",
                    isGenerating = false,
                    reasoningExpandedMap = expandedMap.value,
                    pendingAttachments = emptyList(),
                    speechState = SpeechEngineState(),
                    sandboxResults = emptyMap(),
                    onToggleReasoning = { id ->
                        val cur = expandedMap.value[id] ?: false
                        expandedMap.value = expandedMap.value + (id to !cur)
                    },
                    onOpenAdvancedAiOptions = {},
                    onOpenLanguageSelectorSpeech = {},
                    onSendPrompt = {},
                    onInputTyping = {},
                    onSelectGeminiModel = {},
                    onSelectLocalModel = {},
                    onNewSession = {},
                    onSelectSession = {},
                    onDeleteSession = {},
                    onAttachUri = { _, _ -> },
                    onStartVoiceMic = {},
                    onStopVoiceMic = {},
                    onQuickExport = {},
                    onRunCodeBlock = { _, _, _ -> },
                    onFixCodeWithAi = { _, _, _ -> },
                    onNavigateBrowser = {},
                    onNavigateStudio = {},
                    onNavigatePhotoEditor = {},
                    onNavigateHardwareHud = {},
                    onNavigateVaultSettings = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Verify Quick-Access Icon Bar is rendered and no HUB nodes exist
        composeRule.onNodeWithTag("quick_access_icon_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_nav_browser").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("HUB", substring = true, ignoreCase = true).fetchSemanticsNodes().size)

        // Reasoning body MUST NOT exist initially
        composeRule.onNodeWithTag("reasoning_toggle_button_msg_reason_1").assertTextContains("Mở suy luận")
        composeRule.onNodeWithTag("reasoning_body_msg_reason_1").assertDoesNotExist()
        composeRule.onNodeWithText("Đây là câu trả lời trực tiếp.").assertIsDisplayed()

        // Click explicit toggle button -> reasoning body becomes visible!
        composeRule.onNodeWithTag("reasoning_toggle_button_msg_reason_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reasoning_body_msg_reason_1").assertIsDisplayed()
        composeRule.onNodeWithTag("reasoning_toggle_button_msg_reason_1").assertTextContains("Ẩn suy luận")
    }

    @Test
    fun advancedAiOptionsDialog_togglesPersistChanges() {
        var latestConfig by mutableStateOf(AdvancedAiFeaturesConfig())
        composeRule.setContent {
            SuperSmartAiTheme {
                AdvancedAiOptionsDialog(
                    config = latestConfig,
                    onUpdate = { latestConfig = it },
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("adv_toggle_web_search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("adv_toggle_batch_file_gen").performClick()
        composeRule.waitForIdle()
        assertTrue(latestConfig.enableWebSearch)
        assertTrue(latestConfig.enableSingleBatchFileGen)
    }

    @Test
    fun logoDecoderAndDocumentExport_produceValidPngAndPdfBytes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val logoRes = LogoDecoderEngine.decodeToPng("app_mark.svg", "<svg/>".toByteArray(), 256, "svg")
        assertTrue(logoRes.pngBytes.size > 100)
        // PNG magic: 89 50 4E 47
        assertEquals(0x89.toByte(), logoRes.pngBytes[0])
        assertEquals(0x50.toByte(), logoRes.pngBytes[1])

        val pdfFmt = DocumentExportEngine.supportedFormats.first { it.id == "pdf" }
        val pkg = DocumentExportEngine.exportConversation(
            context = context,
            title = "Robolectric Export",
            messages = listOf(ChatMessageEntity("1", "s", "AI", "Hello PDF", "Gemini", timestampMillis = 1000L)),
            format = pdfFmt
        )
        assertTrue(pkg.parts.first().bytes.isNotEmpty())
        assertEquals("%PDF", String(pkg.parts.first().bytes.copyOfRange(0, 4), Charsets.US_ASCII))
    }

    @Test
    fun securityLockHub_routesDirectlyToAppLockAndChatPicker() {
        var openedPicker = false
        var openedAppLock = false
        var openedAttempts = false
        composeRule.setContent {
            SuperSmartAiTheme {
                SecurityLockHubScreen(
                    securityLocks = emptyList(),
                    unlimitedAttempts = true,
                    maxAttempts = 5L,
                    onOpenChatPicker = { openedPicker = true },
                    onOpenAppLockSetupDirect = { openedAppLock = true },
                    onOpenAttemptLimitConfig = { openedAttempts = true },
                    onRemoveAppLock = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_per_chat_lock_button").performClick()
        composeRule.onNodeWithTag("hub_app_lock_shield_button").performClick()
        composeRule.onNodeWithTag("hub_attempt_limit_button").performClick()
        composeRule.onNodeWithTag("security_hub_cancel_button").assertIsDisplayed()
        assertTrue(openedPicker && openedAppLock && openedAttempts)
    }

    @Test
    fun securityChatPicker_searchesByTitleAndMessageContent() {
        val sessions = listOf(
            ChatSessionEntity("s1", "Kế hoạch du lịch", "Gemini", "CTO", 100L),
            ChatSessionEntity("s2", "Mật mã nội bộ", "Gemini", "CTO", 200L)
        )
        val messages = listOf(
            ChatMessageEntity("m1", "s1", "USER", "Đặt vé đi Đà Nẵng", "User", timestampMillis = 101L),
            ChatMessageEntity("m2", "s2", "AI", "Thuật toán PBKDF2-HMAC-SHA256", "Gemini", timestampMillis = 201L)
        )
        var pickedSessionId = ""
        composeRule.setContent {
            SuperSmartAiTheme {
                SecurityChatPickerScreen(
                    sessions = sessions,
                    allMessages = messages,
                    securityLocks = emptyList(),
                    onSelectChatForLock = { pickedSessionId = it.id },
                    onRemoveChatLock = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        // Filter by message content inside s2
        composeRule.onNodeWithTag("chat_lock_search_input").performTextInput("PBKDF2")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_lock_item_s1").assertDoesNotExist()
        composeRule.onNodeWithTag("chat_lock_item_s2").assertIsDisplayed().performClick()
        assertEquals("s2", pickedSessionId)
    }

    @Test
    fun securityPasswordSetup_supports4Modes_hidesEyeForPattern_andSupports3x3To10x10() {
        var savedMode: PasswordLockMode? = null
        var savedText = ""
        composeRule.setContent {
            SuperSmartAiTheme {
                SecurityPasswordSetupScreen(
                    targetType = "APP",
                    targetId = "APP",
                    targetLabel = "Khiên bảo vệ ứng dụng",
                    existingLock = null,
                    onConfirmSave = { mode, _, txt, _ ->
                        savedMode = mode
                        savedText = txt
                    },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Default DIGITS has visibility toggle
        composeRule.onNodeWithTag("security_password_visibility_toggle").performScrollTo().assertIsDisplayed()

        // Switch to PATTERN -> visibility toggle must NOT exist, grid sizes 3..10 selectable
        composeRule.onNodeWithTag("mode_chip_PATTERN").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("security_password_visibility_toggle").assertDoesNotExist()
        composeRule.onNodeWithTag("grid_size_10").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pattern_lock_canvas_10x10").performScrollTo().assertIsDisplayed()

        // Switch to ALPHANUMERIC, enter valid alphanumeric secret, confirm OK
        composeRule.onNodeWithTag("mode_chip_ALPHANUMERIC").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("security_password_visibility_toggle").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("security_password_input").performScrollTo().performTextInput("Super99")
        composeRule.onNodeWithTag("security_setup_ok_button").performClick()
        composeRule.waitForIdle()
        assertEquals(PasswordLockMode.ALPHANUMERIC, savedMode)
        assertEquals("Super99", savedText)
    }

    @Test
    fun securityAttemptLimit_disablesControlsWhenUnlimited_andAcceptsArbitraryPositiveCount() {
        var savedUnlimited: Boolean? = null
        var savedCount = 0L
        composeRule.setContent {
            SuperSmartAiTheme {
                SecurityAttemptLimitScreen(
                    initialUnlimited = true,
                    initialMaxAttempts = 5L,
                    onConfirmSave = { unlim, cnt ->
                        savedUnlimited = unlim
                        savedCount = cnt
                    },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // When Unlimited is ON, controls below MUST be disabled
        composeRule.onNodeWithTag("attempt_limit_custom_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("attempt_limit_number_input").assertIsNotEnabled()

        // Turn Unlimited OFF -> controls become enabled; enter arbitrarily large integer
        composeRule.onNodeWithTag("attempt_limit_unlimited_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("attempt_limit_custom_button").assertIsEnabled()
        composeRule.onNodeWithTag("attempt_limit_number_input").assertIsEnabled()
        composeRule.onNodeWithTag("attempt_limit_number_input").performTextClearance()
        composeRule.onNodeWithTag("attempt_limit_number_input").performTextInput("250000")
        composeRule.onNodeWithTag("attempt_limit_ok_button").performClick()
        composeRule.waitForIdle()
        assertEquals(false, savedUnlimited)
        assertEquals(250000L, savedCount)
    }

    @Test
    fun appLockManager_enforcesAppAndChatRestrictions_customRetryLimits_andCloudSync() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val localRepo = LocalRepository(db)
            val prefsStore = UserPreferencesStore(context)
            prefsStore.setSecurityAttemptLimitPolicy(unlimitedAttempts = false, maxAttempts = 2L)

            val manager = AppLockManager(
                localRepo = localRepo,
                prefsStore = prefsStore,
                scope = managerScope
            )

            // 1. Save App Lock with retry limit = 2
            val appLock = manager.saveLockAndSync(
                targetType = AppLockManager.TARGET_APP,
                targetId = AppLockManager.TARGET_APP,
                targetLabel = "Khiên bảo vệ ứng dụng",
                mode = PasswordLockMode.DIGITS,
                gridSize = 0,
                textSecret = "2468",
                patternNodes = emptyList()
            )
            assertFalse(appLock.unlimitedAttempts)
            assertEquals(2L, appLock.maxAttempts)

            // Relock app and verify failed attempts -> lockout -> biometric recovery
            manager.lockAppNow()
            assertTrue(manager.isAppLockedNow())

            val firstFail = manager.verifyTextPasscodeAndPersist(appLock, "0000")
            assertTrue(firstFail is LockAttemptResult.WrongAttempt)
            assertEquals(1L, (firstFail as LockAttemptResult.WrongAttempt).remainingAttempts)

            val secondFail = manager.verifyTextPasscodeAndPersist(firstFail.lock, "9999")
            assertTrue(secondFail is LockAttemptResult.LockedOut)
            assertTrue(manager.isAppLockedNow())

            // Even correct secret fails while locked out
            val blockedEvenIfCorrect = manager.verifyTextPasscodeAndPersist(secondFail.lock, "2468")
            assertTrue(blockedEvenIfCorrect is LockAttemptResult.LockedOut)

            // Biometric recovery resets failedAttempts & unlocks
            val recovered = manager.unlockViaBiometricAndPersist(secondFail.lock)
            assertFalse(recovered.lock.lockedOut)
            assertEquals(0L, recovered.lock.failedAttempts)
            assertFalse(manager.isAppLockedNow())

            // 2. Per-Conversation 10x10 Pattern Lock + Access Gating
            val chatLock = manager.saveLockAndSync(
                targetType = AppLockManager.TARGET_CHAT,
                targetId = "chat_vip",
                targetLabel = "Phiên VIP",
                mode = PasswordLockMode.PATTERN,
                gridSize = 10,
                textSecret = "",
                patternNodes = listOf(0, 11, 22, 33)
            )
            manager.lockConversationNow("chat_vip")
            assertTrue(manager.isConversationLocked("chat_vip"))
            assertFalse(manager.requestConversationAccess("chat_vip"))
            assertEquals("chat_vip", manager.pendingUnlockChatId.value)
            assertTrue(manager.requestConversationAccess("chat_public"))

            val chatUnlock = manager.verifyPatternPasscodeAndPersist(chatLock, listOf(0, 11, 22, 33))
            assertTrue(chatUnlock is LockAttemptResult.Unlocked)
            assertFalse(manager.isConversationLocked("chat_vip"))
            assertNull(manager.pendingUnlockChatId.value)

            // 3. Custom retry limit update (unlimited = false, large count = 750000) & Cloud sync merge
            val updatedCap = manager.updateRetryLimitAndSync(unlimitedAttempts = false, maxAttempts = 750000L)
            assertEquals(750000L, updatedCap)
            assertEquals(750000L, localRepo.getSecurityLock("chat_chat_vip")?.maxAttempts)

            manager.syncFromCloud(
                listOf(
                    CloudSecurityLock(
                        id = AppLockManager.POLICY_LOCK_ID,
                        targetType = AppLockManager.TARGET_POLICY,
                        targetId = "GLOBAL_POLICY",
                        passwordMode = AppLockManager.TARGET_POLICY,
                        unlimitedAttempts = true,
                        maxAttempts = 0L
                    )
                )
            )
            assertTrue(prefsStore.preferencesFlow.first().lockUnlimitedAttempts)
        } finally {
            managerScope.cancel()
            db.close()
        }
    }

    @Test
    fun passwordSettingsStateManager_andScreen_handleTabsVisibilityToggleAnd10x10Pattern() {
        // 1. State Manager unit check
        val manager = PasswordSettingsStateManager()
        assertEquals(PasswordInputTab.NUMERIC_TEXT, manager.currentState.activeTab)
        assertTrue(manager.currentState.canTogglePasswordVisibility)

        manager.togglePasswordVisibility()
        assertTrue(manager.currentState.isPasswordVisible)

        manager.onTextInputChanged("12abc34")
        assertEquals("1234", manager.currentState.textSecret) // Filtered strictly for DIGITS

        // Switching to PATTERN resets text buffer, disables visibility toggle & turns off isPasswordVisible
        manager.selectTab(PasswordInputTab.PATTERN)
        assertEquals(PasswordLockMode.PATTERN, manager.currentState.selectedMode)
        assertFalse(manager.currentState.isPasswordVisible)
        assertFalse(manager.currentState.canTogglePasswordVisibility)
        manager.togglePasswordVisibility()
        assertFalse(manager.currentState.isPasswordVisible)

        // 2. Compose UI Tabbed Screen test
        var savedMode: PasswordLockMode? = null
        var savedText = ""
        composeRule.setContent {
            SuperSmartAiTheme {
                PasswordSettingsScreen(
                    targetType = "APP",
                    targetId = "APP",
                    targetLabel = "Khiên bảo vệ ứng dụng",
                    existingLock = null,
                    unlimitedAttempts = false,
                    maxAttempts = 10L,
                    onConfirmSave = { mode, _, txt, _ ->
                        savedMode = mode
                        savedText = txt
                    },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Numeric/Text Tab shows password visibility toggle and reveals text when clicked
        composeRule.onNodeWithTag("security_password_visibility_toggle").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("security_password_input").performScrollTo().performTextInput("4321")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("password_visual_feedback_bar").performScrollTo().assertTextContains("4321")

        // Switch to Pattern Tab (up to 10x10) -> visibility toggle must NOT exist
        composeRule.onNodeWithTag("password_tab_pattern").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("security_password_visibility_toggle").assertDoesNotExist()
        composeRule.onNodeWithTag("grid_size_10").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pattern_lock_canvas_10x10").performScrollTo().assertIsDisplayed()

        // Switch back to Numeric/Text Tab -> select ALPHANUMERIC -> submit valid secret
        composeRule.onNodeWithTag("password_tab_numeric_text").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mode_chip_ALPHANUMERIC").performScrollTo().performClick()
        composeRule.onNodeWithTag("security_password_input").performScrollTo().performTextInput("Smart99")
        composeRule.onNodeWithTag("security_setup_ok_button").performClick()
        composeRule.waitForIdle()

        assertEquals(PasswordLockMode.ALPHANUMERIC, savedMode)
        assertEquals("Smart99", savedText)
    }

    @Test
    fun appLockManager_autoLockTimeout_relocksAppAndChats_andDisableRequiresPassword() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val localRepo = LocalRepository(db)
            val prefsStore = UserPreferencesStore(context)
            val faceAuth = PersonalFaceAuthenticator(context)
            faceAuth.clearAllTemplates()
            val authenticator = AppLockAuthenticator(context)
            // Verify checkCapability executes cleanly on Robolectric JVM
            authenticator.checkCapability()

            var nowMs = 10_000L
            val manager = AppLockManager(
                localRepo = localRepo,
                prefsStore = prefsStore,
                authenticator = authenticator,
                personalFaceAuth = faceAuth,
                clockElapsedMillis = { nowMs },
                scope = managerScope
            )

            // Save App Lock (unlocked on creation) + Per-Chat Lock (unlocked on creation)
            manager.saveLockAndSync(
                targetType = AppLockManager.TARGET_APP,
                targetId = AppLockManager.TARGET_APP,
                targetLabel = "App",
                mode = PasswordLockMode.CUSTOM,
                gridSize = 0,
                textSecret = "@Secret#1",
                patternNodes = emptyList()
            )
            manager.saveLockAndSync(
                targetType = AppLockManager.TARGET_CHAT,
                targetId = "vip_chat",
                targetLabel = "VIP",
                mode = PasswordLockMode.DIGITS,
                gridSize = 0,
                textSecret = "9876",
                patternNodes = emptyList()
            )
            val enrolledVec = FloatArray(FaceGeometryExtractor.VECTOR_SIZE) { if (it == 0) 1f else 0f }
            assertTrue(manager.enrollPersonalFace(AppLockManager.APP_LOCK_ID, enrolledVec))

            // 1. Set 1-minute timeout (60_000ms) -> 30s background keeps BOTH unlocked
            manager.setAutoLockTimeoutAndSync(60_000L)
            manager.onAppBackgrounded(nowMs)
            nowMs += 30_000L
            assertFalse(manager.onAppForegrounded(nowMs))
            assertFalse(manager.isAppLockedNow())
            assertFalse(manager.isConversationLocked("vip_chat"))

            // 2. Background for >= 60_000ms locks BOTH App Lock AND unlocked conversations!
            manager.onAppBackgrounded(nowMs)
            nowMs += 65_000L
            assertTrue(manager.onAppForegrounded(nowMs))
            assertTrue(manager.isAppLockedNow())
            assertTrue(manager.isConversationLocked("vip_chat"))

            // 3. "-1L" (Never auto-lock) stays unlocked even after long background
            manager.unlockViaBiometricAndPersist(localRepo.getSecurityLock(AppLockManager.APP_LOCK_ID)!!)
            manager.setAutoLockTimeoutAndSync(AppPreferences.AUTO_LOCK_NEVER)
            manager.onAppBackgrounded(nowMs)
            nowMs += 500_000L
            assertFalse(manager.onAppForegrounded(nowMs))
            assertFalse(manager.isAppLockedNow())

            // 4. "0L" (Immediate lock) locks immediately on resume
            manager.setAutoLockTimeoutAndSync(AppPreferences.AUTO_LOCK_IMMEDIATE)
            manager.onAppBackgrounded(nowMs)
            assertTrue(manager.onAppForegrounded(nowMs))
            assertTrue(manager.isAppLockedNow())

            // 5. Password-authenticated App Lock disablement:
            // Wrong password must fail & preserve app_lock + personal face
            val wrongDisable = manager.verifyTextAndDisableAppLock("wrong")
            assertTrue(wrongDisable is LockAttemptResult.WrongAttempt)
            assertNotNull(localRepo.getSecurityLock(AppLockManager.APP_LOCK_ID))
            assertTrue(faceAuth.hasEnrolledFace(AppLockManager.APP_LOCK_ID))

            // Correct password verifies and deletes app_lock + personal face template
            val okDisable = manager.verifyTextAndDisableAppLock("@Secret#1")
            assertTrue(okDisable is LockAttemptResult.Unlocked)
            assertNull(localRepo.getSecurityLock(AppLockManager.APP_LOCK_ID))
            assertFalse(faceAuth.hasEnrolledFace(AppLockManager.APP_LOCK_ID))
        } finally {
            managerScope.cancel()
            db.close()
        }
    }

    @Test
    fun faceGeometryExtractor_andPersonalFaceAuthenticator_matchAndIsolateAccurately() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val faceAuth = PersonalFaceAuthenticator(context)
        faceAuth.clearAllTemplates()

        // Same person's landmarks at base scale vs translated + 1.8x scaled
        val personA1 = FacialLandmarksInput(
            leftEye = PointF(100f, 100f),
            rightEye = PointF(200f, 100f),
            noseBase = PointF(150f, 145f),
            mouthLeft = PointF(122f, 192f),
            mouthRight = PointF(178f, 192f),
            mouthBottom = PointF(150f, 206f),
            leftCheek = PointF(96f, 152f),
            rightCheek = PointF(204f, 152f),
            leftEyeOpenProb = 0.9f,
            rightEyeOpenProb = 0.9f
        )
        val personA2ScaledTranslated = FacialLandmarksInput(
            leftEye = PointF(280f, 250f),
            rightEye = PointF(460f, 250f),
            noseBase = PointF(370f, 331f),
            mouthLeft = PointF(319.6f, 415.6f),
            mouthRight = PointF(420.4f, 415.6f),
            mouthBottom = PointF(370f, 440.8f),
            leftCheek = PointF(272.8f, 343.6f),
            rightCheek = PointF(467.2f, 343.6f),
            leftEyeOpenProb = 0.15f,
            rightEyeOpenProb = 0.15f
        )
        // Distinctly proportioned face B
        val personB = FacialLandmarksInput(
            leftEye = PointF(100f, 100f),
            rightEye = PointF(200f, 100f),
            noseBase = PointF(150f, 185f),
            mouthLeft = PointF(105f, 235f),
            mouthRight = PointF(195f, 235f),
            mouthBottom = PointF(150f, 255f),
            leftCheek = PointF(70f, 175f),
            rightCheek = PointF(230f, 175f)
        )

        val vecA1 = FaceGeometryExtractor.extractNormalizedSignature(personA1)!!
        val vecA2 = FaceGeometryExtractor.extractNormalizedSignature(personA2ScaledTranslated)!!
        val vecB = FaceGeometryExtractor.extractNormalizedSignature(personB)!!

        assertTrue(faceAuth.saveFaceTemplate(AppLockManager.APP_LOCK_ID, vecA1))
        assertTrue(faceAuth.verifyCandidate(AppLockManager.APP_LOCK_ID, vecA2) is PersonalFaceVerifyOutcome.Matched)
        assertTrue(faceAuth.verifyCandidate(AppLockManager.APP_LOCK_ID, vecB) is PersonalFaceVerifyOutcome.Mismatch)

        // Liveness tracker multi-frame + blink check
        val tracker = FaceLivenessTracker(requiredStableFrames = 2)
        val p1 = tracker.processFrame(personA1, requireBlink = true)
        assertFalse(p1.readyToComplete)
        val p2 = tracker.processFrame(personA2ScaledTranslated, requireBlink = true)
        assertTrue(p2.readyToComplete)
        assertNotNull(p2.averagedVector)
    }

    @Test
    fun passwordSettings_customMode_autoLockTimeout_toggleAuth_andIsolationUiWork() {
        var step by mutableIntStateOf(0)
        var savedTimeout = -999L
        var customSaved: String? = null
        var toggledDisable = false
        var isolatedChanged: Boolean? = null

        val sampleLock = SecurityLockEntity(
            lockId = "app_lock",
            targetType = "APP",
            targetId = "APP",
            targetLabel = "App",
            passwordMode = "CUSTOM",
            patternGridSize = 0,
            saltBase64 = "salt",
            hashBase64 = "hash",
            unlimitedAttempts = true,
            maxAttempts = 0L
        )

        composeRule.setContent {
            SuperSmartAiTheme {
                when (step) {
                    0 -> AutoLockTimeoutScreen(
                        initialTimeoutMillis = 300_000L,
                        onConfirmSave = { savedTimeout = it },
                        onBack = {}
                    )
                    1 -> PasswordSettingsScreen(
                        targetType = "APP",
                        targetId = "APP",
                        targetLabel = "App",
                        existingLock = null,
                        requireConfirmation = true,
                        hasPersonalFaceEnrolled = false,
                        onConfirmSave = { mode, _, secret, _ ->
                            if (mode == PasswordLockMode.CUSTOM) customSaved = secret
                        },
                        onBack = {}
                    )
                    2 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        PasswordUnlockGateContent(
                            lock = sampleLock,
                            title = "Khóa App",
                            allowCancel = false,
                            onVerifyText = { false },
                            onVerifyPattern = { false },
                            onBiometricRecoverySuccess = {},
                            onCancel = {},
                            hasPersonalFaceEnrolled = true,
                            allowDeviceBiometric = false
                        )
                    }
                    else -> SecurityLockHubScreen(
                        securityLocks = listOf(sampleLock),
                        unlimitedAttempts = true,
                        maxAttempts = 5L,
                        autoLockTimeoutMillis = 60_000L,
                        allowDeviceBiometrics = false,
                        enrolledPersonalFaceTargets = setOf("app_lock"),
                        onOpenChatPicker = {},
                        onOpenAppLockSetupDirect = {},
                        onOpenAttemptLimitConfig = {},
                        onRemoveAppLock = { toggledDisable = true },
                        onToggleAppLockSwitch = { enable -> if (!enable) toggledDisable = true },
                        onSetAllowDeviceBiometrics = { isolatedChanged = it },
                        onBack = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("timeout_preset_60000").performScrollTo().performClick()
        composeRule.onNodeWithTag("auto_lock_timeout_ok_button").performClick()
        composeRule.waitForIdle()
        assertEquals(60_000L, savedTimeout)

        // PasswordSettingsScreen supports CUSTOM ("Tự ý") + confirmation + Personal Face button
        step = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("setup_personal_face_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("mode_chip_CUSTOM").performScrollTo().performClick()
        composeRule.onNodeWithTag("security_password_input").performScrollTo().performTextInput("K!9")
        composeRule.onNodeWithTag("security_password_confirm_input").performScrollTo().performTextInput("K!9")
        composeRule.onNodeWithTag("security_setup_ok_button").performClick()
        composeRule.waitForIdle()
        assertEquals("K!9", customSaved)

        // PasswordUnlockGateContent respects allowDeviceBiometric = false & renders Personal Face button when enrolled
        step = 2
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unlock_personal_face_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("unlock_biometric_button").assertDoesNotExist()

        // Hub screen exposes auto-lock timeout button, App Lock switch, and device biometric isolation switch
        step = 3
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_auto_lock_timeout_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("hub_app_lock_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("hub_device_biometric_isolation_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(toggledDisable)
        assertEquals(true, isolatedChanged)
    }

    @Test
    fun dualAuthMode_fingerprintFirstHandoff_andMandatoryBlinkLivenessWork() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsStore = UserPreferencesStore(context)

        // 1. UserPreferencesStore Dual Auth Mode & Fingerprint-First Fallback sync
        prefsStore.setBiometricAuthMode(BiometricAuthMode.DUAL_PARALLEL)
        var pref = prefsStore.preferencesFlow.first()
        assertEquals(BiometricAuthMode.DUAL_PARALLEL, pref.biometricAuthMode)
        assertTrue(pref.allowDeviceBiometricsForLock)
        assertTrue(pref.fingerprintFirstWithPinFallback)

        prefsStore.setBiometricAuthMode(BiometricAuthMode.PERSONAL_FACE)
        prefsStore.setFingerprintFirstWithPinFallback(false)
        pref = prefsStore.preferencesFlow.first()
        assertEquals(BiometricAuthMode.PERSONAL_FACE, pref.biometricAuthMode)
        assertFalse(pref.allowDeviceBiometricsForLock)
        assertFalse(pref.fingerprintFirstWithPinFallback)

        prefsStore.setBiometricAuthMode(BiometricAuthMode.PASSWORD_ONLY)
        pref = prefsStore.preferencesFlow.first()
        assertEquals(BiometricAuthMode.PASSWORD_ONLY, pref.biometricAuthMode)
        assertFalse(pref.allowDeviceBiometricsForLock)

        // 2. AppLockAuthenticator Keystore CryptoObject status & graceful handoff without hardware Keystore
        val auth = AppLockAuthenticator(context)
        auth.isStrongFingerprintAvailable()
        assertFalse(auth.isFingerprintCryptoBound())
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        var handoffOutcome: FingerprintFirstOutcome? = null
        auth.authenticateFingerprintFirst(activity) { outcome ->
            handoffOutcome = outcome
        }
        assertTrue(handoffOutcome is FingerprintFirstOutcome.SwitchMethodRequested)
        assertTrue((handoffOutcome as FingerprintFirstOutcome.SwitchMethodRequested).isSensorUnavailableOrLocked)

        // 3. Mandatory blink liveness rejects static open eyes even after stable frames, accepts after blink
        val openEyesFrame = FacialLandmarksInput(
            leftEye = PointF(100f, 100f),
            rightEye = PointF(200f, 100f),
            noseBase = PointF(150f, 145f),
            mouthLeft = PointF(122f, 192f),
            mouthRight = PointF(178f, 192f),
            leftEyeOpenProb = 0.92f,
            rightEyeOpenProb = 0.91f
        )
        val blinkFrame = openEyesFrame.copy(leftEyeOpenProb = 0.12f, rightEyeOpenProb = 0.14f)

        val liveness = FaceLivenessTracker(requiredStableFrames = 2)
        val r1 = liveness.processFrame(openEyesFrame, requireBlink = true)
        assertFalse(r1.readyToComplete)
        val r2 = liveness.processFrame(openEyesFrame, requireBlink = true)
        assertFalse("Static photo with no blink must NOT pass mandatory blink liveness", r2.readyToComplete)
        assertFalse(r2.blinkDetected)
        val r3 = liveness.processFrame(blinkFrame, requireBlink = true)
        assertTrue("Open -> blink transition must satisfy mandatory liveness", r3.readyToComplete)
        assertTrue(r3.blinkDetected)
    }

    @Test
    fun passwordSettingsAndUnlockGate_renderDualAuthModesAndMultiPathFallbacks() {
        var step by mutableIntStateOf(0)
        var authMode by mutableStateOf(BiometricAuthMode.PASSWORD_ONLY)
        var fpFirst by mutableStateOf(true)
        var bindClicked = false
        var gateMode by mutableStateOf(BiometricAuthMode.DUAL_PARALLEL)

        val lock = SecurityLockEntity(
            lockId = "app_lock",
            targetType = "APP",
            targetId = "APP",
            targetLabel = "App",
            passwordMode = "DIGITS",
            patternGridSize = 0,
            saltBase64 = "salt",
            hashBase64 = "hash",
            unlimitedAttempts = true,
            maxAttempts = 0L
        )

        composeRule.setContent {
            val hostActivity = remember {
                Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
            }
            SuperSmartAiTheme {
                if (step == 0) {
                    CompositionLocalProvider(LocalContext provides hostActivity) {
                        PasswordSettingsScreen(
                            targetType = "APP",
                            targetId = "APP",
                            targetLabel = "App",
                            existingLock = null,
                            hasPersonalFaceEnrolled = true,
                            biometricAuthMode = authMode,
                            onChangeBiometricAuthMode = { authMode = it },
                            fingerprintFirstWithPinFallback = fpFirst,
                            onSetFingerprintFirstWithPinFallback = { fpFirst = it },
                            isFingerprintCryptoBound = false,
                            onBindScreenFingerprint = { _, cb ->
                                bindClicked = true
                                cb("OK")
                            },
                            onConfirmSave = { _, _, _, _ -> },
                            onBack = {}
                        )
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        PasswordUnlockGateContent(
                            lock = lock,
                            title = "Mở khóa Dual",
                            allowCancel = false,
                            onVerifyText = { false },
                            onVerifyPattern = { false },
                            onBiometricRecoverySuccess = {},
                            onCancel = {},
                            hasPersonalFaceEnrolled = true,
                            allowDeviceBiometric = gateMode.allowsSystemBiometric,
                            biometricAuthMode = gateMode,
                            fingerprintFirstWithPinFallback = false
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // Toggle Dual Auth Switch ON -> activates DUAL_PARALLEL and exposes fingerprint-first + CryptoObject bind button
        composeRule.onNodeWithTag("dual_auth_mode_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dual_auth_mode_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(BiometricAuthMode.DUAL_PARALLEL, authMode)
        composeRule.onNodeWithTag("fingerprint_first_fallback_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bind_screen_fingerprint_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(bindClicked)

        // Individual chips switch cleanly between SYSTEM_BIOMETRIC, PERSONAL_FACE, and DUAL_PARALLEL
        composeRule.onNodeWithTag("auth_mode_chip_SYSTEM_BIOMETRIC").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(BiometricAuthMode.SYSTEM_BIOMETRIC, authMode)

        composeRule.onNodeWithTag("auth_mode_chip_PERSONAL_FACE").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(BiometricAuthMode.PERSONAL_FACE, authMode)
        composeRule.onNodeWithTag("bind_screen_fingerprint_button").assertDoesNotExist()

        // Verify Unlock Gate renders all 4 seamless unlock/fallback buttons in DUAL_PARALLEL
        step = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unlock_personal_face_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("unlock_biometric_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("unlock_phone_face_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("unlock_phone_credential_button").performScrollTo().assertIsDisplayed()

        // Switch to PERSONAL_FACE -> hides phone buttons while preserving App Personal Face ID button
        gateMode = BiometricAuthMode.PERSONAL_FACE
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unlock_personal_face_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("unlock_biometric_button").assertDoesNotExist()
        composeRule.onNodeWithTag("unlock_phone_face_button").assertDoesNotExist()
        composeRule.onNodeWithTag("unlock_phone_credential_button").assertDoesNotExist()

        // Switch to PASSWORD_ONLY -> hides all biometric/face buttons & shows isolated note
        gateMode = BiometricAuthMode.PASSWORD_ONLY
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unlock_personal_face_button").assertDoesNotExist()
        composeRule.onNodeWithTag("unlock_biometric_button").assertDoesNotExist()
        composeRule.onNodeWithTag("unlock_biometric_isolated_note").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unifiedUnlockManager_coordinatesBiometricsPersonalFaceAndDynamicOptions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val faceAuth = PersonalFaceAuthenticator(context)
        faceAuth.clearAllTemplates()
        val appLockAuth = AppLockAuthenticator(context)
        val unified = UnifiedUnlockManager(
            appLockAuthenticator = appLockAuth,
            personalFaceAuthenticator = faceAuth
        )

        val lock = SecurityLockEntity(
            lockId = AppLockManager.APP_LOCK_ID,
            targetType = "APP",
            targetId = "APP",
            targetLabel = "App",
            passwordMode = "DIGITS",
            patternGridSize = 0,
            saltBase64 = "salt",
            hashBase64 = "hash",
            unlimitedAttempts = true,
            maxAttempts = 0L
        )
        val template = FloatArray(FaceGeometryExtractor.VECTOR_SIZE) { if (it == 0) 1f else 0f }
        assertTrue(faceAuth.saveFaceTemplate(lock.lockId, template))

        // 1. Dynamic BottomSheet option filtering across BiometricAuthMode values
        assertEquals(
            listOf(
                UnifiedLoginOption.SYSTEM_UNLOCK,
                UnifiedLoginOption.APP_CUSTOM_PIN_PATTERN,
                UnifiedLoginOption.PERSONAL_FACE
            ),
            unified.getEnabledLoginOptions(lock, BiometricAuthMode.DUAL_PARALLEL, allowDeviceBiometric = true)
        )
        assertEquals(
            listOf(
                UnifiedLoginOption.SYSTEM_UNLOCK,
                UnifiedLoginOption.APP_CUSTOM_PIN_PATTERN
            ),
            unified.getEnabledLoginOptions(lock, BiometricAuthMode.SYSTEM_BIOMETRIC, allowDeviceBiometric = true)
        )
        assertEquals(
            listOf(
                UnifiedLoginOption.APP_CUSTOM_PIN_PATTERN,
                UnifiedLoginOption.PERSONAL_FACE
            ),
            unified.getEnabledLoginOptions(lock, BiometricAuthMode.PERSONAL_FACE, allowDeviceBiometric = false)
        )
        assertEquals(
            listOf(UnifiedLoginOption.APP_CUSTOM_PIN_PATTERN),
            unified.getEnabledLoginOptions(lock, BiometricAuthMode.PASSWORD_ONLY, allowDeviceBiometric = false)
        )

        // 2. Personal Face mismatch immediately exposes fallback / other login options & unlocks on match
        unified.beginUnlockSession(lock.lockId)
        assertFalse(unified.uiState.value.showOtherOptionsButton)
        val wrongVec = FloatArray(FaceGeometryExtractor.VECTOR_SIZE) { if (it == 5) 1f else 0f }
        var failedMsg = ""
        unified.verifyPersonalFaceCandidate(
            lockId = lock.lockId,
            candidateVector = wrongVec,
            onMatched = {},
            onFailed = { failedMsg = it }
        )
        assertTrue(failedMsg.isNotBlank())
        assertTrue(unified.uiState.value.hasBiometricFailedOrDismissed)
        assertTrue(unified.uiState.value.showOtherOptionsButton)

        var matched = false
        unified.verifyPersonalFaceCandidate(
            lockId = lock.lockId,
            candidateVector = template,
            onMatched = { matched = true },
            onFailed = {}
        )
        assertTrue(matched)
        assertFalse(unified.uiState.value.showOtherOptionsButton)

        // 3. Biometric fallback on fingerprint-first failure when Keystore is unavailable
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        var fallbackReason: String? = null
        unified.authenticateFingerprintFirst(
            activity = activity,
            onUnlocked = {},
            onFallbackNeeded = { fallbackReason = it }
        )
        assertNotNull(fallbackReason)
        assertTrue(unified.uiState.value.hasBiometricFailedOrDismissed)
        assertTrue(unified.uiState.value.showOtherOptionsButton)
    }

    @Test
    fun authGate_revealsOtherLoginOptionsAfter2sOrFirstFailure_andBottomSheetSwitchesCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val unified = UnifiedUnlockManager(
            appLockAuthenticator = AppLockAuthenticator(context),
            personalFaceAuthenticator = PersonalFaceAuthenticator(context)
        )
        val lock = SecurityLockEntity(
            lockId = AppLockManager.APP_LOCK_ID,
            targetType = "APP",
            targetId = "APP",
            targetLabel = "App",
            passwordMode = "DIGITS",
            patternGridSize = 0,
            saltBase64 = "salt",
            hashBase64 = "hash",
            unlimitedAttempts = true,
            maxAttempts = 0L
        )
        var screenStep by mutableIntStateOf(0)
        var authMode by mutableStateOf(BiometricAuthMode.DUAL_PARALLEL)
        var credentialTriggered = false
        var switchFromFaceClicked = false

        composeRule.setContent {
            val hostActivity = remember {
                Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
            }
            SuperSmartAiTheme {
                when (screenStep) {
                    0 -> CompositionLocalProvider(LocalContext provides hostActivity) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            PasswordUnlockGateContent(
                                lock = lock,
                                title = "Mở khóa bảo mật",
                                allowCancel = false,
                                onVerifyText = { false },
                                onVerifyPattern = { false },
                                onBiometricRecoverySuccess = {},
                                onCancel = {},
                                hasPersonalFaceEnrolled = true,
                                allowDeviceBiometric = authMode.allowsSystemBiometric,
                                biometricAuthMode = authMode,
                                fingerprintFirstWithPinFallback = false,
                                onTriggerFingerprintFirst = { _, cb ->
                                    cb(FingerprintFirstOutcome.SwitchMethodRequested("Cảm biến bận", false))
                                },
                                onTriggerPhoneCredential = { _, cb ->
                                    credentialTriggered = true
                                    cb(null)
                                },
                                unifiedUnlockManager = unified,
                                biometricExpiryDelayMs = 2_000L
                            )
                        }
                    }
                    else -> PersonalFaceCameraDialog(
                        mode = PersonalFaceScanMode.VERIFY,
                        title = "Xác thực khuôn mặt",
                        onSwitchToAlternativeMethod = { switchFromFaceClicked = true },
                        onDismiss = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // 1. Before 2 seconds & before failure, 'Tùy chọn đăng nhập khác' must NOT appear yet
        composeRule.onNodeWithTag("unlock_other_options_button").assertDoesNotExist()

        // 2. Advance 2 seconds (2000ms biometric expiry) -> 'Tùy chọn đăng nhập khác' appears
        composeRule.mainClock.advanceTimeBy(2_100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unlock_other_options_button").performScrollTo().assertIsDisplayed()

        // Open BottomSheet & verify enabled options: System Unlock, App PIN/Pattern, Personal Face
        composeRule.onNodeWithTag("unlock_other_options_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("other_login_options_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_option_system_unlock").assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_option_app_pin_pattern").assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_option_personal_face").assertIsDisplayed()

        // Select 'Mã PIN/Mẫu tùy chỉnh ứng dụng' -> closes sheet & displays transition notice
        composeRule.onNodeWithTag("sheet_option_app_pin_pattern").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("other_login_options_sheet").assertDoesNotExist()
        composeRule.onNodeWithTag("unlock_switch_method_button").performScrollTo().assertIsDisplayed()

        // Switch mode to SYSTEM_BIOMETRIC (Personal Face disabled) & fail fingerprint -> immediate reveal + no Personal Face option in sheet
        authMode = BiometricAuthMode.SYSTEM_BIOMETRIC
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unlock_biometric_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unlock_switch_method_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("unlock_other_options_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sheet_option_system_unlock").assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_option_app_pin_pattern").assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_option_personal_face").assertDoesNotExist()

        // Tap 'Mở khóa hệ thống' -> invokes Device Credential flow immediately
        composeRule.onNodeWithTag("sheet_option_system_unlock").performClick()
        composeRule.waitForIdle()
        assertTrue(credentialTriggered)

        // 3. PersonalFaceCameraDialog VERIFY mode exposes 'Chuyển đổi phương thức' button
        screenStep = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("personal_face_switch_method_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(switchFromFaceClicked)
    }
}

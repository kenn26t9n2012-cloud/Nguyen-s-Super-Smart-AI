package com.nguyen_super_app.nguyen_super_smart_ai.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "super_smart_ai_prefs_v2")

data class AdvancedAiFeaturesConfig(
    val enableWebSearch: Boolean = false,
    val enableSingleBatchFileGen: Boolean = false,
    val enableCodeLanguageHook: Boolean = true,
    val enableReasoningOutputTagging: Boolean = true
)

enum class BiometricAuthMode(
    val id: String,
    val displayLabel: String,
    val subtitle: String
) {
    PASSWORD_ONLY(
        id = "PASSWORD_ONLY",
        displayLabel = "Chỉ mật khẩu / hình vẽ",
        subtitle = "Không dùng vân tay hoặc khuôn mặt khi mở khóa."
    ),
    SYSTEM_BIOMETRIC(
        id = "SYSTEM_BIOMETRIC",
        displayLabel = "Sinh trắc học hệ thống",
        subtitle = "Ưu tiên vân tay màn hình (Keystore CryptoObject) kèm dự phòng khuôn mặt & PIN điện thoại."
    ),
    PERSONAL_FACE(
        id = "PERSONAL_FACE",
        displayLabel = "ID khuôn mặt cá nhân",
        subtitle = "Dùng CameraX + ML Kit trong app, hoàn toàn độc lập khỏi sinh trắc học điện thoại."
    ),
    DUAL_PARALLEL(
        id = "DUAL_PARALLEL",
        displayLabel = "Xác thực kép (Cả hai)",
        subtitle = "Bật song song Sinh trắc học hệ thống và ID khuôn mặt cá nhân trong ứng dụng."
    );

    val allowsSystemBiometric: Boolean
        get() = this == SYSTEM_BIOMETRIC || this == DUAL_PARALLEL

    val allowsPersonalFace: Boolean
        get() = this == PERSONAL_FACE || this == DUAL_PARALLEL

    companion object {
        fun fromId(raw: String?): BiometricAuthMode =
            entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: PASSWORD_ONLY
    }
}

data class AppPreferences(
    val languageCode: String = "vi",
    val speechLanguageCode: String = "vi",
    val isGuestMode: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val selectedGeminiModel: String = "gemini-3.8-flash",
    val useLocalGgufEngine: Boolean = false,
    val gpuOffloadLayers: Int = 24,
    val batterySuspendThreshold: Int = 15,
    val autoSuspendMinutes: Int = 10,
    val lockUnlimitedAttempts: Boolean = true,
    val lockMaxAttempts: Long = 5L,
    val autoLockTimeoutMillis: Long = DEFAULT_AUTO_LOCK_TIMEOUT_MS,
    val allowDeviceBiometricsForLock: Boolean = false,
    val biometricAuthMode: BiometricAuthMode = BiometricAuthMode.PASSWORD_ONLY,
    val fingerprintFirstWithPinFallback: Boolean = true,
    val advancedAiFeatures: AdvancedAiFeaturesConfig = AdvancedAiFeaturesConfig()
) {
    companion object {
        const val AUTO_LOCK_IMMEDIATE = 0L
        const val DEFAULT_AUTO_LOCK_TIMEOUT_MS = 5 * 60_000L
        const val AUTO_LOCK_NEVER = -1L
    }
}

class UserPreferencesStore(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SPEECH_LANGUAGE_CODE = stringPreferencesKey("speech_language_code")
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val USE_LOCAL_GGUF = booleanPreferencesKey("use_local_gguf")
        val GPU_LAYERS = intPreferencesKey("gpu_layers")
        val BATTERY_THRESHOLD = intPreferencesKey("battery_threshold")
        val AUTO_SUSPEND_MIN = intPreferencesKey("auto_suspend_min")
        val LOCK_UNLIMITED_ATTEMPTS = booleanPreferencesKey("lock_unlimited_attempts")
        val LOCK_MAX_ATTEMPTS = longPreferencesKey("lock_max_attempts")
        val AUTO_LOCK_TIMEOUT_MS = longPreferencesKey("auto_lock_timeout_ms")
        val ALLOW_DEVICE_BIOMETRICS_FOR_LOCK = booleanPreferencesKey("allow_device_biometrics_for_lock")
        val BIOMETRIC_AUTH_MODE = stringPreferencesKey("biometric_auth_mode")
        val FINGERPRINT_FIRST_FALLBACK = booleanPreferencesKey("fingerprint_first_with_pin_fallback")
        val ADV_WEB_SEARCH = booleanPreferencesKey("adv_web_search")
        val ADV_BATCH_FILE_GEN = booleanPreferencesKey("adv_batch_file_gen")
        val ADV_CODE_HOOK = booleanPreferencesKey("adv_code_hook")
        val ADV_REASONING_TAG = booleanPreferencesKey("adv_reasoning_tag")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val legacyAllowDevice = prefs[Keys.ALLOW_DEVICE_BIOMETRICS_FOR_LOCK] ?: false
        val storedMode = prefs[Keys.BIOMETRIC_AUTH_MODE]?.let { BiometricAuthMode.fromId(it) }
            ?: if (legacyAllowDevice) BiometricAuthMode.SYSTEM_BIOMETRIC else BiometricAuthMode.PASSWORD_ONLY

        AppPreferences(
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "vi",
            speechLanguageCode = prefs[Keys.SPEECH_LANGUAGE_CODE] ?: "vi",
            isGuestMode = prefs[Keys.IS_GUEST_MODE] ?: false,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            selectedGeminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-3.8-flash",
            useLocalGgufEngine = prefs[Keys.USE_LOCAL_GGUF] ?: false,
            gpuOffloadLayers = prefs[Keys.GPU_LAYERS] ?: 24,
            batterySuspendThreshold = prefs[Keys.BATTERY_THRESHOLD] ?: 15,
            autoSuspendMinutes = prefs[Keys.AUTO_SUSPEND_MIN] ?: 10,
            lockUnlimitedAttempts = prefs[Keys.LOCK_UNLIMITED_ATTEMPTS] ?: true,
            lockMaxAttempts = (prefs[Keys.LOCK_MAX_ATTEMPTS] ?: 5L).coerceAtLeast(1L),
            autoLockTimeoutMillis = (prefs[Keys.AUTO_LOCK_TIMEOUT_MS] ?: AppPreferences.DEFAULT_AUTO_LOCK_TIMEOUT_MS).coerceAtLeast(-1L),
            allowDeviceBiometricsForLock = storedMode.allowsSystemBiometric || legacyAllowDevice,
            biometricAuthMode = storedMode,
            fingerprintFirstWithPinFallback = prefs[Keys.FINGERPRINT_FIRST_FALLBACK] ?: true,
            advancedAiFeatures = AdvancedAiFeaturesConfig(
                enableWebSearch = prefs[Keys.ADV_WEB_SEARCH] ?: false,
                enableSingleBatchFileGen = prefs[Keys.ADV_BATCH_FILE_GEN] ?: false,
                enableCodeLanguageHook = prefs[Keys.ADV_CODE_HOOK] ?: true,
                enableReasoningOutputTagging = prefs[Keys.ADV_REASONING_TAG] ?: true
            )
        )
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setSpeechLanguageCode(code: String) {
        context.dataStore.edit { it[Keys.SPEECH_LANGUAGE_CODE] = code }
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_GUEST_MODE] = enabled }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setSelectedGeminiModel(model: String) {
        context.dataStore.edit { it[Keys.GEMINI_MODEL] = model }
    }

    suspend fun setUseLocalGguf(useLocal: Boolean) {
        context.dataStore.edit { it[Keys.USE_LOCAL_GGUF] = useLocal }
    }

    suspend fun setGgufTuning(gpuLayers: Int, batteryThreshold: Int, autoSuspendMinutes: Int) {
        context.dataStore.edit {
            it[Keys.GPU_LAYERS] = gpuLayers
            it[Keys.BATTERY_THRESHOLD] = batteryThreshold
            it[Keys.AUTO_SUSPEND_MIN] = autoSuspendMinutes
        }
    }

    suspend fun updateAdvancedAiFeatures(config: AdvancedAiFeaturesConfig) {
        context.dataStore.edit {
            it[Keys.ADV_WEB_SEARCH] = config.enableWebSearch
            it[Keys.ADV_BATCH_FILE_GEN] = config.enableSingleBatchFileGen
            it[Keys.ADV_CODE_HOOK] = config.enableCodeLanguageHook
            it[Keys.ADV_REASONING_TAG] = config.enableReasoningOutputTagging
        }
    }

    suspend fun setSecurityAttemptLimitPolicy(unlimitedAttempts: Boolean, maxAttempts: Long) {
        context.dataStore.edit {
            it[Keys.LOCK_UNLIMITED_ATTEMPTS] = unlimitedAttempts
            it[Keys.LOCK_MAX_ATTEMPTS] = maxAttempts.coerceAtLeast(1L)
        }
    }

    suspend fun setAutoLockTimeoutMillis(timeoutMillis: Long) {
        val safe = timeoutMillis.coerceIn(AppPreferences.AUTO_LOCK_NEVER, 86_400_000L)
        context.dataStore.edit {
            it[Keys.AUTO_LOCK_TIMEOUT_MS] = safe
        }
    }

    suspend fun setAllowDeviceBiometricsForLock(allowed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ALLOW_DEVICE_BIOMETRICS_FOR_LOCK] = allowed
            val current = BiometricAuthMode.fromId(prefs[Keys.BIOMETRIC_AUTH_MODE])
            val nextMode = when {
                allowed && current == BiometricAuthMode.PERSONAL_FACE -> BiometricAuthMode.DUAL_PARALLEL
                allowed && current == BiometricAuthMode.PASSWORD_ONLY -> BiometricAuthMode.SYSTEM_BIOMETRIC
                !allowed && current == BiometricAuthMode.DUAL_PARALLEL -> BiometricAuthMode.PERSONAL_FACE
                !allowed && current == BiometricAuthMode.SYSTEM_BIOMETRIC -> BiometricAuthMode.PASSWORD_ONLY
                else -> current
            }
            prefs[Keys.BIOMETRIC_AUTH_MODE] = nextMode.id
        }
    }

    suspend fun setBiometricAuthMode(mode: BiometricAuthMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_AUTH_MODE] = mode.id
            prefs[Keys.ALLOW_DEVICE_BIOMETRICS_FOR_LOCK] = mode.allowsSystemBiometric
        }
    }

    suspend fun setFingerprintFirstWithPinFallback(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.FINGERPRINT_FIRST_FALLBACK] = enabled
        }
    }
}

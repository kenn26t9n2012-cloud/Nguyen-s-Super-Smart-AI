package com.nguyen_super_app.nguyen_super_smart_ai.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Metadata summary for a stored encrypted API key entry.
 */
data class StoredApiKeyMetadata(
    val keyId: String,
    val label: String,
    val isConfigured: Boolean,
    val maskedPreview: String,
    val updatedAtMillis: Long,
    val isHardwareKeystoreBacked: Boolean
)

/**
 * Centralized secure storage manager backed by Android Keystore (AES-256-GCM MasterKey)
 * and Jetpack Security EncryptedSharedPreferences (AES256_SIV keys / AES256_GCM values).
 *
 * Includes automatic corruption recovery if Keystore / AEAD keys become invalidated,
 * PBKDF2-HMAC-SHA256 local Vault passphrase support, and transient CharArray zeroing.
 */
class EncryptedStorageManager(context: Context) {

    companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "super_smart_ai_keystore_master_key"
        const val ENCRYPTED_PREFS_FILE = "super_smart_ai_aes256_gcm_vault"
        private const val META_UPDATED_AT_SUFFIX = "__updated_at"
        private const val INTERNAL_PASSPHRASE_HASH_KEY = "__vault_passphrase_pbkdf2_hash"
        private const val INTERNAL_PASSPHRASE_SALT_KEY = "__vault_passphrase_salt"
        private const val PBKDF2_ITERATIONS = 12_000

        const val KEY_WEB_SEARCH_TOKEN = "web_search_api_token"
        const val KEY_HUGGINGFACE_TOKEN = "huggingface_read_token"
        const val KEY_CUSTOM_OPENAI_COMPAT = "custom_endpoint_bearer"
        const val KEY_GITHUB_PAT = "github_personal_token"
        const val KEY_GEMINI_OVERRIDE = "gemini_custom_api_key"

        val standardApiKeyDescriptors: List<Pair<String, String>> = listOf(
            KEY_WEB_SEARCH_TOKEN to "Khóa API Tìm kiếm Web (Serper / Tavily / Google CSE)",
            KEY_HUGGINGFACE_TOKEN to "Hugging Face Token (Tải mô hình giới hạn quyền)",
            KEY_CUSTOM_OPENAI_COMPAT to "Bearer Token Endpoint Tùy chỉnh",
            KEY_GITHUB_PAT to "GitHub Personal Access Token",
            KEY_GEMINI_OVERRIDE to "Khóa API Gemini Tùy chỉnh (Tùy chọn)"
        )
    }

    private val appContext: Context = context.applicationContext

    @Volatile
    var isKeystoreBacked: Boolean = false
        private set

    @Volatile
    var lastRecoveryReason: String? = null
        private set

    @Volatile
    private var prefsInstance: SharedPreferences? = null
    private val lock = Any()

    private fun getOrCreatePrefs(): SharedPreferences {
        prefsInstance?.let { return it }
        synchronized(lock) {
            prefsInstance?.let { return it }
            val created = createEncryptedPreferences(allowRecoveryRetry = true)
            prefsInstance = created
            return created
        }
    }

    private fun createEncryptedPreferences(allowRecoveryRetry: Boolean): SharedPreferences {
        return try {
            buildKeystoreEncryptedPrefs()
        } catch (ex: GeneralSecurityException) {
            if (allowRecoveryRetry) {
                recoverCorruptedVault(ex.javaClass.simpleName)
                try {
                    return buildKeystoreEncryptedPrefs()
                } catch (_: Throwable) {
                }
            }
            fallbackPrefs(ex.javaClass.simpleName)
        } catch (ex: Throwable) {
            // Non-Keystore JVM Robolectric environments degrade cleanly without crashing tests
            fallbackPrefs(ex.javaClass.simpleName)
        }
    }

    private fun buildKeystoreEncryptedPrefs(): SharedPreferences {
        val keySpec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val masterKey = MasterKey.Builder(appContext, MASTER_KEY_ALIAS)
            .setKeyGenParameterSpec(keySpec)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            appContext,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        isKeystoreBacked = verifyKeystoreEntryPresent()
        return prefs
    }

    private fun fallbackPrefs(reason: String?): SharedPreferences {
        isKeystoreBacked = false
        if (reason != null && lastRecoveryReason == null) {
            lastRecoveryReason = "Chế độ tương thích phần mềm ($reason)"
        }
        return appContext.getSharedPreferences("${ENCRYPTED_PREFS_FILE}_fallback", Context.MODE_PRIVATE)
    }

    private fun recoverCorruptedVault(reason: String) {
        lastRecoveryReason = "Tự động khôi phục MasterKey AES-256-GCM ($reason)"
        try {
            appContext.deleteSharedPreferences(ENCRYPTED_PREFS_FILE)
        } catch (_: Throwable) {
        }
        try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            if (ks.containsAlias(MASTER_KEY_ALIAS)) {
                ks.deleteEntry(MASTER_KEY_ALIAS)
            }
        } catch (_: Throwable) {
        }
        prefsInstance = null
    }

    private inline fun <T> runWithRecovery(block: (SharedPreferences) -> T): T {
        return try {
            block(getOrCreatePrefs())
        } catch (ex: GeneralSecurityException) {
            synchronized(lock) {
                recoverCorruptedVault(ex.javaClass.simpleName)
                val rebuilt = createEncryptedPreferences(allowRecoveryRetry = false)
                prefsInstance = rebuilt
                block(rebuilt)
            }
        }
    }

    private fun verifyKeystoreEntryPresent(): Boolean {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            ks.containsAlias(MASTER_KEY_ALIAS)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Encrypts and persists an API key using a transient CharArray that is immediately wiped.
     */
    fun saveApiKey(keyId: String, apiKey: CharArray) {
        if (keyId.isBlank()) {
            Arrays.fill(apiKey, '\u0000')
            return
        }
        val normalized = String(apiKey).trim()
        try {
            if (normalized.isEmpty()) {
                deleteApiKey(keyId)
            } else {
                runWithRecovery { prefs ->
                    prefs.edit {
                        putString(keyId, normalized)
                        putLong("$keyId$META_UPDATED_AT_SUFFIX", System.currentTimeMillis())
                    }
                }
            }
        } finally {
            Arrays.fill(apiKey, '\u0000')
        }
    }

    /**
     * Encrypts and persists an API key string.
     */
    fun saveApiKey(keyId: String, apiKey: String) {
        saveApiKey(keyId, apiKey.toCharArray())
    }

    /**
     * Retrieves a decrypted API key, or null if not configured or blank.
     */
    fun getApiKey(keyId: String): String? {
        if (keyId.isBlank()) return null
        val value = runWithRecovery { it.getString(keyId, null) }
        return value?.takeIf { it.isNotBlank() }
    }

    /**
     * Decrypts the API key into a transient CharArray for active invocation and zeroes it on exit.
     */
    inline fun <T> withDecryptedApiKey(keyId: String, block: (CharArray?) -> T): T {
        val transient = readTransientApiKeyChars(keyId)
        return try {
            block(transient)
        } finally {
            if (transient != null) {
                Arrays.fill(transient, '\u0000')
            }
        }
    }

    @PublishedApi
    internal fun readTransientApiKeyChars(keyId: String): CharArray? {
        val raw = getApiKey(keyId)
        return raw?.toCharArray()
    }

    fun hasApiKey(keyId: String): Boolean = getApiKey(keyId) != null

    fun getKeyUpdatedAtMillis(keyId: String): Long {
        if (keyId.isBlank()) return 0L
        return runWithRecovery { it.getLong("$keyId$META_UPDATED_AT_SUFFIX", 0L) }
    }

    fun deleteApiKey(keyId: String) {
        if (keyId.isBlank()) return
        runWithRecovery { prefs ->
            prefs.edit {
                remove(keyId)
                remove("$keyId$META_UPDATED_AT_SUFFIX")
            }
        }
    }

    fun listStoredApiKeyIds(): Set<String> {
        return runWithRecovery { prefs ->
            prefs.all.keys
                .filterNot {
                    it.endsWith(META_UPDATED_AT_SUFFIX) ||
                        it == INTERNAL_PASSPHRASE_HASH_KEY ||
                        it == INTERNAL_PASSPHRASE_SALT_KEY
                }
                .filter { !prefs.getString(it, null).isNullOrBlank() }
                .toSet()
        }
    }

    fun listApiKeyMetadata(
        descriptors: List<Pair<String, String>> = standardApiKeyDescriptors
    ): List<StoredApiKeyMetadata> {
        return descriptors.map { (id, label) ->
            val raw = getApiKey(id)
            val configured = raw != null
            val masked = if (raw != null) {
                if (raw.length <= 6) "••••••"
                else "${raw.take(3)}••••••••${raw.takeLast(3)}"
            } else {
                "Chưa thiết lập"
            }
            StoredApiKeyMetadata(
                keyId = id,
                label = label,
                isConfigured = configured,
                maskedPreview = masked,
                updatedAtMillis = getKeyUpdatedAtMillis(id),
                isHardwareKeystoreBacked = isKeystoreBacked
            )
        }
    }

    fun hasLocalVaultPassphrase(): Boolean {
        return runWithRecovery {
            !it.getString(INTERNAL_PASSPHRASE_HASH_KEY, null).isNullOrBlank() &&
                !it.getString(INTERNAL_PASSPHRASE_SALT_KEY, null).isNullOrBlank()
        }
    }

    fun setLocalVaultPassphrase(passphrase: CharArray): Boolean {
        if (passphrase.size < 4) {
            Arrays.fill(passphrase, '\u0000')
            return false
        }
        return try {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val hash = pbkdf2(passphrase, salt)
            runWithRecovery { prefs ->
                prefs.edit {
                    putString(INTERNAL_PASSPHRASE_SALT_KEY, Base64.getEncoder().encodeToString(salt))
                    putString(INTERNAL_PASSPHRASE_HASH_KEY, Base64.getEncoder().encodeToString(hash))
                }
            }
            true
        } finally {
            Arrays.fill(passphrase, '\u0000')
        }
    }

    fun verifyLocalVaultPassphrase(candidate: CharArray): Boolean {
        return try {
            val (saltB64, hashB64) = runWithRecovery { prefs ->
                prefs.getString(INTERNAL_PASSPHRASE_SALT_KEY, null) to
                    prefs.getString(INTERNAL_PASSPHRASE_HASH_KEY, null)
            }
            if (saltB64.isNullOrBlank() || hashB64.isNullOrBlank()) return false
            val salt = Base64.getDecoder().decode(saltB64)
            val expectedHash = Base64.getDecoder().decode(hashB64)
            val actualHash = pbkdf2(candidate, salt)
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: Throwable) {
            false
        } finally {
            Arrays.fill(candidate, '\u0000')
        }
    }

    private fun pbkdf2(passphrase: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun isBiometricAvailable(): Boolean {
        return try {
            val bm = BiometricManager.from(appContext)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            bm.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Authenticates user identity via AndroidX BiometricPrompt (BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
     * directly inside EncryptedStorageManager before permitting Vault key access or decryption.
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        title: String = "Xác thực Keystore Vault",
        subtitle: String = "Sử dụng sinh trắc học hoặc mã thiết bị để truy cập khóa AES-256-GCM",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val bm = BiometricManager.from(activity)
        if (bm.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Thiết bị chưa bật sinh trắc học hoặc khóa màn hình. Hãy chọn Mật khẩu hoặc OTP.")
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onFailed()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    /**
     * Enforces BiometricPrompt verification before decrypting the requested API key
     * into a transient CharArray, which is wiped immediately after [onDecrypted] completes.
     */
    fun decryptApiKeyWithBiometric(
        activity: FragmentActivity,
        keyId: String,
        title: String = "Giải mã khóa API Vault",
        subtitle: String = "Xác thực sinh trắc học trước khi đọc khóa bí mật",
        onDecrypted: (CharArray?) -> Unit,
        onError: (String) -> Unit
    ) {
        authenticateWithBiometric(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                val transient = readTransientApiKeyChars(keyId)
                try {
                    onDecrypted(transient)
                } finally {
                    if (transient != null) {
                        Arrays.fill(transient, '\u0000')
                    }
                }
            },
            onError = onError,
            onFailed = { onError("Sinh trắc học không khớp, vui lòng thử lại.") }
        )
    }

    fun clearAllApiKeys() {
        runWithRecovery { prefs ->
            prefs.edit {
                clear()
            }
        }
    }
}

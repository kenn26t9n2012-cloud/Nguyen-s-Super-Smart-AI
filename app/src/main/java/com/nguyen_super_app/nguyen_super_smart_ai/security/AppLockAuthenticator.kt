package com.nguyen_super_app.nguyen_super_smart_ai.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class BiometricCapability {
    HARDWARE_AVAILABLE,
    DEVICE_CREDENTIAL_ONLY,
    NOT_ENROLLED,
    UNAVAILABLE
}

sealed interface BiometricAuthResult {
    data class Succeeded(val authenticationType: Int) : BiometricAuthResult
    data object Failed : BiometricAuthResult
    data class Canceled(val reason: String) : BiometricAuthResult
    data class Error(val errorCode: Int, val message: String) : BiometricAuthResult
    data class Unavailable(val reason: String) : BiometricAuthResult
}

/**
 * Multi-path outcome for Fingerprint-First on-screen verification paired with
 * Android Keystore [BiometricPrompt.CryptoObject].
 */
sealed interface FingerprintFirstOutcome {
    data object CryptoSucceeded : FingerprintFirstOutcome
    data class SystemFaceOrCredentialSucceeded(val authenticationType: Int) : FingerprintFirstOutcome
    data class SwitchMethodRequested(
        val reason: String,
        val isSensorUnavailableOrLocked: Boolean
    ) : FingerprintFirstOutcome
    data class KeyInvalidated(val message: String) : FingerprintFirstOutcome
}

/**
 * Hardware-backed Biometric & System Credential authenticator using `androidx.biometric`.
 *
 * Implements:
 * 1) **Screen Fingerprint via Android Keystore `CryptoObject`**: Binds `BIOMETRIC_STRONG`
 *    on-screen fingerprint authentication to an app-owned AES-256-GCM key requiring user
 *    authentication per use, sealing & decrypting a 32-byte verification challenge.
 * 2) **Multi-Path Seamless Fallback**: Hands off cleanly to Phone Face (`BIOMETRIC_WEAK`),
 *    Phone PIN/Pattern/Password (`DEVICE_CREDENTIAL`), App Personal Face ID, or App Password/Pattern
 *    when fingers are wet/scratched, sensor fails/locks out, or user taps to switch.
 */
class AppLockAuthenticator(context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val CHALLENGE_KEY_ALIAS = "super_smart_ai_biometric_challenge_key"
        private const val CHALLENGE_PREFS_MASTER_ALIAS = "super_smart_ai_biometric_prefs_master"
        private const val CHALLENGE_PREFS_FILE = "super_smart_ai_biometric_challenge_store"
        private const val KEY_CHALLENGE_CIPHERTEXT = "challenge_ct"
        private const val KEY_CHALLENGE_IV = "challenge_iv"
        private const val KEY_CHALLENGE_SHA256 = "challenge_sha256"
        private const val GCM_TAG_BITS = 128
        private const val MAX_SOFT_FINGER_RETRIES = 3
    }

    private val appContext = context.applicationContext
    private val storeLock = Any()

    @Volatile
    private var challengePrefs: SharedPreferences? = null

    private fun getChallengeStore(): SharedPreferences {
        challengePrefs?.let { return it }
        synchronized(storeLock) {
            challengePrefs?.let { return it }
            val created = try {
                val spec = KeyGenParameterSpec.Builder(
                    CHALLENGE_PREFS_MASTER_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                val mk = MasterKey.Builder(appContext, CHALLENGE_PREFS_MASTER_ALIAS)
                    .setKeyGenParameterSpec(spec)
                    .build()
                EncryptedSharedPreferences.create(
                    appContext,
                    CHALLENGE_PREFS_FILE,
                    mk,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Throwable) {
                appContext.getSharedPreferences("${CHALLENGE_PREFS_FILE}_fallback", Context.MODE_PRIVATE)
            }
            challengePrefs = created
            return created
        }
    }

    fun checkCapability(): BiometricCapability {
        return try {
            val bm = BiometricManager.from(appContext)
            val strongOrWeak = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            when (bm.canAuthenticate(strongOrWeak)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.HARDWARE_AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    if (bm.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
                        BiometricManager.BIOMETRIC_SUCCESS
                    ) {
                        BiometricCapability.DEVICE_CREDENTIAL_ONLY
                    } else {
                        BiometricCapability.NOT_ENROLLED
                    }
                }
                else -> {
                    val combined = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    if (bm.canAuthenticate(combined) == BiometricManager.BIOMETRIC_SUCCESS) {
                        BiometricCapability.DEVICE_CREDENTIAL_ONLY
                    } else {
                        BiometricCapability.UNAVAILABLE
                    }
                }
            }
        } catch (_: Throwable) {
            BiometricCapability.UNAVAILABLE
        }
    }

    fun isStrongFingerprintAvailable(): Boolean {
        return try {
            BiometricManager.from(appContext)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    fun isHardwareBiometricAvailable(): Boolean =
        checkCapability() == BiometricCapability.HARDWARE_AVAILABLE

    fun isAnyDeviceAuthAvailable(): Boolean {
        val cap = checkCapability()
        return cap == BiometricCapability.HARDWARE_AVAILABLE ||
            cap == BiometricCapability.DEVICE_CREDENTIAL_ONLY
    }

    fun isFingerprintCryptoBound(): Boolean {
        val store = getChallengeStore()
        return !store.getString(KEY_CHALLENGE_CIPHERTEXT, null).isNullOrBlank() &&
            !store.getString(KEY_CHALLENGE_IV, null).isNullOrBlank() &&
            !store.getString(KEY_CHALLENGE_SHA256, null).isNullOrBlank()
    }

    fun clearFingerprintChallenge() {
        getChallengeStore().edit { clear() }
        try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            if (ks.containsAlias(CHALLENGE_KEY_ALIAS)) {
                ks.deleteEntry(CHALLENGE_KEY_ALIAS)
            }
        } catch (_: Throwable) {
        }
    }

    private fun getOrCreateStrongKey(forceRecreate: Boolean = false): SecretKey? {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            if (forceRecreate && ks.containsAlias(CHALLENGE_KEY_ALIAS)) {
                ks.deleteEntry(CHALLENGE_KEY_ALIAS)
            }
            val existing = ks.getKey(CHALLENGE_KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val builder = KeyGenParameterSpec.Builder(
                CHALLENGE_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
            builder.setInvalidatedByBiometricEnrollment(true)
            keyGen.init(builder.build())
            keyGen.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    private fun prepareEncryptCipher(): Cipher? {
        return try {
            val key = getOrCreateStrongKey(forceRecreate = false) ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher
        } catch (ex: KeyPermanentlyInvalidatedException) {
            throw ex
        } catch (_: Throwable) {
            null
        }
    }

    private fun prepareDecryptCipher(): Cipher? {
        val store = getChallengeStore()
        val ivB64 = store.getString(KEY_CHALLENGE_IV, null) ?: return null
        val iv = Base64.getDecoder().decode(ivB64)
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            val key = ks.getKey(CHALLENGE_KEY_ALIAS, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher
        } catch (ex: KeyPermanentlyInvalidatedException) {
            throw ex
        } catch (_: Throwable) {
            null
        }
    }

    private fun sha256Base64(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getEncoder().encodeToString(digest)
    }

    /**
     * Step 1 (Fingerprint-First):
     * Authenticates on-screen fingerprint via [BiometricManager.Authenticators.BIOMETRIC_STRONG]
     * paired with an app-bound Android Keystore [BiometricPrompt.CryptoObject].
     *
     * If no challenge has been sealed yet, it seals a fresh 32-byte challenge with `ENCRYPT_MODE`.
     * Otherwise it decrypts the sealed challenge with `DECRYPT_MODE` and compares SHA-256 hashes.
     *
     * Whenever the user taps "Đổi cách mở khóa (Khuôn mặt / PIN)", fails repeated scans
     * (wet/scratched finger), or hardware is unavailable/locked out, returns
     * [FingerprintFirstOutcome.SwitchMethodRequested] so callers hand off cleanly without dead-ends.
     */
    fun authenticateFingerprintFirst(
        activity: FragmentActivity,
        title: String = "Quét vân tay mở khóa",
        subtitle: String = "Đặt ngón tay lên cảm biến màn hình (Keystore CryptoObject)",
        negativeButtonText: String = "Đổi cách mở khóa (Khuôn mặt / PIN)",
        onOutcome: (FingerprintFirstOutcome) -> Unit
    ) {
        val bm = BiometricManager.from(activity)
        val strongStatus = try {
            bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        } catch (_: Throwable) {
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        }
        if (strongStatus != BiometricManager.BIOMETRIC_SUCCESS) {
            val msg = when (strongStatus) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "Thiết bị chưa cài đặt vân tay. Hãy chuyển sang Khuôn mặt hoặc PIN/Mật khẩu."
                else ->
                    "Cảm biến vân tay màn hình không khả dụng. Hãy chuyển sang phương thức dự phòng."
            }
            onOutcome(FingerprintFirstOutcome.SwitchMethodRequested(msg, isSensorUnavailableOrLocked = true))
            return
        }

        val isBound = isFingerprintCryptoBound()
        val cipher: Cipher? = try {
            if (isBound) prepareDecryptCipher() else prepareEncryptCipher()
        } catch (_: KeyPermanentlyInvalidatedException) {
            clearFingerprintChallenge()
            onOutcome(
                FingerprintFirstOutcome.KeyInvalidated(
                    "Dữ liệu vân tay trên điện thoại đã thay đổi. Vui lòng mở khóa bằng mật khẩu/khuôn mặt để liên kết lại vân tay."
                )
            )
            return
        }

        if (cipher == null) {
            onOutcome(
                FingerprintFirstOutcome.SwitchMethodRequested(
                    "Không thể khởi tạo khóa Keystore CryptoObject. Chuyển sang Khuôn mặt hoặc PIN/Mật khẩu.",
                    isSensorUnavailableOrLocked = true
                )
            )
            return
        }

        try {
            var softFailures = 0
            var resolved = false
            val executor = ContextCompat.getMainExecutor(activity)
            lateinit var prompt: BiometricPrompt
            prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (resolved) return
                        val authCipher = result.cryptoObject?.cipher
                        if (authCipher == null) {
                            resolved = true
                            onOutcome(
                                FingerprintFirstOutcome.SwitchMethodRequested(
                                    "Không nhận được CryptoObject từ phần cứng.",
                                    isSensorUnavailableOrLocked = false
                                )
                            )
                            return
                        }
                        try {
                            if (isBound) {
                                val store = getChallengeStore()
                                val ct = Base64.getDecoder().decode(store.getString(KEY_CHALLENGE_CIPHERTEXT, ""))
                                val expectedHash = store.getString(KEY_CHALLENGE_SHA256, "").orEmpty()
                                val plain = authCipher.doFinal(ct)
                                if (sha256Base64(plain) == expectedHash) {
                                    resolved = true
                                    onOutcome(FingerprintFirstOutcome.CryptoSucceeded)
                                } else {
                                    resolved = true
                                    clearFingerprintChallenge()
                                    onOutcome(
                                        FingerprintFirstOutcome.KeyInvalidated(
                                            "Chữ ký thách thức vân tay không khớp. Vui lòng liên kết lại."
                                        )
                                    )
                                }
                            } else {
                                val randomChallenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
                                val encrypted = authCipher.doFinal(randomChallenge)
                                val iv = authCipher.iv
                                getChallengeStore().edit {
                                    putString(KEY_CHALLENGE_CIPHERTEXT, Base64.getEncoder().encodeToString(encrypted))
                                    putString(KEY_CHALLENGE_IV, Base64.getEncoder().encodeToString(iv))
                                    putString(KEY_CHALLENGE_SHA256, sha256Base64(randomChallenge))
                                }
                                resolved = true
                                onOutcome(FingerprintFirstOutcome.CryptoSucceeded)
                            }
                        } catch (ex: Throwable) {
                            resolved = true
                            onOutcome(
                                FingerprintFirstOutcome.SwitchMethodRequested(
                                    "Lỗi giải mã CryptoObject (${ex.javaClass.simpleName}). Hãy thử Khuôn mặt hoặc PIN/Mật khẩu.",
                                    isSensorUnavailableOrLocked = true
                                )
                            )
                        }
                    }

                    override fun onAuthenticationFailed() {
                        if (resolved) return
                        softFailures += 1
                        if (softFailures >= MAX_SOFT_FINGER_RETRIES) {
                            resolved = true
                            try {
                                prompt.cancelAuthentication()
                            } catch (_: Throwable) {
                            }
                            onOutcome(
                                FingerprintFirstOutcome.SwitchMethodRequested(
                                    "Vân tay chưa khớp sau $MAX_SOFT_FINGER_RETRIES lần (ngón tay ướt/trầy?). Bạn có thể dùng Khuôn mặt hoặc PIN/Mật khẩu.",
                                    isSensorUnavailableOrLocked = false
                                )
                            )
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (resolved) return
                        resolved = true
                        val lockedOrHw = errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                            errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT ||
                            errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                            errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS
                        val reason = when (errorCode) {
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED ->
                                "Đã chuyển sang chọn Khuôn mặt, PIN hoặc Mật khẩu."
                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                                "Cảm biến vân tay tạm khóa — hãy dùng Khuôn mặt hoặc PIN/Mật khẩu."
                            else ->
                                "${errString.ifBlank { "Cảm biến vân tay gặp sự cố" }} — hãy chọn phương thức dự phòng."
                        }
                        onOutcome(FingerprintFirstOutcome.SwitchMethodRequested(reason, lockedOrHw))
                    }
                }
            )

            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText(negativeButtonText)
                .setConfirmationRequired(false)
                .build()

            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        } catch (t: Throwable) {
            onOutcome(
                FingerprintFirstOutcome.SwitchMethodRequested(
                    t.message ?: "Không thể khởi chạy cảm biến vân tay.",
                    isSensorUnavailableOrLocked = true
                )
            )
        }
    }

    /**
     * Step 2 Alternative A: Phone OS Face Unlock / Weak Biometrics (`BIOMETRIC_WEAK`).
     */
    fun authenticatePhoneFaceOrWeakBiometric(
        activity: FragmentActivity,
        title: String = "Khuôn mặt / Sinh trắc học điện thoại",
        subtitle: String = "Sử dụng nhận diện khuôn mặt hoặc sinh trắc học của điện thoại",
        negativeButtonText: String = "Dùng PIN / Mật khẩu",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        authenticateInternal(
            activity = activity,
            authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText,
            allowCredential = false,
            onResult = onResult
        )
    }

    /**
     * Step 2 Alternative B: Phone System PIN / Pattern / Password (`DEVICE_CREDENTIAL`).
     */
    fun authenticateDeviceCredentialOnly(
        activity: FragmentActivity,
        title: String = "Mã khóa của điện thoại",
        subtitle: String = "Nhập mã PIN, hình vẽ hoặc mật khẩu màn hình của điện thoại",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        authenticateInternal(
            activity = activity,
            authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            title = title,
            subtitle = subtitle,
            negativeButtonText = null,
            allowCredential = true,
            onResult = onResult
        )
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Mở khóa bảo mật",
        subtitle: String = "Xác thực vân tay, khuôn mặt hoặc mã khóa thiết bị",
        allowDeviceCredentialFallback: Boolean = true,
        negativeButtonText: String = "Dùng mật khẩu ứng dụng",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val authenticators = if (allowDeviceCredentialFallback) {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        authenticateInternal(
            activity = activity,
            authenticators = authenticators,
            title = title,
            subtitle = subtitle,
            negativeButtonText = if (allowDeviceCredentialFallback) null else negativeButtonText,
            allowCredential = allowDeviceCredentialFallback,
            onResult = onResult
        )
    }

    private fun authenticateInternal(
        activity: FragmentActivity,
        authenticators: Int,
        title: String,
        subtitle: String,
        negativeButtonText: String?,
        allowCredential: Boolean,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val bm = BiometricManager.from(activity)
        val status = try {
            bm.canAuthenticate(authenticators)
        } catch (_: Throwable) {
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        }

        if (status != BiometricManager.BIOMETRIC_SUCCESS) {
            val reason = when (status) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "Thiết bị chưa thiết lập phương thức khóa tương ứng."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    "Thiết bị không hỗ trợ cảm biến sinh trắc học này."
                else ->
                    "Sinh trắc học hoặc mã khóa thiết bị hiện không khả dụng."
            }
            onResult(BiometricAuthResult.Unavailable(reason))
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onResult(BiometricAuthResult.Succeeded(result.authenticationType))
                    }

                    override fun onAuthenticationFailed() {
                        onResult(BiometricAuthResult.Failed)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> {
                                onResult(BiometricAuthResult.Canceled(errString.toString()))
                            }
                            else -> {
                                onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
                            }
                        }
                    }
                }
            )

            val infoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(authenticators)

            if (!allowCredential && !negativeButtonText.isNullOrBlank()) {
                infoBuilder.setNegativeButtonText(negativeButtonText)
            }

            prompt.authenticate(infoBuilder.build())
        } catch (t: Throwable) {
            onResult(BiometricAuthResult.Unavailable(t.message ?: "Không thể khởi chạy BiometricPrompt"))
        }
    }
}

package com.nguyen_super_app.nguyen_super_smart_ai.security

import androidx.fragment.app.FragmentActivity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BiometricAuthMode
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.SecurityLockEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class UnifiedUnlockMethod {
    FINGERPRINT_CRYPTO,
    SYSTEM_BIOMETRIC,
    SYSTEM_FACE,
    PERSONAL_FACE,
    DEVICE_CREDENTIAL,
    APP_CUSTOM_PIN_PATTERN
}

enum class UnifiedLoginOption(
    val testTag: String,
    val title: String,
    val subtitle: String
) {
    SYSTEM_UNLOCK(
        testTag = "sheet_option_system_unlock",
        title = "Mở khóa hệ thống",
        subtitle = "Vân tay, khuôn mặt hoặc mã PIN / mẫu hình / mật khẩu của điện thoại"
    ),
    APP_CUSTOM_PIN_PATTERN(
        testTag = "sheet_option_app_pin_pattern",
        title = "Mã PIN/Mẫu tùy chỉnh ứng dụng",
        subtitle = "Nhập mật khẩu, mã PIN hoặc vẽ mẫu hình riêng của ứng dụng"
    ),
    PERSONAL_FACE(
        testTag = "sheet_option_personal_face",
        title = "Nhận dạng khuôn mặt cá nhân",
        subtitle = "Quét CameraX + ML Kit trong ứng dụng (bắt buộc chớp mắt)"
    )
}

data class UnifiedUnlockUiState(
    val lockId: String = "",
    val activeMethod: UnifiedUnlockMethod? = null,
    val isBiometricInFlight: Boolean = false,
    val hasTimedOutOrExpired: Boolean = false,
    val hasBiometricFailedOrDismissed: Boolean = false,
    val isOptionsSheetOpen: Boolean = false,
    val focusAppInputNonce: Int = 0,
    val statusNotice: String? = null
) {
    val showOtherOptionsButton: Boolean
        get() = hasTimedOutOrExpired || hasBiometricFailedOrDismissed
}

/**
 * Coordinates [AppLockAuthenticator] (system BiometricPrompt / Fingerprint CryptoObject /
 * Phone Face / Device Credential) and [PersonalFaceAuthenticator] (CameraX + ML Kit) into
 * a unified, zero-dead-end fallback unlock workflow.
 */
class UnifiedUnlockManager(
    private val appLockAuthenticator: AppLockAuthenticator,
    private val personalFaceAuthenticator: PersonalFaceAuthenticator
) {

    companion object {
        const val DEFAULT_BIOMETRIC_EXPIRY_MS = 2_000L
    }

    private val _uiState = MutableStateFlow(UnifiedUnlockUiState())
    val uiState: StateFlow<UnifiedUnlockUiState> = _uiState.asStateFlow()

    fun beginUnlockSession(lockId: String) {
        _uiState.value = UnifiedUnlockUiState(lockId = lockId)
    }

    fun notifyBiometricPromptStarted(method: UnifiedUnlockMethod) {
        _uiState.update {
            it.copy(
                activeMethod = method,
                isBiometricInFlight = true
            )
        }
    }

    fun notifyBiometricExpiryElapsed() {
        _uiState.update {
            it.copy(hasTimedOutOrExpired = true)
        }
    }

    fun notifyBiometricFailedOrDismissed(reason: String? = null) {
        _uiState.update {
            it.copy(
                isBiometricInFlight = false,
                hasBiometricFailedOrDismissed = true,
                statusNotice = reason ?: it.statusNotice
            )
        }
    }

    fun markUnlocked() {
        _uiState.update {
            it.copy(
                isBiometricInFlight = false,
                hasTimedOutOrExpired = false,
                hasBiometricFailedOrDismissed = false,
                isOptionsSheetOpen = false,
                statusNotice = null
            )
        }
    }

    fun openOptionsSheet() {
        _uiState.update {
            it.copy(
                isOptionsSheetOpen = true,
                hasBiometricFailedOrDismissed = true
            )
        }
    }

    fun closeOptionsSheet() {
        _uiState.update { it.copy(isOptionsSheetOpen = false) }
    }

    fun switchToAppPinOrPattern(
        notice: String = "Đã chuyển sang nhập Mã PIN / Mẫu hình tùy chỉnh của ứng dụng."
    ) {
        _uiState.update { current ->
            current.copy(
                activeMethod = UnifiedUnlockMethod.APP_CUSTOM_PIN_PATTERN,
                isBiometricInFlight = false,
                isOptionsSheetOpen = false,
                focusAppInputNonce = current.focusAppInputNonce + 1,
                statusNotice = notice
            )
        }
    }

    /**
     * Returns only the BottomSheet login options currently enabled by the user's security policy,
     * lock state, and Personal Face enrollment.
     */
    fun getEnabledLoginOptions(
        lock: SecurityLockEntity,
        biometricAuthMode: BiometricAuthMode?,
        allowDeviceBiometric: Boolean,
        hasPersonalFaceOverride: Boolean? = null
    ): List<UnifiedLoginOption> {
        val systemEnabled = biometricAuthMode?.allowsSystemBiometric ?: allowDeviceBiometric
        val personalFaceEnrolled = hasPersonalFaceOverride ?: personalFaceAuthenticator.hasEnrolledFace(lock.lockId)
        val personalFaceEnabled = personalFaceEnrolled && (biometricAuthMode?.allowsPersonalFace ?: true)
        val appCustomPinEnabled = !lock.lockedOut

        val options = mutableListOf<UnifiedLoginOption>()
        if (systemEnabled) {
            options.add(UnifiedLoginOption.SYSTEM_UNLOCK)
        }
        if (appCustomPinEnabled) {
            options.add(UnifiedLoginOption.APP_CUSTOM_PIN_PATTERN)
        }
        if (personalFaceEnabled) {
            options.add(UnifiedLoginOption.PERSONAL_FACE)
        }
        return options
    }

    fun authenticateFingerprintFirst(
        activity: FragmentActivity,
        title: String = "Quét vân tay mở khóa",
        onUnlocked: (FingerprintFirstOutcome) -> Unit,
        onFallbackNeeded: (String) -> Unit
    ) {
        notifyBiometricPromptStarted(UnifiedUnlockMethod.FINGERPRINT_CRYPTO)
        appLockAuthenticator.authenticateFingerprintFirst(
            activity = activity,
            title = title,
            negativeButtonText = "Chuyển đổi phương thức"
        ) { outcome ->
            when (outcome) {
                is FingerprintFirstOutcome.CryptoSucceeded,
                is FingerprintFirstOutcome.SystemFaceOrCredentialSucceeded -> {
                    markUnlocked()
                    onUnlocked(outcome)
                }
                is FingerprintFirstOutcome.SwitchMethodRequested -> {
                    notifyBiometricFailedOrDismissed(outcome.reason)
                    onFallbackNeeded(outcome.reason)
                }
                is FingerprintFirstOutcome.KeyInvalidated -> {
                    notifyBiometricFailedOrDismissed(outcome.message)
                    onFallbackNeeded(outcome.message)
                }
            }
        }
    }

    fun authenticateSystemUnlock(
        activity: FragmentActivity,
        title: String = "Mở khóa hệ thống",
        onUnlocked: () -> Unit,
        onFallbackNeeded: (String) -> Unit
    ) {
        notifyBiometricPromptStarted(UnifiedUnlockMethod.DEVICE_CREDENTIAL)
        appLockAuthenticator.authenticateDeviceCredentialOnly(
            activity = activity,
            title = title
        ) { result ->
            when (result) {
                is BiometricAuthResult.Succeeded -> {
                    markUnlocked()
                    onUnlocked()
                }
                is BiometricAuthResult.Failed -> {
                    val msg = "Xác thực hệ thống chưa khớp. Bấm 'Chuyển đổi phương thức' để dùng Mã PIN/Mẫu ứng dụng hoặc Khuôn mặt cá nhân."
                    notifyBiometricFailedOrDismissed(msg)
                    onFallbackNeeded(msg)
                }
                is BiometricAuthResult.Canceled -> {
                    val msg = "Đã bỏ qua mở khóa hệ thống — bạn có thể chuyển đổi phương thức ngay."
                    notifyBiometricFailedOrDismissed(msg)
                    onFallbackNeeded(msg)
                }
                is BiometricAuthResult.Error -> {
                    val msg = result.message.ifBlank { "Lỗi xác thực hệ thống." }
                    notifyBiometricFailedOrDismissed(msg)
                    onFallbackNeeded(msg)
                }
                is BiometricAuthResult.Unavailable -> {
                    notifyBiometricFailedOrDismissed(result.reason)
                    onFallbackNeeded(result.reason)
                }
            }
        }
    }

    fun authenticatePhoneFaceOrBiometric(
        activity: FragmentActivity,
        title: String = "Khuôn mặt / Sinh trắc học điện thoại",
        onUnlocked: () -> Unit,
        onFallbackNeeded: (String) -> Unit
    ) {
        notifyBiometricPromptStarted(UnifiedUnlockMethod.SYSTEM_FACE)
        appLockAuthenticator.authenticatePhoneFaceOrWeakBiometric(
            activity = activity,
            title = title,
            negativeButtonText = "Chuyển đổi phương thức"
        ) { result ->
            when (result) {
                is BiometricAuthResult.Succeeded -> {
                    markUnlocked()
                    onUnlocked()
                }
                is BiometricAuthResult.Failed -> {
                    val msg = "Khuôn mặt / sinh trắc học điện thoại không khớp."
                    notifyBiometricFailedOrDismissed(msg)
                    onFallbackNeeded(msg)
                }
                is BiometricAuthResult.Canceled -> {
                    val msg = "Đã chuyển sang chọn phương thức mở khóa khác."
                    notifyBiometricFailedOrDismissed(msg)
                    onFallbackNeeded(msg)
                }
                is BiometricAuthResult.Error -> {
                    val msg = result.message.ifBlank { "Sinh trắc học điện thoại gặp sự cố." }
                    notifyBiometricFailedOrDismissed(msg)
                    onFallbackNeeded(msg)
                }
                is BiometricAuthResult.Unavailable -> {
                    notifyBiometricFailedOrDismissed(result.reason)
                    onFallbackNeeded(result.reason)
                }
            }
        }
    }

    fun verifyPersonalFaceCandidate(
        lockId: String,
        candidateVector: FloatArray,
        onMatched: (Float) -> Unit,
        onFailed: (String) -> Unit
    ): PersonalFaceVerifyOutcome {
        val outcome = personalFaceAuthenticator.verifyCandidate(lockId, candidateVector)
        when (outcome) {
            is PersonalFaceVerifyOutcome.Matched -> {
                markUnlocked()
                onMatched(outcome.similarity)
            }
            is PersonalFaceVerifyOutcome.Mismatch -> {
                val msg = "Khuôn mặt cá nhân không khớp. Hãy dùng 'Chuyển đổi phương thức' để chuyển sang PIN/Mẫu thiết bị hoặc ứng dụng."
                notifyBiometricFailedOrDismissed(msg)
                onFailed(msg)
            }
            PersonalFaceVerifyOutcome.NotEnrolled -> {
                val msg = "Chưa đăng ký Khuôn mặt cá nhân. Hãy chuyển sang phương thức mở khóa khác."
                notifyBiometricFailedOrDismissed(msg)
                onFailed(msg)
            }
        }
        return outcome
    }
}

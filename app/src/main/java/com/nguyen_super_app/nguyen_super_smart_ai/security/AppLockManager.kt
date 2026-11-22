package com.nguyen_super_app.nguyen_super_smart_ai.security

import android.os.SystemClock
import androidx.fragment.app.FragmentActivity
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.CloudSecurityLock
import com.nguyen_super_app.nguyen_super_smart_ai.data.cloud.FirestoreRepository
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AppPreferences
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BiometricAuthMode
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalRepository
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.SecurityLockEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.UserPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RetryLimitPolicy(
    val unlimitedAttempts: Boolean = true,
    val maxAttempts: Long = 5L
)

sealed interface LockAttemptResult {
    val lock: SecurityLockEntity

    data class Unlocked(override val lock: SecurityLockEntity) : LockAttemptResult

    data class WrongAttempt(
        override val lock: SecurityLockEntity,
        val failedAttempts: Long,
        val remainingAttempts: Long?
    ) : LockAttemptResult

    data class LockedOut(override val lock: SecurityLockEntity) : LockAttemptResult
}

/**
 * Centralized security enforcement & persistence manager for:
 * 1) App-wide Lock ("app_lock")
 * 2) Per-Conversation Lock ("chat_<sessionId>")
 * 3) Hardware biometrics via [AppLockAuthenticator] (respecting device-isolation preference)
 * 4) Independent App-Owned Personal Face ID via [PersonalFaceAuthenticator]
 * 5) Background `autoLockTimeout` re-locking both App Lock and all unlocked chats
 * 6) Password-authenticated App Lock toggling (delete on OFF, create new on ON)
 */
class AppLockManager(
    private val localRepo: LocalRepository,
    private val prefsStore: UserPreferencesStore,
    private val firestoreRepoProvider: () -> FirestoreRepository? = { null },
    private val isCloudSyncEnabled: () -> Boolean = { false },
    private val authenticator: AppLockAuthenticator? = null,
    private val personalFaceAuth: PersonalFaceAuthenticator? = null,
    private val clockElapsedMillis: () -> Long = { SystemClock.elapsedRealtime() },
    private val scope: CoroutineScope
) {
    private val _locksCache = MutableStateFlow<List<SecurityLockEntity>>(emptyList())
    val securityLocks: StateFlow<List<SecurityLockEntity>> = _locksCache.asStateFlow()

    @Volatile
    private var lastBackgroundedAtElapsedMs: Long? = null

    init {
        scope.launch {
            localRepo.securityLocks.collect { dbLocks ->
                _locksCache.value = dbLocks
            }
        }
    }

    val appLock: StateFlow<SecurityLockEntity?> = securityLocks
        .map { locks -> locks.firstOrNull { it.targetType == TARGET_APP } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val retryPolicy: StateFlow<RetryLimitPolicy> = prefsStore.preferencesFlow
        .map { pref ->
            RetryLimitPolicy(
                unlimitedAttempts = pref.lockUnlimitedAttempts,
                maxAttempts = pref.lockMaxAttempts.coerceAtLeast(1L)
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, RetryLimitPolicy())

    val autoLockTimeoutMillis: StateFlow<Long> = prefsStore.preferencesFlow
        .map { it.autoLockTimeoutMillis }
        .stateIn(scope, SharingStarted.Eagerly, AppPreferences.DEFAULT_AUTO_LOCK_TIMEOUT_MS)

    val allowDeviceBiometrics: StateFlow<Boolean> = prefsStore.preferencesFlow
        .map { it.allowDeviceBiometricsForLock }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val biometricAuthMode: StateFlow<BiometricAuthMode> = prefsStore.preferencesFlow
        .map { it.biometricAuthMode }
        .stateIn(scope, SharingStarted.Eagerly, BiometricAuthMode.PASSWORD_ONLY)

    val fingerprintFirstWithPinFallback: StateFlow<Boolean> = prefsStore.preferencesFlow
        .map { it.fingerprintFirstWithPinFallback }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val enrolledPersonalFaceTargets: StateFlow<Set<String>> =
        personalFaceAuth?.enrolledTargets ?: MutableStateFlow(emptySet())

    private val _appLockUnlocked = MutableStateFlow(false)
    val appLockUnlocked: StateFlow<Boolean> = _appLockUnlocked.asStateFlow()

    val isAppAccessBlocked: StateFlow<Boolean> = combine(appLock, _appLockUnlocked) { lock, unlocked ->
        lock != null && !unlocked
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _unlockedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedChatIds: StateFlow<Set<String>> = _unlockedChatIds.asStateFlow()

    val lockedChatIds: StateFlow<Set<String>> = securityLocks
        .map { locks -> locks.filter { it.targetType == TARGET_CHAT }.map { it.targetId }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val _pendingUnlockChatId = MutableStateFlow<String?>(null)
    val pendingUnlockChatId: StateFlow<String?> = _pendingUnlockChatId.asStateFlow()

    fun biometricCapability(): BiometricCapability =
        authenticator?.checkCapability() ?: BiometricCapability.UNAVAILABLE

    fun isDeviceAuthenticatorUsable(): Boolean =
        biometricAuthMode.value.allowsSystemBiometric && (authenticator?.isAnyDeviceAuthAvailable() == true)

    fun isFingerprintCryptoBound(): Boolean =
        authenticator?.isFingerprintCryptoBound() == true

    fun isStrongFingerprintAvailable(): Boolean =
        authenticator?.isStrongFingerprintAvailable() == true

    fun hasPersonalFaceForTarget(lockId: String): Boolean =
        personalFaceAuth?.hasEnrolledFace(lockId) == true

    fun isAppLockedNow(): Boolean =
        _locksCache.value.any { it.targetType == TARGET_APP } && !_appLockUnlocked.value

    fun hasConversationPassword(sessionId: String): Boolean =
        _locksCache.value.any { it.targetType == TARGET_CHAT && it.targetId == sessionId }

    fun isConversationLocked(sessionId: String): Boolean =
        hasConversationPassword(sessionId) && sessionId !in _unlockedChatIds.value

    fun getLockForTarget(targetType: String, targetId: String): SecurityLockEntity? {
        val id = lockIdFor(targetType, targetId)
        return _locksCache.value.firstOrNull { it.lockId == id }
    }

    fun remainingAttemptsFor(lock: SecurityLockEntity): Long? {
        if (lock.unlimitedAttempts) return null
        val cap = lock.maxAttempts.coerceAtLeast(1L)
        return (cap - lock.failedAttempts).coerceAtLeast(0L)
    }

    /**
     * Records monotonic elapsed time when the app enters the background (non-configuration change).
     */
    fun onAppBackgrounded(nowElapsedMs: Long = clockElapsedMillis()) {
        if (lastBackgroundedAtElapsedMs == null) {
            lastBackgroundedAtElapsedMs = nowElapsedMs
        }
    }

    /**
     * Evaluates elapsed background duration against [autoLockTimeoutMillis].
     * When exceeded, re-locks BOTH App Lock and ALL unlocked conversations.
     * @return true if timeout elapsed and active unlock states were re-locked.
     */
    fun onAppForegrounded(nowElapsedMs: Long = clockElapsedMillis()): Boolean {
        val backgroundAt = lastBackgroundedAtElapsedMs ?: return false
        lastBackgroundedAtElapsedMs = null

        val timeoutMs = autoLockTimeoutMillis.value
        if (timeoutMs < 0L) return false // Never auto-lock (-1)

        val elapsed = (nowElapsedMs - backgroundAt).coerceAtLeast(0L)
        if (elapsed >= timeoutMs) {
            return relockAllForTimeout()
        }
        return false
    }

    private fun relockAllForTimeout(): Boolean {
        val hadUnlockedApp = _locksCache.value.any { it.targetType == TARGET_APP } && _appLockUnlocked.value
        val hadUnlockedChats = _unlockedChatIds.value.isNotEmpty()
        if (_locksCache.value.any { it.targetType == TARGET_APP }) {
            _appLockUnlocked.value = false
        }
        _unlockedChatIds.value = emptySet()
        _pendingUnlockChatId.value = null
        return hadUnlockedApp || hadUnlockedChats
    }

    fun setAutoLockTimeout(timeoutMillis: Long, onDone: ((Long) -> Unit)? = null) {
        scope.launch {
            val safe = setAutoLockTimeoutAndSync(timeoutMillis)
            onDone?.invoke(safe)
        }
    }

    suspend fun setAutoLockTimeoutAndSync(timeoutMillis: Long): Long {
        val safe = timeoutMillis.coerceIn(AppPreferences.AUTO_LOCK_NEVER, 86_400_000L)
        prefsStore.setAutoLockTimeoutMillis(safe)
        if (isCloudSyncEnabled()) {
            val policy = retryPolicy.value
            val safeMax = if (policy.unlimitedAttempts) 0L else policy.maxAttempts.coerceAtLeast(1L)
            firestoreRepoProvider()?.upsertSecurityLock(
                lockId = POLICY_LOCK_ID,
                targetType = TARGET_POLICY,
                targetId = "GLOBAL_POLICY",
                passwordMode = TARGET_POLICY,
                patternGridSize = 0,
                saltBase64 = "",
                hashBase64 = "",
                unlimitedAttempts = policy.unlimitedAttempts,
                maxAttempts = safeMax,
                failedAttempts = 0L,
                lockedOut = false,
                autoLockTimeoutMillis = safe
            )
        }
        return safe
    }

    fun setAllowDeviceBiometrics(allowed: Boolean) {
        scope.launch {
            prefsStore.setAllowDeviceBiometricsForLock(allowed)
        }
    }

    fun setBiometricAuthMode(mode: BiometricAuthMode) {
        scope.launch {
            prefsStore.setBiometricAuthMode(mode)
        }
    }

    fun setFingerprintFirstWithPinFallback(enabled: Boolean) {
        scope.launch {
            prefsStore.setFingerprintFirstWithPinFallback(enabled)
        }
    }

    fun enrollPersonalFace(lockId: String, template: FloatArray): Boolean {
        return personalFaceAuth?.saveFaceTemplate(lockId, template) == true
    }

    fun removePersonalFace(lockId: String) {
        personalFaceAuth?.removeFaceTemplate(lockId)
    }

    fun requestConversationAccess(sessionId: String): Boolean {
        return if (isConversationLocked(sessionId)) {
            _pendingUnlockChatId.value = sessionId
            false
        } else {
            true
        }
    }

    fun requestUnlockConversation(sessionId: String) {
        _pendingUnlockChatId.value = sessionId
    }

    fun dismissPendingConversationUnlock() {
        _pendingUnlockChatId.value = null
    }

    fun lockAppNow() {
        if (_locksCache.value.any { it.targetType == TARGET_APP }) {
            _appLockUnlocked.value = false
        }
    }

    fun lockConversationNow(sessionId: String) {
        _unlockedChatIds.value = _unlockedChatIds.value - sessionId
    }

    fun resetRuntimeUnlockStates() {
        _appLockUnlocked.value = false
        _unlockedChatIds.value = emptySet()
        _pendingUnlockChatId.value = null
        lastBackgroundedAtElapsedMs = null
    }

    fun saveLock(
        targetType: String,
        targetId: String,
        targetLabel: String,
        mode: PasswordLockMode,
        gridSize: Int,
        textSecret: String,
        patternNodes: List<Int>,
        onSaved: ((SecurityLockEntity) -> Unit)? = null
    ) {
        scope.launch {
            val saved = saveLockAndSync(
                targetType = targetType,
                targetId = targetId,
                targetLabel = targetLabel,
                mode = mode,
                gridSize = gridSize,
                textSecret = textSecret,
                patternNodes = patternNodes
            )
            onSaved?.invoke(saved)
        }
    }

    suspend fun saveLockAndSync(
        targetType: String,
        targetId: String,
        targetLabel: String,
        mode: PasswordLockMode,
        gridSize: Int,
        textSecret: String,
        patternNodes: List<Int>
    ): SecurityLockEntity {
        val rawChars = if (mode == PasswordLockMode.PATTERN) {
            SecurityLockCrypto.canonicalizePattern(gridSize, patternNodes).toCharArray()
        } else {
            textSecret.toCharArray()
        }
        val hashed = SecurityLockCrypto.hashSecret(rawChars)
        val normalizedType = if (targetType == TARGET_APP) TARGET_APP else TARGET_CHAT
        val normalizedTargetId = if (normalizedType == TARGET_APP) TARGET_APP else targetId
        val lockId = lockIdFor(normalizedType, normalizedTargetId)
        val policy = retryPolicy.value
        val safeGrid = if (mode == PasswordLockMode.PATTERN) {
            gridSize.coerceIn(SecurityLockCrypto.MIN_GRID_SIZE, SecurityLockCrypto.MAX_GRID_SIZE)
        } else {
            0
        }
        val maxAtt = if (policy.unlimitedAttempts) 0L else policy.maxAttempts.coerceAtLeast(1L)

        val entity = SecurityLockEntity(
            lockId = lockId,
            targetType = normalizedType,
            targetId = normalizedTargetId,
            targetLabel = targetLabel,
            passwordMode = mode.id,
            patternGridSize = safeGrid,
            saltBase64 = hashed.saltBase64,
            hashBase64 = hashed.hashBase64,
            unlimitedAttempts = policy.unlimitedAttempts,
            maxAttempts = maxAtt,
            failedAttempts = 0L,
            lockedOut = false,
            updatedAtMillis = System.currentTimeMillis()
        )

        if (normalizedType == TARGET_APP) {
            _appLockUnlocked.value = true
        } else {
            _unlockedChatIds.value = _unlockedChatIds.value + normalizedTargetId
        }

        upsertCacheEntry(entity)
        localRepo.upsertSecurityLock(entity)
        syncEntityToCloudIfEnabled(entity)
        return entity
    }

    fun updateRetryLimit(unlimitedAttempts: Boolean, maxAttempts: Long, onDone: ((Long) -> Unit)? = null) {
        scope.launch {
            val safeMax = updateRetryLimitAndSync(unlimitedAttempts, maxAttempts)
            onDone?.invoke(safeMax)
        }
    }

    suspend fun updateRetryLimitAndSync(unlimitedAttempts: Boolean, maxAttempts: Long): Long {
        val safeMax = if (unlimitedAttempts) 0L else maxAttempts.coerceAtLeast(1L)
        prefsStore.setSecurityAttemptLimitPolicy(unlimitedAttempts, maxAttempts.coerceAtLeast(1L))

        val currentLocks = _locksCache.value
        currentLocks.forEach { lock ->
            val updated = lock.copy(
                unlimitedAttempts = unlimitedAttempts,
                maxAttempts = safeMax,
                failedAttempts = 0L,
                lockedOut = false,
                updatedAtMillis = System.currentTimeMillis()
            )
            upsertCacheEntry(updated)
            localRepo.upsertSecurityLock(updated)
            syncEntityToCloudIfEnabled(updated)
        }

        if (isCloudSyncEnabled()) {
            firestoreRepoProvider()?.upsertSecurityLock(
                lockId = POLICY_LOCK_ID,
                targetType = TARGET_POLICY,
                targetId = "GLOBAL_POLICY",
                passwordMode = TARGET_POLICY,
                patternGridSize = 0,
                saltBase64 = "",
                hashBase64 = "",
                unlimitedAttempts = unlimitedAttempts,
                maxAttempts = safeMax,
                failedAttempts = 0L,
                lockedOut = false,
                autoLockTimeoutMillis = autoLockTimeoutMillis.value
            )
        }
        return safeMax
    }

    fun verifyTextPasscode(lock: SecurityLockEntity, candidateText: String): LockAttemptResult {
        val latest = latestLockSnapshot(lock)
        if (latest.lockedOut) return LockAttemptResult.LockedOut(latest)
        val ok = SecurityLockCrypto.verifySecret(candidateText.toCharArray(), latest.saltBase64, latest.hashBase64)
        val outcome = computeAttemptOutcome(latest, ok)
        scope.launch { persistOutcome(outcome.lock) }
        return outcome
    }

    suspend fun verifyTextPasscodeAndPersist(lock: SecurityLockEntity, candidateText: String): LockAttemptResult {
        val latest = localRepo.getSecurityLock(lock.lockId) ?: latestLockSnapshot(lock)
        if (latest.lockedOut) return LockAttemptResult.LockedOut(latest)
        val ok = SecurityLockCrypto.verifySecret(candidateText.toCharArray(), latest.saltBase64, latest.hashBase64)
        val outcome = computeAttemptOutcome(latest, ok)
        persistOutcome(outcome.lock)
        return outcome
    }

    fun verifyPatternPasscode(lock: SecurityLockEntity, nodes: List<Int>): LockAttemptResult {
        val latest = latestLockSnapshot(lock)
        if (latest.lockedOut) return LockAttemptResult.LockedOut(latest)
        val ok = SecurityLockCrypto.verifyPattern(latest.patternGridSize, nodes, latest.saltBase64, latest.hashBase64)
        val outcome = computeAttemptOutcome(latest, ok)
        scope.launch { persistOutcome(outcome.lock) }
        return outcome
    }

    suspend fun verifyPatternPasscodeAndPersist(lock: SecurityLockEntity, nodes: List<Int>): LockAttemptResult {
        val latest = localRepo.getSecurityLock(lock.lockId) ?: latestLockSnapshot(lock)
        if (latest.lockedOut) return LockAttemptResult.LockedOut(latest)
        val ok = SecurityLockCrypto.verifyPattern(latest.patternGridSize, nodes, latest.saltBase64, latest.hashBase64)
        val outcome = computeAttemptOutcome(latest, ok)
        persistOutcome(outcome.lock)
        return outcome
    }

    fun verifyPersonalFace(lock: SecurityLockEntity, candidateTemplate: FloatArray): LockAttemptResult {
        val latest = latestLockSnapshot(lock)
        if (latest.lockedOut) return LockAttemptResult.LockedOut(latest)
        val outcomeFace = personalFaceAuth?.verifyCandidate(latest.lockId, candidateTemplate)
            ?: PersonalFaceVerifyOutcome.NotEnrolled
        val ok = outcomeFace is PersonalFaceVerifyOutcome.Matched
        val outcome = computeAttemptOutcome(latest, ok)
        scope.launch { persistOutcome(outcome.lock) }
        return outcome
    }

    suspend fun verifyPersonalFaceAndPersist(lock: SecurityLockEntity, candidateTemplate: FloatArray): LockAttemptResult {
        val latest = localRepo.getSecurityLock(lock.lockId) ?: latestLockSnapshot(lock)
        if (latest.lockedOut) return LockAttemptResult.LockedOut(latest)
        val outcomeFace = personalFaceAuth?.verifyCandidate(latest.lockId, candidateTemplate)
            ?: PersonalFaceVerifyOutcome.NotEnrolled
        val ok = outcomeFace is PersonalFaceVerifyOutcome.Matched
        val outcome = computeAttemptOutcome(latest, ok)
        persistOutcome(outcome.lock)
        return outcome
    }

    fun unlockViaBiometric(lock: SecurityLockEntity): LockAttemptResult.Unlocked {
        val latest = latestLockSnapshot(lock)
        val outcome = computeAttemptOutcome(latest, success = true) as LockAttemptResult.Unlocked
        scope.launch { persistOutcome(outcome.lock) }
        return outcome
    }

    suspend fun unlockViaBiometricAndPersist(lock: SecurityLockEntity): LockAttemptResult.Unlocked {
        val latest = localRepo.getSecurityLock(lock.lockId) ?: latestLockSnapshot(lock)
        val outcome = computeAttemptOutcome(latest, success = true) as LockAttemptResult.Unlocked
        persistOutcome(outcome.lock)
        return outcome
    }

    fun unlockViaDeviceAuthenticator(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        title: String = "Mở khóa bảo mật",
        subtitle: String = "Xác thực vân tay, khuôn mặt hoặc mã khóa hệ thống",
        onOutcome: (BiometricAuthResult) -> Unit
    ) {
        val auth = authenticator ?: AppLockAuthenticator(activity)
        auth.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            allowDeviceCredentialFallback = true
        ) { result ->
            if (result is BiometricAuthResult.Succeeded) {
                unlockViaBiometric(lock)
            }
            onOutcome(result)
        }
    }

    fun bindFingerprintCryptoForSettings(
        activity: FragmentActivity,
        onOutcome: (FingerprintFirstOutcome) -> Unit
    ) {
        val auth = authenticator ?: AppLockAuthenticator(activity)
        auth.authenticateFingerprintFirst(
            activity = activity,
            title = "Liên kết vân tay màn hình",
            subtitle = "Quét vân tay để niêm phong khóa Android Keystore CryptoObject",
            negativeButtonText = "Đóng",
            onOutcome = onOutcome
        )
    }

    fun authenticateFingerprintFirstForLock(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        title: String = "Quét vân tay mở khóa",
        onOutcome: (FingerprintFirstOutcome) -> Unit
    ) {
        val auth = authenticator ?: AppLockAuthenticator(activity)
        auth.authenticateFingerprintFirst(
            activity = activity,
            title = title,
            subtitle = "Đặt ngón tay lên cảm biến màn hình (Keystore CryptoObject)",
            negativeButtonText = "Đổi cách mở khóa (Khuôn mặt / PIN)"
        ) { result ->
            if (result is FingerprintFirstOutcome.CryptoSucceeded ||
                result is FingerprintFirstOutcome.SystemFaceOrCredentialSucceeded
            ) {
                unlockViaBiometric(lock)
            }
            onOutcome(result)
        }
    }

    fun authenticatePhoneFaceForLock(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        title: String = "Khuôn mặt / Sinh trắc học điện thoại",
        onOutcome: (BiometricAuthResult) -> Unit
    ) {
        val auth = authenticator ?: AppLockAuthenticator(activity)
        auth.authenticatePhoneFaceOrWeakBiometric(
            activity = activity,
            title = title
        ) { result ->
            if (result is BiometricAuthResult.Succeeded) {
                unlockViaBiometric(lock)
            }
            onOutcome(result)
        }
    }

    fun authenticateDeviceCredentialForLock(
        activity: FragmentActivity,
        lock: SecurityLockEntity,
        title: String = "Mã khóa của điện thoại",
        onOutcome: (BiometricAuthResult) -> Unit
    ) {
        val auth = authenticator ?: AppLockAuthenticator(activity)
        auth.authenticateDeviceCredentialOnly(
            activity = activity,
            title = title
        ) { result ->
            if (result is BiometricAuthResult.Succeeded) {
                unlockViaBiometric(lock)
            }
            onOutcome(result)
        }
    }

    /**
     * Password-authenticated App Lock disablement: verifies the active password first;
     * only if verified does it delete `app_lock` and any enrolled app Personal Face template.
     */
    suspend fun verifyTextAndDisableAppLock(candidateText: String): LockAttemptResult? {
        val lock = localRepo.getSecurityLock(APP_LOCK_ID)
            ?: _locksCache.value.firstOrNull { it.targetType == TARGET_APP }
            ?: return null
        val outcome = verifyTextPasscodeAndPersist(lock, candidateText)
        if (outcome is LockAttemptResult.Unlocked) {
            removeLockAndSync(APP_LOCK_ID)
        }
        return outcome
    }

    suspend fun verifyPatternAndDisableAppLock(nodes: List<Int>): LockAttemptResult? {
        val lock = localRepo.getSecurityLock(APP_LOCK_ID)
            ?: _locksCache.value.firstOrNull { it.targetType == TARGET_APP }
            ?: return null
        val outcome = verifyPatternPasscodeAndPersist(lock, nodes)
        if (outcome is LockAttemptResult.Unlocked) {
            removeLockAndSync(APP_LOCK_ID)
        }
        return outcome
    }

    suspend fun verifyPersonalFaceAndDisableAppLock(candidateTemplate: FloatArray): LockAttemptResult? {
        val lock = localRepo.getSecurityLock(APP_LOCK_ID)
            ?: _locksCache.value.firstOrNull { it.targetType == TARGET_APP }
            ?: return null
        val outcome = verifyPersonalFaceAndPersist(lock, candidateTemplate)
        if (outcome is LockAttemptResult.Unlocked) {
            removeLockAndSync(APP_LOCK_ID)
        }
        return outcome
    }

    suspend fun disableAppLockAfterHardwareAuth(): LockAttemptResult.Unlocked? {
        val lock = localRepo.getSecurityLock(APP_LOCK_ID)
            ?: _locksCache.value.firstOrNull { it.targetType == TARGET_APP }
            ?: return null
        val unlocked = unlockViaBiometricAndPersist(lock)
        removeLockAndSync(APP_LOCK_ID)
        return unlocked
    }

    fun removeLock(lockId: String, onRemoved: (() -> Unit)? = null) {
        scope.launch {
            removeLockAndSync(lockId)
            onRemoved?.invoke()
        }
    }

    suspend fun removeLockAndSync(lockId: String) {
        _locksCache.value = _locksCache.value.filterNot { it.lockId == lockId }
        localRepo.deleteSecurityLock(lockId)
        personalFaceAuth?.removeFaceTemplate(lockId)
        if (lockId == APP_LOCK_ID) {
            _appLockUnlocked.value = true
        } else if (lockId.startsWith(CHAT_LOCK_PREFIX)) {
            val sessId = lockId.removePrefix(CHAT_LOCK_PREFIX)
            _unlockedChatIds.value = _unlockedChatIds.value - sessId
            if (_pendingUnlockChatId.value == sessId) {
                _pendingUnlockChatId.value = null
            }
        }
        if (isCloudSyncEnabled()) {
            firestoreRepoProvider()?.deleteSecurityLock(lockId)
        }
    }

    suspend fun syncFromCloud(cloudLocks: List<CloudSecurityLock>) {
        for (c in cloudLocks) {
            if (c.targetType == TARGET_POLICY) {
                prefsStore.setSecurityAttemptLimitPolicy(
                    c.unlimitedAttempts,
                    c.maxAttempts.coerceAtLeast(1L)
                )
                c.autoLockTimeoutMillis?.let { timeout ->
                    prefsStore.setAutoLockTimeoutMillis(timeout)
                }
                continue
            }
            val lockId = c.id.ifBlank { lockIdFor(c.targetType, c.targetId) }
            val localExisting = localRepo.getSecurityLock(lockId)
            val cloudMillis = c.updatedAt?.toDate()?.time ?: 0L
            if (localExisting == null || cloudMillis > localExisting.updatedAtMillis) {
                val merged = SecurityLockEntity(
                    lockId = lockId,
                    targetType = c.targetType,
                    targetId = c.targetId,
                    targetLabel = localExisting?.targetLabel
                        ?: if (c.targetType == TARGET_APP) {
                            "Khiên bảo vệ ứng dụng"
                        } else {
                            "Cuộc trò chuyện ${c.targetId}"
                        },
                    passwordMode = c.passwordMode,
                    patternGridSize = c.patternGridSize.toInt(),
                    saltBase64 = c.saltBase64,
                    hashBase64 = c.hashBase64,
                    unlimitedAttempts = c.unlimitedAttempts,
                    maxAttempts = c.maxAttempts,
                    failedAttempts = c.failedAttempts,
                    lockedOut = c.lockedOut,
                    updatedAtMillis = maxOf(cloudMillis, System.currentTimeMillis())
                )
                upsertCacheEntry(merged)
                localRepo.upsertSecurityLock(merged)
            }
        }
    }

    private fun upsertCacheEntry(entity: SecurityLockEntity) {
        val current = _locksCache.value.toMutableList()
        val idx = current.indexOfFirst { it.lockId == entity.lockId }
        if (idx >= 0) {
            current[idx] = entity
        } else {
            current.add(0, entity)
        }
        _locksCache.value = current
    }

    private fun latestLockSnapshot(lock: SecurityLockEntity): SecurityLockEntity =
        _locksCache.value.firstOrNull { it.lockId == lock.lockId } ?: lock

    private fun computeAttemptOutcome(lock: SecurityLockEntity, success: Boolean): LockAttemptResult {
        return if (success) {
            val updated = lock.copy(
                failedAttempts = 0L,
                lockedOut = false,
                updatedAtMillis = System.currentTimeMillis()
            )
            if (updated.targetType == TARGET_APP) {
                _appLockUnlocked.value = true
            } else {
                _unlockedChatIds.value = _unlockedChatIds.value + updated.targetId
                if (_pendingUnlockChatId.value == updated.targetId) {
                    _pendingUnlockChatId.value = null
                }
            }
            LockAttemptResult.Unlocked(updated)
        } else {
            val nextFailed = lock.failedAttempts + 1L
            val nextLockedOut = !lock.unlimitedAttempts && lock.maxAttempts > 0L && nextFailed >= lock.maxAttempts
            val updated = lock.copy(
                failedAttempts = nextFailed,
                lockedOut = nextLockedOut,
                updatedAtMillis = System.currentTimeMillis()
            )
            if (nextLockedOut) {
                LockAttemptResult.LockedOut(updated)
            } else {
                LockAttemptResult.WrongAttempt(
                    lock = updated,
                    failedAttempts = nextFailed,
                    remainingAttempts = remainingAttemptsFor(updated)
                )
            }
        }
    }

    private suspend fun persistOutcome(updated: SecurityLockEntity) {
        upsertCacheEntry(updated)
        localRepo.upsertSecurityLock(updated)
        if (isCloudSyncEnabled()) {
            firestoreRepoProvider()?.updateSecurityLockAttemptState(
                lockId = updated.lockId,
                failedAttempts = updated.failedAttempts,
                lockedOut = updated.lockedOut
            )
        }
    }

    private suspend fun syncEntityToCloudIfEnabled(entity: SecurityLockEntity) {
        if (!isCloudSyncEnabled()) return
        firestoreRepoProvider()?.upsertSecurityLock(
            lockId = entity.lockId,
            targetType = entity.targetType,
            targetId = entity.targetId,
            passwordMode = entity.passwordMode,
            patternGridSize = entity.patternGridSize,
            saltBase64 = entity.saltBase64,
            hashBase64 = entity.hashBase64,
            unlimitedAttempts = entity.unlimitedAttempts,
            maxAttempts = entity.maxAttempts,
            failedAttempts = entity.failedAttempts,
            lockedOut = entity.lockedOut
        )
    }

    companion object {
        const val TARGET_APP = "APP"
        const val TARGET_CHAT = "CHAT"
        const val TARGET_POLICY = "POLICY"
        const val APP_LOCK_ID = "app_lock"
        const val CHAT_LOCK_PREFIX = "chat_"
        const val POLICY_LOCK_ID = "lock_policy"

        fun lockIdFor(targetType: String, targetId: String): String =
            if (targetType == TARGET_APP) APP_LOCK_ID else "$CHAT_LOCK_PREFIX$targetId"
    }
}


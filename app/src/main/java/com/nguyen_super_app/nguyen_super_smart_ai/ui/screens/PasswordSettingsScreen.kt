package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BiometricAuthMode
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.SecurityLockEntity
import com.nguyen_super_app.nguyen_super_smart_ai.security.PasswordLockMode
import com.nguyen_super_app.nguyen_super_smart_ai.security.PasswordValidationResult
import com.nguyen_super_app.nguyen_super_smart_ai.security.SecurityLockCrypto
import com.nguyen_super_app.nguyen_super_smart_ai.ui.components.PatternLockGrid
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.PersonalFaceCameraDialog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs.PersonalFaceScanMode
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateSurface
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PasswordInputTab(val title: String) {
    NUMERIC_TEXT("Chữ số & Ký tự"),
    PATTERN("Hình vẽ (tối đa 10x10)")
}

data class PasswordSettingsUiState(
    val activeTab: PasswordInputTab = PasswordInputTab.NUMERIC_TEXT,
    val selectedMode: PasswordLockMode = PasswordLockMode.DIGITS,
    val lastTextSubMode: PasswordLockMode = PasswordLockMode.DIGITS,
    val textSecret: String = "",
    val confirmSecret: String = "",
    val isPasswordVisible: Boolean = false,
    val patternGridSize: Int = SecurityLockCrypto.MIN_GRID_SIZE,
    val patternNodes: List<Int> = emptyList(),
    val confirmPatternNodes: List<Int> = emptyList(),
    val requireConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val validation: PasswordValidationResult = SecurityLockCrypto.validateTextSecret(PasswordLockMode.DIGITS, "")
) {
    val canTogglePasswordVisibility: Boolean
        get() = activeTab == PasswordInputTab.NUMERIC_TEXT && selectedMode.supportsVisibilityToggle
}

data class PasswordSettingsSubmission(
    val mode: PasswordLockMode,
    val gridSize: Int,
    val textSecret: String,
    val patternNodes: List<Int>
)

/**
 * Dedicated State Manager for [PasswordSettingsScreen].
 * Coordinates tab switching (Numeric/Text/Custom vs. Pattern up to 10x10), character sub-modes
 * (Số / Chữ không số / Chữ và số / Tự ý), password visibility toggling (strictly restricted to text/digit modes),
 * optional double-entry confirmation, grid matrix resizing (3x3..10x10), input filtering, and submission validation.
 */
class PasswordSettingsStateManager(
    initialLock: SecurityLockEntity? = null,
    requireConfirmation: Boolean = false
) {
    private val _uiState = MutableStateFlow(buildInitialState(initialLock, requireConfirmation))
    val uiState: StateFlow<PasswordSettingsUiState> = _uiState.asStateFlow()

    val currentState: PasswordSettingsUiState
        get() = _uiState.value

    fun selectTab(tab: PasswordInputTab) {
        val prev = _uiState.value
        if (prev.activeTab == tab) return
        _uiState.value = when (tab) {
            PasswordInputTab.PATTERN -> {
                val validation = SecurityLockCrypto.validatePattern(prev.patternGridSize, prev.patternNodes)
                prev.copy(
                    activeTab = PasswordInputTab.PATTERN,
                    selectedMode = PasswordLockMode.PATTERN,
                    textSecret = "",
                    confirmSecret = "",
                    isPasswordVisible = false,
                    errorMessage = null,
                    validation = validation
                )
            }
            PasswordInputTab.NUMERIC_TEXT -> {
                val restoredMode = if (prev.lastTextSubMode == PasswordLockMode.PATTERN) {
                    PasswordLockMode.DIGITS
                } else {
                    prev.lastTextSubMode
                }
                val validation = SecurityLockCrypto.validateTextSecret(restoredMode, "")
                prev.copy(
                    activeTab = PasswordInputTab.NUMERIC_TEXT,
                    selectedMode = restoredMode,
                    textSecret = "",
                    confirmSecret = "",
                    patternNodes = emptyList(),
                    confirmPatternNodes = emptyList(),
                    isPasswordVisible = false,
                    errorMessage = null,
                    validation = validation
                )
            }
        }
    }

    fun selectTextSubMode(mode: PasswordLockMode) {
        if (mode == PasswordLockMode.PATTERN) {
            selectTab(PasswordInputTab.PATTERN)
            return
        }
        val prev = _uiState.value
        val validation = SecurityLockCrypto.validateTextSecret(mode, "")
        _uiState.value = prev.copy(
            activeTab = PasswordInputTab.NUMERIC_TEXT,
            selectedMode = mode,
            lastTextSubMode = mode,
            textSecret = "",
            confirmSecret = "",
            patternNodes = emptyList(),
            confirmPatternNodes = emptyList(),
            isPasswordVisible = false,
            errorMessage = null,
            validation = validation
        )
    }

    fun togglePasswordVisibility() {
        val prev = _uiState.value
        if (!prev.canTogglePasswordVisibility) {
            if (prev.isPasswordVisible) {
                _uiState.value = prev.copy(isPasswordVisible = false)
            }
            return
        }
        _uiState.value = prev.copy(isPasswordVisible = !prev.isPasswordVisible)
    }

    fun setPasswordVisibility(visible: Boolean) {
        val prev = _uiState.value
        _uiState.value = prev.copy(
            isPasswordVisible = visible && prev.canTogglePasswordVisibility
        )
    }

    private fun filterByMode(mode: PasswordLockMode, rawInput: String): String {
        return when (mode) {
            PasswordLockMode.DIGITS -> rawInput.filter { it in '0'..'9' }
            PasswordLockMode.LETTERS -> rawInput.filter { it.isLetter() }
            PasswordLockMode.ALPHANUMERIC -> rawInput.filter { it.isLetterOrDigit() }
            PasswordLockMode.CUSTOM -> rawInput
            PasswordLockMode.PATTERN -> rawInput
        }
    }

    fun onTextInputChanged(rawInput: String) {
        val prev = _uiState.value
        if (prev.selectedMode == PasswordLockMode.PATTERN) return
        val filtered = filterByMode(prev.selectedMode, rawInput)
        _uiState.value = prev.copy(
            textSecret = filtered,
            errorMessage = null,
            validation = SecurityLockCrypto.validateTextSecret(prev.selectedMode, filtered)
        )
    }

    fun onConfirmTextInputChanged(rawInput: String) {
        val prev = _uiState.value
        if (prev.selectedMode == PasswordLockMode.PATTERN) return
        val filtered = filterByMode(prev.selectedMode, rawInput)
        _uiState.value = prev.copy(
            confirmSecret = filtered,
            errorMessage = null
        )
    }

    fun selectPatternGridSize(size: Int) {
        val clamped = size.coerceIn(SecurityLockCrypto.MIN_GRID_SIZE, SecurityLockCrypto.MAX_GRID_SIZE)
        val prev = _uiState.value
        val clearedNodes = emptyList<Int>()
        _uiState.value = prev.copy(
            patternGridSize = clamped,
            patternNodes = clearedNodes,
            confirmPatternNodes = clearedNodes,
            errorMessage = null,
            validation = if (prev.activeTab == PasswordInputTab.PATTERN) {
                SecurityLockCrypto.validatePattern(clamped, clearedNodes)
            } else {
                prev.validation
            }
        )
    }

    fun onPatternNodesChanged(nodes: List<Int>) {
        val prev = _uiState.value
        _uiState.value = prev.copy(
            patternNodes = nodes,
            errorMessage = null,
            validation = SecurityLockCrypto.validatePattern(prev.patternGridSize, nodes)
        )
    }

    fun onConfirmPatternNodesChanged(nodes: List<Int>) {
        val prev = _uiState.value
        _uiState.value = prev.copy(
            confirmPatternNodes = nodes,
            errorMessage = null
        )
    }

    fun clearCurrentInput() {
        val prev = _uiState.value
        if (prev.activeTab == PasswordInputTab.PATTERN) {
            _uiState.value = prev.copy(
                patternNodes = emptyList(),
                confirmPatternNodes = emptyList(),
                errorMessage = null,
                validation = SecurityLockCrypto.validatePattern(prev.patternGridSize, emptyList())
            )
        } else {
            _uiState.value = prev.copy(
                textSecret = "",
                confirmSecret = "",
                errorMessage = null,
                validation = SecurityLockCrypto.validateTextSecret(prev.selectedMode, "")
            )
        }
    }

    fun validateForSubmission(): PasswordSettingsSubmission? {
        val st = _uiState.value
        val validation = if (st.selectedMode == PasswordLockMode.PATTERN) {
            SecurityLockCrypto.validatePattern(st.patternGridSize, st.patternNodes)
        } else {
            SecurityLockCrypto.validateTextSecret(st.selectedMode, st.textSecret)
        }
        if (!validation.isValid) {
            _uiState.value = st.copy(
                validation = validation,
                errorMessage = validation.errorMessage ?: "Mật khẩu chưa hợp lệ."
            )
            return null
        }
        if (st.requireConfirmation) {
            if (st.selectedMode == PasswordLockMode.PATTERN) {
                if (st.patternNodes != st.confirmPatternNodes) {
                    _uiState.value = st.copy(
                        errorMessage = "Hình vẽ xác nhận chưa khớp với hình vẽ ban đầu."
                    )
                    return null
                }
            } else if (st.textSecret != st.confirmSecret) {
                _uiState.value = st.copy(
                    errorMessage = "Mật khẩu xác nhận không khớp."
                )
                return null
            }
        }
        return PasswordSettingsSubmission(
            mode = st.selectedMode,
            gridSize = st.patternGridSize,
            textSecret = st.textSecret,
            patternNodes = st.patternNodes
        )
    }

    companion object {
        private fun buildInitialState(
            initialLock: SecurityLockEntity?,
            requireConfirmation: Boolean
        ): PasswordSettingsUiState {
            val initialMode = initialLock?.let { PasswordLockMode.fromId(it.passwordMode) } ?: PasswordLockMode.DIGITS
            val initialTab = if (initialMode == PasswordLockMode.PATTERN) {
                PasswordInputTab.PATTERN
            } else {
                PasswordInputTab.NUMERIC_TEXT
            }
            val initialGrid = initialLock?.patternGridSize
                ?.takeIf { it in SecurityLockCrypto.MIN_GRID_SIZE..SecurityLockCrypto.MAX_GRID_SIZE }
                ?: SecurityLockCrypto.MIN_GRID_SIZE
            val lastText = if (initialMode == PasswordLockMode.PATTERN) PasswordLockMode.DIGITS else initialMode
            val initialValidation = if (initialTab == PasswordInputTab.PATTERN) {
                SecurityLockCrypto.validatePattern(initialGrid, emptyList())
            } else {
                SecurityLockCrypto.validateTextSecret(initialMode, "")
            }
            return PasswordSettingsUiState(
                activeTab = initialTab,
                selectedMode = initialMode,
                lastTextSubMode = lastText,
                patternGridSize = initialGrid,
                requireConfirmation = requireConfirmation,
                isPasswordVisible = false,
                validation = initialValidation
            )
        }
    }
}

@Composable
fun rememberPasswordSettingsStateManager(
    existingLock: SecurityLockEntity?,
    requireConfirmation: Boolean = false
): PasswordSettingsStateManager {
    return remember(existingLock?.lockId, requireConfirmation) {
        PasswordSettingsStateManager(existingLock, requireConfirmation)
    }
}

/**
 * Tabbed Password Settings UI supporting:
 * - Tab 1: Numeric / Character / Custom Passwords (Số / Chữ không số / Chữ và số / Tự ý) + Show/Hide toggle + Visual feedback
 * - Tab 2: Pattern Grid up to 10x10 (3x3 .. 10x10), strictly without password visibility toggle
 * - Optional App-Owned Personal Face ID enrollment (completely independent from device Face Unlock)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PasswordSettingsScreen(
    targetType: String,
    targetId: String,
    targetLabel: String,
    existingLock: SecurityLockEntity?,
    unlimitedAttempts: Boolean = true,
    maxAttempts: Long = 5L,
    requireConfirmation: Boolean = false,
    hasPersonalFaceEnrolled: Boolean = false,
    onSavePersonalFace: (FloatArray) -> Unit = {},
    onRemovePersonalFace: () -> Unit = {},
    biometricAuthMode: BiometricAuthMode = BiometricAuthMode.PASSWORD_ONLY,
    onChangeBiometricAuthMode: (BiometricAuthMode) -> Unit = {},
    fingerprintFirstWithPinFallback: Boolean = true,
    onSetFingerprintFirstWithPinFallback: (Boolean) -> Unit = {},
    isFingerprintCryptoBound: Boolean = false,
    onBindScreenFingerprint: (FragmentActivity, (String?) -> Unit) -> Unit = { _, _ -> },
    onConfirmSave: (mode: PasswordLockMode, gridSize: Int, textSecret: String, patternNodes: List<Int>) -> Unit,
    onRemoveExistingLock: (() -> Unit)? = null,
    onOpenAttemptLimitConfig: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val stateManager = rememberPasswordSettingsStateManager(existingLock, requireConfirmation)
    val uiState by stateManager.uiState.collectAsState()
    var showEnrollFaceDialog by remember { mutableStateOf(false) }
    var fingerprintBindFeedback by remember { mutableStateOf<String?>(null) }

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
                        text = if (targetType == "APP") "Khiên bảo vệ ứng dụng" else "Khóa: $targetLabel",
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
                            val submission = stateManager.validateForSubmission()
                            if (submission != null) {
                                onConfirmSave(
                                    submission.mode,
                                    submission.gridSize,
                                    submission.textSecret,
                                    submission.patternNodes
                                )
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
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = SlateSurface,
                contentColor = CyanBright
            ) {
                LeadingIconTab(
                    selected = uiState.activeTab == PasswordInputTab.NUMERIC_TEXT,
                    onClick = { stateManager.selectTab(PasswordInputTab.NUMERIC_TEXT) },
                    icon = { Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = {
                        Text(
                            PasswordInputTab.NUMERIC_TEXT.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("password_tab_numeric_text")
                )
                LeadingIconTab(
                    selected = uiState.activeTab == PasswordInputTab.PATTERN,
                    onClick = { stateManager.selectTab(PasswordInputTab.PATTERN) },
                    icon = { Icon(Icons.Rounded.GridOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = {
                        Text(
                            PasswordInputTab.PATTERN.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("password_tab_pattern")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Policy Summary Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (targetType == "APP") Icons.Rounded.Shield else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (targetType == "APP") "Phạm vi: Toàn bộ ứng dụng" else "Cuộc trò chuyện: $targetLabel",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (unlimitedAttempts) "Giới hạn nhập sai: Không giới hạn" else "Giới hạn nhập sai: Tối đa $maxAttempts lần",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (onOpenAttemptLimitConfig != null) {
                            TextButton(
                                onClick = onOpenAttemptLimitConfig,
                                modifier = Modifier.testTag("password_settings_attempt_policy_button")
                            ) {
                                Icon(Icons.Rounded.Numbers, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Giới hạn", color = CyanBright)
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = uiState.activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "password_tab_content"
                ) { tab ->
                    if (tab == PasswordInputTab.PATTERN) {
                        PatternTabContent(
                            uiState = uiState,
                            onSelectGridSize = { stateManager.selectPatternGridSize(it) },
                            onPatternNodesChanged = { stateManager.onPatternNodesChanged(it) },
                            onConfirmPatternNodesChanged = { stateManager.onConfirmPatternNodesChanged(it) }
                        )
                    } else {
                        NumericAndTextTabContent(
                            uiState = uiState,
                            onSelectSubMode = { stateManager.selectTextSubMode(it) },
                            onTextChanged = { stateManager.onTextInputChanged(it) },
                            onConfirmTextChanged = { stateManager.onConfirmTextInputChanged(it) },
                            onToggleVisibility = { stateManager.togglePasswordVisibility() }
                        )
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralError,
                        modifier = Modifier.testTag("password_settings_error_text")
                    )
                }

                // Dual Authentication Mode ("Chế độ xác thực kép") Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(
                        1.dp,
                        if (biometricAuthMode != BiometricAuthMode.PASSWORD_ONLY) {
                            CyanPrimary.copy(alpha = 0.6f)
                        } else {
                            SlateBorder
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dual_auth_mode_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (biometricAuthMode == BiometricAuthMode.DUAL_PARALLEL) EmeraldOk else CyanBright
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chế độ xác thực kép",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = biometricAuthMode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = biometricAuthMode == BiometricAuthMode.DUAL_PARALLEL,
                                onCheckedChange = { enableDual ->
                                    if (enableDual) {
                                        onChangeBiometricAuthMode(BiometricAuthMode.DUAL_PARALLEL)
                                    } else {
                                        val fallback = when {
                                            hasPersonalFaceEnrolled -> BiometricAuthMode.PERSONAL_FACE
                                            isFingerprintCryptoBound -> BiometricAuthMode.SYSTEM_BIOMETRIC
                                            else -> BiometricAuthMode.PASSWORD_ONLY
                                        }
                                        onChangeBiometricAuthMode(fallback)
                                    }
                                },
                                modifier = Modifier.testTag("dual_auth_mode_switch")
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BiometricAuthMode.entries.forEach { authMode ->
                                FilterChip(
                                    selected = biometricAuthMode == authMode,
                                    onClick = { onChangeBiometricAuthMode(authMode) },
                                    label = { Text(authMode.displayLabel) },
                                    modifier = Modifier.testTag("auth_mode_chip_${authMode.name}")
                                )
                            }
                        }

                        if (biometricAuthMode.allowsSystemBiometric) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ưu tiên quét vân tay trước (chuyển Khuôn mặt / PIN khi cần)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (isFingerprintCryptoBound) {
                                            "Đã liên kết khóa Keystore CryptoObject cho vân tay màn hình"
                                        } else {
                                            "Chưa liên kết vân tay màn hình (tự liên kết ở lần quét đầu)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isFingerprintCryptoBound) EmeraldOk else TextSecondary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = fingerprintFirstWithPinFallback,
                                    onCheckedChange = onSetFingerprintFirstWithPinFallback,
                                    modifier = Modifier.testTag("fingerprint_first_fallback_switch")
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activity != null) {
                                        onBindScreenFingerprint(activity) { msg ->
                                            fingerprintBindFeedback = msg
                                        }
                                    } else {
                                        fingerprintBindFeedback = "Không thể mở cảm biến vân tay trong ngữ cảnh hiện tại."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("bind_screen_fingerprint_button")
                            ) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Liên kết / Kiểm tra vân tay màn hình (CryptoObject)")
                            }

                            if (!fingerprintBindFeedback.isNullOrBlank()) {
                                Text(
                                    text = fingerprintBindFeedback!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanBright
                                )
                            }
                        }
                    }
                }

                // Independent App-Owned Personal Face ID Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(
                        1.dp,
                        if (hasPersonalFaceEnrolled) EmeraldOk.copy(alpha = 0.6f) else SlateBorder
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Face,
                                contentDescription = null,
                                tint = if (hasPersonalFaceEnrolled) EmeraldOk else CyanBright
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Khuôn mặt cá nhân (Trong app)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                val faceStateText = when {
                                    hasPersonalFaceEnrolled && biometricAuthMode.allowsPersonalFace ->
                                        "Đã đăng ký & đang bật (${biometricAuthMode.displayLabel}) • Bắt buộc chớp mắt"
                                    hasPersonalFaceEnrolled ->
                                        "Đã đăng ký • Hãy chọn chế độ 'ID khuôn mặt cá nhân' hoặc 'Xác thực kép' để kích hoạt"
                                    else ->
                                        "Không liên quan sinh trắc học điện thoại • CameraX + ML Kit chớp mắt kiểm tra người thật"
                                }
                                Text(
                                    text = faceStateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasPersonalFaceEnrolled) EmeraldOk else TextSecondary
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showEnrollFaceDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("setup_personal_face_button")
                            ) {
                                Icon(Icons.Rounded.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (hasPersonalFaceEnrolled) "Cập nhật Khuôn mặt" else "Đăng ký Khuôn mặt")
                            }
                            if (hasPersonalFaceEnrolled) {
                                OutlinedButton(
                                    onClick = onRemovePersonalFace,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("remove_personal_face_button")
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Xóa khuôn mặt cá nhân", tint = CoralError)
                                }
                            }
                        }
                    }
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

    if (showEnrollFaceDialog) {
        PersonalFaceCameraDialog(
            mode = PersonalFaceScanMode.ENROLL,
            title = if (targetType == "APP") {
                "Đăng ký Khuôn mặt cho Ứng dụng"
            } else {
                "Đăng ký Khuôn mặt: $targetLabel"
            },
            onFaceEnrolled = { vector ->
                onSavePersonalFace(vector)
                showEnrollFaceDialog = false
            },
            onDismiss = { showEnrollFaceDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumericAndTextTabContent(
    uiState: PasswordSettingsUiState,
    onSelectSubMode: (PasswordLockMode) -> Unit,
    onTextChanged: (String) -> Unit,
    onConfirmTextChanged: (String) -> Unit,
    onToggleVisibility: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kiểu nhập mật khẩu", style = MaterialTheme.typography.titleSmall, color = CyanBright)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PasswordLockMode.DIGITS,
                        PasswordLockMode.LETTERS,
                        PasswordLockMode.ALPHANUMERIC,
                        PasswordLockMode.CUSTOM
                    ).forEach { mode ->
                        FilterChip(
                            selected = uiState.selectedMode == mode,
                            onClick = { onSelectSubMode(mode) },
                            label = { Text(mode.displayLabel) },
                            modifier = Modifier.testTag("mode_chip_${mode.name}")
                        )
                    }
                }
                Text(uiState.selectedMode.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PasswordVisualFeedbackIndicator(
                    value = uiState.textSecret,
                    revealed = uiState.isPasswordVisible,
                    isValid = uiState.validation.isValid
                )

                OutlinedTextField(
                    value = uiState.textSecret,
                    onValueChange = onTextChanged,
                    label = { Text("Nhập mật khẩu (${uiState.selectedMode.displayLabel})") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (uiState.selectedMode == PasswordLockMode.DIGITS) {
                            KeyboardType.NumberPassword
                        } else {
                            KeyboardType.Text
                        }
                    ),
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        if (uiState.canTogglePasswordVisibility) {
                            IconButton(
                                onClick = onToggleVisibility,
                                modifier = Modifier.testTag("security_password_visibility_toggle")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (uiState.isPasswordVisible) {
                                        "Ẩn mật khẩu"
                                    } else {
                                        "Hiển thị mật khẩu"
                                    }
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_password_input")
                )

                if (uiState.requireConfirmation) {
                    OutlinedTextField(
                        value = uiState.confirmSecret,
                        onValueChange = onConfirmTextChanged,
                        label = { Text("Nhập lại để xác nhận mật khẩu") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (uiState.selectedMode == PasswordLockMode.DIGITS) {
                                KeyboardType.NumberPassword
                            } else {
                                KeyboardType.Text
                            }
                        ),
                        visualTransformation = if (uiState.isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_password_confirm_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.validation.isValid) {
                            "Hợp lệ (${uiState.textSecret.length} ký tự)"
                        } else {
                            uiState.validation.errorMessage.orEmpty()
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.validation.isValid) EmeraldOk else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.canTogglePasswordVisibility) {
                        TextButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier.testTag("password_visibility_text_button")
                        ) {
                            Text(
                                if (uiState.isPasswordVisible) "Đang hiện mật khẩu" else "Hiện mật khẩu",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanBright
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternTabContent(
    uiState: PasswordSettingsUiState,
    onSelectGridSize: (Int) -> Unit,
    onPatternNodesChanged: (List<Int>) -> Unit,
    onConfirmPatternNodesChanged: (List<Int>) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GridOn, contentDescription = null, tint = CyanBright)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Kích thước ma trận lưới hình (${uiState.patternGridSize}x${uiState.patternGridSize})",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanBright
                )
            }
            Text(
                text = "Chọn kích thước từ 3x3 đến 10x10 và nối tối thiểu 4 điểm. Không hỗ trợ nút hiển thị ký tự cho chế độ hình vẽ.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (SecurityLockCrypto.MIN_GRID_SIZE..SecurityLockCrypto.MAX_GRID_SIZE).forEach { size ->
                    FilterChip(
                        selected = uiState.patternGridSize == size,
                        onClick = { onSelectGridSize(size) },
                        label = { Text("${size}x${size}") },
                        modifier = Modifier.testTag("grid_size_$size")
                    )
                }
            }

            PatternLockGrid(
                gridSize = uiState.patternGridSize,
                selectedNodes = uiState.patternNodes,
                onNodesChanged = onPatternNodesChanged
            )

            if (uiState.requireConfirmation) {
                Text(
                    text = "Vẽ lại hình vẽ một lần nữa để xác nhận",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanBright,
                    modifier = Modifier.testTag("pattern_confirm_grid_section")
                )
                PatternLockGrid(
                    gridSize = uiState.patternGridSize,
                    selectedNodes = uiState.confirmPatternNodes,
                    onNodesChanged = onConfirmPatternNodesChanged
                )
            }
        }
    }
}

@Composable
private fun PasswordVisualFeedbackIndicator(
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
            .semantics(mergeDescendants = true) {}
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

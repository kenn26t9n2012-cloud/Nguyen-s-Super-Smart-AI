package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.LocalizedUiStrings
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.WorldLanguagesCatalog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.auth.GoogleAuthHelper
import com.nguyen_super_app.nguyen_super_smart_ai.ui.auth.MultiAuthMode
import com.nguyen_super_app.nguyen_super_smart_ai.ui.auth.OAuthPlatformsCatalog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics.NeuralCoreHeroIllustration
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanContainer
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthGatewayScreen(
    strings: LocalizedUiStrings,
    currentLanguageCode: String,
    onOpenLanguageSelector: () -> Unit,
    onAuthSuccess: () -> Unit,
    onSkipGuestMode: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var oauthQuery by remember { mutableStateOf("") }
    var showPhoneEmailDialog by remember { mutableStateOf(false) }

    val filteredPlatforms = remember(oauthQuery) {
        OAuthPlatformsCatalog.platforms.filter {
            oauthQuery.isBlank() ||
                it.displayName.contains(oauthQuery, ignoreCase = true) ||
                it.category.contains(oauthQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = SlateDeep
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SlateDeep, SlateCard.copy(alpha = 0.7f), SlateDeep)))
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CyanContainer,
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hybrid Cloud + Offline GGUF", style = MaterialTheme.typography.labelMedium, color = CyanBright)
                        }
                    }

                    AssistChip(
                        onClick = onOpenLanguageSelector,
                        label = { Text(WorldLanguagesCatalog.findByCode(currentLanguageCode).nativeName) },
                        leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = strings.switchLanguage, tint = CyanPrimary) },
                        modifier = Modifier.testTag("auth_language_button")
                    )
                }

                Spacer(Modifier.height(14.dp))
                NeuralCoreHeroIllustration(sizeDp = 104.dp)
                Spacer(Modifier.height(10.dp))

                Text(strings.appTitle, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text(
                    "Gemini Cloud • 50+ Mô hình cục bộ • Trình duyệt Model • 75+ Định dạng",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(Modifier.height(16.dp))

                // Guest Mode Skip Button (Immediate Room DB entry without Firestore)
                OutlinedButton(
                    onClick = onSkipGuestMode,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, CyanPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("skip_guest_button")
                ) {
                    Text(strings.skipGuestMode, color = CyanBright, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = CyanBright)
                }

                Spacer(Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                GoogleAuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = onAuthSuccess,
                                    onAuthError = { err -> statusMessage = err },
                                    scope = scope,
                                    onAuthCancelled = { statusMessage = "Đã hủy chọn tài khoản Google." }
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateDeep),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_signin_button")
                        ) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.signInGoogle, style = MaterialTheme.typography.titleMedium)
                        }

                        OutlinedButton(
                            onClick = { showPhoneEmailDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("phone_email_5mode_button")
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, tint = CyanBright)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.signInPhoneEmail)
                        }

                        HorizontalDivider(color = SlateBorder)

                        Text(strings.oauthSectionTitle, style = MaterialTheme.typography.titleSmall, color = CyanBright)

                        OutlinedTextField(
                            value = oauthQuery,
                            onValueChange = { oauthQuery = it },
                            placeholder = { Text(strings.searchPlatformHint) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Tìm nền tảng") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("oauth_search_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredPlatforms.forEach { platform ->
                                AssistChip(
                                    onClick = {
                                        val act = context as? Activity
                                        if (act != null) {
                                            GoogleAuthHelper.startOAuthProviderFlow(
                                                activity = act,
                                                providerId = platform.providerId,
                                                onSuccess = onAuthSuccess,
                                                onError = { statusMessage = it }
                                            )
                                        }
                                    },
                                    label = { Text(platform.displayName) },
                                    modifier = Modifier.testTag("oauth_chip_${platform.providerId}")
                                )
                            }
                        }
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = SlateElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldOk)
                            Spacer(Modifier.width(8.dp))
                            Text(statusMessage!!, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showPhoneEmailDialog) {
        FiveModePhoneEmailAuthDialog(
            onAuthSuccess = {
                showPhoneEmailDialog = false
                onAuthSuccess()
            },
            onStatus = { statusMessage = it },
            onDismiss = { showPhoneEmailDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiveModePhoneEmailAuthDialog(
    onAuthSuccess: () -> Unit,
    onStatus: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(MultiAuthMode.EMAIL_PASSWORD) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = { Text("Chọn 1 trong 5 chế độ Đăng nhập", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MultiAuthMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m; feedback = null },
                            label = { Text(m.label) },
                            modifier = Modifier.testTag("auth_mode_${m.name}")
                        )
                    }
                }
                Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = CyanBright)

                if (mode == MultiAuthMode.EMAIL_PASSWORD || mode == MultiAuthMode.EMAIL_LINK || mode == MultiAuthMode.EMAIL_PHONE_2FA) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mode == MultiAuthMode.PHONE_OTP || mode == MultiAuthMode.PHONE_PASSWORD || mode == MultiAuthMode.EMAIL_PHONE_2FA) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại (+84...)") },
                        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mode == MultiAuthMode.EMAIL_PASSWORD || mode == MultiAuthMode.PHONE_PASSWORD) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Hiện mật khẩu"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (verificationId != null && (mode == MultiAuthMode.PHONE_OTP || mode == MultiAuthMode.EMAIL_PHONE_2FA)) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("Mã OTP SMS 6 số") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mode == MultiAuthMode.EMAIL_PASSWORD) {
                    TextButton(onClick = {
                        GoogleAuthHelper.sendPasswordReset(email) { msg -> feedback = msg }
                    }) {
                        Text("Quên mật khẩu? Gửi email đặt lại", color = CyanBright)
                    }
                }

                if (!feedback.isNullOrBlank()) {
                    Text(feedback!!, style = MaterialTheme.typography.bodySmall, color = CyanBright)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val act = context as? Activity
                when (mode) {
                    MultiAuthMode.EMAIL_LINK -> {
                        GoogleAuthHelper.sendPasswordlessEmailLink(email, context.packageName) { msg ->
                            feedback = msg
                            onStatus(msg)
                        }
                    }
                    MultiAuthMode.PHONE_OTP, MultiAuthMode.EMAIL_PHONE_2FA -> {
                        val normalizedPhone = GoogleAuthHelper.normalizePhoneE164(phone)
                        if (verificationId == null && act != null && normalizedPhone.length >= 10) {
                            GoogleAuthHelper.requestPhoneOtp(
                                activity = act,
                                phoneNumber = normalizedPhone,
                                callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                                        Firebase.auth.signInWithCredential(cred)
                                            .addOnSuccessListener { onAuthSuccess() }
                                            .addOnFailureListener { feedback = it.localizedMessage }
                                    }

                                    override fun onVerificationFailed(e: FirebaseException) {
                                        feedback = e.localizedMessage ?: "Xác thực SMS chưa sẵn sàng."
                                    }

                                    override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                                        verificationId = vid
                                        feedback = "Đã gửi mã OTP tới $normalizedPhone."
                                    }
                                }
                            )
                        } else if (verificationId != null && otpCode.isNotBlank()) {
                            val cred = PhoneAuthProvider.getCredential(verificationId!!, otpCode.trim())
                            Firebase.auth.signInWithCredential(cred)
                                .addOnSuccessListener { onAuthSuccess() }
                                .addOnFailureListener { feedback = it.localizedMessage }
                        } else {
                            feedback = "Nhập số điện thoại hợp lệ (ví dụ +84912345678 hoặc 0912345678)."
                        }
                    }
                    MultiAuthMode.EMAIL_PASSWORD -> {
                        GoogleAuthHelper.signInWithEmailPassword(
                            email = email,
                            password = password,
                            onSuccess = onAuthSuccess,
                            onError = { err -> feedback = err }
                        )
                    }
                    MultiAuthMode.PHONE_PASSWORD -> {
                        val normalizedPhone = GoogleAuthHelper.normalizePhoneE164(phone)
                        if (normalizedPhone.length < 10 || password.length < 6) {
                            feedback = "Nhập số điện thoại hợp lệ và mật khẩu >= 6 ký tự."
                        } else {
                            feedback = "Chế độ SĐT + Mật khẩu sử dụng xác thực kép qua SMS OTP hoặc Google Sign-In."
                        }
                    }
                }
            }) {
                Icon(Icons.Rounded.AlternateEmail, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Tiếp tục")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}

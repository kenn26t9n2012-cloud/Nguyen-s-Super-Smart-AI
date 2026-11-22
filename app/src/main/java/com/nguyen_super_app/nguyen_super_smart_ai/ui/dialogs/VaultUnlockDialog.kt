package com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.nguyen_super_app.nguyen_super_smart_ai.security.EncryptedStorageManager
import com.nguyen_super_app.nguyen_super_smart_ai.ui.auth.GoogleAuthHelper
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary

enum class VaultUnlockMethod(val label: String) {
    BIOMETRIC("Sinh trắc học"),
    PASSWORD("Mật khẩu"),
    OTP("OTP SĐT / Email")
}

private enum class OtpChannel(val label: String) {
    PHONE_SMS("Mã OTP SĐT"),
    EMAIL_LINK("Xác thực Email")
}

/**
 * Multi-factor pre-decryption verification dialog required before decrypting, editing,
 * or validating stored API keys in Keystore Vault. Strictly excludes Passkeys.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VaultUnlockDialog(
    hasLocalVaultPassphrase: Boolean,
    defaultEmail: String?,
    defaultPhone: String?,
    onVerifyLocalPassphrase: (CharArray) -> Boolean,
    onSetLocalPassphrase: (CharArray) -> Boolean,
    onUnlocked: (methodLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val firebaseUser: FirebaseUser? = remember(context) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth.currentUser else null
        } catch (_: Throwable) {
            null
        }
    }

    var method by remember { mutableStateOf(VaultUnlockMethod.BIOMETRIC) }
    var useAccountPassword by remember { mutableStateOf(firebaseUser?.email != null) }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var otpChannel by remember { mutableStateOf(OtpChannel.PHONE_SMS) }
    var phoneInput by remember { mutableStateOf(defaultPhone.orEmpty()) }
    var emailInput by remember { mutableStateOf(defaultEmail ?: firebaseUser?.email.orEmpty()) }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var emailVerificationDispatched by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Row {
                Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = CyanBright)
                Spacer(Modifier.width(8.dp))
                Text("Xác thực mở khóa API Vault", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Bắt buộc xác thực trước khi giải mã hoặc kiểm tra khóa API (AES-256-GCM).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VaultUnlockMethod.entries.forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = {
                                method = m
                                feedback = null
                                isError = false
                            },
                            label = { Text(m.label) },
                            modifier = Modifier.testTag("vault_unlock_tab_${m.name}")
                        )
                    }
                }

                when (method) {
                    VaultUnlockMethod.BIOMETRIC -> {
                        Text(
                            "Sử dụng vân tay, khuôn mặt hoặc mã PIN/mẫu hình thiết bị thông qua BiometricPrompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanBright
                        )
                        Button(
                            onClick = {
                                if (activity == null) {
                                    isError = true
                                    feedback = "Không tìm thấy FragmentActivity để mở BiometricPrompt."
                                    return@Button
                                }
                                EncryptedStorageManager(activity).authenticateWithBiometric(
                                    activity = activity,
                                    title = "Giải mã Keystore API Vault",
                                    subtitle = "Xác thực sinh trắc học trước khi truy cập và kiểm tra khóa API",
                                    onSuccess = { onUnlocked("Sinh trắc học") },
                                    onError = { err ->
                                        isError = true
                                        feedback = err
                                    },
                                    onFailed = {
                                        isError = true
                                        feedback = "Sinh trắc học không khớp, vui lòng thử lại."
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_unlock_biometric_button")
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Xác thực Sinh trắc học ngay")
                        }
                    }

                    VaultUnlockMethod.PASSWORD -> {
                        if (firebaseUser?.email != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = useAccountPassword,
                                    onClick = { useAccountPassword = true; feedback = null },
                                    label = { Text("Mật khẩu Tài khoản") }
                                )
                                FilterChip(
                                    selected = !useAccountPassword,
                                    onClick = { useAccountPassword = false; feedback = null },
                                    label = { Text("Mật khẩu Vault") }
                                )
                            }
                        }

                        val isCreatingLocalPassphrase = !useAccountPassword && !hasLocalVaultPassphrase
                        Text(
                            when {
                                useAccountPassword -> "Nhập mật khẩu tài khoản (${firebaseUser?.email}) để xác thực lại."
                                isCreatingLocalPassphrase -> "Chưa có Mật khẩu Vault cục bộ. Nhập mật khẩu (>= 4 ký tự) để tạo và mở khóa."
                                else -> "Nhập Mật khẩu Vault cục bộ để giải mã khóa API."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanBright
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = {
                                Text(
                                    if (isCreatingLocalPassphrase) "Tạo Mật khẩu Vault (>= 4 ký tự)"
                                    else "Mật khẩu xác thực"
                                )
                            },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_unlock_password_input")
                        )

                        Button(
                            onClick = {
                                if (useAccountPassword) {
                                    GoogleAuthHelper.reauthenticateAccountPassword(
                                        password = passwordInput,
                                        onSuccess = {
                                            passwordInput = ""
                                            onUnlocked("Mật khẩu tài khoản")
                                        },
                                        onError = { err ->
                                            isError = true
                                            feedback = err
                                        }
                                    )
                                } else if (isCreatingLocalPassphrase) {
                                    val chars = passwordInput.toCharArray()
                                    if (onSetLocalPassphrase(chars)) {
                                        passwordInput = ""
                                        onUnlocked("Mật khẩu Vault cục bộ")
                                    } else {
                                        isError = true
                                        feedback = "Mật khẩu Vault cần tối thiểu 4 ký tự."
                                    }
                                } else {
                                    val chars = passwordInput.toCharArray()
                                    if (onVerifyLocalPassphrase(chars)) {
                                        passwordInput = ""
                                        onUnlocked("Mật khẩu Vault cục bộ")
                                    } else {
                                        isError = true
                                        feedback = "Mật khẩu Vault không chính xác."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_unlock_password_submit")
                        ) {
                            Text(if (isCreatingLocalPassphrase) "Lưu mật khẩu & Mở khóa" else "Xác nhận Mật khẩu")
                        }
                    }

                    VaultUnlockMethod.OTP -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OtpChannel.entries.forEach { ch ->
                                FilterChip(
                                    selected = otpChannel == ch,
                                    onClick = { otpChannel = ch; feedback = null; isError = false },
                                    label = { Text(ch.label) }
                                )
                            }
                        }

                        if (otpChannel == OtpChannel.PHONE_SMS) {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("Số điện thoại (+84... hoặc 09...)") },
                                leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (verificationId != null) {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it },
                                    label = { Text("Nhập mã OTP SMS 6 số") },
                                    leadingIcon = { Icon(Icons.Rounded.Sms, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val normalized = GoogleAuthHelper.normalizePhoneE164(phoneInput)
                                        if (activity == null || normalized.length < 10) {
                                            isError = true
                                            feedback = "Nhập số điện thoại hợp lệ để nhận OTP SMS."
                                            return@OutlinedButton
                                        }
                                        GoogleAuthHelper.requestPhoneOtp(
                                            activity = activity,
                                            phoneNumber = normalized,
                                            callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                                override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                                                    val user = try {
                                                        if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth.currentUser else null
                                                    } catch (_: Throwable) { null }
                                                    val task = if (user != null) user.reauthenticate(cred) else Firebase.auth.signInWithCredential(cred)
                                                    task.addOnSuccessListener { onUnlocked("OTP SĐT") }
                                                        .addOnFailureListener {
                                                            isError = true
                                                            feedback = it.localizedMessage ?: "Xác thực OTP SĐT thất bại."
                                                        }
                                                }

                                                override fun onVerificationFailed(e: FirebaseException) {
                                                    isError = true
                                                    feedback = e.localizedMessage ?: "Gửi SMS OTP thất bại."
                                                }

                                                override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                                                    verificationId = vid
                                                    isError = false
                                                    feedback = "Đã gửi mã OTP tới $normalized."
                                                }
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (verificationId == null) "Gửi OTP SMS" else "Gửi lại")
                                }
                                if (verificationId != null) {
                                    Button(
                                        onClick = {
                                            if (otpCode.trim().length < 4) {
                                                isError = true
                                                feedback = "Nhập đầy đủ mã OTP SMS."
                                                return@Button
                                            }
                                            val cred = PhoneAuthProvider.getCredential(verificationId!!, otpCode.trim())
                                            val user = try {
                                                if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth.currentUser else null
                                            } catch (_: Throwable) { null }
                                            val task = if (user != null) user.reauthenticate(cred) else Firebase.auth.signInWithCredential(cred)
                                            task.addOnSuccessListener { onUnlocked("OTP SĐT") }
                                                .addOnFailureListener {
                                                    isError = true
                                                    feedback = it.localizedMessage ?: "Mã OTP SMS không đúng."
                                                }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Xác nhận OTP") }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email nhận xác thực") },
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (!GoogleAuthHelper.isValidEmail(emailInput)) {
                                            isError = true
                                            feedback = "Vui lòng nhập địa chỉ email hợp lệ."
                                            return@OutlinedButton
                                        }
                                        val user = try {
                                            if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth.currentUser else null
                                        } catch (_: Throwable) { null }
                                        if (user != null && user.email.equals(emailInput.trim(), ignoreCase = true) && !user.isEmailVerified) {
                                            user.sendEmailVerification()
                                                .addOnSuccessListener {
                                                    emailVerificationDispatched = true
                                                    isError = false
                                                    feedback = "Đã gửi email xác thực tới ${emailInput.trim()}. Sau khi xác nhận, nhấn Kiểm tra."
                                                }
                                                .addOnFailureListener {
                                                    isError = true
                                                    feedback = it.localizedMessage ?: "Không thể gửi email xác thực."
                                                }
                                        } else {
                                            GoogleAuthHelper.sendPasswordlessEmailLink(emailInput, context.packageName) { msg ->
                                                emailVerificationDispatched = msg.startsWith("Đã gửi")
                                                isError = !emailVerificationDispatched
                                                feedback = msg
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Gửi mã / Liên kết Email")
                                }
                                if (emailVerificationDispatched || firebaseUser?.email != null) {
                                    Button(
                                        onClick = {
                                            val user = try {
                                                if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth.currentUser else null
                                            } catch (_: Throwable) { null }
                                            if (user != null) {
                                                user.reload().addOnSuccessListener {
                                                    if (user.isEmailVerified) {
                                                        onUnlocked("Xác thực Email")
                                                    } else {
                                                        isError = true
                                                        feedback = "Email chưa được xác nhận. Hãy mở hộp thư và bấm liên kết xác thực."
                                                    }
                                                }.addOnFailureListener {
                                                    isError = true
                                                    feedback = it.localizedMessage ?: "Không thể kiểm tra trạng thái email."
                                                }
                                            } else {
                                                isError = true
                                                feedback = "Vui lòng bấm liên kết trong email để hoàn tất đăng nhập xác thực."
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Kiểm tra")
                                    }
                                }
                            }
                        }
                    }
                }

                if (!feedback.isNullOrBlank()) {
                    Text(
                        feedback!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) CoralError else EmeraldOk
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

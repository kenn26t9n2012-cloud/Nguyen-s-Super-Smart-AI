package com.nguyen_super_app.nguyen_super_smart_ai.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.nguyen_super_app.nguyen_super_smart_ai.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class OAuthPlatformSpec(
    val providerId: String,
    val displayName: String,
    val category: String
)

object OAuthPlatformsCatalog {
    val platforms: List<OAuthPlatformSpec> = listOf(
        OAuthPlatformSpec("github.com", "GitHub", "Developer"),
        OAuthPlatformSpec("apple.com", "Apple", "Ecosystem"),
        OAuthPlatformSpec("microsoft.com", "Microsoft", "Enterprise"),
        OAuthPlatformSpec("yahoo.com", "Yahoo", "Email"),
        OAuthPlatformSpec("twitter.com", "Twitter / X", "Social"),
        OAuthPlatformSpec("facebook.com", "Facebook", "Social"),
        OAuthPlatformSpec("linkedin.com", "LinkedIn", "Professional"),
        OAuthPlatformSpec("gitlab.com", "GitLab", "Developer"),
        OAuthPlatformSpec("discord.com", "Discord", "Community"),
        OAuthPlatformSpec("slack.com", "Slack", "Workspace"),
        OAuthPlatformSpec("bitbucket.org", "Bitbucket", "Developer"),
        OAuthPlatformSpec("twitch.tv", "Twitch", "Media")
    )
}

enum class MultiAuthMode(val label: String, val subtitle: String) {
    PHONE_OTP("SĐT + OTP", "Xác thực qua mã SMS một lần"),
    PHONE_PASSWORD("SĐT + Mật khẩu", "Đăng nhập bằng số điện thoại và mật khẩu"),
    EMAIL_LINK("Email Link (Không mật khẩu)", "Gửi liên kết đăng nhập trực tiếp tới hộp thư"),
    EMAIL_PHONE_2FA("Email + SĐT xác thực kép", "Xác thực hai lớp Email kết hợp SMS"),
    EMAIL_PASSWORD("Email + Mật khẩu", "Kèm tính năng Quên mật khẩu")
}

object GoogleAuthHelper {
    private const val TAG = "Auth"

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: NoCredentialException) {
                onUnauthenticated()
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Thiếu cấu hình Google Sign-In: không tìm thấy default_web_client_id.")
            return
        }
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        scope.launch {
            try {
                val activity = context as? Activity
                    ?: throw IllegalStateException("Cần Activity Context cho CredentialManager.")
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Loại thông tin xác thực không hợp lệ.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credential available on device: ${e.message}")
                onAuthError("Thiết bị chưa có tài khoản Google khả dụng. Bạn có thể đăng nhập bằng SĐT/Email hoặc nhấn Bỏ qua.")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Đăng nhập Google thất bại.")
            }
        }
    }

    fun startOAuthProviderFlow(
        activity: Activity,
        providerId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val provider = OAuthProvider.newBuilder(providerId).build()
        val auth = Firebase.auth
        val task = if (auth.currentUser != null) {
            auth.currentUser!!.startActivityForLinkWithProvider(activity, provider)
        } else {
            auth.startActivityForSignInWithProvider(activity, provider)
        }
        task.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onError(
                e.localizedMessage
                    ?: "Nền tảng $providerId cần kích hoạt Client ID/Secret trong Firebase Console."
            )
        }
    }

    fun sendPasswordReset(
        email: String,
        onDone: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onDone("Vui lòng nhập địa chỉ email trước khi đặt lại mật khẩu.")
            return
        }
        Firebase.auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                onDone("Đã gửi email đặt lại mật khẩu tới ${email.trim()}.")
            }
            .addOnFailureListener { e ->
                onDone(e.localizedMessage ?: "Không thể gửi email đặt lại mật khẩu.")
            }
    }

    fun sendPasswordlessEmailLink(
        email: String,
        packageName: String,
        onDone: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onDone("Vui lòng nhập email để nhận liên kết đăng nhập.")
            return
        }
        val settings = ActionCodeSettings.newBuilder()
            .setUrl("https://supersmartai.page.link/login")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(packageName, true, "27")
            .build()
        Firebase.auth.sendSignInLinkToEmail(email.trim(), settings)
            .addOnSuccessListener {
                onDone("Đã gửi liên kết không mật khẩu tới ${email.trim()}.")
            }
            .addOnFailureListener { e ->
                onDone(
                    e.localizedMessage
                        ?: "Email Link cần bật Email/Password provider trong Firebase Console. Bạn có thể dùng Google Sign-In ngay."
                )
            }
    }

    fun normalizePhoneE164(rawPhone: String): String {
        val cleaned = rawPhone.trim().replace(" ", "").replace("-", "")
        if (cleaned.isEmpty()) return ""
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("0") && cleaned.length >= 9 -> "+84${cleaned.drop(1)}"
            else -> "+$cleaned"
        }
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.contains("@") && trimmed.substringAfter("@").contains(".")
    }

    fun signInWithEmailPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail) || password.length < 6) {
            onError("Vui lòng nhập email hợp lệ và mật khẩu tối thiểu 6 ký tự.")
            return
        }
        Firebase.auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(
                    e.localizedMessage
                        ?: "Đăng nhập thất bại. Kiểm tra thông tin hoặc kích hoạt Email/Password trong Firebase Console."
                )
            }
    }

    fun reauthenticateAccountPassword(
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = Firebase.auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            onError("Tài khoản hiện tại chưa liên kết Email/Mật khẩu. Hãy dùng mật khẩu Vault cục bộ, Sinh trắc học hoặc OTP.")
            return
        }
        if (password.isBlank()) {
            onError("Vui lòng nhập mật khẩu tài khoản.")
            return
        }
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Mật khẩu tài khoản không khớp hoặc cần đăng nhập lại.")
            }
    }

    fun requestPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val normalized = normalizePhoneE164(phoneNumber)
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(normalized)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}

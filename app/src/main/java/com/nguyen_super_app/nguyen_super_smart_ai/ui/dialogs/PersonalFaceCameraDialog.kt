package com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nguyen_super_app.nguyen_super_smart_ai.security.FaceLivenessTracker
import com.nguyen_super_app.nguyen_super_smart_ai.security.FacialLandmarksInput
import com.nguyen_super_app.nguyen_super_smart_ai.security.PersonalFaceAuthenticator
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class PersonalFaceScanMode {
    ENROLL,
    VERIFY
}

/**
 * Circular CameraX + On-Device ML Kit Face Detection dialog for App-Owned Personal Face ID.
 * Completely independent of the Android phone's system Face Unlock, enforcing mandatory
 * live eye-blink liveness on BOTH enrollment and verification, with seamless fallback to
 * Fingerprint / Phone PIN / App Password during unlock.
 */
@Composable
fun PersonalFaceCameraDialog(
    mode: PersonalFaceScanMode,
    title: String,
    onFaceEnrolled: (FloatArray) -> Unit = {},
    onVerifyCandidate: (FloatArray) -> Boolean = { false },
    onVerifiedSuccess: () -> Unit = {},
    onSwitchToAlternativeMethod: (() -> Unit)? = null,
    onVerificationFailedOrDismissed: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var statusText by remember(mode) {
        mutableStateOf(
            if (mode == PersonalFaceScanMode.ENROLL) {
                "Đưa khuôn mặt vào khung tròn và chớp mắt nhẹ để đăng ký."
            } else {
                "Đưa khuôn mặt đã đăng ký vào khung và chớp mắt nhẹ để mở khóa."
            }
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progressRatio by remember { mutableFloatStateOf(0f) }
    var retryToken by remember { mutableIntStateOf(0) }
    val completedGuard = remember(retryToken) { AtomicBoolean(false) }

    val livenessTracker = remember(mode, retryToken) {
        FaceLivenessTracker(
            requiredStableFrames = if (mode == PersonalFaceScanMode.ENROLL) 5 else 3
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val handleDismiss = {
        if (mode == PersonalFaceScanMode.VERIFY && !completedGuard.get()) {
            onVerificationFailedOrDismissed?.invoke(
                "Đã bỏ qua quét khuôn mặt cá nhân — bạn có thể chuyển đổi phương thức ngay."
            )
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        containerColor = SlateCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Face, contentDescription = null, tint = CyanBright)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = SlateElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bắt buộc chớp mắt kiểm tra người thật cả khi đăng ký lẫn mở khóa. Vector đặc trưng được mã hóa AES-256-GCM trong Android Keystore, tách biệt khỏi điện thoại.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (!hasCameraPermission) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CameraFront,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Cần quyền Camera trước để quét và nhận diện khuôn mặt cá nhân.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("personal_face_request_camera_button")
                        ) {
                            Text("Cấp quyền Camera")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(CircleShape)
                            .background(SlateElevated)
                            .border(
                                BorderStroke(
                                    3.dp,
                                    if (errorMessage != null) CoralError
                                    else if (progressRatio >= 1f) EmeraldOk
                                    else CyanBright
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FrontCameraFacePreview(
                            retryToken = retryToken,
                            lifecycleOwner = lifecycleOwner,
                            onFaceFrame = { landmarks ->
                                if (completedGuard.get()) return@FrontCameraFacePreview
                                val progress = livenessTracker.processFrame(
                                    input = landmarks,
                                    requireBlink = true
                                )
                                progressRatio = (progress.collectedCount.toFloat() / progress.requiredCount.toFloat())
                                    .coerceIn(0f, 1f)
                                statusText = progress.statusText
                                val vector = progress.averagedVector
                                if (progress.readyToComplete && vector != null && completedGuard.compareAndSet(false, true)) {
                                    if (mode == PersonalFaceScanMode.ENROLL) {
                                        onFaceEnrolled(vector)
                                    } else {
                                        val ok = onVerifyCandidate(vector)
                                        if (ok) {
                                            onVerifiedSuccess()
                                        } else {
                                            val mismatchMsg = "Khuôn mặt không khớp với dữ liệu đã đăng ký trong ứng dụng."
                                            errorMessage = mismatchMsg
                                            onVerificationFailedOrDismissed?.invoke(mismatchMsg)
                                        }
                                    }
                                }
                            },
                            onNoFaceFrame = {
                                if (!completedGuard.get()) {
                                    statusText = "Đặt khuôn mặt rõ ánh sáng vào giữa khung tròn"
                                }
                            },
                            onCameraError = { err ->
                                errorMessage = err
                                if (mode == PersonalFaceScanMode.VERIFY) {
                                    onVerificationFailedOrDismissed?.invoke(err)
                                }
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyanBright
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (errorMessage != null) CoralError else CyanBright
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralError
                        )
                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                progressRatio = 0f
                                retryToken += 1
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Thử quét lại")
                        }
                    }
                }

                if (mode == PersonalFaceScanMode.VERIFY && onSwitchToAlternativeMethod != null) {
                    OutlinedButton(
                        onClick = onSwitchToAlternativeMethod,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("personal_face_switch_method_button")
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Chuyển đổi phương thức")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = handleDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("personal_face_cancel_button")
            ) {
                Text("Hủy / Đóng", color = CyanBright)
            }
        }
    )
}

@Composable
private fun FrontCameraFacePreview(
    retryToken: Int,
    lifecycleOwner: LifecycleOwner,
    onFaceFrame: (FacialLandmarksInput) -> Unit,
    onNoFaceFrame: () -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember(retryToken) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceDetector = remember { PersonalFaceAuthenticator.buildFaceDetector() }

    DisposableEffect(lifecycleOwner, retryToken) {
        var boundProvider: ProcessCameraProvider? = null
        val isDisposed = AtomicBoolean(false)

        PersonalFaceAuthenticator.bindCameraXFaceStream(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            analysisExecutor = analysisExecutor,
            faceDetector = faceDetector,
            isDisposed = isDisposed,
            onProviderBound = { provider -> boundProvider = provider },
            onFaceLandmarks = onFaceFrame,
            onNoFace = onNoFaceFrame,
            onError = onCameraError
        )

        onDispose {
            isDisposed.set(true)
            try {
                boundProvider?.unbindAll()
            } catch (_: Throwable) {
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            faceDetector.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

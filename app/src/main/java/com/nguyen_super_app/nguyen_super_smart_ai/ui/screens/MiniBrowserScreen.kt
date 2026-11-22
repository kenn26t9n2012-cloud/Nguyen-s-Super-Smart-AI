package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import android.annotation.SuppressLint
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nguyen_super_app.nguyen_super_smart_ai.engine.browser.BrowserDownloadState
import com.nguyen_super_app.nguyen_super_smart_ai.engine.browser.DownloadPipelinePhase
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.AmberWarn
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateSurface
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary

/**
 * Custom Embedded Model Browser (`MiniBrowserScreen`):
 * - Lets users browse Hugging Face, Civitai, or any web page freely.
 * - Hooks `WebView.setDownloadListener` into `ModelDownloadPipeline` to validate extensions,
 *   download archives, extract safely, recursively inspect binary magic bytes across 50 formats,
 *   and purge invalid archives automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniBrowserScreen(
    downloadState: BrowserDownloadState,
    onDownloadRequested: (url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) -> Unit,
    onDismissBanner: () -> Unit,
    onBack: () -> Unit
) {
    val bookmarks = remember {
        listOf(
            "Hugging Face GGUF" to "https://huggingface.co/models?library=gguf&sort=trending",
            "ONNX Models" to "https://huggingface.co/models?library=onnx&sort=trending",
            "SafeTensors Hub" to "https://huggingface.co/models?library=safetensors",
            "LiteRT / TFLite" to "https://ai.google.dev/edge/litert"
        )
    }

    var currentUrl by remember { mutableStateOf(bookmarks.first().second) }
    var urlInput by remember { mutableStateOf(currentUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("browser_back_nav_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Trình duyệt Tải Mô hình AI", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = { webViewRef?.goBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Trang trước")
                    }
                    IconButton(onClick = { webViewRef?.goForward() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Trang sau")
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Tải lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Address Bar + Go
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("Nhập URL trang web hoặc liên kết mô hình...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("browser_url_input")
                )
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = {
                        val normalized = if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) {
                            urlInput.trim()
                        } else {
                            "https://${urlInput.trim()}"
                        }
                        currentUrl = normalized
                        webViewRef?.loadUrl(normalized)
                    },
                    modifier = Modifier.testTag("browser_go_button")
                ) {
                    Text("Đi")
                }
            }

            // Quick Bookmarks Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bookmarks.forEach { (label, url) ->
                    AssistChip(
                        onClick = {
                            urlInput = url
                            currentUrl = url
                            webViewRef?.loadUrl(url)
                        },
                        leadingIcon = { Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(label) }
                    )
                }
            }

            // Pipeline Status / Warning Banner
            if (downloadState.phase != DownloadPipelinePhase.IDLE) {
                val border = when (downloadState.phase) {
                    DownloadPipelinePhase.REJECTED_INVALID_EXTENSION,
                    DownloadPipelinePhase.PURGED_NO_VALID_MODELS,
                    DownloadPipelinePhase.FAILED -> CoralError
                    DownloadPipelinePhase.REGISTERED_SUCCESS -> EmeraldOk
                    else -> CyanPrimary
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.5.dp, border),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("browser_download_banner")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (downloadState.phase) {
                                    DownloadPipelinePhase.PURGED_NO_VALID_MODELS -> Icons.Rounded.DeleteForever
                                    DownloadPipelinePhase.REJECTED_INVALID_EXTENSION, DownloadPipelinePhase.FAILED -> Icons.Rounded.Warning
                                    DownloadPipelinePhase.REGISTERED_SUCCESS -> Icons.Rounded.Shield
                                    else -> Icons.Rounded.Download
                                },
                                contentDescription = null,
                                tint = border
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                downloadState.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissBanner) { Text("Ẩn") }
                        }
                        if (downloadState.phase == DownloadPipelinePhase.DOWNLOADING ||
                            downloadState.phase == DownloadPipelinePhase.EXTRACTING ||
                            downloadState.phase == DownloadPipelinePhase.SCANNING_MAGIC_BYTES
                        ) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadState.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = CyanBright
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Bảo vệ 50 định dạng AI: Tự kiểm tra đuôi & Magic Bytes, giải nén an toàn và xóa vĩnh viễn gói rác.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            // Embedded Android WebView
            Card(
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!url.isNullOrBlank()) {
                                        urlInput = url
                                        currentUrl = url
                                    }
                                }
                            }
                            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                                onDownloadRequested(url, userAgent, contentDisposition, mimeType)
                            })
                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

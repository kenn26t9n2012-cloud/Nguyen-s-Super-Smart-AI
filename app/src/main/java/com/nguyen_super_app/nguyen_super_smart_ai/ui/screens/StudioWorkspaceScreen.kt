package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.BuildQueueJobEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ExportJobEntity
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.PromptTemplateEntity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.BatchExportResult
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.DocumentExportEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.ExportCategory
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.ExportFormatSpec
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.DecodedLogoResult
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.LogoDecoderEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.IpynbExecutionReport
import com.nguyen_super_app.nguyen_super_smart_ai.engine.security.SignatureAlgorithm
import com.nguyen_super_app.nguyen_super_smart_ai.engine.security.SignatureBundle
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.BinaryDecompileReport
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.ToolchainDescriptor
import com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics.FormatEmblemCanvas
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanContainer
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateSurface
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudioWorkspaceScreen(
    exportJobs: List<ExportJobEntity>,
    batchExportResult: BatchExportResult?,
    buildQueueJobs: List<BuildQueueJobEntity>,
    toolchains: List<ToolchainDescriptor>,
    decompilerReport: BinaryDecompileReport?,
    ipynbReport: IpynbExecutionReport?,
    decodedLogoResult: DecodedLogoResult?,
    signatureBundle: SignatureBundle?,
    signatureVerified: Boolean?,
    promptTemplates: List<PromptTemplateEntity>,
    activePersonaId: String,
    onExportSingle: (ExportFormatSpec) -> Unit,
    onExportBatch: (List<ExportFormatSpec>) -> Unit,
    onSaveBatchZip: (android.net.Uri, ByteArray, String) -> Unit,
    onEnqueueBuild: (targetKind: String, projectName: String, sourceCode: String) -> Unit,
    onUpdateToolchains: () -> Unit,
    onAnalyzeBinary: (fileName: String, bytes: ByteArray) -> Unit,
    onRunIpynb: (ipynbJson: String, title: String) -> Unit,
    onDecodeLogo: (fileName: String, rawBytes: ByteArray, formatId: String) -> Unit,
    onSaveLogoPng: (android.net.Uri, ByteArray, String) -> Unit,
    onSignPayload: (payloadText: String, alg: SignatureAlgorithm, passphrase: String) -> Unit,
    onSelectPersona: (String) -> Unit,
    onUsePromptTemplate: (String) -> Unit,
    onCreateTemplate: (title: String, category: String, content: String, kind: String, langTag: String?) -> Unit,
    onBack: () -> Unit
) {
    val tabs = listOf(
        "Xuất 75+ & Hàng loạt",
        "APK / EXE & Build Queue",
        "Decompiler & .ipynb",
        "Logo 70+ & Chữ ký 20+",
        "Mẫu Prompt & Persona"
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("studio_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Studio Tệp & Công cụ Phát triển", style = MaterialTheme.typography.titleMedium) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateSurface,
                contentColor = CyanPrimary
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1) },
                        modifier = Modifier.testTag("studio_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> ExportAndBatchTab(exportJobs, batchExportResult, onExportSingle, onExportBatch, onSaveBatchZip)
                1 -> ApkExeAndBuildQueueTab(buildQueueJobs, toolchains, onEnqueueBuild, onUpdateToolchains)
                2 -> DecompilerAndIpynbTab(decompilerReport, ipynbReport, onAnalyzeBinary, onRunIpynb)
                3 -> LogoAndSignatureTab(decodedLogoResult, signatureBundle, signatureVerified, onDecodeLogo, onSaveLogoPng, onSignPayload)
                else -> PromptAndPersonaTab(promptTemplates, activePersonaId, onSelectPersona, onUsePromptTemplate, onCreateTemplate)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExportAndBatchTab(
    exportJobs: List<ExportJobEntity>,
    batchExportResult: BatchExportResult?,
    onExportSingle: (ExportFormatSpec) -> Unit,
    onExportBatch: (List<ExportFormatSpec>) -> Unit,
    onSaveBatchZip: (android.net.Uri, ByteArray, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<ExportCategory?>(null) }
    var batchSelectedIds by remember { mutableStateOf(setOf("pdf", "html", "docx", "pptx", "md")) }

    val saveBatchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val res = batchExportResult
        if (uri != null && res != null) {
            onSaveBatchZip(uri, res.zipBytes, res.bundleFileName)
        }
    }

    val filtered = remember(query, selectedCat) {
        DocumentExportEngine.supportedFormats.filter { fmt ->
            (selectedCat == null || fmt.category == selectedCat) &&
                (query.isBlank() || fmt.displayName.contains(query, ignoreCase = true) || fmt.extension.contains(query, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BatchExportEngine (${batchSelectedIds.size} định dạng đã chọn)", style = MaterialTheme.typography.titleSmall, color = CyanBright)
                Button(
                    onClick = {
                        val chosen = DocumentExportEngine.supportedFormats.filter { it.id in batchSelectedIds }
                        onExportBatch(chosen)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_batch_export_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Xuất hàng loạt thành 1 gói .ZIP")
                }

                if (batchExportResult != null) {
                    Text(
                        "Sẵn sàng: ${batchExportResult.bundleFileName} (${batchExportResult.zipBytes.size} bytes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldOk
                    )
                    OutlinedButton(onClick = { saveBatchLauncher.launch(batchExportResult.bundleFileName) }) {
                        Text("Lưu gói Batch ZIP")
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm trong ${DocumentExportEngine.supportedFormats.size} định dạng (pdf, docx, pptx, ipynb, png...)") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = selectedCat == null, onClick = { selectedCat = null }, label = { Text("Tất cả (${DocumentExportEngine.supportedFormats.size})") })
            ExportCategory.entries.forEach { cat ->
                FilterChip(selected = selectedCat == cat, onClick = { selectedCat = cat }, label = { Text(cat.label) })
            }
        }

        filtered.forEach { fmt ->
            val checked = fmt.id in batchSelectedIds
            Surface(
                color = SlateElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            batchSelectedIds = if (isChecked) batchSelectedIds + fmt.id else batchSelectedIds - fmt.id
                        }
                    )
                    FormatEmblemCanvas()
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fmt.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${fmt.category.label} • .${fmt.extension}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    OutlinedButton(onClick = { onExportSingle(fmt) }) {
                        Text("Xuất")
                    }
                }
            }
        }

        if (exportJobs.isNotEmpty()) {
            HorizontalDivider(color = SlateBorder)
            Text("Lịch sử Hàng đợi Xuất gần đây", style = MaterialTheme.typography.titleSmall)
            exportJobs.take(6).forEach { job ->
                Text("• ${job.title} (${job.sizeBytes}B) • SHA: ${job.sha256Hash.take(12)}...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ApkExeAndBuildQueueTab(
    buildQueueJobs: List<BuildQueueJobEntity>,
    toolchains: List<ToolchainDescriptor>,
    onEnqueueBuild: (String, String, String) -> Unit,
    onUpdateToolchains: () -> Unit
) {
    var projectName by remember { mutableStateOf("SuperToolProject") }
    var targetKind by remember { mutableStateOf("APK") }
    var sourceCode by remember {
        mutableStateOf(
            "package com.nguyen_super_app.generated\n\nimport androidx.compose.material3.Text\nimport androidx.compose.runtime.Composable\n\n@Composable\nfun AppEntry() { Text(\"Built on Android\") }"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Trình biên dịch Dự án APK / EXE (WorkManager Bền vững)", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetKind == "APK",
                        onClick = { targetKind = "APK" },
                        label = { Text("Android APK Project") }
                    )
                    FilterChip(
                        selected = targetKind == "EXE",
                        onClick = {
                            targetKind = "EXE"
                            sourceCode = "#include <iostream>\nint main() { std::cout << \"Hello Win64\"; return 0; }"
                        },
                        label = { Text("Windows EXE Bundle") }
                    )
                }
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Tên dự án") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sourceCode,
                    onValueChange = { sourceCode = it },
                    label = { Text("Mã nguồn đầu vào") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
                Button(
                    onClick = { onEnqueueBuild(targetKind, projectName, sourceCode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enqueue_build_button")
                ) {
                    Icon(Icons.Rounded.Build, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Đưa vào Hàng đợi Build ($targetKind)")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ToolchainAutoUpdater", style = MaterialTheme.typography.titleSmall, color = CyanBright)
                    OutlinedButton(onClick = onUpdateToolchains) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kiểm tra & Cập nhật")
                    }
                }
                toolchains.forEach { tc ->
                    Text(
                        "• ${tc.name}: ${tc.currentVersion} -> ${tc.latestVersion} (${tc.lastCheckedNote})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tc.isUpToDate) EmeraldOk else CyanBright
                    )
                }
            }
        }

        Text("Hàng đợi Build Bền vững (${buildQueueJobs.size} tác vụ)", style = MaterialTheme.typography.titleSmall)
        buildQueueJobs.forEach { job ->
            Surface(
                color = SlateElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${job.projectName} [${job.targetKind}] • ${job.status} (${job.progressPercent}%)", style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(progress = { job.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text(job.diagnosticLog, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DecompilerAndIpynbTab(
    decompilerReport: BinaryDecompileReport?,
    ipynbReport: IpynbExecutionReport?,
    onAnalyzeBinary: (String, ByteArray) -> Unit,
    onRunIpynb: (String, String) -> Unit
) {
    val context = LocalContext.current
    val binaryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
            onAnalyzeBinary("selected_binary_${System.currentTimeMillis()}.bin", bytes)
        }
    }
    var ipynbText by remember {
        mutableStateOf(
            """
            {
              "nbformat": 4,
              "metadata": {"kernelspec": {"language": "python", "name": "python3"}},
              "cells": [
                {"cell_type": "markdown", "source": ["# Thử nghiệm Jupyter Notebook Ngoại tuyến"]},
                {"cell_type": "code", "source": ["x = 12\n", "y = 8\n", "print(x * y + 4)"]}
              ]
            }
            """.trimIndent()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BinaryDecompilerEngine (.dex / .so / .exe / .apk / .wasm)", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { binaryPicker.launch(arrayOf("*/*")) }) {
                        Text("Chọn tệp nhị phân")
                    }
                    OutlinedButton(onClick = {
                        val sampleDex = byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x39, 0x00) + "MainActivity_SuperSmartAI".toByteArray()
                        onAnalyzeBinary("classes.dex", sampleDex)
                    }) {
                        Text("Phân tích mẫu DEX")
                    }
                }
                if (decompilerReport != null) {
                    Text("${decompilerReport.detectedFormat} • ${decompilerReport.architectureHint}", style = MaterialTheme.typography.labelMedium, color = EmeraldOk)
                    Text(
                        decompilerReport.pseudoDisassembly,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jupyter Notebook (.ipynb) Offline Runner", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                OutlinedTextField(
                    value = ipynbText,
                    onValueChange = { ipynbText = it },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
                Button(
                    onClick = { onRunIpynb(ipynbText, "sample.ipynb") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Chạy tất cả Cell .ipynb")
                }
                if (ipynbReport != null) {
                    Text("Đã chạy ${ipynbReport.codeCellsExecuted}/${ipynbReport.totalCells} cells:", style = MaterialTheme.typography.labelMedium, color = EmeraldOk)
                    ipynbReport.cellResults.forEach { cell ->
                        Text("[Cell ${cell.cellIndex} • ${cell.cellType}] => ${cell.output}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogoAndSignatureTab(
    decodedLogoResult: DecodedLogoResult?,
    signatureBundle: SignatureBundle?,
    signatureVerified: Boolean?,
    onDecodeLogo: (String, ByteArray, String) -> Unit,
    onSaveLogoPng: (android.net.Uri, ByteArray, String) -> Unit,
    onSignPayload: (String, SignatureAlgorithm, String) -> Unit
) {
    var selectedLogoFmt by remember { mutableStateOf(LogoDecoderEngine.supportedLogoFormats.first()) }
    var signText by remember { mutableStateOf("Super Smart AI Verified Document Payload") }
    var passphrase by remember { mutableStateOf("SecretPassphrase2026") }
    var selectedAlg by remember { mutableStateOf(SignatureAlgorithm.SHA_256) }

    val saveLogoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val res = decodedLogoResult
        if (uri != null && res != null) onSaveLogoPng(uri, res.pngBytes, res.outputFileName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LogoDecoderEngine (${LogoDecoderEngine.supportedLogoFormats.size} định dạng -> PNG)", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LogoDecoderEngine.supportedLogoFormats.take(20).forEach { spec ->
                        FilterChip(
                            selected = selectedLogoFmt.id == spec.id,
                            onClick = { selectedLogoFmt = spec },
                            label = { Text(spec.id.uppercase()) }
                        )
                    }
                }
                Button(
                    onClick = {
                        onDecodeLogo("brand_logo.${selectedLogoFmt.id}", "<svg/>".toByteArray(), selectedLogoFmt.id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Giải mã & Xuất PNG 512px (${selectedLogoFmt.displayName})")
                }
                if (decodedLogoResult != null) {
                    Text(decodedLogoResult.summary, style = MaterialTheme.typography.bodySmall, color = EmeraldOk)
                    val bmp = remember(decodedLogoResult.pngBytes) {
                        BitmapFactory.decodeByteArray(decodedLogoResult.pngBytes, 0, decodedLogoResult.pngBytes.size)
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Logo PNG Preview",
                            modifier = Modifier.size(128.dp)
                        )
                    }
                    OutlinedButton(onClick = { saveLogoLauncher.launch(decodedLogoResult.outputFileName) }) {
                        Text("Lưu Logo PNG")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DigitalSignatureEngine (${SignatureAlgorithm.entries.size} thuật toán + Timestamp)", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                OutlinedTextField(
                    value = signText,
                    onValueChange = { signText = it },
                    label = { Text("Dữ liệu cần ký & đóng dấu thời gian") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Khóa bí mật / Passphrase") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SignatureAlgorithm.entries.forEach { alg ->
                        FilterChip(
                            selected = selectedAlg == alg,
                            onClick = { selectedAlg = alg },
                            label = { Text(alg.id) }
                        )
                    }
                }
                Button(
                    onClick = { onSignPayload(signText, selectedAlg, passphrase) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ký số & Đóng dấu Timestamp (${selectedAlg.id})")
                }
                if (signatureBundle != null) {
                    Text(
                        "Xác thực Timestamp: ${if (signatureVerified == true) "HỢP LỆ" else "KHÔNG KHỚP"}\nDigest/Sig: ${signatureBundle.digestOrSignatureHex.take(48)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (signatureVerified == true) EmeraldOk else CyanBright
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptAndPersonaTab(
    templates: List<PromptTemplateEntity>,
    activePersonaId: String,
    onSelectPersona: (String) -> Unit,
    onUsePromptTemplate: (String) -> Unit,
    onCreateTemplate: (String, String, String, String, String?) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Lập trình") }
    var newContent by remember { mutableStateOf("") }
    var newKind by remember { mutableStateOf("PROMPT") }
    var filterCat by remember { mutableStateOf("Tất cả") }
    val categories = listOf("Tất cả", "Lập trình", "Giải toán", "Viết văn", "Dịch thuật", "DevOps")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(selected = filterCat == cat, onClick = { filterCat = cat }, label = { Text(cat) })
            }
        }

        templates.filter { filterCat == "Tất cả" || it.category == filterCat }.forEach { tmpl ->
            val isPersonaActive = tmpl.kind == "PERSONA" && tmpl.id == activePersonaId
            Surface(
                color = if (isPersonaActive) CyanContainer else SlateElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isPersonaActive) CyanPrimary else SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${tmpl.kind}: ${tmpl.title}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (isPersonaActive) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Đang dùng", tint = EmeraldOk)
                        }
                    }
                    Text(tmpl.content, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = {
                        if (tmpl.kind == "PERSONA") onSelectPersona(tmpl.id) else onUsePromptTemplate(tmpl.content)
                    }) {
                        Text(if (tmpl.kind == "PERSONA") "Kích hoạt Persona" else "Chèn vào Chat")
                    }
                }
            }
        }

        HorizontalDivider(color = SlateBorder)
        Text("Tạo Mẫu Prompt / Persona mới", style = MaterialTheme.typography.titleSmall, color = CyanBright)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = newKind == "PROMPT", onClick = { newKind = "PROMPT" }, label = { Text("Prompt") })
            FilterChip(selected = newKind == "PERSONA", onClick = { newKind = "PERSONA" }, label = { Text("Persona") })
        }
        OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Tiêu đề") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("Danh mục") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newContent, onValueChange = { newContent = it }, label = { Text("Nội dung chỉ thị") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                onCreateTemplate(newTitle, newCategory, newContent, newKind, null)
                newTitle = ""
                newContent = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Lưu mẫu")
        }
    }
}

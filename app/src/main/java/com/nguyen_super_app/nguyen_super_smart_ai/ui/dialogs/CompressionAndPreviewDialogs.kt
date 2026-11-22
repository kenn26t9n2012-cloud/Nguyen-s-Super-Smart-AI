package com.nguyen_super_app.nguyen_super_smart_ai.ui.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AdvancedAiFeaturesConfig
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.CompressionFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.MultiFormatCompressionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.GeneratedExportPackage
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.AmberWarn
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

/**
 * Advanced AI Features Options Dialog ("Hộp thoại Tùy chọn Tính năng AI nâng cao").
 * Controls explicit user opt-in for Web Search, Single/Batch File Creation,
 * Prompt Code Language Hook, and Reasoning <think> tagging.
 */
@Composable
fun AdvancedAiOptionsDialog(
    config: AdvancedAiFeaturesConfig,
    onUpdate: (AdvancedAiFeaturesConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CyanPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Tùy chọn Tính năng AI nâng cao", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Các tính năng nâng cao chỉ kích hoạt khi công tắc được BẬT VÀ câu hỏi có yêu cầu rõ ràng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OptionToggleCard(
                    title = "Tìm kiếm Web / Google Search",
                    subtitle = "Tra cứu dữ liệu trực tuyến khi bạn yêu cầu tìm kiếm web",
                    icon = Icons.Rounded.Public,
                    checked = config.enableWebSearch,
                    tag = "adv_toggle_web_search",
                    onCheckedChange = { onUpdate(config.copy(enableWebSearch = it)) }
                )

                OptionToggleCard(
                    title = "Tạo file đơn lẻ & hàng loạt",
                    subtitle = "Xuất file hoặc gói đa định dạng tự động khi có yêu cầu trong Chat",
                    icon = Icons.Rounded.Description,
                    checked = config.enableSingleBatchFileGen,
                    tag = "adv_toggle_batch_file_gen",
                    onCheckedChange = { onUpdate(config.copy(enableSingleBatchFileGen = it)) }
                )

                OptionToggleCard(
                    title = "Gắn ngữ cảnh ngôn ngữ lập trình",
                    subtitle = "Ưu tiên fenced code block theo ngôn ngữ của Prompt / Persona",
                    icon = Icons.Rounded.Code,
                    checked = config.enableCodeLanguageHook,
                    tag = "adv_toggle_code_hook",
                    onCheckedChange = { onUpdate(config.copy(enableCodeLanguageHook = it)) }
                )

                OptionToggleCard(
                    title = "Tách luồng suy luận <think>",
                    subtitle = "Cho phép mô hình đặt tư duy vào thẻ <think> (luôn thu gọn cho tới khi bạn bấm mở)",
                    icon = Icons.Rounded.Psychology,
                    checked = config.enableReasoningOutputTagging,
                    tag = "adv_toggle_reasoning_tag",
                    onCheckedChange = { onUpdate(config.copy(enableReasoningOutputTagging = it)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("adv_options_done_button")
            ) {
                Text("Xong")
            }
        }
    )
}

@Composable
private fun OptionToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (checked) CyanContainer.copy(alpha = 0.45f) else SlateElevated,
        border = BorderStroke(1.dp, if (checked) CyanPrimary else SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (checked) CyanBright else TextSecondary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

/**
 * Compression Picker supporting 15+ formats and defaulting strictly to NONE (Không nén).
 */
@Composable
fun UnifiedCompressionPickerDialog(
    fileName: String,
    originalSizeBytes: Long,
    onConfirm: (CompressionFormat, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(CompressionFormat.NONE) }
    var saveToPrivateFolder by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Compress, contentDescription = null, tint = CyanPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Nén & Đóng gói tệp", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tệp: $fileName (${originalSizeBytes} bytes)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                val estBytes = MultiFormatCompressionEngine.estimateCompressedSize(originalSizeBytes, selected)
                Text(
                    "Ước tính sau xử lý: $estBytes bytes • RAM: ${selected.estimatedRamMb} MB",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanBright
                )
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CompressionFormat.entries, key = { it.id }) { fmt ->
                        val isSelected = fmt == selected
                        Surface(
                            onClick = { selected = fmt },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyanContainer else SlateElevated,
                            border = BorderStroke(1.dp, if (isSelected) CyanPrimary else SlateBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compression_option_${fmt.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fmt.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(fmt.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                if (isSelected) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Đã chọn", tint = CyanBright)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveToPrivateFolder,
                        onCheckedChange = { saveToPrivateFolder = it },
                        modifier = Modifier.testTag("compression_save_vault_checkbox")
                    )
                    Text("Lưu vào Thư mục Riêng (Vault)", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected, saveToPrivateFolder) },
                modifier = Modifier.testTag("compression_confirm_button")
            ) {
                Text("Áp dụng (${selected.id})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/**
 * Visual File Preview, Live Text Editor, Document Split Slider, AES PDF Password Lock,
 * SHA-256 Verifier, and Android SAF CreateDocument Export Saver.
 */
@Composable
fun VisualFileEditorDialog(
    exportPackage: GeneratedExportPackage,
    onRegenerate: (maxMessagesPerPart: Int, pdfPassword: String?) -> Unit,
    onSavePartToUri: (android.net.Uri, ByteArray, String) -> Unit,
    onDismiss: () -> Unit
) {
    val firstPart = exportPackage.parts.first()
    var editedTitle by remember(exportPackage) { mutableStateOf(firstPart.fileName) }
    val initialText = remember(exportPackage) {
        if (exportPackage.primaryFormat.extension in setOf("txt", "md", "html", "json", "csv", "xml", "yaml", "py", "kt", "sql", "ipynb", "tex", "svg")) {
            String(firstPart.bytes, Charsets.UTF_8)
        } else {
            "[Tệp nhị phân ${exportPackage.primaryFormat.displayName} • ${firstPart.bytes.size} bytes]\nSHA-256: ${firstPart.sha256Hash}"
        }
    }
    var editableBody by remember(exportPackage) { mutableStateOf(initialText) }
    var splitSize by remember { mutableFloatStateOf(50f) }
    var pdfPassword by remember { mutableStateOf("") }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(exportPackage.primaryFormat.mimeType)
    ) { uri ->
        if (uri != null) {
            val bytesToSave = if (exportPackage.primaryFormat.extension in setOf("txt", "md", "html", "json", "csv", "xml", "yaml", "py", "kt", "sql", "ipynb", "tex", "svg")) {
                editableBody.toByteArray(Charsets.UTF_8)
            } else {
                firstPart.bytes
            }
            onSavePartToUri(uri, bytesToSave, editedTitle)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text("Xem trước & Chỉnh sửa: ${exportPackage.primaryFormat.displayName}", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Tên tệp xuất") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = EmeraldOk)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "SHA-256: ${firstPart.sha256Hash.take(24)}... (${exportPackage.parts.size} phần)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldOk
                    )
                }

                Text("Tách tệp lớn (${splitSize.toInt()} tin nhắn/phần):", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = splitSize,
                    onValueChange = { splitSize = it },
                    valueRange = 5f..100f
                )

                if (exportPackage.primaryFormat.id == "pdf") {
                    OutlinedTextField(
                        value = pdfPassword,
                        onValueChange = { pdfPassword = it },
                        label = { Text("Mật khẩu bảo vệ PDF (AES-256)") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = AmberWarn) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedButton(
                    onClick = { onRegenerate(splitSize.toInt(), pdfPassword.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tái tạo với cấu hình Tách/Mật khẩu")
                }

                HorizontalDivider(color = SlateBorder)

                Surface(
                    color = SlateDeep,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editableBody,
                        onValueChange = { editableBody = it },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 210.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { safLauncher.launch(editedTitle) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateDeep),
                modifier = Modifier.testTag("export_save_saf_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Lưu tệp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

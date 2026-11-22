package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.MediaProcessingEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.PhotoEditParams
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.PhotoFilterPreset
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateSurface
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.viewmodel.PhotoEditorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    state: PhotoEditorState,
    onPickPhotoUri: (android.net.Uri) -> Unit,
    onCreateSamplePhoto: () -> Unit,
    onUpdateParams: (PhotoEditParams) -> Unit,
    onSendToChat: () -> Unit,
    onSavePngToUri: (android.net.Uri, ByteArray, String) -> Unit,
    onBack: () -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickPhotoUri(uri)
    }

    val saveSaf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bmp = state.editedBitmap
        if (uri != null && bmp != null) {
            val bytes = MediaProcessingEngine.encodeBitmap(bmp, "PNG")
            onSavePngToUri(uri, bytes, "edited_${System.currentTimeMillis()}.png")
        }
    }

    val params = state.params

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("photo_editor_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Trình sửa ảnh & Media", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Mở ảnh", tint = CyanBright)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateCard,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val bmp = state.editedBitmap
                    if (bmp != null) {
                        val imgBmp = remember(bmp) { bmp.asImageBitmap() }
                        Image(
                            bitmap = imgBmp,
                            contentDescription = "Ảnh đang chỉnh sửa",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(state.statusNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) {
                                    Text("Chọn ảnh (Photo Picker)")
                                }
                                OutlinedButton(onClick = onCreateSamplePhoto) {
                                    Text("Tạo ảnh mẫu")
                                }
                            }
                        }
                    }
                }
            }

            if (state.editedBitmap != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onUpdateParams(params.copy(rotationDegrees = (params.rotationDegrees + 90) % 360)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.RotateRight, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Xoay 90°")
                            }
                            OutlinedButton(
                                onClick = { onUpdateParams(params.copy(flipHorizontal = !params.flipHorizontal)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Flip, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Lật ngang")
                            }
                        }

                        Text("Độ sáng: ${params.brightness.toInt()}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = params.brightness,
                            onValueChange = { onUpdateParams(params.copy(brightness = it)) },
                            valueRange = -80f..80f
                        )

                        Text("Độ tương phản: ${"%.2f".format(params.contrast)}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = params.contrast,
                            onValueChange = { onUpdateParams(params.copy(contrast = it)) },
                            valueRange = 0.5f..1.8f
                        )

                        Text("Độ bão hòa màu: ${"%.2f".format(params.saturation)}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = params.saturation,
                            onValueChange = { onUpdateParams(params.copy(saturation = it)) },
                            valueRange = 0f..2.0f
                        )

                        Text("Bộ lọc màu:", style = MaterialTheme.typography.labelMedium, color = CyanBright)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PhotoFilterPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = params.filter == preset,
                                    onClick = { onUpdateParams(params.copy(filter = preset)) },
                                    label = { Text(preset.label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = params.watermarkText,
                            onValueChange = { onUpdateParams(params.copy(watermarkText = it)) },
                            label = { Text("Watermark văn bản trên ảnh") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onSendToChat, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Gửi vào Chat")
                            }
                            OutlinedButton(
                                onClick = { saveSaf.launch("super_smart_edited_${System.currentTimeMillis()}.png") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Lưu PNG")
                            }
                        }
                    }
                }
            }
        }
    }
}

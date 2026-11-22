package com.nguyen_super_app.nguyen_super_smart_ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.WorldLanguage
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.WorldLanguagesCatalog
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanContainer
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary

/**
 * Reusable LanguageSelector component providing a searchable picker for 100+ languages
 * with Tiếng Việt (vi) prioritized separately at the top of the list, designed for
 * Speech-to-Text (Vosk/Whisper) and UI localization configurations.
 */
@Composable
fun LanguageSelector(
    selectedCode: String,
    onSelect: (WorldLanguage) -> Unit,
    modifier: Modifier = Modifier,
    isSpeechMode: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    val vietnamese = remember { WorldLanguagesCatalog.languages.first { it.code == "vi" } }
    val others = remember(query) {
        WorldLanguagesCatalog.languages
            .filter { it.code != "vi" }
            .filter {
                query.isBlank() ||
                    it.nativeName.contains(query, ignoreCase = true) ||
                    it.englishName.contains(query, ignoreCase = true) ||
                    it.vietnameseName.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm trong ${WorldLanguagesCatalog.languages.size} ngôn ngữ (vi, en, ja, zh, ko...)") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Tìm ngôn ngữ") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("language_selector_search_input")
        )
        Spacer(Modifier.height(10.dp))

        // Tiếng Việt pinned separately at top regardless of search unless hidden by filter? Keep always pinned at top!
        Surface(
            onClick = { onSelect(vietnamese) },
            shape = RoundedCornerShape(14.dp),
            color = if (selectedCode.equals("vi", ignoreCase = true)) CyanContainer else SlateElevated,
            border = BorderStroke(1.5.dp, CyanPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("language_selector_vi_featured")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSpeechMode) Icons.Rounded.Mic else Icons.Rounded.Star,
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tiếng Việt (Ưu tiên hàng đầu)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanBright
                    )
                    Text(
                        text = "Code: vi • ${if (isSpeechMode) "Tối ưu Vosk-VN / Whisper-Multi" else "Ngôn ngữ hiển thị mặc định"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (selectedCode.equals("vi", ignoreCase = true)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Đang chọn Tiếng Việt", tint = EmeraldOk)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ngôn ngữ toàn cầu (${others.size} / ${WorldLanguagesCatalog.languages.size - 1} khác)",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(others, key = { it.code }) { lang ->
                val isSel = selectedCode.equals(lang.code, ignoreCase = true)
                Surface(
                    onClick = { onSelect(lang) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) CyanContainer else SlateElevated,
                    border = BorderStroke(1.dp, if (isSel) CyanPrimary else SlateBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_selector_item_${lang.code}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${lang.nativeName} (${lang.englishName})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${lang.vietnameseName} • ${lang.code}${if (lang.isRtl) " • RTL" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (isSel) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Đã chọn", tint = CyanBright)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectorDialog(
    selectedCode: String,
    title: String = "Chọn Ngôn ngữ (100+ Thế giới)",
    isSpeechMode: Boolean = true,
    onSelect: (WorldLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSpeechMode) Icons.Rounded.Mic else Icons.Rounded.Language,
                    contentDescription = null,
                    tint = CyanPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            LanguageSelector(
                selectedCode = selectedCode,
                isSpeechMode = isSpeechMode,
                onSelect = { lang ->
                    onSelect(lang)
                    onDismiss()
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

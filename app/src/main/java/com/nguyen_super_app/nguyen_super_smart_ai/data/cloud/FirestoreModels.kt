package com.nguyen_super_app.nguyen_super_smart_ai.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserAccountProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val folderKey: String = "",
    val recoveryEmail: String? = null,
    val recoveryPhone: String? = null,
    val linkedProviders: List<String> = emptyList(),
    val preferredLanguageCode: String = "vi",
    val biometricEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateFolderItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val folderKey: String = "",
    val fileName: String = "",
    val mimeType: String = "application/octet-stream",
    val compressionMethod: String = "NONE",
    val originalSizeBytes: Long = 0L,
    val compressedSizeBytes: Long = 0L,
    val sha256Hash: String = "",
    val chunkCount: Long = 0L,
    val category: String = "DOCUMENT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateFolderChunk(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val chunkIndex: Long = 0L,
    val payloadBase64: String = "",
    val chunkSha256: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudPromptPersona(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = "PROMPT",
    val title: String = "",
    val category: String = "",
    val content: String = "",
    val preferredLanguageTag: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudChatBackup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val modelLabel: String = "",
    val messageCount: Long = 0L,
    val paqSummaryBase64: String = "",
    val sha256Hash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudSecurityLock(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetType: String = "APP",
    val targetId: String = "APP",
    val passwordMode: String = "DIGITS",
    val patternGridSize: Long = 0L,
    val saltBase64: String = "",
    val hashBase64: String = "",
    val unlimitedAttempts: Boolean = true,
    val maxAttempts: Long = 0L,
    val failedAttempts: Long = 0L,
    val lockedOut: Boolean = false,
    val autoLockTimeoutMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

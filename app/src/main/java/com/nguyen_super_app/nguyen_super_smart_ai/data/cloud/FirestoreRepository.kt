package com.nguyen_super_app.nguyen_super_smart_ai.data.cloud

import android.content.Context
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.nguyen_super_app.nguyen_super_smart_ai.R
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.CompressionFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.CompressionResult
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.MultiFormatCompressionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqCompressionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.firebase.OperationType
import com.nguyen_super_app.nguyen_super_smart_ai.firebase.handleFirestoreError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Người dùng chưa đăng nhập Cloud Firebase.")

    fun buildFolderKey(displayName: String, uid: String): String {
        val cleanName = displayName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "User" }.take(32)
        return "${cleanName}_${uid}"
    }

    fun observeProfile(userId: String): Flow<UserAccountProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap -> snap.toObject(UserAccountProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(preferredLanguageCode: String = "vi"): Result<UserAccountProfile> {
        var docPath = "users/unknown"
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("Not authenticated")
            val uid = user.uid
            docPath = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserAccountProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) return Result.success(existing)
            }
            val name = user.displayName?.take(60)?.ifBlank { "SuperUser" } ?: "SuperUser"
            val folderKey = buildFolderKey(name, uid)
            val providers = user.providerData.map { it.providerId }.filter { it != "firebase" }.distinct().take(15)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to name,
                "folderKey" to folderKey,
                "linkedProviders" to providers,
                "preferredLanguageCode" to preferredLanguageCode.take(12),
                "biometricEnabled" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val refreshed = docRef.get().await().toObject(UserAccountProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(refreshed ?: UserAccountProfile(id = uid, userId = uid, displayName = name, folderKey = folderKey, linkedProviders = providers, preferredLanguageCode = preferredLanguageCode))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateAccountProfile(
        displayName: String,
        recoveryEmail: String?,
        recoveryPhone: String?,
        preferredLanguageCode: String,
        biometricEnabled: Boolean,
        linkedProviders: List<String>
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid"
            val cleanName = displayName.trim().take(60).ifBlank { "SuperUser" }
            val folderKey = buildFolderKey(cleanName, uid)
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "folderKey" to folderKey,
                "preferredLanguageCode" to preferredLanguageCode.take(12),
                "biometricEnabled" to biometricEnabled,
                "linkedProviders" to linkedProviders.take(15),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!recoveryEmail.isNullOrBlank()) updates["recoveryEmail"] = recoveryEmail.trim().take(120)
            if (!recoveryPhone.isNullOrBlank()) updates["recoveryPhone"] = recoveryPhone.trim().take(30)

            db.collection("users").document(uid).update(updates).await()
            auth.currentUser?.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()
            )?.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observePrivateFolders(userId: String): Flow<List<PrivateFolderItem>> = flow {
        val path = "users/$userId/private_folders"
        emitAll(
            db.collection("users").document(userId).collection("private_folders")
                .snapshots()
                .map { snap -> snap.toObjects(PrivateFolderItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun uploadPrivateFolderItem(
        folderKey: String,
        mimeType: String,
        category: String,
        compressionResult: CompressionResult
    ): Result<String> {
        var docPath = "users/unknown/private_folders"
        return try {
            val uid = requireUserId()
            val itemRef = db.collection("users").document(uid).collection("private_folders").document()
            val itemId = itemRef.id
            docPath = itemRef.path

            val metadataPayload = mapOf(
                "userId" to uid,
                "folderKey" to folderKey.take(120),
                "fileName" to compressionResult.outputFileName.take(200),
                "mimeType" to mimeType.take(100),
                "compressionMethod" to compressionResult.format.id,
                "originalSizeBytes" to compressionResult.originalSizeBytes,
                "compressedSizeBytes" to compressionResult.compressedSizeBytes,
                "sha256Hash" to compressionResult.sha256Hash,
                "chunkCount" to compressionResult.chunks.size.toLong(),
                "category" to category.take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            itemRef.set(metadataPayload).await()

            for (chunk in compressionResult.chunks) {
                val b64 = Base64.encodeToString(chunk.bytes, Base64.NO_WRAP)
                val chunkPayload = mapOf(
                    "userId" to uid,
                    "itemId" to itemId,
                    "chunkIndex" to chunk.index.toLong(),
                    "payloadBase64" to b64,
                    "chunkSha256" to chunk.sha256,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                itemRef.collection("chunks").document("chunk_${chunk.index}").set(chunkPayload).await()
            }
            Result.success(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun downloadPrivateFolderItemOnDemand(item: PrivateFolderItem, context: Context? = null): Result<ByteArray> {
        val path = "users/${auth.currentUser?.uid ?: "unauth"}/private_folders/${item.id}/chunks"
        return try {
            val uid = requireUserId()
            val snapshot = db.collection("users").document(uid)
                .collection("private_folders").document(item.id)
                .collection("chunks")
                .get()
                .await()
            val chunks = snapshot.toObjects(PrivateFolderChunk::class.java)
                .sortedBy { it.chunkIndex }
            val assembled = ByteArrayOutputStream()
            for (c in chunks) {
                val decoded = Base64.decode(c.payloadBase64, Base64.DEFAULT)
                assembled.write(decoded)
            }
            val rawBytes = assembled.toByteArray()
            val format = CompressionFormat.entries.firstOrNull { it.id == item.compressionMethod } ?: CompressionFormat.NONE
            val output = MultiFormatCompressionEngine.decompress(rawBytes, format, context)
            Result.success(output)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun deletePrivateFolderItem(itemId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauth"
        val path = "users/$uid/private_folders/$itemId"
        return try {
            val authedUid = requireUserId()
            val itemRef = db.collection("users").document(authedUid).collection("private_folders").document(itemId)
            val chunkDocs = itemRef.collection("chunks").get().await()
            for (c in chunkDocs.documents) {
                c.reference.delete().await()
            }
            itemRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observePromptPersonas(userId: String): Flow<List<CloudPromptPersona>> = flow {
        val path = "users/$userId/prompts_personas"
        emitAll(
            db.collection("users").document(userId).collection("prompts_personas")
                .snapshots()
                .map { snap -> snap.toObjects(CloudPromptPersona::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun syncPromptPersona(
        docId: String,
        kind: String,
        title: String,
        category: String,
        content: String,
        preferredLanguageTag: String?
    ): Result<Unit> {
        val cleanId = docId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "tmpl_${System.currentTimeMillis()}" }
        var docPath = "users/unknown/prompts_personas/$cleanId"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid/prompts_personas/$cleanId"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "kind" to if (kind == "PERSONA") "PERSONA" else "PROMPT",
                "title" to title.trim().take(100),
                "category" to category.trim().take(50),
                "content" to content.trim().take(4000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!preferredLanguageTag.isNullOrBlank()) {
                payload["preferredLanguageTag"] = preferredLanguageTag.trim().take(30)
            }
            val docRef = db.collection("users").document(uid)
                .collection("prompts_personas").document(cleanId)
            if (docRef.get().await().exists()) {
                val updates = mutableMapOf<String, Any>(
                    "kind" to if (kind == "PERSONA") "PERSONA" else "PROMPT",
                    "title" to title.trim().take(100),
                    "category" to category.trim().take(50),
                    "content" to content.trim().take(4000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!preferredLanguageTag.isNullOrBlank()) {
                    updates["preferredLanguageTag"] = preferredLanguageTag.trim().take(30)
                }
                docRef.update(updates).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun backupChatSession(
        sessionId: String,
        title: String,
        modelLabel: String,
        messageCount: Int,
        transcriptText: String
    ): Result<Unit> {
        val cleanId = sessionId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        var docPath = "users/unknown/chat_backups/$cleanId"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid/chat_backups/$cleanId"
            val paqBytes = PaqCompressionEngine.compress(transcriptText.toByteArray(Charsets.UTF_8))
            val b64 = Base64.encodeToString(paqBytes, Base64.NO_WRAP).take(490_000)
            val sha = PaqCompressionEngine.sha256Hex(paqBytes)
            val cleanTitle = title.trim().take(140).ifBlank { "Cuộc trò chuyện" }
            val cleanModel = modelLabel.trim().take(80).ifBlank { "Gemini" }
            val docRef = db.collection("users").document(uid).collection("chat_backups").document(cleanId)
            if (docRef.get().await().exists()) {
                val updates = mapOf(
                    "title" to cleanTitle,
                    "modelLabel" to cleanModel,
                    "messageCount" to messageCount.toLong(),
                    "paqSummaryBase64" to b64,
                    "sha256Hash" to sha,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "modelLabel" to cleanModel,
                    "messageCount" to messageCount.toLong(),
                    "paqSummaryBase64" to b64,
                    "sha256Hash" to sha,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeSecurityLocks(userId: String): Flow<List<CloudSecurityLock>> = flow {
        val path = "users/$userId/security_locks"
        emitAll(
            db.collection("users").document(userId).collection("security_locks")
                .snapshots()
                .map { snap -> snap.toObjects(CloudSecurityLock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertSecurityLock(
        lockId: String,
        targetType: String,
        targetId: String,
        passwordMode: String,
        patternGridSize: Int,
        saltBase64: String,
        hashBase64: String,
        unlimitedAttempts: Boolean,
        maxAttempts: Long,
        failedAttempts: Long = 0L,
        lockedOut: Boolean = false,
        autoLockTimeoutMillis: Long? = null
    ): Result<Unit> {
        val cleanId = lockId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "app_lock" }
        var docPath = "users/unknown/security_locks/$cleanId"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid/security_locks/$cleanId"
            val docRef = db.collection("users").document(uid).collection("security_locks").document(cleanId)
            val safeGridSize = if (passwordMode == "PATTERN") patternGridSize.coerceIn(3, 10).toLong() else 0L
            val safeMax = if (unlimitedAttempts) 0L else maxAttempts.coerceAtLeast(1L)
            val safeTimeout = autoLockTimeoutMillis?.coerceIn(-1L, 86_400_000L)
            if (docRef.get().await().exists()) {
                val updates = mutableMapOf<String, Any>(
                    "passwordMode" to passwordMode,
                    "patternGridSize" to safeGridSize,
                    "saltBase64" to saltBase64.take(128),
                    "hashBase64" to hashBase64.take(256),
                    "unlimitedAttempts" to unlimitedAttempts,
                    "maxAttempts" to safeMax,
                    "failedAttempts" to failedAttempts.coerceAtLeast(0L),
                    "lockedOut" to lockedOut,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeTimeout != null) {
                    updates["autoLockTimeoutMillis"] = safeTimeout
                }
                docRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "passwordMode" to passwordMode,
                    "patternGridSize" to safeGridSize,
                    "saltBase64" to saltBase64.take(128),
                    "hashBase64" to hashBase64.take(256),
                    "unlimitedAttempts" to unlimitedAttempts,
                    "maxAttempts" to safeMax,
                    "failedAttempts" to failedAttempts.coerceAtLeast(0L),
                    "lockedOut" to lockedOut,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeTimeout != null) {
                    payload["autoLockTimeoutMillis"] = safeTimeout
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSecurityLockAttemptState(
        lockId: String,
        failedAttempts: Long,
        lockedOut: Boolean
    ): Result<Unit> {
        val cleanId = lockId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        var docPath = "users/unknown/security_locks/$cleanId"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid/security_locks/$cleanId"
            val docRef = db.collection("users").document(uid).collection("security_locks").document(cleanId)
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "failedAttempts" to failedAttempts.coerceAtLeast(0L),
                        "lockedOut" to lockedOut,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSecurityLock(lockId: String): Result<Unit> {
        val cleanId = lockId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        var docPath = "users/unknown/security_locks/$cleanId"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid/security_locks/$cleanId"
            db.collection("users").document(uid).collection("security_locks").document(cleanId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun exportAllCloudAccountData(): Result<String> {
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid).get().await()
            val folders = db.collection("users").document(uid).collection("private_folders").get().await()
            val prompts = db.collection("users").document(uid).collection("prompts_personas").get().await()
            val backups = db.collection("users").document(uid).collection("chat_backups").get().await()
            val locks = db.collection("users").document(uid).collection("security_locks").get().await()

            val json = JSONObject().apply {
                put("uid", uid)
                put("profile", JSONObject(userDoc.data ?: emptyMap<String, Any>()))
                put("privateFolders", JSONArray(folders.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
                put("promptsPersonas", JSONArray(prompts.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
                put("chatBackups", JSONArray(backups.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
                put("securityLocks", JSONArray(locks.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
            }.toString(2)
            Result.success(json)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountAndCloudData(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val folders = userRef.collection("private_folders").get().await()
            for (f in folders.documents) {
                val chunks = f.reference.collection("chunks").get().await()
                chunks.documents.forEach { it.reference.delete().await() }
                f.reference.delete().await()
            }
            userRef.collection("prompts_personas").get().await().documents.forEach { it.reference.delete().await() }
            userRef.collection("chat_backups").get().await().documents.forEach { it.reference.delete().await() }
            userRef.collection("security_locks").get().await().documents.forEach { it.reference.delete().await() }
            userRef.delete().await()
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }
}

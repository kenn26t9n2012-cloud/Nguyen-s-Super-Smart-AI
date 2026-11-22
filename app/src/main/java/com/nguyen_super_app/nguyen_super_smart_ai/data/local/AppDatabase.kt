package com.nguyen_super_app.nguyen_super_smart_ai.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelLabel: String,
    val activePersonaName: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "USER", "AI", "SYSTEM_ERROR"
    val content: String,
    val engineTag: String,
    val attachmentSummary: String? = null,
    val timestampMillis: Long
)

@Entity(tableName = "prompt_templates")
data class PromptTemplateEntity(
    @PrimaryKey val id: String,
    val kind: String, // "PROMPT" or "PERSONA"
    val title: String,
    val category: String,
    val content: String,
    val preferredLanguageTag: String? = null,
    val isPinned: Boolean = false
)

@Entity(tableName = "export_jobs")
data class ExportJobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val formatId: String,
    val extension: String,
    val status: String, // "QUEUED", "READY", "SAVED", "FAILED"
    val sha256Hash: String,
    val sizeBytes: Long,
    val createdAtMillis: Long
)

@Entity(tableName = "local_vault_files")
data class LocalVaultFileEntity(
    @PrimaryKey val id: String,
    val folderKey: String,
    val fileName: String,
    val compressionMethod: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val sha256Hash: String,
    val chunkCount: Int,
    val localFilePath: String,
    val createdAtMillis: Long
)

@Entity(tableName = "local_ai_models")
data class LocalAiModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val formatId: String,
    val fileName: String,
    val localPath: String,
    val sizeBytes: Long,
    val parameterBillions: Float,
    val quantizationId: String,
    val sourceUrl: String,
    val magicSignatureSummary: String,
    val isVisibleInList: Boolean = true,
    val isInTrash: Boolean = false,
    val addedAtMillis: Long
)

@Entity(tableName = "build_queue_jobs")
data class BuildQueueJobEntity(
    @PrimaryKey val id: String,
    val targetKind: String, // "APK", "EXE", "BATCH_EXPORT"
    val projectName: String,
    val status: String, // "QUEUED", "BUILDING", "SUCCEEDED", "FAILED"
    val progressPercent: Int,
    val outputArtifactName: String,
    val sha256Hash: String,
    val diagnosticLog: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "gemini_response_cache")
data class GeminiCacheEntity(
    @PrimaryKey val id: String,
    val promptKey: String,
    val responseContent: String,
    val modelName: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "extracted_text_files")
data class ExtractedTextFileEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val sourceInfo: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface GeminiCacheDao {
    @Query("SELECT * FROM gemini_response_cache ORDER BY createdAtMillis DESC")
    fun observeCache(): Flow<List<GeminiCacheEntity>>

    @Query("SELECT * FROM gemini_response_cache WHERE promptKey = :promptKey LIMIT 1")
    suspend fun getByKey(promptKey: String): GeminiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: GeminiCacheEntity)

    @Query("DELETE FROM gemini_response_cache WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ExtractedTextDao {
    @Query("SELECT * FROM extracted_text_files ORDER BY createdAtMillis DESC")
    fun observeFiles(): Flow<List<ExtractedTextFileEntity>>

    @Query("SELECT * FROM extracted_text_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExtractedTextFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: ExtractedTextFileEntity)

    @Query("DELETE FROM extracted_text_files WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMillis DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis DESC")
    fun observeAllMessages(): Flow<List<ChatMessageEntity>>
}

@Entity(tableName = "security_locks")
data class SecurityLockEntity(
    @PrimaryKey val lockId: String, // "app_lock" or "chat_<sessionId>"
    val targetType: String, // "APP" or "CHAT"
    val targetId: String, // "APP" or sessionId
    val targetLabel: String,
    val passwordMode: String, // "DIGITS", "LETTERS", "ALPHANUMERIC", "PATTERN"
    val patternGridSize: Int = 3, // 3..10 when PATTERN, 0 otherwise
    val saltBase64: String,
    val hashBase64: String,
    val unlimitedAttempts: Boolean = true,
    val maxAttempts: Long = 0L, // > 0 when unlimitedAttempts == false
    val failedAttempts: Long = 0L,
    val lockedOut: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface SecurityLockDao {
    @Query("SELECT * FROM security_locks ORDER BY updatedAtMillis DESC")
    fun observeAllLocks(): Flow<List<SecurityLockEntity>>

    @Query("SELECT * FROM security_locks WHERE lockId = :lockId LIMIT 1")
    suspend fun getLock(lockId: String): SecurityLockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLock(lock: SecurityLockEntity)

    @Query("DELETE FROM security_locks WHERE lockId = :lockId")
    suspend fun deleteLock(lockId: String)
}

@Dao
interface PromptTemplateDao {
    @Query("SELECT * FROM prompt_templates ORDER BY isPinned DESC, title ASC")
    fun observeTemplates(): Flow<List<PromptTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: PromptTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(templates: List<PromptTemplateEntity>)

    @Query("DELETE FROM prompt_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)
}

@Dao
interface ExportJobDao {
    @Query("SELECT * FROM export_jobs ORDER BY createdAtMillis DESC")
    fun observeJobs(): Flow<List<ExportJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: ExportJobEntity)

    @Query("DELETE FROM export_jobs WHERE id = :id")
    suspend fun deleteJob(id: String)
}

@Dao
interface LocalVaultDao {
    @Query("SELECT * FROM local_vault_files ORDER BY createdAtMillis DESC")
    fun observeFiles(): Flow<List<LocalVaultFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFile(file: LocalVaultFileEntity)

    @Query("DELETE FROM local_vault_files WHERE id = :id")
    suspend fun deleteFile(id: String)
}

@Dao
interface LocalAiModelDao {
    @Query("SELECT * FROM local_ai_models ORDER BY addedAtMillis DESC")
    fun observeModels(): Flow<List<LocalAiModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModel(model: LocalAiModelEntity)

    @Query("DELETE FROM local_ai_models WHERE id = :id")
    suspend fun deleteModel(id: String)
}

@Dao
interface BuildQueueDao {
    @Query("SELECT * FROM build_queue_jobs ORDER BY updatedAtMillis DESC")
    fun observeJobs(): Flow<List<BuildQueueJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: BuildQueueJobEntity)

    @Query("DELETE FROM build_queue_jobs WHERE id = :id")
    suspend fun deleteJob(id: String)
}

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        PromptTemplateEntity::class,
        ExportJobEntity::class,
        LocalVaultFileEntity::class,
        LocalAiModelEntity::class,
        BuildQueueJobEntity::class,
        SecurityLockEntity::class,
        GeminiCacheEntity::class,
        ExtractedTextFileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun promptTemplateDao(): PromptTemplateDao
    abstract fun exportJobDao(): ExportJobDao
    abstract fun localVaultDao(): LocalVaultDao
    abstract fun localAiModelDao(): LocalAiModelDao
    abstract fun buildQueueDao(): BuildQueueDao
    abstract fun securityLockDao(): SecurityLockDao
    abstract fun geminiCacheDao(): GeminiCacheDao
    abstract fun extractedTextDao(): ExtractedTextDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nguyen_super_smart_ai.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class LocalRepository(private val db: AppDatabase) {
    val sessions: Flow<List<ChatSessionEntity>> = db.chatDao().observeSessions()
    val templates: Flow<List<PromptTemplateEntity>> = db.promptTemplateDao().observeTemplates()
    val exportJobs: Flow<List<ExportJobEntity>> = db.exportJobDao().observeJobs()
    val localVaultFiles: Flow<List<LocalVaultFileEntity>> = db.localVaultDao().observeFiles()
    val localAiModels: Flow<List<LocalAiModelEntity>> = db.localAiModelDao().observeModels()
    val buildQueueJobs: Flow<List<BuildQueueJobEntity>> = db.buildQueueDao().observeJobs()
    val securityLocks: Flow<List<SecurityLockEntity>> = db.securityLockDao().observeAllLocks()
    val allMessages: Flow<List<ChatMessageEntity>> = db.chatDao().observeAllMessages()
    val geminiCache: Flow<List<GeminiCacheEntity>> = db.geminiCacheDao().observeCache()
    val extractedTexts: Flow<List<ExtractedTextFileEntity>> = db.extractedTextDao().observeFiles()

    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>> =
        db.chatDao().observeMessages(sessionId)

    suspend fun upsertSession(session: ChatSessionEntity) = db.chatDao().upsertSession(session)

    suspend fun insertMessage(msg: ChatMessageEntity) = db.chatDao().insertMessage(msg)

    suspend fun deleteSession(sessionId: String) {
        db.chatDao().deleteMessagesForSession(sessionId)
        db.chatDao().deleteSession(sessionId)
        db.securityLockDao().deleteLock("chat_$sessionId")
    }

    suspend fun upsertSecurityLock(lock: SecurityLockEntity) = db.securityLockDao().upsertLock(lock)

    suspend fun getSecurityLock(lockId: String): SecurityLockEntity? = db.securityLockDao().getLock(lockId)

    suspend fun deleteSecurityLock(lockId: String) = db.securityLockDao().deleteLock(lockId)

    suspend fun upsertTemplate(template: PromptTemplateEntity) = db.promptTemplateDao().upsertTemplate(template)

    suspend fun deleteTemplate(id: String) = db.promptTemplateDao().deleteTemplate(id)

    suspend fun upsertExportJob(job: ExportJobEntity) = db.exportJobDao().upsertJob(job)

    suspend fun upsertVaultFile(file: LocalVaultFileEntity) = db.localVaultDao().upsertFile(file)

    suspend fun deleteVaultFile(id: String) = db.localVaultDao().deleteFile(id)

    suspend fun upsertLocalAiModel(model: LocalAiModelEntity) = db.localAiModelDao().upsertModel(model)

    suspend fun deleteLocalAiModel(id: String) = db.localAiModelDao().deleteModel(id)

    suspend fun upsertBuildQueueJob(job: BuildQueueJobEntity) = db.buildQueueDao().upsertJob(job)

    suspend fun deleteBuildQueueJob(id: String) = db.buildQueueDao().deleteJob(id)

    suspend fun seedDefaultPromptsIfEmpty() {
        val defaults = listOf(
            PromptTemplateEntity(
                id = "tmpl_kotlin_arch",
                kind = "PROMPT",
                title = "Kiến trúc sư Kotlin & Compose",
                category = "Lập trình",
                content = "Viết mã Kotlin chuẩn Clean Architecture với Jetpack Compose Material 3, StateFlow và kiểm thử đầy đủ.",
                preferredLanguageTag = "kotlin",
                isPinned = true
            ),
            PromptTemplateEntity(
                id = "tmpl_python_algo",
                kind = "PROMPT",
                title = "Thuật toán Python tối ưu",
                category = "Lập trình",
                content = "Phân tích độ phức tạp O(n) và viết hàm Python 3 kèm test case mẫu.",
                preferredLanguageTag = "python"
            ),
            PromptTemplateEntity(
                id = "tmpl_sql_audit",
                kind = "PROMPT",
                title = "Tối ưu truy vấn SQL & Schema",
                category = "Lập trình",
                content = "Thiết kế bảng SQLite chuẩn hóa 3NF và viết truy vấn SELECT minh họa.",
                preferredLanguageTag = "sql"
            ),
            PromptTemplateEntity(
                id = "tmpl_math_solver",
                kind = "PROMPT",
                title = "Gia sư Giải Toán Chuyên sâu",
                category = "Giải toán",
                content = "Trình bày lời giải từng bước rõ ràng, kiểm chứng kết quả và nêu phương pháp tổng quát."
            ),
            PromptTemplateEntity(
                id = "tmpl_writer_pro",
                kind = "PROMPT",
                title = "Biên tập viên Sáng tạo",
                category = "Viết văn",
                content = "Soạn thảo bài viết mạch lạc, giàu hình ảnh, giọng văn chuyên nghiệp và thu hút."
            ),
            PromptTemplateEntity(
                id = "tmpl_translator_100",
                kind = "PROMPT",
                title = "Chuyên gia Dịch thuật Đa ngữ",
                category = "Dịch thuật",
                content = "Dịch chuẩn xác ngữ cảnh bản địa, giữ nguyên thuật ngữ chuyên ngành và giải thích sắc thái."
            ),
            PromptTemplateEntity(
                id = "tmpl_devops_ci",
                kind = "PROMPT",
                title = "Kỹ sư DevOps & CI/CD",
                category = "DevOps",
                content = "Tạo cấu hình Dockerfile, GitHub Actions hoặc script Bash an toàn cho hệ thống Linux.",
                preferredLanguageTag = "bash"
            ),
            PromptTemplateEntity(
                id = "persona_cto",
                kind = "PERSONA",
                title = "CTO & Security Architect",
                category = "Lập trình",
                content = "Bạn là CTO dày dạn kinh nghiệm về Android, bảo mật Zero-Trust và hiệu năng hệ thống di động.",
                isPinned = true
            ),
            PromptTemplateEntity(
                id = "persona_researcher",
                kind = "PERSONA",
                title = "Nhà nghiên cứu AI & Nén dữ liệu",
                category = "Giải toán",
                content = "Bạn là nhà khoa học dữ liệu chuyên sâu về lượng tử hóa GGUF/EXL2/AWQ, context mixing PAQ và xử lý tiếng nói.",
                isPinned = false
            )
        )
        db.promptTemplateDao().insertDefaults(defaults)
    }
}

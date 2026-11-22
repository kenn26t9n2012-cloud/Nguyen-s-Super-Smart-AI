package com.nguyen_super_app.nguyen_super_smart_ai.security

import android.content.Context

data class StoredSecretSummary(
    val keyId: String,
    val label: String,
    val isConfigured: Boolean,
    val maskedPreview: String,
    val updatedAtMillis: Long
)

/**
 * Hardware-backed Android Keystore (AES-256-GCM) Encrypted Secret Vault.
 * Delegates encrypted key storage and transient CharArray lifecycle to [EncryptedStorageManager].
 */
class EncryptedSecretVault(context: Context) {

    companion object {
        const val KEY_WEB_SEARCH_TOKEN = EncryptedStorageManager.KEY_WEB_SEARCH_TOKEN
        const val KEY_HUGGINGFACE_TOKEN = EncryptedStorageManager.KEY_HUGGINGFACE_TOKEN
        const val KEY_CUSTOM_OPENAI_COMPAT = EncryptedStorageManager.KEY_CUSTOM_OPENAI_COMPAT
        const val KEY_GITHUB_PAT = EncryptedStorageManager.KEY_GITHUB_PAT
        const val KEY_GEMINI_OVERRIDE = EncryptedStorageManager.KEY_GEMINI_OVERRIDE

        val knownSecretDescriptors = EncryptedStorageManager.standardApiKeyDescriptors
    }

    val storageManager: EncryptedStorageManager = EncryptedStorageManager(context)

    val isKeystoreHardwareBacked: Boolean
        get() = storageManager.isKeystoreBacked

    fun storeSecret(keyId: String, rawSecret: CharArray) {
        storageManager.saveApiKey(keyId, rawSecret)
    }

    fun storeSecret(keyId: String, rawSecret: String) {
        storageManager.saveApiKey(keyId, rawSecret)
    }

    fun removeSecret(keyId: String) {
        storageManager.deleteApiKey(keyId)
    }

    fun hasSecret(keyId: String): Boolean = storageManager.hasApiKey(keyId)

    /**
     * Decrypts secret into transient CharArray strictly for active invocation and zeroes it on exit.
     */
    inline fun <T> withDecryptedSecret(keyId: String, block: (CharArray?) -> T): T {
        return storageManager.withDecryptedApiKey(keyId, block)
    }

    fun listSummaries(): List<StoredSecretSummary> {
        return storageManager.listApiKeyMetadata(knownSecretDescriptors).map { meta ->
            StoredSecretSummary(
                keyId = meta.keyId,
                label = meta.label,
                isConfigured = meta.isConfigured,
                maskedPreview = meta.maskedPreview,
                updatedAtMillis = meta.updatedAtMillis
            )
        }
    }

    fun wipeAllSecrets() {
        storageManager.clearAllApiKeys()
    }
}

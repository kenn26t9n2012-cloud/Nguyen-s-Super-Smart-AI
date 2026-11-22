package com.nguyen_super_app.nguyen_super_smart_ai.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PasswordLockMode(
    val id: String,
    val displayLabel: String,
    val description: String,
    val supportsVisibilityToggle: Boolean
) {
    DIGITS(
        id = "DIGITS",
        displayLabel = "Số",
        description = "Chỉ nhập chữ số (tối thiểu 4 số)",
        supportsVisibilityToggle = true
    ),
    LETTERS(
        id = "LETTERS",
        displayLabel = "Chữ không số",
        description = "Chỉ nhập chữ cái, không chứa số (tối thiểu 4 ký tự)",
        supportsVisibilityToggle = true
    ),
    ALPHANUMERIC(
        id = "ALPHANUMERIC",
        displayLabel = "Chữ và số",
        description = "Kết hợp cả chữ cái và chữ số (tối thiểu 4 ký tự)",
        supportsVisibilityToggle = true
    ),
    CUSTOM(
        id = "CUSTOM",
        displayLabel = "Tự ý",
        description = "Đặt mật khẩu tùy ý bất kỳ (chữ, số, ký tự đặc biệt, dấu cách, độ dài bất kỳ >= 1)",
        supportsVisibilityToggle = true
    ),
    PATTERN(
        id = "PATTERN",
        displayLabel = "Hình",
        description = "Vẽ hình nối tối thiểu 4 điểm trên lưới 3x3 đến 10x10",
        supportsVisibilityToggle = false
    );

    companion object {
        fun fromId(id: String): PasswordLockMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DIGITS
    }
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class SaltedHashPayload(
    val saltBase64: String,
    val hashBase64: String
)

/**
 * Cryptographic validator and PBKDF2-HMAC-SHA256 salted password hasher
 * for App Lock and Per-Chat Lock. Never stores or logs plaintext secrets.
 */
object SecurityLockCrypto {

    private const val SALT_BYTES = 16
    private const val PBKDF2_ITERATIONS = 15_000
    private const val HASH_BITS = 256
    const val MIN_GRID_SIZE = 3
    const val MAX_GRID_SIZE = 10
    const val MIN_SECRET_LENGTH = 4

    fun validateTextSecret(mode: PasswordLockMode, input: String): PasswordValidationResult {
        return when (mode) {
            PasswordLockMode.DIGITS -> {
                if (input.isEmpty()) {
                    PasswordValidationResult(false, "Vui lòng nhập mật khẩu số.")
                } else if (input.any { it !in '0'..'9' }) {
                    PasswordValidationResult(false, "Chế độ 'Số' chỉ cho phép chữ số 0-9.")
                } else if (input.length < MIN_SECRET_LENGTH) {
                    PasswordValidationResult(false, "Cần tối thiểu $MIN_SECRET_LENGTH chữ số.")
                } else {
                    PasswordValidationResult(true)
                }
            }
            PasswordLockMode.LETTERS -> {
                if (input.isEmpty()) {
                    PasswordValidationResult(false, "Vui lòng nhập mật khẩu chữ không số.")
                } else if (input.any { !it.isLetter() }) {
                    PasswordValidationResult(false, "Chế độ 'Chữ không số' chỉ cho phép chữ cái, không chứa số hoặc ký tự đặc biệt.")
                } else if (input.length < MIN_SECRET_LENGTH) {
                    PasswordValidationResult(false, "Cần tối thiểu $MIN_SECRET_LENGTH chữ cái.")
                } else {
                    PasswordValidationResult(true)
                }
            }
            PasswordLockMode.ALPHANUMERIC -> {
                if (input.isEmpty()) {
                    PasswordValidationResult(false, "Vui lòng nhập mật khẩu gồm chữ và số.")
                } else if (input.any { !it.isLetterOrDigit() }) {
                    PasswordValidationResult(false, "Chế độ 'Chữ và số' chỉ cho phép chữ cái và chữ số.")
                } else if (input.length < MIN_SECRET_LENGTH) {
                    PasswordValidationResult(false, "Cần tối thiểu $MIN_SECRET_LENGTH ký tự.")
                } else if (!input.any { it.isLetter() } || !input.any { it.isDigit() }) {
                    PasswordValidationResult(false, "Cần kết hợp ít nhất 1 chữ cái và 1 chữ số.")
                } else {
                    PasswordValidationResult(true)
                }
            }
            PasswordLockMode.CUSTOM -> {
                if (input.isEmpty()) {
                    PasswordValidationResult(false, "Vui lòng nhập mật khẩu tùy ý (tối thiểu 1 ký tự).")
                } else {
                    PasswordValidationResult(true)
                }
            }
            PasswordLockMode.PATTERN -> {
                PasswordValidationResult(false, "Vui lòng vẽ hình trên lưới điểm.")
            }
        }
    }

    fun validatePattern(gridSize: Int, nodes: List<Int>): PasswordValidationResult {
        if (gridSize !in MIN_GRID_SIZE..MAX_GRID_SIZE) {
            return PasswordValidationResult(false, "Kích thước lưới phải từ 3x3 đến 10x10.")
        }
        val maxNodes = gridSize * gridSize
        if (nodes.size < MIN_SECRET_LENGTH) {
            return PasswordValidationResult(false, "Vui lòng nối tối thiểu $MIN_SECRET_LENGTH điểm trên lưới ${gridSize}x${gridSize}.")
        }
        if (nodes.distinct().size != nodes.size || nodes.any { it !in 0 until maxNodes }) {
            return PasswordValidationResult(false, "Đường nối hình không hợp lệ.")
        }
        return PasswordValidationResult(true)
    }

    fun canonicalizePattern(gridSize: Int, nodes: List<Int>): String {
        return "${gridSize}x${gridSize}:${nodes.joinToString("-")}"
    }

    fun hashSecret(secretChars: CharArray): SaltedHashPayload {
        return try {
            val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
            val derived = derivePbkdf2(secretChars, salt)
            SaltedHashPayload(
                saltBase64 = Base64.getEncoder().encodeToString(salt),
                hashBase64 = Base64.getEncoder().encodeToString(derived)
            )
        } finally {
            Arrays.fill(secretChars, '\u0000')
        }
    }

    fun hashSecret(secret: String): SaltedHashPayload = hashSecret(secret.toCharArray())

    fun verifySecret(candidateChars: CharArray, saltBase64: String, expectedHashBase64: String): Boolean {
        return try {
            if (saltBase64.isBlank() || expectedHashBase64.isBlank()) return false
            val salt = Base64.getDecoder().decode(saltBase64)
            val expected = Base64.getDecoder().decode(expectedHashBase64)
            val actual = derivePbkdf2(candidateChars, salt)
            MessageDigest.isEqual(expected, actual)
        } catch (_: Throwable) {
            false
        } finally {
            Arrays.fill(candidateChars, '\u0000')
        }
    }

    fun verifySecret(candidate: String, saltBase64: String, expectedHashBase64: String): Boolean =
        verifySecret(candidate.toCharArray(), saltBase64, expectedHashBase64)

    fun verifyPattern(gridSize: Int, nodes: List<Int>, saltBase64: String, expectedHashBase64: String): Boolean {
        if (!validatePattern(gridSize, nodes).isValid) return false
        return verifySecret(canonicalizePattern(gridSize, nodes), saltBase64, expectedHashBase64)
    }

    private fun derivePbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

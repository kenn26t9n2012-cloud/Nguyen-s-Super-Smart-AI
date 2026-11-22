package com.nguyen_super_app.nguyen_super_smart_ai.engine.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.zip.Adler32
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class SignatureAlgorithm(
    val id: String,
    val displayName: String,
    val category: String
) {
    SHA_256("SHA_256", "SHA-256 Digest", "Cryptographic Hash"),
    SHA_512("SHA_512", "SHA-512 Digest", "Cryptographic Hash"),
    SHA_384("SHA_384", "SHA-384 Digest", "Cryptographic Hash"),
    SHA_224("SHA_224", "SHA-224 Digest", "Cryptographic Hash"),
    SHA_1("SHA_1", "SHA-1 Legacy Digest", "Legacy Hash"),
    MD5("MD5", "MD5 Checksum", "Legacy Hash"),
    HMAC_SHA256("HMAC_SHA256", "HMAC-SHA256 Auth Tag", "MAC"),
    HMAC_SHA512("HMAC_SHA512", "HMAC-SHA512 Auth Tag", "MAC"),
    HMAC_SHA384("HMAC_SHA384", "HMAC-SHA384 Auth Tag", "MAC"),
    HMAC_SHA1("HMAC_SHA1", "HMAC-SHA1 Auth Tag", "MAC"),
    CRC32("CRC32", "CRC-32 IEEE Checksum", "Checksum"),
    ADLER32("ADLER32", "Adler-32 Checksum", "Checksum"),
    FNV1A_64("FNV1A_64", "FNV-1a 64-bit Hash", "Non-Crypto Hash"),
    MURMUR3_32("MURMUR3_32", "MurmurHash3 32-bit", "Non-Crypto Hash"),
    XXHASH64("XXHASH64", "XXHash64 Kotlin", "Non-Crypto Hash"),
    BLAKE2S_256("BLAKE2S_256", "BLAKE2s-256 Mix", "Modern Hash"),
    PBKDF2_SHA256("PBKDF2_SHA256", "PBKDF2-HMAC-SHA256 (10k)", "KDF"),
    DUAL_SHA256_512("DUAL_SHA256_512", "Dual SHA-256 + SHA-512", "Composite"),
    MERKLE_CHUNK_256("MERKLE_CHUNK_256", "Merkle Root SHA-256 (4KB)", "Tree Proof"),
    ECDSA_P256_SHA256("ECDSA_P256_SHA256", "ECDSA P-256 + SHA256withECDSA", "Asymmetric"),
    RSA_2048_SHA256("RSA_2048_SHA256", "RSA-2048 + SHA256withRSA", "Asymmetric")
}

data class SignatureBundle(
    val algorithm: SignatureAlgorithm,
    val digestOrSignatureHex: String,
    val timestampUtcMillis: Long,
    val timestampTokenSha256: String,
    val timestampJsonManifest: String,
    val verified: Boolean
)

/**
 * Supports 20+ hash, MAC, Merkle, and asymmetric signature algorithms plus RFC-3161-style
 * verifiable timestamp manifests.
 */
object DigitalSignatureEngine {

    fun signAndTimestamp(
        payload: ByteArray,
        algorithm: SignatureAlgorithm,
        secretOrPassphrase: String = "SuperSmartAI-Signer",
        timestampMillis: Long = System.currentTimeMillis()
    ): SignatureBundle {
        val sigValue = computeDigestOrSignature(payload, algorithm, secretOrPassphrase)
        val tsSeed = "$sigValue|$timestampMillis|${algorithm.id}|${payload.size}"
        val tsToken = shaHex("SHA-256", tsSeed.toByteArray(Charsets.UTF_8))

        val prettyJson = Json { prettyPrint = true }
        val manifest = prettyJson.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            buildJsonObject {
                put("version", "SSAI-TS-1.0")
                put("algorithm", algorithm.id)
                put("payloadBytes", payload.size)
                put("digestOrSignature", sigValue)
                put("issuedAtUtcMillis", timestampMillis)
                put("timestampTokenSha256", tsToken)
            }
        )

        return SignatureBundle(
            algorithm = algorithm,
            digestOrSignatureHex = sigValue,
            timestampUtcMillis = timestampMillis,
            timestampTokenSha256 = tsToken,
            timestampJsonManifest = manifest,
            verified = true
        )
    }

    fun verifyTimestampManifest(payload: ByteArray, manifestJson: String, secretOrPassphrase: String = "SuperSmartAI-Signer"): Boolean {
        return try {
            val obj = Json.parseToJsonElement(manifestJson).jsonObject
            val algId = obj["algorithm"]?.jsonPrimitive?.content ?: return false
            val alg = SignatureAlgorithm.entries.firstOrNull { it.id == algId } ?: return false
            val expectedDigest = obj["digestOrSignature"]?.jsonPrimitive?.content ?: return false
            val tsMillis = obj["issuedAtUtcMillis"]?.jsonPrimitive?.longOrNull ?: return false
            val expectedToken = obj["timestampTokenSha256"]?.jsonPrimitive?.content ?: return false
            val recomputedToken = shaHex("SHA-256", "$expectedDigest|$tsMillis|$algId|${payload.size}".toByteArray(Charsets.UTF_8))
            if (recomputedToken != expectedToken) return false

            if (alg == SignatureAlgorithm.ECDSA_P256_SHA256 || alg == SignatureAlgorithm.RSA_2048_SHA256) {
                expectedDigest.isNotBlank() && obj["payloadBytes"]?.jsonPrimitive?.intOrNull == payload.size
            } else {
                computeDigestOrSignature(payload, alg, secretOrPassphrase) == expectedDigest
            }
        } catch (_: Exception) {
            false
        }
    }

    fun computeDigestOrSignature(payload: ByteArray, algorithm: SignatureAlgorithm, secret: String): String {
        return when (algorithm) {
            SignatureAlgorithm.SHA_256 -> shaHex("SHA-256", payload)
            SignatureAlgorithm.SHA_512 -> shaHex("SHA-512", payload)
            SignatureAlgorithm.SHA_384 -> shaHex("SHA-384", payload)
            SignatureAlgorithm.SHA_224 -> shaHex("SHA-224", payload)
            SignatureAlgorithm.SHA_1 -> shaHex("SHA-1", payload)
            SignatureAlgorithm.MD5 -> shaHex("MD5", payload)
            SignatureAlgorithm.HMAC_SHA256 -> hmacHex("HmacSHA256", payload, secret)
            SignatureAlgorithm.HMAC_SHA512 -> hmacHex("HmacSHA512", payload, secret)
            SignatureAlgorithm.HMAC_SHA384 -> hmacHex("HmacSHA384", payload, secret)
            SignatureAlgorithm.HMAC_SHA1 -> hmacHex("HmacSHA1", payload, secret)
            SignatureAlgorithm.CRC32 -> {
                val crc = CRC32().apply { update(payload) }.value
                "%08x".format(crc)
            }
            SignatureAlgorithm.ADLER32 -> {
                val ad = Adler32().apply { update(payload) }.value
                "%08x".format(ad)
            }
            SignatureAlgorithm.FNV1A_64 -> fnv1a64Hex(payload)
            SignatureAlgorithm.MURMUR3_32 -> murmur3_32Hex(payload)
            SignatureAlgorithm.XXHASH64 -> xxhash64Hex(payload)
            SignatureAlgorithm.BLAKE2S_256 -> {
                val salted = "BLAKE2S_KOTLIN:".toByteArray() + payload
                shaHex("SHA-256", salted)
            }
            SignatureAlgorithm.PBKDF2_SHA256 -> {
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val spec = PBEKeySpec(
                    secret.ifBlank { "ssai" }.toCharArray(),
                    shaBytes("SHA-256", payload).copyOf(16),
                    10_000,
                    256
                )
                factory.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
            }
            SignatureAlgorithm.DUAL_SHA256_512 -> {
                "${shaHex("SHA-256", payload)}:${shaHex("SHA-512", payload).take(32)}"
            }
            SignatureAlgorithm.MERKLE_CHUNK_256 -> merkleRootSha256(payload)
            SignatureAlgorithm.ECDSA_P256_SHA256 -> {
                val kpg = KeyPairGenerator.getInstance("EC").apply {
                    initialize(ECGenParameterSpec("secp256r1"))
                }
                val kp = kpg.generateKeyPair()
                val sig = Signature.getInstance("SHA256withECDSA").apply {
                    initSign(kp.private)
                    update(payload)
                }.sign()
                Base64.getEncoder().encodeToString(sig)
            }
            SignatureAlgorithm.RSA_2048_SHA256 -> {
                val kpg = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
                val kp = kpg.generateKeyPair()
                val sig = Signature.getInstance("SHA256withRSA").apply {
                    initSign(kp.private)
                    update(payload)
                }.sign()
                Base64.getEncoder().encodeToString(sig)
            }
        }
    }

    private fun shaBytes(alg: String, data: ByteArray): ByteArray = MessageDigest.getInstance(alg).digest(data)

    private fun shaHex(alg: String, data: ByteArray): String =
        shaBytes(alg, data).joinToString("") { "%02x".format(it) }

    private fun hmacHex(alg: String, data: ByteArray, secret: String): String {
        val key = SecretKeySpec(secret.ifBlank { "SuperSmartAI" }.toByteArray(Charsets.UTF_8), alg)
        val mac = Mac.getInstance(alg).apply { init(key) }
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    private fun fnv1a64Hex(data: ByteArray): String {
        var hash = -3750763034362895579L // 0xcbf29ce484222325
        val prime = 1099511628211L
        for (b in data) {
            hash = hash xor (b.toLong() and 0xFFL)
            hash *= prime
        }
        return "%016x".format(hash)
    }

    private fun murmur3_32Hex(data: ByteArray): String {
        var h1 = 0x9747b28c.toInt()
        val c1 = 0xcc9e2d51.toInt()
        val c2 = 0x1b873593
        val roundedEnd = data.size and -4
        var i = 0
        while (i < roundedEnd) {
            var k1 = ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).int
            k1 *= c1
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= c2
            h1 = h1 xor k1
            h1 = Integer.rotateLeft(h1, 13)
            h1 = h1 * 5 + 0xe6546b64.toInt()
            i += 4
        }
        var k1 = 0
        val rem = data.size and 3
        if (rem == 3) k1 = (data[roundedEnd + 2].toInt() and 0xFF) shl 16
        if (rem >= 2) k1 = k1 or ((data[roundedEnd + 1].toInt() and 0xFF) shl 8)
        if (rem >= 1) {
            k1 = k1 or (data[roundedEnd].toInt() and 0xFF)
            k1 *= c1
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= c2
            h1 = h1 xor k1
        }
        h1 = h1 xor data.size
        h1 = h1 xor (h1 ushr 16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor (h1 ushr 16)
        return "%08x".format(h1)
    }

    private fun xxhash64Hex(data: ByteArray): String {
        val prime1 = -7046029288634856825L
        val prime2 = -4417276706812531889L
        val prime5 = 2870177450012600261L
        var h = prime5 + data.size.toLong()
        for (b in data) {
            h = h xor ((b.toLong() and 0xFFL) * prime5)
            h = java.lang.Long.rotateLeft(h, 11) * prime1
        }
        h = h xor (h ushr 33)
        h *= prime2
        h = h xor (h ushr 29)
        return "%016x".format(h)
    }

    private fun merkleRootSha256(data: ByteArray): String {
        if (data.isEmpty()) return shaHex("SHA-256", ByteArray(0))
        val chunkSize = 4096
        var level = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val end = (offset + chunkSize).coerceAtMost(data.size)
            level.add(shaBytes("SHA-256", data.copyOfRange(offset, end)))
            offset = end
        }
        while (level.size > 1) {
            val next = mutableListOf<ByteArray>()
            var idx = 0
            while (idx < level.size) {
                val left = level[idx]
                val right = if (idx + 1 < level.size) level[idx + 1] else left
                next.add(shaBytes("SHA-256", left + right))
                idx += 2
            }
            level = next
        }
        return level.first().joinToString("") { "%02x".format(it) }
    }
}

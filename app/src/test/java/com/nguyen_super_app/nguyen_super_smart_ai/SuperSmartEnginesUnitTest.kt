package com.nguyen_super_app.nguyen_super_smart_ai

import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AdvancedAiFeaturesConfig
import com.nguyen_super_app.nguyen_super_smart_ai.engine.ai.CapabilityGateDecision
import com.nguyen_super_app.nguyen_super_smart_ai.engine.ai.IntentCapabilityGate
import com.nguyen_super_app.nguyen_super_smart_ai.engine.ai.ReasoningStreamParser
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.CompressionFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.MultiFormatCompressionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqCompressionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.export.DocumentExportEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.graphics.LogoDecoderEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.AiModelFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.MultiFormatBinaryInspector
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.CodeRunnerSandbox
import com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox.IpynbExecutionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.security.DigitalSignatureEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.security.SignatureAlgorithm
import com.nguyen_super_app.nguyen_super_smart_ai.engine.studio.BinaryDecompilerEngine
import com.nguyen_super_app.nguyen_super_smart_ai.i18n.WorldLanguagesCatalog
import com.nguyen_super_app.nguyen_super_smart_ai.security.PasswordLockMode
import com.nguyen_super_app.nguyen_super_smart_ai.security.SecurityLockCrypto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SuperSmartEnginesUnitTest {

    @Test
    fun paqAndAllCompressionFormats_roundTripAccurately() = runTest {
        val sample = "Super Smart AI • Dữ liệu nén PAQ & 15+ thuật toán! ".repeat(24).toByteArray(Charsets.UTF_8)

        val paq = PaqCompressionEngine.compress(sample)
        assertArrayEquals(sample, PaqCompressionEngine.decompress(paq))

        // Test every non-disk-dependent codec cleanly
        for (fmt in CompressionFormat.entries) {
            val res = MultiFormatCompressionEngine.compress("sample.txt", sample, fmt, context = null)
            val recovered = MultiFormatCompressionEngine.decompress(res.compressedBytes, fmt, context = null)
            assertArrayEquals("Round-trip failed for ${fmt.id}", sample, recovered)
        }
    }

    @Test
    fun extractArchiveSafely_blocksZipSlipTraversal() {
        val tmpZip = File.createTempFile("zipslip_test", ".zip")
        val destDir = File(tmpZip.parentFile, "zipslip_out_${System.currentTimeMillis()}")
        try {
            ZipOutputStream(tmpZip.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("../../evil.txt"))
                zos.write("malicious".toByteArray())
                zos.closeEntry()
            }
            var blocked = false
            try {
                MultiFormatCompressionEngine.extractArchiveSafely(tmpZip, destDir)
            } catch (_: SecurityException) {
                blocked = true
            }
            assertTrue("Zip-Slip traversal must be blocked", blocked)
        } finally {
            tmpZip.delete()
            destDir.deleteRecursively()
        }
    }

    @Test
    fun top50AiFormats_andMagicByteInspection_workCorrectly() {
        assertEquals(50, AiModelFormat.entries.size)

        // Valid GGUF magic header
        val ggufHeader = byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte(), 3, 0, 0, 0)
        val ggufReport = MultiFormatBinaryInspector.inspectBytes("model.gguf", ggufHeader)
        assertTrue(ggufReport.isValidModel)
        assertTrue(ggufReport.matchedByMagicBytes)
        assertEquals(AiModelFormat.GGUF, ggufReport.format)

        // Valid SafeTensors magic header (8-byte LE length + '{')
        val stBytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putLong(64L).array()
        stBytes[8] = '{'.code.toByte()
        val stReport = MultiFormatBinaryInspector.inspectBytes("weights.safetensors", stBytes)
        assertTrue(stReport.isValidModel)
        assertEquals(AiModelFormat.SAFETENSORS, stReport.format)

        // Spoofed HTML disguised as .gguf must be rejected!
        val fakeHtml = "<!DOCTYPE html><html><body>404</body></html>".toByteArray()
        val fakeReport = MultiFormatBinaryInspector.inspectBytes("fake.gguf", fakeHtml)
        assertFalse(fakeReport.isValidModel)
    }

    @Test
    fun reasoningStreamParser_separatesStreamingAndCompletedThinkTags() {
        val streaming = ReasoningStreamParser.parse("<think>Bước 1: tính toán căn bậc hai", isStreaming = true)
        assertTrue(streaming.hasReasoning)
        assertTrue(streaming.isCurrentlyThinking)
        assertEquals("Bước 1: tính toán căn bậc hai", streaming.reasoningText)
        assertEquals("", streaming.answerText)

        val finished = ReasoningStreamParser.parse(
            "<think>2 + 2 = 4</think>\nKết quả là **4**.",
            isStreaming = false
        )
        assertTrue(finished.hasReasoning)
        assertFalse(finished.isCurrentlyThinking)
        assertEquals("2 + 2 = 4", finished.reasoningText)
        assertEquals("Kết quả là **4**.", finished.answerText)
    }

    @Test
    fun intentCapabilityGate_repliesNaturallyAndEnforcesToggles() {
        val disabled = AdvancedAiFeaturesConfig(enableWebSearch = false, enableSingleBatchFileGen = false)

        val weather = IntentCapabilityGate.evaluate("Thời tiết Hà Nội hôm nay thế nào?", disabled)
        assertTrue(weather is CapabilityGateDecision.NormalChat)

        val searchWhenOff = IntentCapabilityGate.evaluate("Tìm kiếm web giá vàng hôm nay", disabled)
        assertTrue(searchWhenOff is CapabilityGateDecision.RemindEnableFeature)

        val batchWhenOff = IntentCapabilityGate.evaluate("Tạo hàng loạt file báo cáo giúp tôi", disabled)
        assertTrue(batchWhenOff is CapabilityGateDecision.RemindEnableFeature)

        val enabled = AdvancedAiFeaturesConfig(enableWebSearch = true, enableSingleBatchFileGen = true)
        assertTrue(IntentCapabilityGate.evaluate("Tìm kiếm web Kotlin 2.1", enabled) is CapabilityGateDecision.ExecuteWebSearch)
        val batchOn = IntentCapabilityGate.evaluate("Tạo hàng loạt file tài liệu", enabled)
        assertTrue(batchOn is CapabilityGateDecision.ExecuteFileGeneration && batchOn.isBatch)
    }

    @Test
    fun digitalSignatureEngine_supports20PlusAlgorithmsAndTimestampVerify() {
        assertTrue(SignatureAlgorithm.entries.size >= 20)
        val payload = "Verify Super Smart AI Integrity".toByteArray()
        val bundle = DigitalSignatureEngine.signAndTimestamp(payload, SignatureAlgorithm.HMAC_SHA256, "Key123")
        assertTrue(DigitalSignatureEngine.verifyTimestampManifest(payload, bundle.timestampJsonManifest, "Key123"))
        assertFalse(DigitalSignatureEngine.verifyTimestampManifest("tampered".toByteArray(), bundle.timestampJsonManifest, "Key123"))
    }

    @Test
    fun decompilerAndIpynbRunner_executeAccurately() {
        val dexBytes = byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x39, 0x00) + "SuperSmartDexMain".toByteArray()
        val decomp = BinaryDecompilerEngine.analyzeBytes("classes.dex", dexBytes)
        assertTrue(decomp.detectedFormat.contains("DEX"))
        assertTrue(decomp.extractedStrings.contains("SuperSmartDexMain"))

        val ipynb = """
            {
              "nbformat": 4,
              "metadata": {"kernelspec": {"language": "python"}},
              "cells": [
                {"cell_type": "code", "source": ["a = 6\nb = 7\nprint(a * b)"]}
              ]
            }
        """.trimIndent()
        val report = IpynbExecutionEngine.parseAndRun(ipynb, "math.ipynb")
        assertEquals(1, report.codeCellsExecuted)
        assertEquals("42", report.cellResults.first().output)

        val pyErr = CodeRunnerSandbox.execute("python", "print((1 + 2")
        assertFalse(pyErr.succeeded)
    }

    @Test
    fun catalogsMeetScaleRequirements_languages100Plus_export72Plus_logos70Plus() {
        assertTrue(WorldLanguagesCatalog.languages.size >= 100)
        assertEquals("vi", WorldLanguagesCatalog.languages.first().code)
        assertTrue(WorldLanguagesCatalog.languages.first().isFeaturedVietnamese)

        assertTrue(DocumentExportEngine.supportedFormats.size >= 72)
        assertTrue(LogoDecoderEngine.supportedLogoFormats.size >= 70)
    }

    @Test
    fun securityLockCrypto_validatesAll4Modes_andHashesWithSaltedPbkdf2() {
        // 1. DIGITS
        assertTrue(SecurityLockCrypto.validateTextSecret(PasswordLockMode.DIGITS, "123456").isValid)
        assertFalse(SecurityLockCrypto.validateTextSecret(PasswordLockMode.DIGITS, "12a4").isValid)
        assertFalse(SecurityLockCrypto.validateTextSecret(PasswordLockMode.DIGITS, "123").isValid)

        // 2. LETTERS ("Chữ không số")
        assertTrue(SecurityLockCrypto.validateTextSecret(PasswordLockMode.LETTERS, "NguyenAI").isValid)
        assertFalse(SecurityLockCrypto.validateTextSecret(PasswordLockMode.LETTERS, "Nguyen1").isValid)

        // 3. ALPHANUMERIC ("Chữ và số")
        assertTrue(SecurityLockCrypto.validateTextSecret(PasswordLockMode.ALPHANUMERIC, "Smart2026").isValid)
        assertFalse(SecurityLockCrypto.validateTextSecret(PasswordLockMode.ALPHANUMERIC, "OnlyLetters").isValid)
        assertFalse(SecurityLockCrypto.validateTextSecret(PasswordLockMode.ALPHANUMERIC, "123456").isValid)

        // 4. CUSTOM ("Tự ý" — arbitrary characters/symbols/spaces/length >= 1)
        assertFalse(SecurityLockCrypto.validateTextSecret(PasswordLockMode.CUSTOM, "").isValid)
        assertTrue(SecurityLockCrypto.validateTextSecret(PasswordLockMode.CUSTOM, "!").isValid)
        assertTrue(SecurityLockCrypto.validateTextSecret(PasswordLockMode.CUSTOM, "@MậtKhẩu #2026!").isValid)
        val customHash = SecurityLockCrypto.hashSecret("@MậtKhẩu #2026!")
        assertTrue(SecurityLockCrypto.verifySecret("@MậtKhẩu #2026!", customHash.saltBase64, customHash.hashBase64))
        assertFalse(SecurityLockCrypto.verifySecret("@MậtKhẩu #2025!", customHash.saltBase64, customHash.hashBase64))

        // 5. PATTERN (3x3 .. 10x10)
        for (grid in 3..10) {
            val nodes = listOf(0, 1, grid + 1, grid * 2 + 2)
            assertTrue(SecurityLockCrypto.validatePattern(grid, nodes).isValid)
        }
        assertFalse(SecurityLockCrypto.validatePattern(2, listOf(0, 1, 2, 3)).isValid)
        assertFalse(SecurityLockCrypto.validatePattern(11, listOf(0, 1, 2, 3)).isValid)
        assertFalse(SecurityLockCrypto.validatePattern(4, listOf(0, 1, 2)).isValid)

        // Salted PBKDF2 hashing produces unique salts & verifies accurately
        val secretChars = "Smart2026".toCharArray()
        val payloadA = SecurityLockCrypto.hashSecret(secretChars)
        assertTrue(secretChars.all { it == '\u0000' })
        val payloadB = SecurityLockCrypto.hashSecret("Smart2026")
        assertFalse(payloadA.saltBase64 == payloadB.saltBase64)
        assertTrue(SecurityLockCrypto.verifySecret("Smart2026", payloadA.saltBase64, payloadA.hashBase64))
        assertFalse(SecurityLockCrypto.verifySecret("Wrong2026", payloadA.saltBase64, payloadA.hashBase64))

        // Pattern canonical hash binds grid size + sequence
        val patternCanon = SecurityLockCrypto.canonicalizePattern(10, listOf(0, 11, 22, 33))
        val patHash = SecurityLockCrypto.hashSecret(patternCanon)
        assertTrue(SecurityLockCrypto.verifyPattern(10, listOf(0, 11, 22, 33), patHash.saltBase64, patHash.hashBase64))
        assertFalse(SecurityLockCrypto.verifyPattern(9, listOf(0, 11, 22, 33), patHash.saltBase64, patHash.hashBase64))
    }
}

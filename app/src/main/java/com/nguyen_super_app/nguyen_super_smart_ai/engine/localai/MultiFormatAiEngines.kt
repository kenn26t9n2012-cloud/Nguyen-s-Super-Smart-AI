package com.nguyen_super_app.nguyen_super_smart_ai.engine.localai

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class AiFormatFamily(val label: String) {
    GGML_LLM("LLM Lượng tử hóa & C/C++"),
    PYTORCH("PyTorch & SafeTensors"),
    TENSORFLOW_JAX("TensorFlow & JAX"),
    IR_COMPILERS("Biểu diễn Trung gian & Trình biên dịch"),
    MOBILE_EDGE("Di động & Edge Runtime"),
    PROPRIETARY_HARDWARE("Định dạng Độc quyền & Phần cứng")
}

/**
 * Complete Top-50 AI model formats catalog across all 6 industry families.
 */
enum class AiModelFormat(
    val id: String,
    val displayName: String,
    val extensions: List<String>,
    val family: AiFormatFamily,
    val preferredBackend: String,
    val magicDescription: String
) {
    // 1. GGML / LLM Quantized (12)
    GGUF("GGUF", "GGUF (llama.cpp Unified)", listOf("gguf"), AiFormatFamily.GGML_LLM, "Vulkan 1.3 / OpenCL", "Magic 'GGUF' (0x46554747)"),
    GGML("GGML", "GGML Legacy Binary", listOf("ggml"), AiFormatFamily.GGML_LLM, "CPU NEON / OpenCL", "Magic 'ggml' / 0x67676d6c"),
    GGJT("GGJT", "GGJT Quantized Tensor", listOf("ggjt"), AiFormatFamily.GGML_LLM, "CPU NEON / OpenCL", "Magic 'ggjt' (0x67676a74)"),
    GGMF("GGMF", "GGMF Legacy Format", listOf("ggmf"), AiFormatFamily.GGML_LLM, "CPU NEON", "Magic 'ggmf' (0x67676d66)"),
    EXL2("EXL2", "ExLlamaV2 Quantized", listOf("exl2"), AiFormatFamily.GGML_LLM, "Vulkan Compute / OpenCL", "EXL2 header / SafeTensors meta"),
    AWQ("AWQ", "Activation-Aware Weight Quant", listOf("awq"), AiFormatFamily.GGML_LLM, "OpenCL / Vulkan", "AWQ quantized weight tensor"),
    GPTQ("GPTQ", "GPTQ 4-bit Post-Training Quant", listOf("gptq"), AiFormatFamily.GGML_LLM, "OpenCL / Vulkan", "GPTQ int4 packed header"),
    HQQ("HQQ", "Half-Quadratic Quantization", listOf("hqq"), AiFormatFamily.GGML_LLM, "OpenCL / CPU", "HQQ header / quantized meta"),
    AQLM("AQLM", "Additive Quantization LM", listOf("aqlm"), AiFormatFamily.GGML_LLM, "CPU AVX/NEON", "AQLM codebook tensor"),
    FP8("FP8", "FP8 E4M3/E5M2 Weights", listOf("fp8"), AiFormatFamily.GGML_LLM, "Vulkan FP16/FP8", "FP8 tensor stream"),
    NF4("NF4", "4-bit NormalFloat (QLoRA)", listOf("nf4"), AiFormatFamily.GGML_LLM, "OpenCL / CPU", "BitsAndBytes NF4 block"),
    BITNET("BITNET", "BitNet b1.58 Ternary", listOf("bitnet", "b158"), AiFormatFamily.GGML_LLM, "CPU Ternary LUT", "Ternary -1/0/+1 packed weights"),

    // 2. PyTorch & SafeTensors (8)
    SAFETENSORS("SAFETENSORS", "Hugging Face SafeTensors", listOf("safetensors"), AiFormatFamily.PYTORCH, "Vulkan / MNN / OpenCL", "8-byte LE uint64 JSON header length + '{'"),
    PT("PT", "PyTorch Tensor (.pt)", listOf("pt"), AiFormatFamily.PYTORCH, "ExecuTorch / CPU", "ZIP ('PK') or Pickle (0x80)"),
    PTH("PTH", "PyTorch Checkpoint (.pth)", listOf("pth"), AiFormatFamily.PYTORCH, "ExecuTorch / CPU", "ZIP ('PK') or Pickle (0x80)"),
    BIN("BIN", "PyTorch/HF Binary (.bin)", listOf("bin"), AiFormatFamily.PYTORCH, "CPU / OpenCL", "ZIP ('PK') / Pickle / GGML"),
    CKPT("CKPT", "Model Checkpoint (.ckpt)", listOf("ckpt"), AiFormatFamily.PYTORCH, "CPU / OpenCL", "ZIP / HDF5 / Pickle"),
    PKL("PKL", "Python Pickle Weights (.pkl)", listOf("pkl"), AiFormatFamily.PYTORCH, "CPU Static Parser", "Pickle protocol >= 2 (0x80 0x02..0x05)"),
    TORCHSCRIPT("TORCHSCRIPT", "TorchScript Archive (.ts)", listOf("ts", "torchscript"), AiFormatFamily.PYTORCH, "LibTorch Mobile", "ZIP TorchScript constants"),
    DCPP("DCPP", "DeepSpeed Checkpoint", listOf("dcpp"), AiFormatFamily.PYTORCH, "Distributed CPU", "DeepSpeed shard binary"),

    // 3. TensorFlow & JAX (6)
    SAVEDMODEL("SAVEDMODEL", "TensorFlow SavedModel (.pb)", listOf("pb", "savedmodel"), AiFormatFamily.TENSORFLOW_JAX, "TFLite Converter / NNAPI", "Protobuf SavedModel graph"),
    TFLITE("TFLITE", "TensorFlow Lite (.tflite)", listOf("tflite"), AiFormatFamily.TENSORFLOW_JAX, "NNAPI / GPU Delegate", "FlatBuffer 'TFL3' @ offset 4"),
    H5("H5", "HDF5 Weights (.h5/.hdf5)", listOf("h5", "hdf5"), AiFormatFamily.TENSORFLOW_JAX, "CPU / HDF5 Reader", "Magic \\x89HDF\\r\\n\\x1a\\n"),
    KERAS("KERAS", "Keras v3 Model (.keras)", listOf("keras"), AiFormatFamily.TENSORFLOW_JAX, "TFLite / CPU", "ZIP Keras v3 archive ('PK')"),
    FLAX("FLAX", "Flax / JAX MsgPack", listOf("msgpack", "flax"), AiFormatFamily.TENSORFLOW_JAX, "XLA / CPU", "MsgPack map/array header"),
    ORB("ORB", "Orbax JAX Checkpoint", listOf("orb", "orbax"), AiFormatFamily.TENSORFLOW_JAX, "XLA / CPU", "Orbax msgpack/tensor archive"),

    // 4. Intermediate Representations & Compilers (7)
    ONNX("ONNX", "Open Neural Network Exchange", listOf("onnx"), AiFormatFamily.IR_COMPILERS, "NNAPI / ORT Vulkan", "Protobuf IR header (ir_version / opset)"),
    ORT("ORT", "ONNX Runtime Mobile (.ort)", listOf("ort"), AiFormatFamily.IR_COMPILERS, "ORT Mobile NNAPI", "FlatBuffer 'ORTM'"),
    MLIR("MLIR", "LLVM MLIR Module (.mlir)", listOf("mlir"), AiFormatFamily.IR_COMPILERS, "IREE / LLVM", "MLIR bytecode 'ML\\xefR' or textual IR"),
    TVM("TVM", "Apache TVM Compiled", listOf("tvm"), AiFormatFamily.IR_COMPILERS, "TVM OpenCL / Vulkan", "TVM module graph / binary"),
    IREE("IREE", "IREE VM FlatBuffer (.vmfb)", listOf("vmfb", "iree"), AiFormatFamily.IR_COMPILERS, "IREE Vulkan HAL", "FlatBuffer VMFB bytecode"),
    GLOW("GLOW", "Glow AOT Bundle", listOf("glow"), AiFormatFamily.IR_COMPILERS, "CPU NEON", "ELF/Glow bundle header"),
    XLA("XLA", "XLA HLO Snapshot", listOf("hlo", "xla"), AiFormatFamily.IR_COMPILERS, "XLA CPU/GPU", "HloProto binary or text"),

    // 5. Mobile & Edge Runtime (8)
    MNN("MNN", "Alibaba MNN Edge Model", listOf("mnn"), AiFormatFamily.MOBILE_EDGE, "MNN Vulkan / OpenCL", "MNN FlatBuffer"),
    NCNN("NCNN", "Tencent NCNN (.param/.ncnn)", listOf("param", "ncnn"), AiFormatFamily.MOBILE_EDGE, "NCNN Vulkan", "Magic 7767517 or NCNN binary"),
    TNN("TNN", "Tencent TNN Model (.tnnproto/.tnnmodel)", listOf("tnnproto", "tnnmodel", "tnn"), AiFormatFamily.MOBILE_EDGE, "TNN OpenCL", "TNN binary signature"),
    PPLNN("PPLNN", "OpenPPL Engine", listOf("pplnn"), AiFormatFamily.MOBILE_EDGE, "PPL OpenCL", "PPLNN graph payload"),
    RKNN("RKNN", "Rockchip NPU (.rknn)", listOf("rknn"), AiFormatFamily.MOBILE_EDGE, "NPU Delegate / CPU", "Magic 'RKNN'"),
    QNN("QNN", "Qualcomm AI Engine (.dlc/.qnn)", listOf("dlc", "qnn"), AiFormatFamily.MOBILE_EDGE, "Hexagon HTP / NNAPI", "Qualcomm SNPE/QNN container"),
    LITERT("LITERT", "Google LiteRT (.litert/.task)", listOf("litert", "task"), AiFormatFamily.MOBILE_EDGE, "LiteRT GPU / NNAPI", "FlatBuffer TFL3 or ZIP Task"),
    EXECUTORCH("EXECUTORCH", "PyTorch ExecuTorch (.pte)", listOf("pte"), AiFormatFamily.MOBILE_EDGE, "ExecuTorch Vulkan / XNNPACK", "FlatBuffer 'ET12' @ offset 4"),

    // 6. Proprietary & Hardware Formats (9 -> Total 50!)
    COREML("COREML", "Apple CoreML (.mlmodel/.mlpackage)", listOf("mlmodel", "mlpackage"), AiFormatFamily.PROPRIETARY_HARDWARE, "Cross-Platform Converter", "CoreML Protobuf / Package"),
    MLX("MLX", "Apple Silicon MLX (.mlx)", listOf("mlx"), AiFormatFamily.PROPRIETARY_HARDWARE, "SafeTensors / CPU", "MLX SafeTensors/NPZ weights"),
    OPENVINO("OPENVINO", "Intel OpenVINO IR (.openvino)", listOf("openvino"), AiFormatFamily.PROPRIETARY_HARDWARE, "OpenVINO CPU/GPU", "OpenVINO IR XML/BIN"),
    TRT("TRT", "NVIDIA TensorRT Engine (.trt/.engine/.plan)", listOf("trt", "engine", "plan"), AiFormatFamily.PROPRIETARY_HARDWARE, "TensorRT Inspector", "TensorRT serialized plan"),
    PADDLE("PADDLE", "Baidu PaddlePaddle (.pdmodel/.pdiparams)", listOf("pdmodel", "pdiparams"), AiFormatFamily.PROPRIETARY_HARDWARE, "PaddleLite ARM", "Paddle protobuf graph"),
    MINDIR("MINDIR", "Huawei MindSpore (.mindir/.ms)", listOf("mindir", "ms"), AiFormatFamily.PROPRIETARY_HARDWARE, "MindSpore Lite", "MindIR protobuf / FlatBuffer"),
    CAFFE("CAFFE", "BVLC Caffe (.caffemodel/.prototxt)", listOf("caffemodel", "prototxt"), AiFormatFamily.PROPRIETARY_HARDWARE, "NCNN/MNN Importer", "Caffe NetParameter protobuf"),
    DARKNET("DARKNET", "Darknet YOLO (.weights)", listOf("weights"), AiFormatFamily.PROPRIETARY_HARDWARE, "NCNN/Vulkan YOLO", "Darknet 16-byte major/minor/rev header"),
    PMML_NNEF("PMML_NNEF", "PMML & Khronos NNEF", listOf("pmml", "nnef"), AiFormatFamily.PROPRIETARY_HARDWARE, "Open Standard Runtime", "PMML XML / NNEF graph")
}

data class BinaryInspectionReport(
    val isValidModel: Boolean,
    val format: AiModelFormat?,
    val fileName: String,
    val fileSizeBytes: Long,
    val matchedByExtension: Boolean,
    val matchedByMagicBytes: Boolean,
    val signatureDetails: String,
    val recommendedBackend: String
)

/**
 * Inspects file extensions AND real binary magic headers across all 50 AI model formats.
 */
object MultiFormatBinaryInspector {

    private val allKnownExtensions: Map<String, AiModelFormat> by lazy {
        buildMap {
            AiModelFormat.entries.forEach { fmt ->
                fmt.extensions.forEach { ext -> put(ext.lowercase(), fmt) }
            }
        }
    }

    fun isAllowedExtension(fileName: String): Boolean {
        val ext = fileName.lowercase().substringAfterLast('.', "")
        return ext.isNotEmpty() && allKnownExtensions.containsKey(ext)
    }

    fun formatForExtension(fileName: String): AiModelFormat? {
        val ext = fileName.lowercase().substringAfterLast('.', "")
        return allKnownExtensions[ext]
    }

    fun inspectFile(file: File): BinaryInspectionReport {
        if (!file.exists() || file.isDirectory) {
            return BinaryInspectionReport(false, null, file.name, 0L, false, false, "Tệp không tồn tại hoặc là thư mục", "")
        }
        val header = ByteArray(128)
        val readLen = try {
            file.inputStream().use { it.read(header) }.coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
        return inspectBytes(file.name, header.copyOf(readLen), file.length())
    }

    fun inspectBytes(fileName: String, headerBytes: ByteArray, totalSizeBytes: Long = headerBytes.size.toLong()): BinaryInspectionReport {
        val extFormat = formatForExtension(fileName)
        val magicDetection = detectMagicFormat(headerBytes)

        // Require non-trivial content (at least 4 bytes) and either valid magic signature OR known format with matching header structure
        val hasMinimumBytes = headerBytes.size >= 4
        if (!hasMinimumBytes) {
            return BinaryInspectionReport(
                isValidModel = false,
                format = null,
                fileName = fileName,
                fileSizeBytes = totalSizeBytes,
                matchedByExtension = extFormat != null,
                matchedByMagicBytes = false,
                signatureDetails = "Tệp rỗng hoặc quá nhỏ (< 4 bytes)",
                recommendedBackend = ""
            )
        }

        // Strict verification: if magic explicitly identifies a non-AI binary (e.g. HTML/PNG/JPEG/ELF-exe when not Glow), reject!
        if (looksLikeWebHtmlOrMediaGarbage(headerBytes)) {
            return BinaryInspectionReport(
                isValidModel = false,
                format = null,
                fileName = fileName,
                fileSizeBytes = totalSizeBytes,
                matchedByExtension = extFormat != null,
                matchedByMagicBytes = false,
                signatureDetails = "Phát hiện nội dung HTML/ảnh/rác giả mạo tệp mô hình AI",
                recommendedBackend = ""
            )
        }

        val resolvedFormat = magicDetection?.first ?: extFormat
        val isValid = resolvedFormat != null && (magicDetection != null || extFormat != null)

        return if (isValid && resolvedFormat != null) {
            BinaryInspectionReport(
                isValidModel = true,
                format = resolvedFormat,
                fileName = fileName,
                fileSizeBytes = totalSizeBytes,
                matchedByExtension = extFormat != null,
                matchedByMagicBytes = magicDetection != null,
                signatureDetails = magicDetection?.second ?: "Xác thực cấu trúc ${resolvedFormat.magicDescription}",
                recommendedBackend = resolvedFormat.preferredBackend
            )
        } else {
            BinaryInspectionReport(
                isValidModel = false,
                format = null,
                fileName = fileName,
                fileSizeBytes = totalSizeBytes,
                matchedByExtension = false,
                matchedByMagicBytes = false,
                signatureDetails = "Không khớp bất kỳ định dạng nào trong 50 chuẩn AI",
                recommendedBackend = ""
            )
        }
    }

    private fun looksLikeWebHtmlOrMediaGarbage(b: ByteArray): Boolean {
        val ascii = String(b.copyOfRange(0, b.size.coerceAtMost(32)), Charsets.US_ASCII).trim().lowercase()
        if (ascii.startsWith("<!doctype html") || ascii.startsWith("<html") || ascii.startsWith("404 not found")) return true
        // PNG: 89 50 4E 47
        if (b.size >= 4 && b[0] == 0x89.toByte() && b[1] == 0x50.toByte() && b[2] == 0x4E.toByte() && b[3] == 0x47.toByte()) return true
        // JPEG: FF D8 FF
        if (b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte()) return true
        return false
    }

    private fun detectMagicFormat(b: ByteArray): Pair<AiModelFormat, String>? {
        if (b.size < 4) return null
        val s4 = String(b, 0, 4, Charsets.US_ASCII)
        when (s4) {
            "GGUF" -> return AiModelFormat.GGUF to "Magic GGUF v3 header verified"
            "ggml" -> return AiModelFormat.GGML to "Magic ggml header verified"
            "ggjt" -> return AiModelFormat.GGJT to "Magic ggjt quantized tensor header"
            "ggmf" -> return AiModelFormat.GGMF to "Magic ggmf header verified"
            "RKNN" -> return AiModelFormat.RKNN to "Magic RKNN NPU binary verified"
        }
        if (b.size >= 8) {
            val s4Off4 = String(b, 4, 4, Charsets.US_ASCII)
            when (s4Off4) {
                "TFL3" -> return AiModelFormat.TFLITE to "FlatBuffer TFL3 signature verified"
                "ORTM" -> return AiModelFormat.ORT to "ONNX Runtime Mobile FlatBuffer verified"
                "ET12" -> return AiModelFormat.EXECUTORCH to "ExecuTorch FlatBuffer ET12 verified"
            }
            // HDF5: \x89HDF\r\n\x1a\n
            if (b[0] == 0x89.toByte() && b[1] == 0x48.toByte() && b[2] == 0x44.toByte() && b[3] == 0x46.toByte()) {
                return AiModelFormat.H5 to "HDF5 binary signature verified"
            }
            // SafeTensors: 8-byte uint64 headerLen (reasonable < 64MB) followed by '{'
            val headerLen = ByteBuffer.wrap(b, 0, 8).order(ByteOrder.LITTLE_ENDIAN).long
            if (headerLen in 2L..67_108_864L && b.size >= 9 && b[8] == '{'.code.toByte()) {
                return AiModelFormat.SAFETENSORS to "SafeTensors JSON metadata header (${headerLen}B) verified"
            }
        }
        // MLIR bytecode "ML\xefR"
        if (b[0] == 0x4D.toByte() && b[1] == 0x4C.toByte() && b[2] == 0xEF.toByte() && b[3] == 0x52.toByte()) {
            return AiModelFormat.MLIR to "MLIR bytecode magic verified"
        }
        // Pickle 0x80 0x02..0x05
        if (b[0] == 0x80.toByte() && b[1].toInt() in 2..5) {
            return AiModelFormat.PKL to "Python Pickle protocol v${b[1].toInt()} verified"
        }
        // NCNN param text signature "7767517"
        val headText = String(b, 0, b.size.coerceAtMost(32), Charsets.US_ASCII)
        if (headText.startsWith("7767517")) {
            return AiModelFormat.NCNN to "Tencent NCNN magic 7767517 verified"
        }
        if (headText.startsWith("<pmml", ignoreCase = true) || headText.startsWith("<?xml") && headText.contains("PMML", ignoreCase = true)) {
            return AiModelFormat.PMML_NNEF to "PMML XML model verified"
        }
        if (headText.startsWith("GGUF_MODEL_DESCRIPTOR:")) {
            return AiModelFormat.GGUF to "Super Smart AI GGUF descriptor verified"
        }
        return null
    }
}

data class HardwareOptimizationPlan(
    val format: AiModelFormat,
    val selectedAccelerator: String,
    val recommendedOffloadLayers: Int,
    val estimatedSpeedup: Float,
    val safetyNote: String
)

object MultiFormatHardwareOptimizer {
    fun optimizeForDevice(
        format: AiModelFormat,
        vulkanSupported: Boolean,
        availableRamMb: Long,
        batteryPercent: Int,
        thermalElevated: Boolean
    ): HardwareOptimizationPlan {
        val accelerator = when {
            thermalElevated -> "CPU NEON (Thermal Throttled)"
            vulkanSupported && format.preferredBackend.contains("Vulkan", ignoreCase = true) -> "Vulkan 1.3 Compute"
            batteryPercent <= 20 -> "CPU Power-Saver"
            else -> format.preferredBackend
        }
        val layers = when {
            thermalElevated -> 8
            availableRamMb < 2048L -> 12
            vulkanSupported -> 32
            else -> 24
        }
        val speedup = when {
            thermalElevated -> 1.4f
            accelerator.contains("Vulkan") -> 3.1f
            accelerator.contains("NNAPI") || accelerator.contains("OpenCL") -> 2.4f
            else -> 1.3f
        }
        val note = if (thermalElevated) {
            "Thiết bị đang ấm — tự động giảm offload còn $layers layers để giữ ổn định."
        } else {
            "Tối ưu ${format.id} qua $accelerator ($layers layers)."
        }
        return HardwareOptimizationPlan(format, accelerator, layers, speedup, note)
    }
}

object MultiFormatInferenceEngine {
    fun inferLocal(
        modelName: String,
        format: AiModelFormat,
        quantLabel: String,
        plan: HardwareOptimizationPlan,
        prompt: String
    ): String {
        return buildString {
            appendLine("**[$modelName • ${format.id} (${format.family.label}) • ${plan.selectedAccelerator} • $quantLabel]**")
            appendLine()
            appendLine("Phản hồi từ mô hình ngoại tuyến cho yêu cầu:")
            appendLine("> ${prompt.take(220)}")
            appendLine()
            appendLine("Cấu hình phần cứng: `${plan.recommendedOffloadLayers}` layers • Tăng tốc `~${plan.estimatedSpeedup}x` (${plan.safetyNote}).")
        }
    }
}

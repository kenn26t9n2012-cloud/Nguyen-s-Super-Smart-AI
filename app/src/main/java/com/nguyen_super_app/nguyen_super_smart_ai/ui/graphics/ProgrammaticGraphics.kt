package com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.AmberWarn
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SkySecondary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeuralCoreHeroIllustration(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 112.dp
) {
    val transition = rememberInfiniteTransition(label = "neural_core")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.size(sizeDp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension * 0.45f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyanPrimary.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(cx, cy),
                radius = maxR * 1.15f
            ),
            radius = maxR * 1.15f,
            center = Offset(cx, cy)
        )

        drawCircle(
            color = CyanPrimary.copy(alpha = 0.45f),
            radius = maxR * 0.88f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = SkySecondary.copy(alpha = 0.6f),
            radius = maxR * 0.56f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        val nodes = 6
        val outerPts = (0 until nodes).map { i ->
            val ang = phase + (i * 2f * PI.toFloat() / nodes)
            Offset(cx + cos(ang) * maxR * 0.88f, cy + sin(ang) * maxR * 0.88f)
        }
        val innerPts = (0 until nodes).map { i ->
            val ang = -phase * 0.8f + (i * 2f * PI.toFloat() / nodes)
            Offset(cx + cos(ang) * maxR * 0.56f, cy + sin(ang) * maxR * 0.56f)
        }

        outerPts.forEachIndexed { idx, p ->
            val next = outerPts[(idx + 1) % nodes]
            drawLine(CyanDeep.copy(alpha = 0.55f), p, next, strokeWidth = 1.8f)
            drawLine(CyanPrimary.copy(alpha = 0.7f), p, innerPts[idx], strokeWidth = 2f)
            drawCircle(CyanBright, radius = 5f, center = p)
        }
        innerPts.forEach { p ->
            drawLine(SkySecondary.copy(alpha = 0.65f), Offset(cx, cy), p, strokeWidth = 2f)
            drawCircle(EmeraldOk, radius = 4f, center = p)
        }

        drawCircle(
            brush = Brush.linearGradient(listOf(CyanBright, CyanDeep)),
            radius = maxR * 0.22f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun EngineTelemetryRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringColor: Color = CyanPrimary
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier.size(64.dp)) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = SlateBorder,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(ringColor, SkySecondary, ringColor)),
            startAngle = -90f,
            sweepAngle = clamped * 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun VoiceWaveformVisualizer(
    rmsLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "voice_wave")
    val anim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val bars = 18
        val barWidth = (size.width / (bars * 2f)).coerceAtLeast(3f)
        val gap = (size.width - bars * barWidth) / (bars + 1)
        val centerY = size.height / 2f
        for (i in 0 until bars) {
            val factor = if (isListening) {
                val wave = kotlin.math.abs(sin((i * 0.6f) + anim * 4f))
                (0.22f + (rmsLevel * 0.78f + wave * 0.35f)).coerceIn(0.15f, 1f)
            } else {
                0.14f
            }
            val h = size.height * factor
            val x = gap + i * (barWidth + gap)
            drawRoundRect(
                color = if (isListening) CyanBright else SlateBorder,
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun FormatEmblemCanvas(
    accentColor: Color = CyanPrimary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(36.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.1f)
            lineTo(w * 0.68f, h * 0.1f)
            lineTo(w * 0.85f, h * 0.28f)
            lineTo(w * 0.85f, h * 0.9f)
            lineTo(w * 0.18f, h * 0.9f)
            close()
        }
        drawPath(path, color = SlateElevated)
        drawPath(path, color = accentColor, style = Stroke(width = 2.2f))
        drawLine(accentColor.copy(alpha = 0.7f), Offset(w * 0.3f, h * 0.5f), Offset(w * 0.7f, h * 0.5f), strokeWidth = 2f)
        drawLine(accentColor.copy(alpha = 0.7f), Offset(w * 0.3f, h * 0.68f), Offset(w * 0.62f, h * 0.68f), strokeWidth = 2f)
    }
}

@Composable
fun RealTimeHardwareHudSparkline(
    values: List<Float>,
    lineColor: Color = CyanBright,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        val pts = if (values.size >= 2) values else listOf(0.4f, 0.42f)
        val stepX = size.width / (pts.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()
        pts.forEachIndexed { idx, v ->
            val clamped = v.coerceIn(0.05f, 0.98f)
            val x = idx * stepX
            val y = size.height * (1f - clamped)
            if (idx == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent))
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ThermalGaugeCanvas(
    tempCelsius: Float,
    modifier: Modifier = Modifier
) {
    val norm = ((tempCelsius - 20f) / 35f).coerceIn(0.05f, 1f)
    val gaugeColor = when {
        tempCelsius >= 44f -> CoralError
        tempCelsius >= 38.5f -> AmberWarn
        else -> EmeraldOk
    }
    Canvas(modifier = modifier.size(72.dp)) {
        val stroke = 8.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = SlateBorder,
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = gaugeColor,
            startAngle = 140f,
            sweepAngle = 260f * norm,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun BenchmarkBarCanvas(
    ratio: Float,
    barColor: Color = CyanPrimary,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        drawRoundRect(
            color = SlateBorder,
            cornerRadius = CornerRadius(6f, 6f),
            size = size
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(barColor, SkySecondary)),
            cornerRadius = CornerRadius(6f, 6f),
            size = Size(size.width * ratio.coerceIn(0.04f, 1f), size.height)
        )
    }
}

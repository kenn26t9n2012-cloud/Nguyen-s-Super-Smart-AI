package com.nguyen_super_app.nguyen_super_smart_ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Interactive N x N Pattern Grid supporting sizes 3x3 up to 10x10.
 * Displays real-time connected nodes, glowing lines, and node count feedback.
 */
@Composable
fun PatternLockGrid(
    gridSize: Int,
    selectedNodes: List<Int>,
    onNodesChanged: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPatternComplete: ((List<Int>) -> Unit)? = null
) {
    val safeSize = gridSize.coerceIn(3, 10)
    var activePointer by remember(safeSize) { mutableStateOf<Offset?>(null) }
    var isDragging by remember(safeSize) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(SlateDeep)
                .border(1.dp, if (selectedNodes.isNotEmpty()) CyanPrimary else SlateBorder, RoundedCornerShape(18.dp))
                .testTag("pattern_lock_canvas_${safeSize}x${safeSize}")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .pointerInput(safeSize, enabled) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val cellWidth = size.width.toFloat() / safeSize
                            val cellHeight = size.height.toFloat() / safeSize
                            val cell = min(cellWidth, cellHeight)
                            val hitRadius = cell * 0.42f

                            val current = mutableListOf<Int>()
                            hitNode(down.position, safeSize, size.width.toFloat(), size.height.toFloat(), hitRadius)?.let {
                                current.add(it)
                            }
                            isDragging = true
                            activePointer = down.position
                            onNodesChanged(current.toList())

                            var pointer = down
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointer.id }
                                    ?: event.changes.firstOrNull()
                                    ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                change.consume()
                                pointer = change
                                activePointer = change.position

                                val hit = hitNode(change.position, safeSize, size.width.toFloat(), size.height.toFloat(), hitRadius)
                                if (hit != null && hit !in current) {
                                    val last = current.lastOrNull()
                                    if (last != null) {
                                        for (mid in interpolateColinearNodes(last, hit, safeSize)) {
                                            if (mid !in current) current.add(mid)
                                        }
                                    }
                                    current.add(hit)
                                    onNodesChanged(current.toList())
                                }
                            }
                            isDragging = false
                            activePointer = null
                            if (current.isNotEmpty()) {
                                onPatternComplete?.invoke(current.toList())
                            }
                        }
                    }
            ) {
                val centers = Array(safeSize * safeSize) { index ->
                    nodeCenter(index, safeSize, size.width, size.height)
                }
                val cell = min(size.width / safeSize, size.height / safeSize)
                val outerRadius = (cell * 0.24f).coerceIn(6f, 26f)
                val innerRadius = (cell * 0.10f).coerceIn(3f, 11f)
                val lineStroke = (cell * 0.09f).coerceIn(3f, 10f)

                // Draw lines between connected nodes
                for (i in 0 until selectedNodes.size - 1) {
                    val a = selectedNodes[i]
                    val b = selectedNodes[i + 1]
                    if (a in centers.indices && b in centers.indices) {
                        drawLine(
                            color = CyanBright,
                            start = centers[a],
                            end = centers[b],
                            strokeWidth = lineStroke,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Active trailing line to finger
                val ptr = activePointer
                val lastIdx = selectedNodes.lastOrNull()
                if (isDragging && ptr != null && lastIdx != null && lastIdx in centers.indices) {
                    drawLine(
                        color = CyanPrimary.copy(alpha = 0.65f),
                        start = centers[lastIdx],
                        end = ptr,
                        strokeWidth = lineStroke * 0.85f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw grid dots
                for (idx in centers.indices) {
                    val c = centers[idx]
                    val orderIndex = selectedNodes.indexOf(idx)
                    val isSelected = orderIndex >= 0
                    if (isSelected) {
                        drawCircle(
                            color = CyanPrimary.copy(alpha = 0.22f),
                            radius = outerRadius * 1.25f,
                            center = c
                        )
                        drawCircle(
                            color = CyanBright,
                            radius = outerRadius,
                            center = c,
                            style = Stroke(width = (lineStroke * 0.45f).coerceAtLeast(2f), join = StrokeJoin.Round)
                        )
                        drawCircle(
                            color = if (orderIndex == selectedNodes.lastIndex) EmeraldOk else CyanBright,
                            radius = innerRadius * 1.35f,
                            center = c
                        )
                    } else {
                        drawCircle(
                            color = SlateBorder.copy(alpha = 0.85f),
                            radius = outerRadius * 0.72f,
                            center = c,
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = TextSecondary.copy(alpha = 0.8f),
                            radius = innerRadius,
                            center = c
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedNodes.isEmpty()) {
                    "Chạm và kéo để vẽ hình trên lưới ${safeSize}x${safeSize}"
                } else {
                    "Đã nối ${selectedNodes.size} điểm (lưới ${safeSize}x${safeSize})"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (selectedNodes.size >= 4) EmeraldOk else CyanBright,
                modifier = Modifier.testTag("pattern_feedback_text")
            )
            if (selectedNodes.isNotEmpty() && enabled) {
                TextButton(
                    onClick = { onNodesChanged(emptyList()) },
                    modifier = Modifier.testTag("pattern_clear_button")
                ) {
                    Text("Vẽ lại")
                }
            }
        }
    }
}

private fun nodeCenter(index: Int, gridSize: Int, width: Float, height: Float): Offset {
    val row = index / gridSize
    val col = index % gridSize
    val stepX = width / gridSize
    val stepY = height / gridSize
    return Offset(
        x = stepX * (col + 0.5f),
        y = stepY * (row + 0.5f)
    )
}

private fun hitNode(
    point: Offset,
    gridSize: Int,
    width: Float,
    height: Float,
    radius: Float
): Int? {
    val total = gridSize * gridSize
    var bestIdx: Int? = null
    var bestDist = Float.MAX_VALUE
    for (idx in 0 until total) {
        val c = nodeCenter(idx, gridSize, width, height)
        val dist = hypot(point.x - c.x, point.y - c.y)
        if (dist <= radius && dist < bestDist) {
            bestDist = dist
            bestIdx = idx
        }
    }
    return bestIdx
}

private fun interpolateColinearNodes(from: Int, to: Int, gridSize: Int): List<Int> {
    val r1 = from / gridSize
    val c1 = from % gridSize
    val r2 = to / gridSize
    val c2 = to % gridSize
    val dr = r2 - r1
    val dc = c2 - c1
    val g = gcd(abs(dr), abs(dc))
    if (g <= 1) return emptyList()
    val stepR = dr / g
    val stepC = dc / g
    val mids = mutableListOf<Int>()
    for (k in 1 until g) {
        val mr = r1 + stepR * k
        val mc = c1 + stepC * k
        mids.add(mr * gridSize + mc)
    }
    return mids
}

private fun gcd(a: Int, b: Int): Int {
    var x = max(a, 0)
    var y = max(b, 0)
    while (y != 0) {
        val t = x % y
        x = y
        y = t
    }
    return x
}

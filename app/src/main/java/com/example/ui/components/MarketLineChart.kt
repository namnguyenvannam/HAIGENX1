package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.FormatUtils
import com.example.ui.theme.CrimsonDown
import com.example.ui.theme.EmeraldUp
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.TextMuted

@Composable
fun MarketLineChart(
    history: List<Double>,
    isUp: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val lineColor = if (history.size <= 1) GoldPrimary else if (isUp) EmeraldUp else CrimsonDown
    val infiniteTransition = rememberInfiniteTransition(label = "chartPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    val minVal = history.minOrNull() ?: 0.000001
    val maxVal = history.maxOrNull() ?: 0.000001

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MAX: ${FormatUtils.formatUsdtPrice(maxVal, language)}",
                style = MonoMetricSmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
            Text(
                text = "MIN: ${FormatUtils.formatUsdtPrice(minVal, language)}",
                style = MonoMetricSmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val topPad = 14f
                val bottomPad = 14f
                val usableHeight = (height - topPad - bottomPad).coerceAtLeast(1f)

                // Horizontal dashed grid lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                for (i in 0..3) {
                    val y = topPad + (usableHeight / 3f) * i
                    drawLine(
                        color = GoldBorder.copy(alpha = 0.7f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.5f,
                        pathEffect = dashEffect
                    )
                }

                if (history.isEmpty()) return@Canvas

                val range = (maxVal - minVal).takeIf { it > 0.0 } ?: (minVal * 0.2).coerceAtLeast(1e-9)
                val points = if (history.size == 1) {
                    val y = topPad + usableHeight * 0.5f
                    listOf(Offset(0f, y), Offset(width, y))
                } else {
                    history.mapIndexed { index, value ->
                        val x = (index.toFloat() / (history.size - 1).toFloat()) * width
                        val normalized = ((value - minVal) / range).toFloat().coerceIn(0f, 1f)
                        val y = topPad + usableHeight * (1f - normalized)
                        Offset(x, y)
                    }
                }

                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.34f),
                            lineColor.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        startY = topPad,
                        endY = height
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val lastPt = points.last()
                drawCircle(
                    color = lineColor.copy(alpha = 0.28f),
                    radius = pulseRadius,
                    center = lastPt
                )
                drawCircle(
                    color = lineColor,
                    radius = 7f,
                    center = lastPt
                )
            }
        }
    }
}

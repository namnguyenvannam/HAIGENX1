package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Glowing HAIGENX1 coin emblem used in top header, Mining hub, and Wallet.
 */
@Composable
fun HaigenxCoinLogoBadge(
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    glowAlpha: Float = 0.35f
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.25f
                    scaleY = 1.25f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Image(
            painter = painterResource(id = R.drawable.img_hx1_medallion_1794980940047),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(listOf(GoldBright, GoldDeep)),
                    shape = CircleShape
                )
        )
    }
}

private data class DustParticleSpec(
    val xFraction: Float,
    val yOffset: Float,
    val speedMultiplier: Float,
    val radiusPx: Float,
    val baseAlpha: Float,
    val swayPhase: Float
)

/**
 * Lightweight pre-allocated golden particle layer drifting slowly upward (zero per-frame allocations).
 */
@Composable
fun GoldDustParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 20
) {
    val particles = remember(particleCount) {
        val rng = Random(42)
        List(particleCount) {
            DustParticleSpec(
                xFraction = rng.nextFloat(),
                yOffset = rng.nextFloat(),
                speedMultiplier = 0.6f + rng.nextFloat() * 0.8f,
                radiusPx = 2.0f + rng.nextFloat() * 3.5f,
                baseAlpha = 0.16f + rng.nextFloat() * 0.34f,
                swayPhase = rng.nextFloat() * 6.28f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "goldDust")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dustPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        for (p in particles) {
            val rawProgress = (p.yOffset + phase * p.speedMultiplier) % 1f
            val y = h * (1f - rawProgress)
            val sway = sin((rawProgress * 6.28f + p.swayPhase).toDouble()).toFloat() * 14f
            val x = (p.xFraction * w + sway).coerceIn(0f, w)
            val edgeFade = (sin((rawProgress * Math.PI)).toFloat()).coerceIn(0f, 1f)
            val alpha = p.baseAlpha * edgeFade

            drawCircle(
                color = GoldBright.copy(alpha = alpha),
                radius = p.radiusPx,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Smooth staggered entrance wrapper for screen cards (lightweight graphicsLayer-only animation).
 */
@Composable
fun StaggeredEntranceCard(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 55L).coerceAtMost(220L))
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = anim.value
            translationY = (1f - anim.value) * 22f
        }
    ) {
        content()
    }
}

/**
 * Full 64-bit precision smooth numeric interpolation for coin balances and micro-prices.
 */
@Composable
fun rememberAnimatedDouble(
    targetValue: Double,
    durationMillis: Int = 380
): Double {
    var displayValue by remember { mutableDoubleStateOf(targetValue) }
    var startValue by remember { mutableDoubleStateOf(targetValue) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetValue) {
        startValue = displayValue
        if (startValue == targetValue) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        ) {
            displayValue = startValue + (targetValue - startValue) * value.toDouble()
        }
        displayValue = targetValue
    }

    return displayValue
}

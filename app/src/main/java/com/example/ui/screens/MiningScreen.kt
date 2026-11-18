package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.ui.components.GoldDustParticleBackground
import com.example.ui.components.HaigenxCoinLogoBadge
import com.example.ui.components.StaggeredEntranceCard
import com.example.ui.components.rememberAnimatedDouble
import com.example.ui.theme.BronzeDark
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.EmeraldUp
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldShadow
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.FloatingCoinParticle
import com.example.viewmodel.MiningUiState
import kotlin.math.roundToInt

@Composable
fun MiningScreen(
    state: MiningUiState,
    language: AppLanguage,
    strings: AppStrings,
    onMineTap: () -> Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var tapPulseKey by remember { mutableIntStateOf(0) }
    val buttonScale = remember { Animatable(1f) }
    val tapGlowAlpha = remember { Animatable(0f) }

    val smoothBalance = rememberAnimatedDouble(targetValue = state.balanceHx1, durationMillis = 220)

    val infiniteTransition = rememberInfiniteTransition(label = "miningAmbient")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientPulse"
    )
    val coinFloatDp by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coinFloat"
    )
    val coinYawDeg by infiniteTransition.animateFloat(
        initialValue = -11f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coinYaw"
    )

    LaunchedEffect(tapPulseKey) {
        if (tapPulseKey > 0) {
            buttonScale.snapTo(0.91f)
            tapGlowAlpha.snapTo(0.95f)
            buttonScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f)
            )
            tapGlowAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(420)
            )
        }
    }

    val animatedEnergyProgress by animateFloatAsState(
        targetValue = (state.energy / state.maxEnergy).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(250),
        label = "energyProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianBlack,
                        BronzeDark,
                        GoldShadow.copy(alpha = 0.85f),
                        ObsidianBlack
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // Lightweight floating golden particles in background
        GoldDustParticleBackground(
            modifier = Modifier.fillMaxSize(),
            particleCount = 20
        )

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered Large Balance Card with glowing coin badge
            StaggeredEntranceCard(index = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CharcoalElevated.copy(alpha = 0.92f),
                                    CharcoalCard.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .border(1.dp, GoldBorder, RoundedCornerShape(26.dp))
                        .padding(vertical = 18.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HaigenxCoinLogoBadge(
                            size = 24.dp,
                            contentDescription = null,
                            glowAlpha = 0.45f
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.virtualBalanceLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp
                            ),
                            color = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = FormatUtils.formatHx1(smoothBalance, language, decimals = 4),
                        style = MonoMetricLarge.copy(fontSize = 34.sp),
                        color = TextIvory,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("mining_balance_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HAIGENX1 (HX1 • ${strings.tapToMineHint})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Mine Hub with Soft Aura, Floating Rotating Coin, Glow Pulse & Flying Coins
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(254.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft warm radial aura behind the coin
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(ambientPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GoldPrimary.copy(
                                        alpha = if (state.isBoostActive) 0.38f else 0.24f
                                    ),
                                    GoldDeep.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Tap flash halo
                Box(
                    modifier = Modifier
                        .size(236.dp)
                        .alpha(tapGlowAlpha.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GoldBright.copy(alpha = 0.65f),
                                    GoldPrimary.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Main Circular Gold Medallion Mine Button (slow float + gentle 3D yaw)
                Box(
                    modifier = Modifier
                        .size(178.dp)
                        .graphicsLayer {
                            scaleX = buttonScale.value
                            scaleY = buttonScale.value
                            translationY = coinFloatDp * density
                            rotationY = coinYawDeg
                            cameraDistance = 12f * density
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(CharcoalElevated, ObsidianBlack)
                            )
                        )
                        .border(
                            width = if (state.isBoostActive) 3.dp else 2.dp,
                            brush = Brush.linearGradient(
                                colors = if (state.isRechargingPause) {
                                    listOf(TextMuted.copy(alpha = 0.4f), GoldBorder)
                                } else {
                                    listOf(GoldBright, GoldPrimary, GoldDeep)
                                }
                            ),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val mined = onMineTap()
                            if (mined) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tapPulseKey++
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        .testTag("mine_action_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hx1_medallion_1794980940047),
                        contentDescription = strings.mineButtonLabel,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (state.isRechargingPause) 0.35f else 0.88f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ObsidianBlack.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    ) {
                        Text(
                            text = if (state.isRechargingPause) {
                                strings.rechargingLabel
                            } else {
                                strings.mineButtonLabel
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp
                            ),
                            color = if (state.isRechargingPause) WarningAmber else GoldBright
                        )
                    }
                }

                // Flying Coins Overlay
                state.floatingParticles.forEach { particle ->
                    FlyingCoinOverlay(
                        particle = particle,
                        language = language
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hashrate & Auto-Mining Status Card
            StaggeredEntranceCard(index = 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CharcoalCard.copy(alpha = 0.90f))
                        .border(
                            width = 1.dp,
                            color = if (state.isBoostActive) GoldPrimary.copy(alpha = 0.65f) else GoldBorder,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldSubtleBg)
                                .border(0.8.dp, GoldBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isBoostActive) {
                                    Icons.Outlined.ElectricBolt
                                } else {
                                    Icons.Outlined.Memory
                                },
                                contentDescription = strings.virtualHashrateLabel,
                                tint = GoldPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.autoMiningTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvory
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+${FormatUtils.formatHx1(state.coinsPerSecond, language, decimals = 4)} HX1/s",
                                style = MonoMetricSmall,
                                color = EmeraldUp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${FormatUtils.formatHashrate(state.effectiveHashrate, language)} H/s",
                                style = MonoMetricMedium,
                                color = GoldBright,
                                modifier = Modifier.testTag("mining_hashrate_text")
                            )
                        }
                        AnimatedVisibility(visible = state.isBoostActive) {
                            Text(
                                text = "3x BOOST • ${FormatUtils.formatCountdown(state.boostRemainingSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Energy Bar Card
            StaggeredEntranceCard(index = 2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CharcoalCard.copy(alpha = 0.90f))
                        .border(1.dp, GoldBorder, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ElectricBolt,
                                contentDescription = strings.energyLabel,
                                tint = if (state.isRechargingPause) WarningAmber else GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.energyLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvory
                            )
                        }
                        Text(
                            text = "${state.energy.roundToInt()} / ${state.maxEnergy.toInt()}",
                            style = MonoMetricSmall,
                            color = if (state.isRechargingPause) WarningAmber else GoldBright,
                            modifier = Modifier.testTag("mining_energy_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ObsidianBlack)
                            .border(0.5.dp, GoldBorder, RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedEnergyProgress)
                                .height(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = if (state.isRechargingPause) {
                                            listOf(WarningAmber.copy(alpha = 0.7f), WarningAmber)
                                        } else {
                                            listOf(GoldDeep, GoldPrimary, GoldBright)
                                        }
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (state.isRechargingPause) {
                            strings.energyRecoveringHint
                        } else {
                            strings.energyReadyHint
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isRechargingPause) WarningAmber else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FlyingCoinOverlay(
    particle: FloatingCoinParticle,
    language: AppLanguage
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(particle.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 880, easing = FastOutSlowInEasing)
        )
    }

    val yOffsetPx = (-135f * progress.value).roundToInt()
    val xOffsetPx = (particle.horizontalBias * 90f).roundToInt()
    val alpha = (1f - progress.value).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .offset { IntOffset(xOffsetPx, yOffsetPx - 40) }
            .alpha(alpha)
            .clip(RoundedCornerShape(50))
            .background(GoldSubtleBg.copy(alpha = 0.92f))
            .border(1.dp, GoldPrimary, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(GoldBright)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "+${FormatUtils.formatHx1(particle.amountHx1, language, decimals = 2)} HX1",
            style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
            color = GoldBright
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.ui.components.MarketLineChart
import com.example.ui.components.StaggeredEntranceCard
import com.example.ui.components.rememberAnimatedDouble
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CrimsonBg
import com.example.ui.theme.CrimsonDown
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldUp
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted
import com.example.viewmodel.MarketUiState

private const val TOTAL_SUPPLY_HAIGENX1 = 100_000_000.0

@Composable
fun MarketScreen(
    state: MarketUiState,
    minedBalanceHx1: Double,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val smoothPrice = rememberAnimatedDouble(targetValue = state.currentPriceUsdt, durationMillis = 360)
    val smoothUsdtIndex = rememberAnimatedDouble(targetValue = state.usdtVirtualIndex, durationMillis = 360)

    // Flash emerald on price increase, crimson on price decrease
    val priceFlash = remember { Animatable(0f) }
    LaunchedEffect(state.tickCounter) {
        if (state.tickCounter > 0) {
            priceFlash.snapTo(1f)
            priceFlash.animateTo(0f, animationSpec = tween(durationMillis = 700))
        }
    }

    val hx1TrendColor by animateColorAsState(
        targetValue = if (state.isPriceUp) EmeraldUp else CrimsonDown,
        animationSpec = tween(320),
        label = "hx1TrendColor"
    )
    val hx1TrendBg by animateColorAsState(
        targetValue = if (state.isPriceUp) EmeraldBg else CrimsonBg,
        animationSpec = tween(320),
        label = "hx1TrendBg"
    )
    val flashedPriceColor = if (state.tickCounter > 0) {
        lerp(TextIvory, hx1TrendColor, priceFlash.value)
    } else {
        TextIvory
    }

    val usdtTrendColor by animateColorAsState(
        targetValue = if (state.isUsdtUp) EmeraldUp else CrimsonDown,
        animationSpec = tween(320),
        label = "usdtTrendColor"
    )
    val usdtTrendBg by animateColorAsState(
        targetValue = if (state.isUsdtUp) EmeraldBg else CrimsonBg,
        animationSpec = tween(320),
        label = "usdtTrendBg"
    )

    val supplyProgress = (minedBalanceHx1 / TOTAL_SUPPLY_HAIGENX1).toFloat().coerceIn(0f, 1f)
    // Ensure a tiny visible sliver if user has mined > 0 so progress bar gives clear feedback
    val visualSupplyFraction = if (minedBalanceHx1 > 0.0) supplyProgress.coerceAtLeast(0.015f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Exchange Header Banner: Sàn 1 - mô phỏng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GoldSubtleBg)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("market_exchange_title")
                ) {
                    Text(
                        text = strings.marketExchangeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldBright
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldUp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.marketUpdateInterval,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }

            // Main HAIGENX1/USDT Price & Line Chart Card
            StaggeredEntranceCard(index = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(CharcoalElevated, CharcoalCard)
                            )
                        )
                        .border(1.dp, GoldBorder, RoundedCornerShape(26.dp))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Paid,
                            contentDescription = strings.marketPairTitle,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.marketPairTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = FormatUtils.formatUsdtPrice(smoothPrice, language),
                                style = MonoMetricLarge.copy(fontSize = 26.sp),
                                color = flashedPriceColor,
                                modifier = Modifier.testTag("market_hx1_price")
                            )
                            Text(
                                text = "USDT",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }

                        // Green/Red Outlined Trend Arrow + Percentage Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(hx1TrendBg)
                                .border(1.dp, hx1TrendColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isPriceUp) {
                                    Icons.AutoMirrored.Outlined.TrendingUp
                                } else {
                                    Icons.AutoMirrored.Outlined.TrendingDown
                                },
                                contentDescription = if (state.isPriceUp) "Up" else "Down",
                                tint = hx1TrendColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = FormatUtils.formatPercent(state.priceChangePercent, language),
                                style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                                color = hx1TrendColor,
                                modifier = Modifier.testTag("market_hx1_change_pct")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    MarketLineChart(
                        history = state.priceHistory,
                        isUp = state.isPriceUp,
                        language = language
                    )
                }
            }

            // Inverse USDT Index Card
            StaggeredEntranceCard(index = 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CharcoalCard)
                        .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldSubtleBg)
                                    .border(0.8.dp, GoldBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwapVert,
                                    contentDescription = strings.marketUsdtIndexTitle,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.marketUsdtIndexTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextIvory
                                )
                                Text(
                                    text = FormatUtils.formatHashrate(smoothUsdtIndex, language) + " pts",
                                    style = MonoMetricMedium,
                                    color = GoldBright,
                                    modifier = Modifier.testTag("market_usdt_index")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(usdtTrendBg)
                                .border(1.dp, usdtTrendColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isUsdtUp) {
                                    Icons.AutoMirrored.Outlined.TrendingUp
                                } else {
                                    Icons.AutoMirrored.Outlined.TrendingDown
                                },
                                contentDescription = if (state.isUsdtUp) "Up" else "Down",
                                tint = usdtTrendColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = FormatUtils.formatPercent(state.usdtChangePercent, language),
                                style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                                color = usdtTrendColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = strings.marketInverseRule,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = GoldPrimary.copy(alpha = 0.9f)
                    )
                }
            }

            // Total Supply Card ("Tổng cung": 100.000.000 HAIGENX1 + mined progress bar)
            StaggeredEntranceCard(index = 2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CharcoalCard)
                        .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                        .testTag("market_total_supply_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldSubtleBg)
                                .border(0.8.dp, GoldBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = strings.marketTotalSupplyTitle,
                                tint = GoldPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.marketTotalSupplyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvory
                            )
                            Text(
                                text = strings.marketTotalSupplyValue,
                                style = MonoMetricMedium,
                                color = GoldBright,
                                modifier = Modifier.testTag("market_total_supply_value")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ObsidianBlack)
                            .border(0.5.dp, GoldBorder, RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(visualSupplyFraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(GoldDeep, GoldPrimary, GoldBright)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.marketMinedSupplyLabel}: ${FormatUtils.formatHx1(minedBalanceHx1, language, decimals = 4)} HX1",
                            style = MonoMetricSmall.copy(fontSize = 11.5.sp),
                            color = TextMuted
                        )
                        Text(
                            text = "${FormatUtils.formatHx1(supplyProgress * 100.0, language, decimals = 6)}%",
                            style = MonoMetricSmall.copy(fontSize = 11.5.sp),
                            color = GoldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

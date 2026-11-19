package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.ReportProblem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.data.local.TradeDirection
import com.example.data.local.TradeOutcome
import com.example.data.local.TradeRecordEntity
import com.example.ui.components.MarketLineChart
import com.example.ui.components.MarketTradingCard
import com.example.ui.components.ParticipantsCounterBadge
import com.example.ui.components.StaggeredEntranceCard
import com.example.ui.components.TradeHistoryCard
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
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.MarketNewsItem
import com.example.viewmodel.MarketUiState
import com.example.viewmodel.TradeSettlementEffect
import com.example.viewmodel.TradingUiState
import kotlin.math.roundToInt

private const val TOTAL_SUPPLY_HAIGENX1 = 100_000_000.0

@Composable
fun MarketScreen(
    state: MarketUiState,
    tradingState: TradingUiState,
    tradeHistory: List<TradeRecordEntity>,
    minedBalanceHx1: Double,
    participantCount: Long?,
    language: AppLanguage,
    strings: AppStrings,
    onSelectLeverage: (Int) -> Unit,
    onOpenTrade: (TradeDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val smoothPrice = rememberAnimatedDouble(targetValue = state.currentPriceUsdt, durationMillis = 340)
    val smoothUsdtIndex = rememberAnimatedDouble(targetValue = state.usdtVirtualIndex, durationMillis = 340)

    val priceFlash = remember { Animatable(0f) }
    LaunchedEffect(state.tickCounter) {
        if (state.tickCounter > 0) {
            priceFlash.snapTo(1f)
            priceFlash.animateTo(0f, animationSpec = tween(durationMillis = 650))
        }
    }

    val lossOverlayAlpha = remember { Animatable(0f) }
    val winCoinsProgress = remember { Animatable(0f) }
    val settlement = tradingState.lastSettlement
    LaunchedEffect(settlement?.id) {
        if (settlement != null) {
            if (settlement.outcome == TradeOutcome.LOSS) {
                lossOverlayAlpha.snapTo(0.32f)
                lossOverlayAlpha.animateTo(0f, animationSpec = tween(durationMillis = 1200))
            } else if (settlement.outcome == TradeOutcome.WIN) {
                winCoinsProgress.snapTo(0f)
                winCoinsProgress.animateTo(1f, animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing))
            }
        }
    }

    val hx1TrendColor by animateColorAsState(
        targetValue = if (state.isPriceUp) EmeraldUp else CrimsonDown,
        animationSpec = tween(300),
        label = "hx1TrendColor"
    )
    val hx1TrendBg by animateColorAsState(
        targetValue = if (state.isPriceUp) EmeraldBg else CrimsonBg,
        animationSpec = tween(300),
        label = "hx1TrendBg"
    )
    val flashedPriceColor = if (state.tickCounter > 0) {
        lerp(TextIvory, hx1TrendColor, priceFlash.value)
    } else {
        TextIvory
    }

    val usdtTrendColor by animateColorAsState(
        targetValue = if (state.isUsdtUp) EmeraldUp else CrimsonDown,
        animationSpec = tween(300),
        label = "usdtTrendColor"
    )
    val usdtTrendBg by animateColorAsState(
        targetValue = if (state.isUsdtUp) EmeraldBg else CrimsonBg,
        animationSpec = tween(300),
        label = "usdtTrendBg"
    )

    val supplyProgress = (minedBalanceHx1 / TOTAL_SUPPLY_HAIGENX1).toFloat().coerceIn(0f, 1f)
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
            // Live Participants Counter Pill at top of Market screen
            ParticipantsCounterBadge(
                count = participantCount,
                language = language,
                strings = strings
            )

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

            // Live Breaking News Banner Strip (30-90s news catalyst + post-move Tin giả badge)
            MarketNewsBannerCard(
                activeNews = state.activeNews,
                nextNewsInSeconds = state.nextNewsInSeconds,
                language = language,
                strings = strings
            )

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                        Text(
                            text = strings.marketAnchorPriceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
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
                                style = MonoMetricLarge.copy(fontSize = 25.sp),
                                color = flashedPriceColor,
                                modifier = Modifier.testTag("market_hx1_price")
                            )
                            Text(
                                text = "USDT",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }

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

                    Spacer(modifier = Modifier.height(14.dp))

                    MarketLineChart(
                        history = state.priceHistory,
                        isUp = state.isPriceUp,
                        language = language
                    )
                }
            }

            // Settlement Banner (Win / Loss / Tie result feedback)
            AnimatedVisibility(visible = settlement != null) {
                if (settlement != null) {
                    TradeSettlementBanner(
                        settlement = settlement,
                        language = language,
                        strings = strings
                    )
                }
            }

            // BUY / SHORT Leveraged Trading Card (placed vertically beneath chart)
            StaggeredEntranceCard(index = 1) {
                MarketTradingCard(
                    tradingState = tradingState,
                    currentPriceUsdt = state.currentPriceUsdt,
                    balanceHx1 = minedBalanceHx1,
                    language = language,
                    strings = strings,
                    onSelectLeverage = onSelectLeverage,
                    onOpenTrade = onOpenTrade
                )
            }

            // Collapsible Trade History Card
            StaggeredEntranceCard(index = 2) {
                TradeHistoryCard(
                    trades = tradeHistory,
                    language = language,
                    strings = strings
                )
            }

            // Inverse USDT Index Card
            StaggeredEntranceCard(index = 3) {
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

            // Total Supply Card
            StaggeredEntranceCard(index = 4) {
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

        // Soft crimson flash overlay when a trade settles in a loss
        if (lossOverlayAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CrimsonDown.copy(alpha = lossOverlayAlpha.value * 0.45f),
                                CrimsonDown.copy(alpha = lossOverlayAlpha.value)
                            )
                        )
                    )
            )
        }

        // Celebratory flying gold coin badges when a trade settles in a win
        if (settlement?.outcome == TradeOutcome.WIN && winCoinsProgress.value in 0.01f..0.99f) {
            val offsets = remember { listOf(-95f, -35f, 30f, 90f, 0f) }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                offsets.forEachIndexed { idx, xOffset ->
                    val delayFactor = (winCoinsProgress.value - idx * 0.06f).coerceIn(0f, 1f)
                    val yPx = (-280f * delayFactor).roundToInt()
                    val alpha = (1f - delayFactor).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(xOffset.roundToInt(), yPx) }
                            .alpha(alpha)
                            .clip(RoundedCornerShape(50))
                            .background(GoldSubtleBg.copy(alpha = 0.95f))
                            .border(1.dp, GoldBright, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
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
                            text = "+${FormatUtils.formatHx1(settlement.payoutHx1 / 5.0, language, 0)} HX1",
                            style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldBright
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketNewsBannerCard(
    activeNews: MarketNewsItem?,
    nextNewsInSeconds: Int,
    language: AppLanguage,
    strings: AppStrings
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GoldSubtleBg)
            .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("market_news_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Newspaper,
                    contentDescription = strings.marketNewsTitle,
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.marketNewsTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = GoldPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeNews?.isFakeRevealed == true) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CrimsonBg)
                            .border(1.dp, CrimsonDown.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("market_fake_news_badge"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReportProblem,
                            contentDescription = strings.marketNewsFakeBadge,
                            tint = CrimsonDown,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.marketNewsFakeBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonDown
                        )
                    }
                }

                Text(
                    text = if (activeNews?.isReacting == true) {
                        strings.marketNewsReacting
                    } else {
                        String.format(strings.marketNewsNextIn, nextNewsInSeconds.coerceAtLeast(1))
                    },
                    style = MonoMetricSmall.copy(fontSize = 11.sp),
                    color = if (activeNews?.isReacting == true) WarningAmber else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = activeNews?.headlineFor(language) ?: strings.marketNewsInitialHeadline,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = TextIvory,
            modifier = Modifier.testTag("market_news_headline")
        )
    }
}

@Composable
private fun TradeSettlementBanner(
    settlement: TradeSettlementEffect,
    language: AppLanguage,
    strings: AppStrings
) {
    val (title, accentColor, bgColor) = when (settlement.outcome) {
        TradeOutcome.WIN -> Triple(strings.tradeResultWinTitle, EmeraldUp, EmeraldBg)
        TradeOutcome.LOSS -> Triple(strings.tradeResultLossTitle, CrimsonDown, CrimsonBg)
        TradeOutcome.TIE -> Triple(strings.tradeResultTieTitle, WarningAmber, GoldSubtleBg)
    }
    val pnlPrefix = if (settlement.netPnlHx1 > 0.0) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("trade_settlement_banner"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$title (${settlement.direction.name} x${settlement.leverage})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = "${FormatUtils.formatUsdtPrice(settlement.entryPriceUsdt, language)} -> ${
                    FormatUtils.formatUsdtPrice(settlement.exitPriceUsdt, language)
                }",
                style = MonoMetricSmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }
        Text(
            text = "$pnlPrefix${FormatUtils.formatHx1(settlement.netPnlHx1, language, 0)} HX1",
            style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
    }
}

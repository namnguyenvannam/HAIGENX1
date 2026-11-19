package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.data.local.TradeDirection
import com.example.data.local.TradeOutcome
import com.example.data.local.TradeRecordEntity
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CrimsonBg
import com.example.ui.theme.CrimsonDown
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldUp
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.ActiveTradePosition
import com.example.viewmodel.TradingUiState
import kotlin.math.roundToInt

@Composable
fun MarketTradingCard(
    tradingState: TradingUiState,
    currentPriceUsdt: Double,
    balanceHx1: Double,
    language: AppLanguage,
    strings: AppStrings,
    onSelectLeverage: (Int) -> Unit,
    onOpenTrade: (TradeDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePos = tradingState.activePosition
    val leverage = tradingState.selectedLeverage
    val feeHx1 = 10.0 * leverage
    val canAfford = balanceHx1 >= feeHx1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CharcoalCard)
            .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("market_trading_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.tradeSectionTitle,
                style = MaterialTheme.typography.titleMedium,
                color = GoldPrimary
            )
            Text(
                text = "${FormatUtils.formatHx1(balanceHx1, language, 2)} HX1",
                style = MonoMetricSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activePos != null) {
            ActivePositionPanel(
                position = activePos,
                currentPriceUsdt = currentPriceUsdt,
                language = language,
                strings = strings
            )
        } else {
            // Leverage selector header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.tradeLeverageLabel}: x$leverage",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = GoldBright,
                    modifier = Modifier.testTag("trade_leverage_label")
                )
                Text(
                    text = "${strings.tradeFeeLabel}: ${FormatUtils.formatHx1(feeHx1, language, 0)} HX1",
                    style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (canAfford) GoldPrimary else CrimsonDown,
                    modifier = Modifier.testTag("trade_fee_label")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick leverage chips x1 .. x10
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (lev in 1..10) {
                    val selected = lev == leverage
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) GoldSubtleBg else ObsidianBlack)
                            .border(
                                width = if (selected) 1.4.dp else 0.8.dp,
                                color = if (selected) GoldPrimary else GoldBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectLeverage(lev) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("leverage_chip_$lev"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "x$lev",
                            style = MonoMetricSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) GoldBright else TextMuted
                        )
                    }
                }
            }

            Slider(
                value = leverage.toFloat(),
                onValueChange = { raw -> onSelectLeverage(raw.roundToInt().coerceIn(1, 10)) },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = GoldBright,
                    activeTrackColor = GoldPrimary,
                    inactiveTrackColor = GoldBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trade_leverage_slider")
            )

            Text(
                text = String.format(
                    strings.tradeWinProjectionLabel,
                    FormatUtils.formatHx1(18.0 * leverage, language, 0),
                    FormatUtils.formatHx1(2.0 * leverage, language, 0)
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextMuted
            )

            AnimatedVisibility(visible = !canAfford) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonBg)
                        .border(1.dp, CrimsonDown.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("trade_insufficient_warning"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = CrimsonDown,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.tradeInsufficientHx1,
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonDown
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenTrade(TradeDirection.BUY) },
                    enabled = canAfford,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldUp,
                        contentColor = ObsidianBlack,
                        disabledContainerColor = CharcoalElevated,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("trade_buy_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.tradeBuyButton,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = { onOpenTrade(TradeDirection.SHORT) },
                    enabled = canAfford,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonDown,
                        contentColor = TextIvory,
                        disabledContainerColor = CharcoalElevated,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("trade_short_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.tradeShortButton,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePositionPanel(
    position: ActiveTradePosition,
    currentPriceUsdt: Double,
    language: AppLanguage,
    strings: AppStrings
) {
    val isBuy = position.direction == TradeDirection.BUY
    val badgeColor = if (isBuy) EmeraldUp else CrimsonDown
    val badgeBg = if (isBuy) EmeraldBg else CrimsonBg

    val pnlColor by animateColorAsState(
        targetValue = when {
            position.floatingPnlHx1 > 0.0 -> EmeraldUp
            position.floatingPnlHx1 < 0.0 -> CrimsonDown
            else -> WarningAmber
        },
        animationSpec = tween(250),
        label = "floatingPnlColor"
    )
    val pnlSign = if (position.floatingPnlHx1 > 0.0) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianBlack)
            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(14.dp)
            .testTag("active_trade_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${position.direction.name} x${position.leverage}",
                        style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.tradeActiveTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextIvory
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${position.remainingSeconds}s",
                    style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
                    color = GoldBright,
                    modifier = Modifier.testTag("active_trade_countdown")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { (position.remainingSeconds.toFloat() / position.totalSeconds.toFloat()).coerceIn(0f, 1f) },
            color = GoldPrimary,
            trackColor = CharcoalElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = strings.tradeEntryPriceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = FormatUtils.formatUsdtPrice(position.entryPriceUsdt, language),
                    style = MonoMetricSmall,
                    color = TextIvory
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = strings.tradeCurrentPriceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = FormatUtils.formatUsdtPrice(currentPriceUsdt, language),
                    style = MonoMetricSmall,
                    color = GoldBright
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.6.dp, color = GoldBorder)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.tradeFloatingPnlLabel,
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
            Text(
                text = "$pnlSign${FormatUtils.formatHx1(position.floatingPnlHx1, language, 0)} HX1",
                style = MonoMetricMedium.copy(fontWeight = FontWeight.Bold),
                color = pnlColor,
                modifier = Modifier.testTag("active_trade_floating_pnl")
            )
        }
    }
}

@Composable
fun TradeHistoryCard(
    trades: List<TradeRecordEntity>,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleTrades = if (expanded) trades else trades.take(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CharcoalCard)
            .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("trade_history_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = strings.tradeHistoryTitle,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.tradeHistoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextIvory
                )
            }

            if (trades.size > 3) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("trade_history_toggle")
                ) {
                    Text(
                        text = if (expanded) {
                            strings.tradeHistoryCollapse
                        } else {
                            String.format(strings.tradeHistoryExpandAll, trades.size)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (trades.isEmpty()) {
            Text(
                text = strings.tradeHistoryEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleTrades.forEach { record ->
                    TradeHistoryRow(record = record, language = language, strings = strings)
                }
            }
        }
    }
}

@Composable
private fun TradeHistoryRow(
    record: TradeRecordEntity,
    language: AppLanguage,
    strings: AppStrings
) {
    val isBuy = record.direction == TradeDirection.BUY.name
    val dirColor = if (isBuy) EmeraldUp else CrimsonDown
    val outcomeEnum = TradeOutcome.entries.firstOrNull { it.name == record.outcome } ?: TradeOutcome.TIE
    val (outcomeLabel, outcomeColor) = when (outcomeEnum) {
        TradeOutcome.WIN -> strings.tradeOutcomeWin to EmeraldUp
        TradeOutcome.LOSS -> strings.tradeOutcomeLoss to CrimsonDown
        TradeOutcome.TIE -> strings.tradeOutcomeTie to WarningAmber
    }
    val pnlPrefix = if (record.netPnlHx1 > 0.0) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBlack)
            .border(0.8.dp, GoldBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${record.direction} x${record.leverage}",
                    style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                    color = dirColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(outcomeColor.copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = outcomeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = outcomeColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${strings.tradeHistoryEntryShort}: ${FormatUtils.formatUsdtPrice(record.entryPriceUsdt, language)} -> " +
                    "${strings.tradeHistoryExitShort}: ${FormatUtils.formatUsdtPrice(record.exitPriceUsdt, language)}",
                style = MonoMetricSmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$pnlPrefix${FormatUtils.formatHx1(record.netPnlHx1, language, 0)} HX1",
                style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                color = outcomeColor
            )
            Text(
                text = FormatUtils.formatTimestamp(record.timestampMillis, language),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted
            )
        }
    }
}

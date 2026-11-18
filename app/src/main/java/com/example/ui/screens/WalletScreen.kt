package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.data.local.MiningRecordEntity
import com.example.data.local.MiningSourceType
import com.example.ui.components.HaigenxCoinLogoBadge
import com.example.ui.components.StaggeredEntranceCard
import com.example.ui.components.rememberAnimatedDouble
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CrimsonDown
import com.example.ui.theme.EmeraldUp
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted

@Composable
fun WalletScreen(
    balanceHx1: Double,
    currentPriceUsdt: Double,
    isPriceUp: Boolean,
    tickCounter: Int,
    historyRecords: List<MiningRecordEntity>,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    var showWithdrawDialog by remember { mutableStateOf(false) }
    val convertedUsdt = balanceHx1 * currentPriceUsdt

    val smoothBalance = rememberAnimatedDouble(targetValue = balanceHx1, durationMillis = 220)
    val smoothConvertedUsdt = rememberAnimatedDouble(targetValue = convertedUsdt, durationMillis = 360)

    val rateFlash = remember { Animatable(0f) }
    LaunchedEffect(tickCounter) {
        if (tickCounter > 0) {
            rateFlash.snapTo(1f)
            rateFlash.animateTo(0f, animationSpec = tween(durationMillis = 700))
        }
    }
    val flashedUsdtColor = if (tickCounter > 0) {
        val targetTrend = if (isPriceUp) EmeraldUp else CrimsonDown
        lerp(GoldPrimary, targetTrend, rateFlash.value)
    } else {
        GoldPrimary
    }

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            containerColor = CharcoalElevated,
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = strings.walletWithdrawDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldBright,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = strings.walletWithdrawDialogBody,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextIvory,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("withdraw_simulation_dialog_text")
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWithdrawDialog = false },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier.testTag("withdraw_dialog_confirm_button")
                ) {
                    Text(
                        text = strings.walletWithdrawDialogConfirm,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "wallet_summary_card") {
                StaggeredEntranceCard(index = 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(GoldSubtleBg, CharcoalCard)
                                )
                            )
                            .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HaigenxCoinLogoBadge(
                                    size = 34.dp,
                                    contentDescription = strings.walletTitle,
                                    glowAlpha = 0.5f
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.walletTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = GoldBright
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = strings.walletVirtualBalance,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FormatUtils.formatHx1(smoothBalance, language, decimals = 4)} HX1",
                            style = MonoMetricLarge.copy(fontSize = 28.sp),
                            color = TextIvory,
                            modifier = Modifier.testTag("wallet_hx1_balance")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = strings.walletEstimatedUsdt,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "≈ ${FormatUtils.formatUsdtValue(smoothConvertedUsdt, language)} USDT",
                            style = MonoMetricMedium,
                            color = flashedUsdtColor,
                            modifier = Modifier.testTag("wallet_usdt_equivalent")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${strings.walletSyncRateLabel}: 1 HX1 = ${FormatUtils.formatUsdtPrice(currentPriceUsdt, language)} USDT",
                            style = MonoMetricSmall.copy(fontSize = 11.5.sp),
                            color = TextMuted,
                            modifier = Modifier.testTag("wallet_synced_rate")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showWithdrawDialog = true },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = ObsidianBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("withdraw_usdt_button")
                        ) {
                            Text(
                                text = strings.walletWithdrawButton,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }

            item(key = "history_header") {
                StaggeredEntranceCard(index = 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.walletHistoryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextIvory
                        )
                    }
                }
            }

            if (historyRecords.isEmpty()) {
                item(key = "history_empty") {
                    StaggeredEntranceCard(index = 2) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(CharcoalCard)
                                .border(1.dp, GoldBorder, RoundedCornerShape(22.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.walletHistoryEmpty,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(historyRecords, key = { _, record -> record.id }) { idx, record ->
                    StaggeredEntranceCard(index = (idx + 2).coerceAtMost(5)) {
                        MiningHistoryItem(
                            record = record,
                            language = language,
                            strings = strings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiningHistoryItem(
    record: MiningRecordEntity,
    language: AppLanguage,
    strings: AppStrings
) {
    val sourceEnum = runCatching { MiningSourceType.valueOf(record.sourceType) }
        .getOrDefault(MiningSourceType.MANUAL)

    val (icon, label) = when (sourceEnum) {
        MiningSourceType.MANUAL -> Icons.Outlined.TouchApp to strings.sourceManual
        MiningSourceType.AUTO -> Icons.Outlined.Memory to strings.sourceAuto
        MiningSourceType.BOOST_BONUS -> Icons.Outlined.ElectricBolt to strings.sourceBoost
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalCard)
            .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
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
                    imageVector = icon,
                    contentDescription = label,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextIvory
                )
                Text(
                    text = "${FormatUtils.formatTimestamp(record.timestampMillis, language)} • ${
                        FormatUtils.formatHashrate(record.hashrateSnapshot, language)
                    } H/s",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }
        }

        Text(
            text = "+${FormatUtils.formatHx1(record.amountHx1, language, decimals = 4)} HX1",
            style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
            color = EmeraldUp
        )
    }
}

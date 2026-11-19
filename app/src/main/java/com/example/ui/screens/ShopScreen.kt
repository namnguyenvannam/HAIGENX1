package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.ui.components.HaigenxCoinLogoBadge
import com.example.ui.components.StaggeredEntranceCard
import com.example.ui.components.rememberAnimatedDouble
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldUp
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted
import com.example.viewmodel.MiningUiState
import com.example.viewmodel.UpgradeCategory
import com.example.viewmodel.UpgradeItemUi
import com.example.viewmodel.UpgradeType

@Composable
fun ShopScreen(
    miningState: MiningUiState,
    upgradeItems: List<UpgradeItemUi>,
    language: AppLanguage,
    strings: AppStrings,
    onPurchaseUpgrade: (UpgradeType) -> Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val smoothBalance = rememberAnimatedDouble(targetValue = miningState.balanceHx1, durationMillis = 240)
    val multiplierItems = upgradeItems.filter { it.type.category == UpgradeCategory.MULTIPLIER }
    val coreItems = upgradeItems.filter { it.type.category == UpgradeCategory.CORE }

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
            // Header & Permanent Multiplier Summary Card
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
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                        .padding(18.dp)
                        .testTag("shop_summary_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = strings.upgradeShopTitle,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.upgradeShopTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GoldBright
                                )
                                Text(
                                    text = strings.upgradeShopSubtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Balance row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(ObsidianBlack.copy(alpha = 0.72f))
                            .border(0.8.dp, GoldBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HaigenxCoinLogoBadge(
                                size = 22.dp,
                                contentDescription = null,
                                glowAlpha = 0.35f
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.upgradeShopBalanceLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = "${FormatUtils.formatHx1(smoothBalance, language, 2)} HX1",
                            style = MonoMetricLarge.copy(fontSize = 20.sp),
                            color = TextIvory,
                            modifier = Modifier.testTag("shop_balance_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Permanent Multiplier + Effective Hashrate badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GoldSubtleBg)
                                .border(0.8.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesomeMotion,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.shopCurrentMultiplierLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "×${FormatUtils.formatHashrate(miningState.permanentMultiplier, language)}",
                                style = MonoMetricSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = GoldBright,
                                modifier = Modifier.testTag("shop_permanent_multiplier_text")
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(EmeraldBg)
                                .border(0.8.dp, EmeraldUp.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = null,
                                    tint = EmeraldUp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.shopEffectiveHashrateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${FormatUtils.formatHashrate(miningState.effectiveHashrate, language)} H/s",
                                style = MonoMetricSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = EmeraldUp,
                                modifier = Modifier.testTag("shop_effective_hashrate_text")
                            )
                        }
                    }
                }
            }

            // Section 1: Permanent Hashrate Multipliers
            StaggeredEntranceCard(index = 1) {
                Text(
                    text = strings.shopSectionMultipliers,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GoldPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            multiplierItems.forEachIndexed { idx, item ->
                StaggeredEntranceCard(index = 2 + idx) {
                    ShopUpgradeCard(
                        item = item,
                        language = language,
                        strings = strings,
                        isFeaturedMultiplier = true,
                        onPurchase = {
                            if (onPurchaseUpgrade(item.type)) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
            }

            // Section 2: Core Rig Upgrades
            StaggeredEntranceCard(index = 4) {
                Text(
                    text = strings.shopSectionCore,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }

            coreItems.forEachIndexed { idx, item ->
                StaggeredEntranceCard(index = 5 + idx) {
                    ShopUpgradeCard(
                        item = item,
                        language = language,
                        strings = strings,
                        isFeaturedMultiplier = false,
                        onPurchase = {
                            if (onPurchaseUpgrade(item.type)) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShopUpgradeCard(
    item: UpgradeItemUi,
    language: AppLanguage,
    strings: AppStrings,
    isFeaturedMultiplier: Boolean,
    onPurchase: () -> Unit
) {
    val (icon, title, desc) = when (item.type) {
        UpgradeType.OVERCLOCK_MULTIPLIER -> Triple(
            Icons.Outlined.Bolt,
            strings.upgradeOverclockTitle,
            strings.upgradeOverclockDesc
        )
        UpgradeType.QUANTUM_TURBO_RIG -> Triple(
            Icons.Outlined.RocketLaunch,
            strings.upgradeTurboTitle,
            strings.upgradeTurboDesc
        )
        UpgradeType.HASHRATE_CORE -> Triple(
            Icons.Outlined.Memory,
            strings.upgradeHashrateTitle,
            strings.upgradeHashrateDesc
        )
        UpgradeType.TAP_DRILL -> Triple(
            Icons.Outlined.TouchApp,
            strings.upgradeTapTitle,
            strings.upgradeTapDesc
        )
        UpgradeType.ENERGY_CELL -> Triple(
            Icons.Outlined.BatteryChargingFull,
            strings.upgradeEnergyTitle,
            strings.upgradeEnergyDesc
        )
    }

    val progress = (item.level.toFloat() / item.maxLevel.toFloat()).coerceIn(0f, 1f)
    val cardBorder = if (isFeaturedMultiplier) GoldPrimary.copy(alpha = 0.55f) else GoldBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isFeaturedMultiplier) {
                    Brush.horizontalGradient(listOf(GoldSubtleBg, CharcoalCard))
                } else {
                    Brush.horizontalGradient(listOf(CharcoalCard, CharcoalCard))
                }
            )
            .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("shop_card_${item.type.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldSubtleBg)
                        .border(0.8.dp, GoldBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isFeaturedMultiplier) GoldBright else GoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextIvory
                        )
                    }
                    Text(
                        text = "${strings.upgradeLevelPrefix} ${item.level} / ${item.maxLevel}",
                        style = MonoMetricSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                        color = GoldBright,
                        modifier = Modifier.testTag("shop_level_${item.type.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onPurchase,
                enabled = item.canAfford,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = ObsidianBlack,
                    disabledContainerColor = ObsidianBlack,
                    disabledContentColor = TextMuted
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("upgrade_buy_${item.type.name.lowercase()}")
            ) {
                Text(
                    text = when {
                        item.isMaxed -> strings.upgradeMaxLevel
                        else -> "${FormatUtils.formatHx1(item.nextCostHx1, language, 0)} HX1"
                    },
                    style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (item.isMaxed) item.currentStatText else "${item.currentStatText} → ${item.nextStatText}",
                style = MonoMetricSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = EmeraldUp
            )
            if (!item.canAfford && !item.isMaxed) {
                Text(
                    text = strings.upgradeNotEnoughBalance,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = if (isFeaturedMultiplier) GoldBright else GoldPrimary,
            trackColor = ObsidianBlack
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory

@Composable
fun FixedSimulationTopBar(
    strings: AppStrings,
    isBoostActive: Boolean,
    boostRemainingSeconds: Long,
    isSettingsScreen: Boolean,
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBlack, CharcoalSurface.copy(alpha = 0.96f))
                )
            )
            .statusBarsPadding()
    ) {
        // Fixed light-gold banner always visible at top of every screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoldSubtleBg)
                .border(
                    width = 0.5.dp,
                    color = GoldBorder
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("fixed_simulation_disclaimer"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = strings.disclaimerBanner,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.3.sp
                ),
                color = GoldBright,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = TextOverflow.Clip
            )
        }

        // Compact Luxury App Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isSettingsScreen) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backLabel,
                            tint = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextIvory
                    )
                } else {
                    HaigenxCoinLogoBadge(
                        size = 28.dp,
                        contentDescription = "HAIGENX1 Logo",
                        glowAlpha = 0.45f
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HAIGENX1",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        ),
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MINER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.3.sp
                        ),
                        color = TextIvory.copy(alpha = 0.78f)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GoldSubtleBg)
                            .border(0.8.dp, GoldPrimary.copy(alpha = 0.75f), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .testTag("header_haigenx1_badge")
                    ) {
                        Text(
                            text = "HAIGENX1",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = GoldBright
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = isBoostActive && !isSettingsScreen) {
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(GoldPrimary.copy(alpha = 0.16f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = strings.boostActiveLabel,
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "3x ${FormatUtils.formatCountdown(boostRemainingSeconds)}",
                            style = MonoMetricSmall.copy(fontSize = 11.sp),
                            color = GoldBright
                        )
                    }
                }

                if (!isSettingsScreen) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(CharcoalSurface)
                            .border(1.dp, GoldBorder, CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = strings.settingsTitle,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

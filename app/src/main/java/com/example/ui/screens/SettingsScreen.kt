package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted

@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    strings: AppStrings,
    onSelectLanguage: (AppLanguage) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

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
            // Language Selector Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CharcoalCard)
                    .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = strings.settingsLanguageLabel,
                        tint = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.settingsLanguageLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextIvory
                        )
                        Text(
                            text = strings.settingsLanguageDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = currentLanguage == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) GoldSubtleBg else ObsidianBlack)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) GoldPrimary else GoldBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectLanguage(lang) }
                                .padding(vertical = 12.dp)
                                .testTag("lang_${lang.code}_option"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) GoldBright else TextMuted
                            )
                        }
                    }
                }
            }

            // Privacy & Entertainment Notice Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CharcoalCard)
                    .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = strings.settingsPrivacyTitle,
                    tint = GoldPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.settingsPrivacyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextIvory
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.settingsPrivacyDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            // Version Card: Phiên bản 0.0.1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CharcoalCard)
                    .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = strings.settingsVersionLabel,
                        tint = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.settingsVersionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextIvory
                    )
                }
                Text(
                    text = strings.settingsVersionValue,
                    style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold),
                    color = GoldBright,
                    modifier = Modifier.testTag("app_version_text")
                )
            }
        }
    }
}

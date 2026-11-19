package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldUp
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    accountDisplayName: String,
    isCloudSynced: Boolean,
    currentLanguage: AppLanguage,
    strings: AppStrings,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSaveDisplayName: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    var nameInput by remember(accountDisplayName) { mutableStateOf(accountDisplayName) }
    var showSavedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(showSavedBanner) {
        if (showSavedBanner) {
            delay(2500L)
            showSavedBanner = false
        }
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
            // Miner Profile & Display Name Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = strings.settingsAccountTitle,
                            tint = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = accountDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvory
                            )
                            Text(
                                text = strings.settingsAccountTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isCloudSynced) EmeraldBg else GoldSubtleBg)
                            .border(
                                0.8.dp,
                                if (isCloudSynced) EmeraldUp.copy(alpha = 0.45f) else GoldBorder,
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCloudSynced) Icons.Outlined.CloudDone else Icons.Outlined.Save,
                            contentDescription = null,
                            tint = if (isCloudSynced) EmeraldUp else GoldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCloudSynced) strings.settingsCloudSyncedBadge else strings.settingsLocalModeBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isCloudSynced) EmeraldUp else GoldBright
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.settingsSignedInAs,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Editable Display Name row
                Text(
                    text = strings.settingsDisplayNameLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 80) nameInput = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = strings.settingsDisplayNameHint,
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GoldBorder,
                            focusedTextColor = TextIvory,
                            unfocusedTextColor = TextIvory,
                            cursorColor = GoldPrimary,
                            focusedContainerColor = ObsidianBlack,
                            unfocusedContainerColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_display_name_input")
                    )

                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onSaveDisplayName(nameInput)
                                showSavedBanner = true
                            }
                        },
                        enabled = nameInput.isNotBlank() && nameInput.trim() != accountDisplayName,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = ObsidianBlack,
                            disabledContainerColor = ObsidianBlack,
                            disabledContentColor = TextMuted
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("settings_save_name_button")
                    ) {
                        Text(
                            text = strings.settingsSaveNameButton,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                AnimatedVisibility(visible = showSavedBanner) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = EmeraldUp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.settingsDisplayNameSaved,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldUp
                        )
                    }
                }

            }

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

            // Version Card: Phiên bản 0.0.5
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

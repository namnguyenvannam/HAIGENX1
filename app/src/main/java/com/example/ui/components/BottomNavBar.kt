package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppStrings
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.TextMuted

enum class MainTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    MINING("mining", Icons.Filled.Bolt, Icons.Outlined.Bolt, "tab_mining"),
    SHOP("shop", Icons.Filled.Storefront, Icons.Outlined.Storefront, "tab_shop"),
    AI("ai", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "tab_ai"),
    MARKET("market", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, "tab_market"),
    WALLET("wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_wallet")
}

const val ROUTE_SETTINGS = "settings"

@Composable
fun HaigenxBottomBar(
    currentRoute: String?,
    strings: AppStrings,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CharcoalSurface)
    ) {
        HorizontalDivider(
            thickness = 0.8.dp,
            color = GoldBorder,
            modifier = Modifier.fillMaxWidth()
        )
        NavigationBar(
            containerColor = CharcoalSurface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = currentRoute == tab.route
                val label = when (tab) {
                    MainTab.MINING -> strings.tabMine
                    MainTab.SHOP -> strings.tabShop
                    MainTab.AI -> strings.tabAi
                    MainTab.MARKET -> strings.tabMarket
                    MainTab.WALLET -> strings.tabWallet
                }
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.testTag(tab.testTag),
                    icon = {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldPrimary,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = GoldSubtleBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        }
    }
}

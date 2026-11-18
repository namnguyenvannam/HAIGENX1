package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val HaigenxDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = ObsidianBlack,
    primaryContainer = GoldSubtleBg,
    onPrimaryContainer = GoldBright,
    secondary = GoldDeep,
    onSecondary = ObsidianBlack,
    secondaryContainer = CharcoalElevated,
    onSecondaryContainer = GoldPrimary,
    tertiary = EmeraldUp,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextIvory,
    surface = CharcoalSurface,
    onSurface = TextIvory,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = TextMuted,
    outline = GoldBorder,
    outlineVariant = GoldMuted,
    error = CrimsonDown,
    onError = TextIvory
)

val HaigenxShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun HaigenxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HaigenxDarkColorScheme,
        typography = HaigenxTypography,
        shapes = HaigenxShapes,
        content = content
    )
}

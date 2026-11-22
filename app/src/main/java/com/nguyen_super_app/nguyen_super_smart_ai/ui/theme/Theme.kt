package com.nguyen_super_app.nguyen_super_smart_ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkSlateCyanColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = SlateDeep,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanBright,
    secondary = SkySecondary,
    onSecondary = SlateDeep,
    secondaryContainer = SkyContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldOk,
    onTertiary = SlateDeep,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = TextPrimary,
    background = SlateDeep,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = SlateDeep,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateElevated,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = CoralError,
    onError = TextPrimary,
    errorContainer = CoralContainer,
    onErrorContainer = TextPrimary
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SuperSmartAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkSlateCyanColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

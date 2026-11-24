package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val AlSaherDarkColorScheme = darkColorScheme(
    primary = CyanSpark,
    onPrimary = OnCyanSpark,
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = AmberGold,
    onSecondary = OnAmberGold,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = EmeraldMint,
    onTertiary = OnEmeraldMint,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    error = RoseAlert,
    onError = Color.White,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline.copy(alpha = 0.55f)
)

private val AlSaherLightColorScheme = lightColorScheme(
    primary = CyanLightPrimary,
    onPrimary = Color.White,
    primaryContainer = CyanLightContainer,
    onPrimaryContainer = OnCyanLightContainer,
    secondary = AmberLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberLightContainer,
    onSecondaryContainer = OnAmberLightContainer,
    tertiary = EmeraldLightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldLightContainer,
    onTertiaryContainer = OnEmeraldLightContainer,
    error = RoseAlert,
    onError = Color.White,
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = DaylightSurfaceHighest,
    outline = DaylightOutline,
    outlineVariant = DaylightOutline.copy(alpha = 0.65f)
)

private val AlSaherShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AlSaherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AlSaherDarkColorScheme else AlSaherLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AlSaherTypography,
        shapes = AlSaherShapes,
        content = content
    )
}

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.DarkModeOption

val LocalAppThemeMode = staticCompositionLocalOf { AppThemeMode.MATERIAL_YOU }

// Material You Default Palettes
private val MaterialYouDarkScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = Color(0xFF002C71),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = SecondaryTealDark,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = TertiaryAmberDark,
    onTertiary = Color(0xFF451A03),
    background = SurfaceDark,
    onBackground = Color(0xFFF3F4F6),
    surface = SurfaceDark,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFF9CA3AF)
)

private val MaterialYouLightScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = TertiaryAmber,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700
)

// Frosted Glass Palettes
private val FrostedGlassDarkScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0C2440),
    primaryContainer = Color(0xFF1E3A8A).copy(alpha = 0.8f),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A).copy(alpha = 0.8f),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = FrostedDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = FrostedDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = FrostedDarkCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = FrostedDarkBorder
)

private val FrostedGlassLightScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF99F6E4),
    onSecondaryContainer = Color(0xFF0F766E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = FrostedLightBackground,
    onBackground = Slate900,
    surface = FrostedLightSurface,
    onSurface = Slate900,
    surfaceVariant = FrostedLightCard,
    onSurfaceVariant = Slate700,
    outline = FrostedLightBorder
)

// Clear UI Palettes
private val ClearDarkScheme = darkColorScheme(
    primary = Color(0xFF67E8F9),
    onPrimary = Color(0xFF083344),
    primaryContainer = Color(0xFF164E63).copy(alpha = 0.7f),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59).copy(alpha = 0.7f),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFDE047),
    onTertiary = Color(0xFF422006),
    background = ClearDarkBackground,
    onBackground = Color(0xFFFFFFFF),
    surface = ClearDarkSurface,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = ClearDarkCard,
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = ClearDarkBorder
)

private val ClearLightScheme = lightColorScheme(
    primary = Color(0xFF0369A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF075985),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    background = ClearLightBackground,
    onBackground = Slate900,
    surface = ClearLightSurface,
    onSurface = Slate900,
    surfaceVariant = ClearLightCard,
    onSurfaceVariant = Slate800,
    outline = ClearLightBorder
)

@Composable
fun NovaFilesTheme(
    themeMode: AppThemeMode = AppThemeMode.MATERIAL_YOU,
    darkModeOption: DarkModeOption = DarkModeOption.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (darkModeOption) {
        DarkModeOption.SYSTEM -> isSystemDark
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
    }

    val context = LocalContext.current

    val colorScheme = when (themeMode) {
        AppThemeMode.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) MaterialYouDarkScheme else MaterialYouLightScheme
            }
        }
        AppThemeMode.FROSTED_GLASS -> {
            if (isDark) FrostedGlassDarkScheme else FrostedGlassLightScheme
        }
        AppThemeMode.CLEAR_TRANSPARENT -> {
            if (isDark) ClearDarkScheme else ClearLightScheme
        }
    }

    CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

fun Modifier.glassBorder(
    themeMode: AppThemeMode,
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean = true
): Modifier {
    return when (themeMode) {
        AppThemeMode.FROSTED_GLASS -> {
            this.border(
                width = 1.dp,
                color = if (isDark) FrostedDarkBorder else FrostedLightBorder,
                shape = shape
            )
        }
        AppThemeMode.CLEAR_TRANSPARENT -> {
            this.border(
                width = 1.dp,
                color = if (isDark) ClearDarkBorder else ClearLightBorder,
                shape = shape
            )
        }
        AppThemeMode.MATERIAL_YOU -> this
    }
}

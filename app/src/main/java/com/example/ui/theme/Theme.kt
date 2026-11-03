package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CoffeeAmber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF45240B),
    onPrimaryContainer = CoffeeAmberLight,
    secondary = CoffeeTeal,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0C384B),
    onSecondaryContainer = CoffeeTeal,
    tertiary = CoffeeGreen,
    onTertiary = Color.Black,
    background = CoffeeDarkBackground,
    onBackground = CoffeeDarkText,
    surface = CoffeeDarkSurface,
    onSurface = CoffeeDarkText,
    surfaceVariant = CoffeeDarkElevated,
    onSurfaceVariant = CoffeeDarkTextMuted,
    outline = CoffeeDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeeLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF3B1500),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2EFFF),
    onSecondaryContainer = Color(0xFF003554),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = CoffeeLightBackground,
    onBackground = CoffeeLightText,
    surface = CoffeeLightSurface,
    onSurface = CoffeeLightText,
    surfaceVariant = CoffeeLightElevated,
    onSurfaceVariant = CoffeeLightTextMuted,
    outline = CoffeeLightBorder
)

@Composable
fun CoffeeGrindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve distinctive coffee branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

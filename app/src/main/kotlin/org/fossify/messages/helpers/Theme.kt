package org.fossify.messages.helpers

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DynamicGreen80,
    secondary = DarkGreenTertiary,
    tertiary = DynamicLightGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnSurface,
    onSecondary = DarkOnSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkBubbleReceived,
    primaryContainer = DarkBubbleSent,
    onPrimaryContainer = DarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = DynamicGreen40,
    secondary = LightGreenSecondary,
    tertiary = LightGreenTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnSurface,
    onSecondary = LightOnSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightBubbleReceived,
    primaryContainer = LightBubbleSent,
    onPrimaryContainer = LightOnSurface
)

@Composable
fun MessagesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true, // Hooked up to settings preferences
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MessagesTypography,
        content = content
    )
}

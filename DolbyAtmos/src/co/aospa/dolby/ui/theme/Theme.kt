/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Fallback palette for builds without dynamic color. Everything else follows the wallpaper so the
 * app looks native next to AOSP Settings.
 */
private val DolbyLightColors =
    lightColorScheme(
        primary = Color(0xFF3355FF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDDE1FF),
        onPrimaryContainer = Color(0xFF001158),
        secondary = Color(0xFF5A5D72),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDFE1F9),
        onSecondaryContainer = Color(0xFF171A2C),
        tertiary = Color(0xFF00696E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF9CF1F6),
        onTertiaryContainer = Color(0xFF002022),
        background = Color(0xFFFBF8FF),
        onBackground = Color(0xFF1B1B21),
        surface = Color(0xFFFBF8FF),
        onSurface = Color(0xFF1B1B21),
        surfaceVariant = Color(0xFFE3E1EC),
        onSurfaceVariant = Color(0xFF46464F),
        outline = Color(0xFF767680),
        outlineVariant = Color(0xFFC7C5D0),
    )

private val DolbyDarkColors =
    darkColorScheme(
        primary = Color(0xFFB9C3FF),
        onPrimary = Color(0xFF001D8F),
        primaryContainer = Color(0xFF1B3BE0),
        onPrimaryContainer = Color(0xFFDDE1FF),
        secondary = Color(0xFFC3C5DD),
        onSecondary = Color(0xFF2C2F42),
        secondaryContainer = Color(0xFF424659),
        onSecondaryContainer = Color(0xFFDFE1F9),
        tertiary = Color(0xFF80D4DA),
        onTertiary = Color(0xFF00373A),
        tertiaryContainer = Color(0xFF004F53),
        onTertiaryContainer = Color(0xFF9CF1F6),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF46464F),
        onSurfaceVariant = Color(0xFFC7C5D0),
        outline = Color(0xFF90909A),
        outlineVariant = Color(0xFF46464F),
    )

private val DolbyShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(34.dp),
    )

@Composable
fun DolbyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (darkTheme) {
            DolbyDarkColors
        } else {
            DolbyLightColors
        }

    MaterialTheme(colorScheme = colorScheme, shapes = DolbyShapes, content = content)
}

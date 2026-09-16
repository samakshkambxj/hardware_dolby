/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.lunaris.dolby.ui.components.AccentChoice

private val Indigo10 = Color(0xFF0A0A5C)
private val Indigo20 = Color(0xFF212490)
private val Indigo30 = Color(0xFF3639B4)
private val Indigo40 = Color(0xFF4B4FCF)
private val Indigo80 = Color(0xFFC0C1FF)
private val Indigo90 = Color(0xFFE2E0FF)

private val Slate10 = Color(0xFF191A2C)
private val Slate20 = Color(0xFF2E2F42)
private val Slate30 = Color(0xFF444559)
private val Slate40 = Color(0xFF5C5D72)
private val Slate80 = Color(0xFFC5C4DD)
private val Slate90 = Color(0xFFE1E0F9)

private val Amber10 = Color(0xFF271900)
private val Amber20 = Color(0xFF402D00)
private val Amber30 = Color(0xFF5C4200)
private val Amber40 = Color(0xFF7A5900)
private val Amber80 = Color(0xFFF5BF48)
private val Amber90 = Color(0xFFFFDF9B)

private val Red10 = Color(0xFF410002)
private val Red20 = Color(0xFF690005)
private val Red30 = Color(0xFF93000A)
private val Red40 = Color(0xFFBA1A1A)
private val Red80 = Color(0xFFFFB4AB)
private val Red90 = Color(0xFFFFDAD6)

private val NeutralWhite = Color(0xFFFFFFFF)
private val NeutralBlack = Color(0xFF000000)

private val LightSurface = Color(0xFFFCF8FF)
private val LightSurfaceDim = Color(0xFFDBD9E0)
private val LightOnSurface = Color(0xFF1B1B21)
private val LightSurfaceVariant = Color(0xFFE4E1EC)
private val LightOnSurfaceVariant = Color(0xFF46464F)
private val LightOutline = Color(0xFF777680)
private val LightOutlineVariant = Color(0xFFC7C5D0)
private val LightContainerLowest = Color(0xFFFFFFFF)
private val LightContainerLow = Color(0xFFF6F2FA)
private val LightContainer = Color(0xFFF0ECF4)
private val LightContainerHigh = Color(0xFFEAE7EF)
private val LightContainerHighest = Color(0xFFE4E1E9)
private val LightInverseSurface = Color(0xFF303036)
private val LightInverseOnSurface = Color(0xFFF2EFF7)

private val DarkSurface = Color(0xFF131318)
private val DarkSurfaceBright = Color(0xFF39383F)
private val DarkOnSurface = Color(0xFFE4E1E9)
private val DarkSurfaceVariant = Color(0xFF46464F)
private val DarkOnSurfaceVariant = Color(0xFFC7C5D0)
private val DarkOutline = Color(0xFF91909A)
private val DarkOutlineVariant = Color(0xFF46464F)
private val DarkContainerLowest = Color(0xFF0E0E13)
private val DarkContainerLow = Color(0xFF1B1B21)
private val DarkContainer = Color(0xFF1F1F25)
private val DarkContainerHigh = Color(0xFF2A292F)
private val DarkContainerHighest = Color(0xFF35343A)

internal val DolbyLightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = NeutralWhite,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    inversePrimary = Indigo80,

    secondary = Slate40,
    onSecondary = NeutralWhite,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,

    tertiary = Amber40,
    onTertiary = NeutralWhite,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,

    error = Red40,
    onError = NeutralWhite,
    errorContainer = Red90,
    onErrorContainer = Red10,

    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = Indigo40,
    surfaceBright = LightSurface,
    surfaceDim = LightSurfaceDim,
    surfaceContainerLowest = LightContainerLowest,
    surfaceContainerLow = LightContainerLow,
    surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,

    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = NeutralBlack
)

internal val DolbyDarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    inversePrimary = Indigo40,

    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,

    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,

    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,

    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = Indigo80,
    surfaceBright = DarkSurfaceBright,
    surfaceDim = DarkSurface,
    surfaceContainerLowest = DarkContainerLowest,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,

    inverseSurface = DarkOnSurface,
    inverseOnSurface = LightInverseSurface,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = NeutralBlack
)

/**
 * Fixed per-accent primary ramp. Tertiary/error/surfaces stay on the Dolby
 * baseline so one picker cannot blow up contrast elsewhere.
 */
private data class AccentRamp(
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val lightInverse: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val darkInverse: Color,
    /** Representative dot for the Page Style swatch picker. */
    val preview: Color
)

private val AccentRamps: Map<AccentChoice, AccentRamp> = mapOf(
    AccentChoice.AZURE to AccentRamp(
        lightPrimary = Color(0xFF0061A4), lightContainer = Color(0xFFD1E4FF),
        lightOnContainer = Color(0xFF001D35), lightInverse = Color(0xFF9ECAFF),
        darkPrimary = Color(0xFF9ECAFF), darkOnPrimary = Color(0xFF003258),
        darkContainer = Color(0xFF00497D), darkOnContainer = Color(0xFFD1E4FF),
        darkInverse = Color(0xFF0061A4), preview = Color(0xFF0061A4)
    ),
    AccentChoice.VIOLET to AccentRamp(
        lightPrimary = Color(0xFF6750A4), lightContainer = Color(0xFFEADDFF),
        lightOnContainer = Color(0xFF21005D), lightInverse = Color(0xFFD0BCFF),
        darkPrimary = Color(0xFFD0BCFF), darkOnPrimary = Color(0xFF381E72),
        darkContainer = Color(0xFF4F378B), darkOnContainer = Color(0xFFEADDFF),
        darkInverse = Color(0xFF6750A4), preview = Color(0xFF6750A4)
    ),
    AccentChoice.TEAL to AccentRamp(
        lightPrimary = Color(0xFF006A60), lightContainer = Color(0xFF70F7EC),
        lightOnContainer = Color(0xFF00201D), lightInverse = Color(0xFF4EDAD2),
        darkPrimary = Color(0xFF4EDAD2), darkOnPrimary = Color(0xFF003731),
        darkContainer = Color(0xFF005049), darkOnContainer = Color(0xFF70F7EC),
        darkInverse = Color(0xFF006A60), preview = Color(0xFF006A60)
    ),
    AccentChoice.JADE to AccentRamp(
        lightPrimary = Color(0xFF4C662B), lightContainer = Color(0xFFCDEDA3),
        lightOnContainer = Color(0xFF102000), lightInverse = Color(0xFFB1D18A),
        darkPrimary = Color(0xFFB1D18A), darkOnPrimary = Color(0xFF1F3700),
        darkContainer = Color(0xFF354E16), darkOnContainer = Color(0xFFCDEDA3),
        darkInverse = Color(0xFF4C662B), preview = Color(0xFF4C662B)
    ),
    AccentChoice.GOLD to AccentRamp(
        lightPrimary = Amber40, lightContainer = Amber90,
        lightOnContainer = Amber10, lightInverse = Amber80,
        darkPrimary = Amber80, darkOnPrimary = Amber20,
        darkContainer = Amber30, darkOnContainer = Amber90,
        darkInverse = Amber40, preview = Amber40
    ),
    AccentChoice.ROSE to AccentRamp(
        lightPrimary = Color(0xFF984061), lightContainer = Color(0xFFFFD9E3),
        lightOnContainer = Color(0xFF3E001D), lightInverse = Color(0xFFFFB0C8),
        darkPrimary = Color(0xFFFFB0C8), darkOnPrimary = Color(0xFF5D1135),
        darkContainer = Color(0xFF7A2949), darkOnContainer = Color(0xFFFFD9E3),
        darkInverse = Color(0xFF984061), preview = Color(0xFF984061)
    ),
    AccentChoice.RUBY to AccentRamp(
        lightPrimary = Red40, lightContainer = Red90,
        lightOnContainer = Red10, lightInverse = Red80,
        darkPrimary = Red80, darkOnPrimary = Red20,
        darkContainer = Red30, darkOnContainer = Red90,
        darkInverse = Red40, preview = Red40
    )
)

val AccentChoice.previewColor: Color
    get() = AccentRamps[this]?.preview ?: Indigo40

/**
 * Resolves the app [ColorScheme] from Page Style choices. Dynamic color is
 * handled by the caller ([DolbyTheme]) — this only covers the static path.
 */
fun dolbyColorScheme(
    accent: AccentChoice,
    darkTheme: Boolean,
    amoled: Boolean
): ColorScheme {
    val base = if (darkTheme) DolbyDarkColorScheme else DolbyLightColorScheme
    val ramp = AccentRamps[accent]
    val accented = if (ramp == null) {
        base
    } else if (darkTheme) {
        base.copy(
            primary = ramp.darkPrimary,
            onPrimary = ramp.darkOnPrimary,
            primaryContainer = ramp.darkContainer,
            onPrimaryContainer = ramp.darkOnContainer,
            inversePrimary = ramp.darkInverse,
            surfaceTint = ramp.darkPrimary
        )
    } else {
        base.copy(
            primary = ramp.lightPrimary,
            onPrimary = NeutralWhite,
            primaryContainer = ramp.lightContainer,
            onPrimaryContainer = ramp.lightOnContainer,
            inversePrimary = ramp.lightInverse,
            surfaceTint = ramp.lightPrimary
        )
    }
    if (!darkTheme || !amoled) return accented
    // True-black surfaces for OLED; content colors stay readable on black.
    return accented.copy(
        background = NeutralBlack,
        surface = NeutralBlack,
        surfaceDim = NeutralBlack,
        surfaceContainerLowest = NeutralBlack,
        surfaceContainerLow = NeutralBlack,
        surfaceContainer = Color(0xFF101014),
        surfaceContainerHigh = Color(0xFF17171C),
        surfaceContainerHighest = Color(0xFF1F1F25),
        surfaceVariant = Color(0xFF17171C)
    )
}

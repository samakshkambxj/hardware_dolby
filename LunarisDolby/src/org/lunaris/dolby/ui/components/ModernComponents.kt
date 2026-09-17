/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.data.EasterEggs
import org.lunaris.dolby.domain.models.ActiveAudioDevice
import org.lunaris.dolby.domain.models.AudioDeviceCategory
import org.lunaris.dolby.domain.models.ProfileSettings
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel
import org.lunaris.dolby.utils.*

// Press-bounce lives in Bouncy.kt (single source of truth).
// squishable() there is gesture-safe + scroll-aware; do not duplicate it here.

/** Page Style > Cards: container/border/elevation for all settings cards. */
@Composable
fun PageStyle.cardContainer(): Color = when (cardStyle) {
    PageCardStyle.FILLED -> MaterialTheme.colorScheme.surfaceContainerLow
    PageCardStyle.OUTLINED -> MaterialTheme.colorScheme.surface
    PageCardStyle.ELEVATED -> MaterialTheme.colorScheme.surfaceContainerLow
}

@Composable
fun PageStyle.cardBorder(): BorderStroke? = when (cardStyle) {
    PageCardStyle.OUTLINED ->
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    else -> null
}

@Composable
fun PageStyle.cardElevation(): CardElevation = CardDefaults.cardElevation(
    defaultElevation = if (cardStyle == PageCardStyle.ELEVATED) 4.dp else 0.dp
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun tileSelectionBorder(selected: Boolean): BorderStroke {
    val width by animateDpAsState(
        targetValue = if (selected) 1.1.dp else 0.8.dp,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tile_border_width"
    )
    val color by animateColorAsState(
        targetValue = (if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.8f),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tile_border_color"
    )
    return BorderStroke(width, color)
}

@Composable
fun DolbyLogo(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    leftColor: Color = color,
    rightColor: Color = color
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dolby_logo_left),
            contentDescription = stringResource(R.string.dolby_title),
            tint = leftColor,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            painter = painterResource(R.drawable.ic_dolby_logo_right),
            contentDescription = null,
            tint = rightColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Loading indicator: the Dolby mark spinning in place. Used while the
 * decoder list is queried; static when the user disabled animations.
 */
@Composable
fun SpinningDolbyLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (rememberReducedMotion()) {
        DolbyLogo(
            modifier = modifier.size(size),
            color = color
        )
        return
    }
    val spin = rememberInfiniteTransition(label = "dolby_logo_spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing)
        ),
        label = "angle"
    )
    DolbyLogo(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = angle },
        color = color
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActiveAudioDeviceCard(
    device: ActiveAudioDevice,
    modifier: Modifier = Modifier,
    /** When non-null the card opens the output picker on tap. */
    onClick: (() -> Unit)? = null
) {
    val categoryLabel = when (device.category) {
        AudioDeviceCategory.SPEAKER -> stringResource(R.string.audio_output_speaker)
        AudioDeviceCategory.WIRED -> stringResource(R.string.audio_output_wired)
        AudioDeviceCategory.BLUETOOTH -> stringResource(R.string.audio_output_bluetooth)
        AudioDeviceCategory.USB -> stringResource(R.string.audio_output_usb)
        AudioDeviceCategory.OTHER -> stringResource(R.string.audio_output_unknown)
    }
    val pageStyle by rememberPageStyle()

    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier.fillMaxWidth(),
        shape = pageStyle.cardShape,
        colors = CardDefaults.cardColors(
            // Opaque on purpose: the floating particle layer renders behind
            // this card, and any alpha lets particles bleed through it.
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = pageStyle.cardBorder(),
        elevation = pageStyle.cardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioDeviceIcon(
                category = device.category,
                active = true,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.audio_output_active_device),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.output_title),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DolbyMainCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onEasterEggUnlocked: () -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var logoTaps by remember { mutableStateOf(0) }
    val pageStyle by rememberPageStyle()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = pageStyle.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = pageStyle.cardContainer()
        ),
        border = pageStyle.cardBorder(),
        elevation = pageStyle.cardElevation()
    ) {
        Column {
            // Gradient banner vs flat primaryContainer (Page Style > Header).
            val bannerModifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .then(
                    if (pageStyle.bannerGradient) {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    pageStyle.cardContainer()
                                )
                            )
                        )
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    }
                )
            Box(
                modifier = bannerModifier,
                contentAlignment = Alignment.Center
            ) {
                if (pageStyle.showBannerWaveform) {
                    AnimatedWaveformBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp),
                        barColor = MaterialTheme.colorScheme.primary,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        barCount = 56,
                        animated = enabled
                    )
                }

                DolbyLogo(
                    modifier = Modifier
                        .height(60.dp)
                        .squishable(enabled = true, scaleDown = 0.9f)
                        .clickable {
                            logoTaps = EasterEggs.recordLogoTap(context)
                            when {
                                logoTaps >= EasterEggs.LOGO_TAP_TARGET -> {
                                    logoTaps = 0
                                    // unlock() is idempotent; the celebration replays
                                    // on every completion even when already owned.
                                    EasterEggs.unlock(context, EasterEggs.BADGE_PERSISTENT)
                                    scope.launch {
                                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.DOUBLE_CLICK)
                                    }
                                    ToastHelper.showToast(
                                        context,
                                        context.getString(R.string.egg_logo_unlocked)
                                    )
                                    onEasterEggUnlocked()
                                }
                                logoTaps == 3 -> {
                                    ToastHelper.showToast(
                                        context,
                                        context.getString(R.string.egg_logo_tease_1)
                                    )
                                }
                                logoTaps == 5 -> {
                                    ToastHelper.showToast(
                                        context,
                                        context.getString(R.string.egg_logo_tease_2)
                                    )
                                }
                            }
                        }
                )
            }

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dolby_enable),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (enabled) stringResource(R.string.dolby_on) 
                                  else stringResource(R.string.dolby_off),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { 
                            scope.launch {
                                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                            }
                            onEnabledChange(it)
                        },
                        thumbContent = {
                            Crossfade(
                                targetState = enabled,
                                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                                label = "switch_icon"
                            ) { isChecked ->
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProfileSelector(
    currentProfile: Int,
    onProfileChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dirtyProfiles: Set<Int> = emptySet(),
    onResetProfile: ((Int) -> Unit)? = null
) {
    ProfileCarousel(
        currentProfile = currentProfile,
        onProfileChange = onProfileChange,
        modifier = modifier,
        dirtyProfiles = dirtyProfiles,
        onResetProfile = onResetProfile
    )
}

@Composable
fun ModernSettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val pageStyle by rememberPageStyle()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = pageStyle.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = pageStyle.cardContainer()
        ),
        border = pageStyle.cardBorder(),
        elevation = pageStyle.cardElevation()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                SettingsCardIcon(icon = icon)

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

/**
 * Leading icon badge shared by [ModernSettingsCard] and standalone
 * expandable cards. Honors Page Style icon color/shape/size.
 */
@Composable
fun SettingsCardIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val pageStyle by rememberPageStyle()
    val iconBg: Color
    val iconTint: Color
    when (pageStyle.iconStyle) {
        PageIconStyle.ACCENT -> {
            iconBg = MaterialTheme.colorScheme.primaryContainer
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
        }
        PageIconStyle.PLAIN -> {
            iconBg = MaterialTheme.colorScheme.surfaceContainerHighest
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
        }
        PageIconStyle.FILLED -> {
            iconBg = MaterialTheme.colorScheme.primary
            iconTint = MaterialTheme.colorScheme.onPrimary
        }
    }
    Surface(
        modifier = modifier.size(pageStyle.iconSize.box),
        shape = pageStyle.iconShape.shape(),
        color = iconBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(pageStyle.iconSize.icon)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernSettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    val containerColor by animateColorAsState(
        targetValue = if (checked)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "switch_row_container"
    )
    val titleColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else if (checked) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    } else if (checked) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clip(CircleShape),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = titleColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = titleColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = { 
                    if (!enabled) return@Switch
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                    }
                    onCheckedChange(it)
                },
                enabled = enabled,
                thumbContent = {
                    Crossfade(
                        targetState = checked,
                        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                        label = "switch_icon"
                    ) { isChecked ->
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ModernSettingSlider(
    title: String,
    value: Int,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    valueLabel: (Int) -> String = { it.toString() }
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }
    var lastHapticValue by remember { mutableIntStateOf(value) }
    // Drag-local thumb: ViewModel writes (HAL + loadSettings on IO, or
    // Dynamics engine + disk persist) return asynchronously and would
    // otherwise keep arriving after release, dragging the thumb after lift.
    // So the thumb only follows the finger while dragging; the confirmed
    // value is committed once on release and re-synced afterwards.
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!isDragging) {
            sliderValue = value.toFloat()
            lastHapticValue = value
        } else if (kotlin.math.abs(value - sliderValue.toInt()) > (valueRange.endInclusive - valueRange.start) * 0.25f) {
            // Large external jump under an active drag (preset load, band-mode
            // switch): drop the lock and snap instead of fighting it.
            isDragging = false
            sliderValue = value.toFloat()
            lastHapticValue = value
        }
    }

    val displayValue = sliderValue.toInt()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = valueLabel(displayValue),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                isDragging = true
                val intValue = newValue.toInt()
                if (intValue != lastHapticValue) {
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TEXTURE_TICK)
                    }
                    lastHapticValue = intValue
                }
                sliderValue = newValue
            },
            onValueChangeFinished = {
                isDragging = false
                // Single commit on release: avoids queuing one HAL/IO write per
                // drag tick, whose late loadSettings emissions kept moving the
                // thumb after lift. The label already tracked the finger via
                // sliderValue, so this stays responsive.
                onValueChange(sliderValue)
                // Re-sync to the confirmed ViewModel value on the next
                // emission; if the HAL clamped it, LaunchedEffect snaps back.
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                inactiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
    }
}

@Composable
fun ModernSettingSelector(
    title: String,
    currentValue: Int,
    entries: Int,
    values: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val entryList = stringArrayResource(entries)
    val valueList = stringArrayResource(values)
    val currentIndex = valueList.indexOfFirst { it.toIntOrNull() == currentValue }
    val label = entryList.getOrElse(currentIndex.coerceAtLeast(0)) { "" }
    var expanded by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box {
                Surface(
                    onClick = { 
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TICK)
                        }
                        expanded = true 
                    },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    entryList.forEachIndexed { index, entry ->
                        val value = valueList.getOrNull(index)?.toIntOrNull() ?: return@forEachIndexed
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    entry,
                                    color = if (value == currentValue) 
                                        MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                scope.launch {
                                    haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                                }
                                expanded = false
                                onValueChange(value)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernIeqSelector(
    currentPreset: Int,
    onPresetChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ieqEntries = stringArrayResource(R.array.dolby_ieq_entries)
    val ieqValues = stringArrayResource(R.array.dolby_ieq_values)
    
    val ieqIcons = mapOf(
        0 to Icons.Default.PowerOff,
        1 to Icons.Default.GraphicEq,
        2 to Icons.Rounded.Balance,
        3 to Icons.Default.Whatshot
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dolby_ieq),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until minOf(2, ieqEntries.size)) {
                    val entry = ieqEntries[i]
                    val value = ieqValues[i].toInt()
                    val isSelected = currentPreset == value
                    
                    IeqTile(
                        entry = entry,
                        value = value,
                        isSelected = isSelected,
                        icon = ieqIcons[value] ?: Icons.Default.GraphicEq,
                        onPresetChange = onPresetChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            if (ieqEntries.size > 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 2 until minOf(4, ieqEntries.size)) {
                        val entry = ieqEntries[i]
                        val value = ieqValues[i].toInt()
                        val isSelected = currentPreset == value
                        IeqTile(
                            entry = entry,
                            value = value,
                            isSelected = isSelected,
                            icon = ieqIcons[value] ?: Icons.Default.GraphicEq,
                            onPresetChange = onPresetChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IeqTile(
    entry: String,
    value: Int,
    isSelected: Boolean,
    icon: ImageVector,
    onPresetChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    Surface(
        onClick = { 
            scope.launch {
                haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
            }
            onPresetChange(value) 
        },
        modifier = modifier
            .height(72.dp)
            .squishable(enabled = true, scaleDown = 0.93f),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface,
        shape = if (isSelected)
            MaterialTheme.shapes.extraLarge
        else
            MaterialTheme.shapes.large,
        border = tileSelectionBorder(isSelected)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = if (isSelected)
                    MaterialTheme.shapes.extraLarge
                else
                    MaterialTheme.shapes.medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = entry,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ModernConfirmDialog(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Info,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            ApplyDialogWindowBlur()
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(android.R.string.yes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(android.R.string.no))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge
    )
}

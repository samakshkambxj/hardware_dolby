/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.dolbyatmos.R
import org.dolbyatmos.data.EasterEggs
import org.dolbyatmos.domain.models.ActiveAudioDevice
import org.dolbyatmos.domain.models.AudioDeviceCategory
import org.dolbyatmos.domain.models.ProfileSettings
import org.dolbyatmos.ui.viewmodel.DolbyViewModel
import org.dolbyatmos.utils.*

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
    modifier: Modifier = Modifier
) {
    val categoryLabel = when (device.category) {
        AudioDeviceCategory.SPEAKER -> stringResource(R.string.audio_output_speaker)
        AudioDeviceCategory.WIRED -> stringResource(R.string.audio_output_wired)
        AudioDeviceCategory.BLUETOOTH -> stringResource(R.string.audio_output_bluetooth)
        AudioDeviceCategory.USB -> stringResource(R.string.audio_output_usb)
        AudioDeviceCategory.OTHER -> stringResource(R.string.audio_output_unknown)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
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
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Output",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(
                    text = stringResource(R.string.output_active),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DolbyMainCard(
    enabled: Boolean,
    live: Boolean = false,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onEasterEggUnlocked: () -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var logoTaps by remember { mutableStateOf(0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1250f, 900f)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudioStatusPill(
                        text = stringResource(
                            when {
                                live -> R.string.studio_live
                                enabled -> R.string.studio_ready
                                else -> R.string.studio_off
                            }
                        ),
                        active = enabled
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    DolbyLogo(
                        modifier = Modifier
                            .size(42.dp)
                            .squishable(enabled = true, scaleDown = 0.9f)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                logoTaps = EasterEggs.recordLogoTap(context)
                                when {
                                    logoTaps >= EasterEggs.LOGO_TAP_TARGET -> {
                                        logoTaps = 0
                                        EasterEggs.unlock(
                                            context,
                                            EasterEggs.BADGE_PERSISTENT
                                        )
                                        scope.launch {
                                            haptic.performHaptic(
                                                HapticFeedbackHelper.HapticIntensity.DOUBLE_CLICK
                                            )
                                        }
                                        ToastHelper.showToast(
                                            context,
                                            context.getString(R.string.egg_logo_unlocked)
                                        )
                                        onEasterEggUnlocked()
                                    }
                                    logoTaps == 3 -> ToastHelper.showToast(
                                        context,
                                        context.getString(R.string.egg_logo_tease_1)
                                    )
                                    logoTaps == 5 -> ToastHelper.showToast(
                                        context,
                                        context.getString(R.string.egg_logo_tease_2)
                                    )
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = stringResource(R.string.dolby_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        if (enabled) R.string.studio_tagline_on
                        else R.string.studio_tagline_off
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(26.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToggleButton(
                        checked = enabled,
                        onCheckedChange = {
                            scope.launch {
                                haptic.performHaptic(
                                    HapticFeedbackHelper.HapticIntensity.CLICK
                                )
                            }
                            onEnabledChange(it)
                        },
                        modifier = Modifier.height(52.dp),
                        shapes = ToggleButtonDefaults.shapes(),
                        contentPadding = PaddingValues(
                            horizontal = 22.dp,
                            vertical = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector = if (enabled) Icons.Rounded.GraphicEq
                            else Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (enabled) R.string.dolby_on else R.string.dolby_off
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (enabled) {
                        AnimatedEqualizerIconDynamic(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 52.dp,
                            barCount = 7
                        )
                    }
                }
            }
        }
    }
}

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
        shape = rememberAnimatedCardShape(),
        colors = CardDefaults.cardColors(
            containerColor = pageStyle.cardContainer()
        ),
        border = pageStyle.cardBorder(),
        elevation = pageStyle.cardElevation()
    ) {
        CardBody(title = title, icon = icon, content = content)
    }
}

@Composable
private fun CardBody(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
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
            .clip(if (checked) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large),
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
    valueLabel: (Int) -> String = { it.toString() },
    /** Trailing icon drawn inside the inactive end, volume-panel style. */
    icon: ImageVector? = null,
    /** Warning tint on the active fill and value chip (e.g. safe volume). */
    warning: Boolean = false,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }
    var lastHapticValue by remember { mutableIntStateOf(value) }
    // Drag-local thumb: ViewModel writes (HAL + loadSettings on IO, or
    // disk persist) return asynchronously and would otherwise keep
    // arriving after release, dragging the thumb after lift.
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
                color = if (warning) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = valueLabel(displayValue),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (warning) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Chunky volume-panel bar: full-bleed rounded track with a thin
        // cursor thumb and an optional icon in the inactive end. Bespoke
        // drag handling (not M3 Slider) because Slider insets its track by
        // the default thumb radius, which leaves gaps at both bar ends.
        ChunkyBar(
            value = sliderValue,
            valueRange = valueRange,
            steps = steps,
            icon = icon,
            warning = warning,
            onLiveChange = { newValue ->
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
            onCommit = {
                isDragging = false
                // Single commit on release: avoids queuing one HAL/IO write per
                // drag tick, whose late loadSettings emissions kept moving the
                // thumb after lift. The label already tracked the finger via
                // sliderValue, so this stays responsive.
                onValueChange(sliderValue)
                // Re-sync to the confirmed ViewModel value on the next
                // emission; if the HAL clamped it, LaunchedEffect snaps back.
            },
        )
    }
}

@Composable
private fun ChunkyBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: ImageVector?,
    warning: Boolean,
    onLiveChange: (Float) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barHeight = 44.dp
    val barShape = RoundedCornerShape(14.dp)
    val density = LocalDensity.current
    val isLtr =
        LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Ltr
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
    val interval = span / (steps + 1).coerceAtLeast(1)
    fun snap(raw: Float): Float {
        if (steps <= 0) return raw.coerceIn(valueRange.start, valueRange.endInclusive)
        val n = ((raw - valueRange.start) / interval).roundToInt()
        return (valueRange.start + n * interval)
            .coerceIn(valueRange.start, valueRange.endInclusive)
    }
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun setFromX(xPx: Float) {
        val logical = if (isLtr) xPx else barWidthPx - xPx
        onLiveChange(snap(valueRange.start + (logical / barWidthPx.coerceAtLeast(1f)) * span))
    }

    val activeColor = if (warning) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier
            .fillMaxWidth()
            .height(barHeight)
            .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                barShape
            )
            .pointerInput(valueRange, steps) {
                detectTapGestures(onTap = {
                    setFromX(it.x)
                    onCommit()
                })
            }
            .pointerInput(valueRange, steps) {
                detectDragGestures(
                    onDragStart = { setFromX(it.x) },
                    onDragEnd = { onCommit() },
                    onDragCancel = { onCommit() },
                    onDrag = { change, _ -> setFromX(change.position.x) },
                )
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val delta = when (event.key) {
                    Key.DirectionLeft -> if (isLtr) -interval else interval
                    Key.DirectionRight -> if (isLtr) interval else -interval
                    else -> return@onKeyEvent false
                }
                onLiveChange(snap(value + delta))
                onCommit()
                true
            }
            .progressSemantics(value, valueRange, steps),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(barHeight)
                .align(if (isLtr) Alignment.CenterStart else Alignment.CenterEnd)
                .background(activeColor, barShape)
        )
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
                    .size(24.dp)
            )
        }
        // Cursor line riding the fill boundary: taller than the bar so it
        // pokes out top and bottom like the reference, clamped horizontally.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    val half = 1.5.dp.toPx()
                    val x = if (isLtr) fraction * barWidthPx
                    else (1f - fraction) * barWidthPx
                    translationX = (x - half).coerceIn(0f, (barWidthPx - half * 2).coerceAtLeast(0f))
                }
                .size(width = 3.dp, height = barHeight + 12.dp)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
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

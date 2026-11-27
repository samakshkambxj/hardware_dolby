/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *           (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Lightweight haptic wrapper used by the ported Lunaris components. */
object HapticFeedbackHelper {
    enum class HapticIntensity {
        TEXTURE_TICK,
        TICK,
        CLICK,
        DOUBLE_CLICK,
        HEAVY_CLICK,
    }
}

@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val view = LocalView.current
    return remember(view) { HapticFeedback(view) }
}

class HapticFeedback(private val view: View) {
    fun performHaptic(intensity: HapticFeedbackHelper.HapticIntensity) {
        val constant =
            when (intensity) {
                HapticFeedbackHelper.HapticIntensity.TEXTURE_TICK ->
                    HapticFeedbackConstants.CLOCK_TICK
                HapticFeedbackHelper.HapticIntensity.TICK -> HapticFeedbackConstants.CLOCK_TICK
                HapticFeedbackHelper.HapticIntensity.CLICK -> HapticFeedbackConstants.CONTEXT_CLICK
                HapticFeedbackHelper.HapticIntensity.DOUBLE_CLICK ->
                    HapticFeedbackConstants.CONTEXT_CLICK
                HapticFeedbackHelper.HapticIntensity.HEAVY_CLICK ->
                    HapticFeedbackConstants.LONG_PRESS
            }
        view.performHapticFeedback(constant)
    }

    fun click() = performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
    fun doubleClick() = performHaptic(HapticFeedbackHelper.HapticIntensity.DOUBLE_CLICK)
    fun heavyClick() = performHaptic(HapticFeedbackHelper.HapticIntensity.HEAVY_CLICK)
    fun tick() = performHaptic(HapticFeedbackHelper.HapticIntensity.TICK)
    fun textureTick() = performHaptic(HapticFeedbackHelper.HapticIntensity.TEXTURE_TICK)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Modifier.expressivePress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed && enabled) scaleDown else 1f,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "expressive_press_scale",
        )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DolbyPageHeading(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (badgeText != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (badgeIcon != null) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DolbyTopAppBar(
    title: String,
    showTitle: Boolean,
    scrolled: Boolean = showTitle,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val containerColor by
        animateColorAsState(
            targetValue =
                if (scrolled) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.background
                },
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "topbar_container",
        )

    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = showTitle,
                enter =
                    fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                        slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 3 },
                exit =
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                        slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) {
                            -it / 3
                        },
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernSettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(bottom = if (subtitle != null) 14.dp else 16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (trailing != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailing()
                }
            }

            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernConfirmDialog(
    title: String,
    message: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
) {
    val iconContainer =
        if (isDestructive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    val iconTint =
        if (isDestructive) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    val confirmBg =
        if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    val confirmFg =
        if (isDestructive) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onPrimary
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = iconContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = iconTint,
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = confirmBg,
                        contentColor = confirmFg,
                    ),
            ) {
                Text(
                    text = stringResource(android.R.string.yes),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
            ) {
                Text(
                    text = stringResource(android.R.string.no),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = iconTint,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

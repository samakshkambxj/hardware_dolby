/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R
import co.aospa.dolby.ui.DolbyProfile

/**
 * Three-card profile carousel, same composition as the 17 branch but with Material 3 tonal
 * containers instead of saturated brand gradients.
 */
@Composable
fun ProfileCarousel(
    profiles: List<DolbyProfile>,
    selectedValue: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (profiles.isEmpty()) return
    val index = profiles.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
    val previous = profiles.getOrNull(index - 1)
    val current = profiles[index]
    val next = profiles.getOrNull(index + 1)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Profile",
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            previous?.let {
                ProfileCard(
                    profile = it,
                    selected = false,
                    enabled = enabled,
                    onClick = { onSelect(it.value) },
                )
                Spacer(Modifier.width(8.dp))
            }
            ProfileCard(
                profile = current,
                selected = true,
                enabled = enabled,
                onClick = { onSelect(current.value) },
            )
            next?.let {
                Spacer(Modifier.width(8.dp))
                ProfileCard(
                    profile = it,
                    selected = false,
                    enabled = enabled,
                    onClick = { onSelect(it.value) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            profiles.forEach { profile ->
                val active = profile.value == current.value
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 2.dp)
                            .height(6.dp)
                            .width(if (active) 22.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                            .clickable(enabled = enabled) { onSelect(profile.value) }
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = enabled, onClick = onReset)
                    .height(44.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint =
                    if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Reset profile",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color =
                    if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: DolbyProfile,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scale by
        animateFloatAsState(targetValue = if (selected) 1f else 0.84f, label = "profileScale")
    val contentAlpha by
        animateFloatAsState(targetValue = if (selected) 1f else 0.62f, label = "profileAlpha")
    val shape = RoundedCornerShape(26.dp)
    val brush = profileBrush(profile.value)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = contentAlpha
                }
                .width(if (selected) 184.dp else 150.dp)
                .height(140.dp)
                .clip(shape)
                .background(brush)
                .then(
                    if (selected) {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier
                    }
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .size(if (selected) 54.dp else 48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = profile.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (selected) 28.dp else 24.dp),
                    tint = contentColor,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = profile.name,
                fontSize = if (selected) 14.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            if (selected) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .width(22.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(contentColor)
                )
            }
        }
    }
}

/** Material 3 tonal gradients per profile; no brand colours are hard-coded. */
@Composable
private fun profileBrush(value: Int): Brush {
    val scheme = MaterialTheme.colorScheme
    val colors =
        when (value) {
            0 -> listOf(scheme.primaryContainer, scheme.secondaryContainer)
            1 -> listOf(scheme.tertiaryContainer, scheme.primaryContainer)
            2 -> listOf(scheme.secondaryContainer, scheme.tertiaryContainer)
            4 -> listOf(scheme.primaryContainer, scheme.tertiaryContainer)
            else -> listOf(scheme.surfaceContainerHigh, scheme.secondaryContainer)
        }
    return Brush.linearGradient(colors)
}

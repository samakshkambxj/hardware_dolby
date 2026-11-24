/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R
import co.aospa.dolby.geq.ui.EqualizerTab
import co.aospa.dolby.geq.ui.EqualizerViewModel

enum class DolbyTab {
    Home,
    Equalizer,
}

/**
 * Hosts both destinations behind a floating two-item navigation bar.
 *
 * The scroll states live here so switching tabs does not lose the user's position.
 */
@Composable
fun DolbyApp(
    dolbyViewModel: DolbyViewModel,
    equalizerViewModel: EqualizerViewModel,
    initialTab: DolbyTab = DolbyTab.Home,
) {
    val state by dolbyViewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val homeScroll = rememberScrollState()
    val equalizerScroll = rememberScrollState()

    // The equalizer is backed by the active profile's preference file. Reload whenever the
    // Home tab switches profile so edits never land in the previous profile.
    LaunchedEffect(state.profile) { equalizerViewModel.reload() }

    // Equalizer edits go straight to the controller, so refresh the Home summary when coming back.
    LaunchedEffect(tab) {
        if (tab == DolbyTab.Home) dolbyViewModel.refresh()
    }

    // A root Surface is required: it provides LocalContentColor, so Text outside the individual
    // cards inherits onBackground instead of the framework default (black).
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                DolbyTab.Home ->
                    HomeScreen(
                        state = state,
                        scrollState = homeScroll,
                        onToggle = dolbyViewModel::setDsOn,
                        onProfile = dolbyViewModel::setProfile,
                        onIeq = dolbyViewModel::setIeq,
                        onDialogue = dolbyViewModel::setDialogue,
                        onBass = dolbyViewModel::setBass,
                        onVolumeLeveler = dolbyViewModel::setVolumeLeveler,
                        onSpeakerVirtualizer = dolbyViewModel::setSpeakerVirtualizer,
                        onHeadphoneVirtualizer = dolbyViewModel::setHeadphoneVirtualizer,
                        onStereo = dolbyViewModel::setStereo,
                        onOpenEqualizer = { tab = DolbyTab.Equalizer },
                        onReset = {
                            dolbyViewModel.resetProfile()
                            equalizerViewModel.reload()
                        },
                    )
                DolbyTab.Equalizer ->
                    EqualizerTab(
                        viewModel = equalizerViewModel,
                        profileName = state.profileName,
                        scrollState = equalizerScroll,
                    )
            }

            FloatingNavBar(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun FloatingNavBar(
    selected: DolbyTab,
    onSelect: (DolbyTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(33.dp)

    Row(
        modifier =
            modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .height(66.dp)
                .shadow(elevation = 12.dp, shape = shape, clip = false)
                .clip(shape)
                .background(colors.surfaceContainer)
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.5f), shape)
                .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(
            tab = DolbyTab.Home,
            labelRes = R.string.dolby_home,
            iconRes = R.drawable.ic_home,
            filledIconRes = R.drawable.ic_home_filled,
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        NavItem(
            tab = DolbyTab.Equalizer,
            labelRes = R.string.dolby_equalizer,
            iconRes = R.drawable.ic_equalizer,
            filledIconRes = R.drawable.ic_equalizer_filled,
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NavItem(
    tab: DolbyTab,
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    @DrawableRes filledIconRes: Int,
    selected: DolbyTab,
    onSelect: (DolbyTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val active = selected == tab
    val label = stringResource(id = labelRes)
    val view = LocalView.current

    val container by
        animateColorAsState(
            targetValue = if (active) colors.primaryContainer else Color.Transparent,
            label = "nav_container",
        )
    val content by
        animateColorAsState(
            targetValue = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
            label = "nav_content",
        )

    // Selection pop: one gentle overshoot, never a wobble.
    val iconScale by
        animateFloatAsState(
            targetValue = if (active) 1f else 0.9f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
            label = "nav_icon_scale",
        )

    Row(
        modifier =
            modifier
                .fillMaxHeight()
                .clip(CircleShape)
                .background(container)
                .clickable(role = Role.Tab) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onSelect(tab)
                }
                .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tab == DolbyTab.Equalizer) {
            AnimatedEqualizerIcon(
                color = content,
                modifier =
                    Modifier.semantics {
                            if (!active) contentDescription = label
                        }
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
            )
        } else {
            val idle = rememberInfiniteTransition()
            val bob by
                idle.animateFloat(
                    initialValue = 0f,
                    targetValue = -2.5f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                )
            val tilt by
                idle.animateFloat(
                    initialValue = -4f,
                    targetValue = 4f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = 2100, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                )

            Icon(
                painter = painterResource(id = if (active) filledIconRes else iconRes),
                contentDescription = if (active) null else label,
                tint = content,
                modifier =
                    Modifier.size(21.dp)
                        .offset(y = bob.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            rotationZ = tilt
                        },
            )
        }

        // The label only exists on the active tab and grows out of the icon, the way the 17
        // navbar collapses unselected tabs to their glyph.
        AnimatedVisibility(
            visible = active,
            enter =
                expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Start,
                ) + fadeIn(animationSpec = tween(durationMillis = 120)),
            exit =
                shrinkHorizontally(
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                    shrinkTowards = Alignment.Start,
                ) + fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

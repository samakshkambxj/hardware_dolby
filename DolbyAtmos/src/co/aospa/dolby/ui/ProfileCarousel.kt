/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import co.aospa.dolby.R
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private val PROFILE_ICONS =
    mapOf(
        0 to R.drawable.ic_profile_dynamic,
        1 to R.drawable.ic_profile_movie,
        2 to R.drawable.ic_profile_music,
        3 to R.drawable.ic_profile_custom,
        4 to R.drawable.ic_profile_spatial,
    )

/**
 * Cover-flow profile picker adapted from the 17 branch.
 *
 * Pages snap one at a time, side pages tilt and fade away, and the selected card floats gently.
 * The carousel reads the same [R.array.dolby_profile_values] order the controller uses, so the
 * page index and the profile id stay decoupled.
 *
 * Colours are pure Material 3 tonal roles: the selected card uses `primaryContainer`, the rest
 * `surfaceContainerHigh` on a `surfaceContainer` card, so the carousel follows the system palette
 * like the rest of Settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileCarousel(
    currentProfile: Int,
    onProfileChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val labels = stringArrayResource(id = R.array.dolby_profile_entries)
    val values = stringArrayResource(id = R.array.dolby_profile_values)
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val initialPage = values.indexOfFirst { it.toInt() == currentProfile }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { labels.size })
    var lastPage by remember { mutableIntStateOf(initialPage) }

    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page == lastPage) return@LaunchedEffect
        lastPage = page
        if (!enabled) return@LaunchedEffect
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onProfileChange(values[page].toInt())
    }

    // Follow profile changes made elsewhere (tile, reset) without fighting a drag in progress.
    LaunchedEffect(currentProfile) {
        val target = values.indexOfFirst { it.toInt() == currentProfile }
        if (target >= 0 && target != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(target)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.dolby_profile_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentPadding = PaddingValues(horizontal = 64.dp),
                pageSpacing = 8.dp,
                userScrollEnabled = enabled,
            ) { page ->
                val value = values[page].toInt()
                val pageOffset =
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                ProfileCard(
                    label = labels[page],
                    iconRes = PROFILE_ICONS[value] ?: R.drawable.ic_profile_dynamic,
                    isSelected = page == pagerState.currentPage,
                    pageOffset = pageOffset,
                    enabled = enabled,
                    onClick = {
                        if (enabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    },
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(labels.size) { index ->
                    val selected = pagerState.currentPage == index

                    Box(
                        modifier =
                            Modifier.padding(3.dp)
                                .size(width = if (selected) 24.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) colors.primary else colors.outlineVariant
                                )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    label: String,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    pageOffset: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
    val scale = lerp(start = 0.8f, stop = 1f, fraction = fraction)
    val alpha = lerp(start = 0.5f, stop = 1f, fraction = fraction)
    val density = LocalDensity.current

    // 3D cover-flow tilt: side pages lean away with perspective.
    val tiltY = (pageOffset * -14f).coerceIn(-14f, 14f)

    val iconScale by
        animateFloatAsState(
            targetValue = if (isSelected) 1f else 0.9f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "profile_icon_scale",
        )

    val idle = rememberInfiniteTransition()
    val bob by
        idle.animateFloat(
            initialValue = 0f,
            targetValue = -4f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
        )

    Card(
        modifier =
            Modifier.fillMaxHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    rotationY = tiltY
                    cameraDistance = 12f * density.density
                }
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh,
                contentColor =
                    if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    modifier =
                        Modifier.size(56.dp)
                            .offset(y = (if (isSelected) bob else 0f).dp)
                            .scale(iconScale),
                    shape = CircleShape,
                    color = if (isSelected) colors.primary else colors.surfaceContainerHighest,
                    contentColor =
                        if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                Spacer(Modifier.height(6.dp))

                if (isSelected) {
                    Box(
                        Modifier.height(2.dp)
                            .width(24.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                }
            }

            if (isSelected) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(24.dp),
                    shape = CircleShape,
                    color = colors.primary,
                    contentColor = colors.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

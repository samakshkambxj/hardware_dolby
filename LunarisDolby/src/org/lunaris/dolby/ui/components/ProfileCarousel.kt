/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.utils.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileCarousel(
    currentProfile: Int,
    onProfileChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles = stringArrayResource(R.array.dolby_profile_entries)
    val profileValues = stringArrayResource(R.array.dolby_profile_values)
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    val profileIcons = mapOf(
        0 to Icons.Default.AutoAwesome,
        1 to Icons.Default.Movie,
        2 to Icons.Default.MusicNote,
        3 to Icons.Default.SportsEsports,
        4 to Icons.Default.Work,
        5 to Icons.Default.Coffee,
        6 to Icons.Default.Favorite
    )
    
    val profilePalettes = rememberProfilePalettes()

    val initialPage = profileValues.indexOfFirst { it.toInt() == currentProfile }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { profiles.size }
    )
    
    var lastPage by remember { mutableIntStateOf(initialPage) }
    
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != lastPage) {
            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
            lastPage = pagerState.currentPage
            
            if (pagerState.currentPage != initialPage) {
                val selectedValue = profileValues[pagerState.currentPage].toInt()
                onProfileChange(selectedValue)
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.dolby_profile_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentPadding = PaddingValues(horizontal = 64.dp),
                pageSpacing = 8.dp
            ) { page ->
                val profileValue = profileValues[page].toInt()
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                
                ProfileCard(
                    profile = profiles[page],
                    icon = profileIcons[profileValue] ?: Icons.Default.Tune,
                    palette = profilePalettes.getOrElse(profileValue) { profilePalettes[0] },
                    isSelected = page == pagerState.currentPage,
                    pageOffset = pageOffset,
                    onClick = {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                            pagerState.animateScrollToPage(page)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(profiles.size) { index ->
                    val isSelected = pagerState.currentPage == index

                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .size(
                                width = if (isSelected) 24.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    profilePalettes.getOrElse(index) { profilePalettes[0] }.start
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

/**
 * A profile's colour pair. Each profile gets its own vivid fixed gradient so
 * the carousel looks colorful regardless of dynamic-color theming.
 * Content is always white for contrast on the saturated gradients.
 */
internal data class ProfilePalette(
    val start: Color,
    val end: Color,
    val content: Color
)

@Composable
private fun rememberProfilePalettes(): List<ProfilePalette> {
    return remember {
        val content = Color.White
        listOf(
            // 0 Dynamic — deep violet → bright sky blue
            ProfilePalette(Color(0xFF6200EA), Color(0xFF00B0FF), content),
            // 1 Movie — pure red → vivid orange
            ProfilePalette(Color(0xFFFF1744), Color(0xFFFF9100), content),
            // 2 Music — neon fuchsia → deep violet
            ProfilePalette(Color(0xFFD500F9), Color(0xFF6200EA), content),
            // 3 Game — neon green → cyan
            ProfilePalette(Color(0xFF00E676), Color(0xFF00B8D4), content),
            // 4 Work — vivid blue → bright cyan
            ProfilePalette(Color(0xFF2962FF), Color(0xFF00E5FF), content),
            // 5 Casual — bright amber → ember orange
            ProfilePalette(Color(0xFFFFAB00), Color(0xFFFF3D00), content),
            // 6 Mood — hot pink → violet
            ProfilePalette(Color(0xFFF50057), Color(0xFF7C4DFF), content)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileCard(
    profile: String,
    icon: ImageVector,
    palette: ProfilePalette,
    isSelected: Boolean,
    pageOffset: Float,
    onClick: () -> Unit
) {
    val scale = lerp(
        start = 0.8f,
        stop = 1f,
        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
    )
    
    val alpha = lerp(
        start = 0.5f,
        stop = 1f,
        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
    )

    val density = LocalDensity.current
    // 3D cover-flow tilt: side pages lean away with perspective.
    val tiltY = (pageOffset * -14f).coerceIn(-14f, 14f)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                rotationY = tiltY
                cameraDistance = 12f * density.density
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = palette.end,
            contentColor = palette.content
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(palette.start, palette.end)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.85f,
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                    label = "icon_scale"
                )

                // Idle 3D motion — only the selected icon animates to save work.
                var bobY = 0f
                var swayY = 0f
                var sheenAlpha = 0.45f
                if (isSelected) {
                    val idle = rememberInfiniteTransition(label = "icon_idle")
                    bobY = idle.animateFloat(
                        initialValue = 0f,
                        targetValue = -5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bob"
                    ).value
                    swayY = idle.animateFloat(
                        initialValue = -10f,
                        targetValue = 10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2400, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "sway"
                    ).value
                    sheenAlpha = idle.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.55f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "sheen"
                    ).value
                }

                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .offset(y = bobY.dp)
                        .scale(iconScale)
                        .shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    color = palette.content.copy(alpha = 0.22f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                    ) {
                        // Extruded drop layer for depth.
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(36.dp)
                                .offset(x = 1.5.dp, y = 2.dp)
                        )
                        // Main face, counter-tilted for floating parallax + idle sway.
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.content,
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    rotationY = -tiltY * 0.6f + swayY
                                    cameraDistance = 12f * density.density
                                }
                        )
                        // Glossy top-light sheen, breathing with the idle motion.
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = sheenAlpha),
                                            Color.Transparent
                                        ),
                                        radius = 220f
                                    )
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = profile,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.content,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isSelected) {
                    Surface(
                        modifier = Modifier.height(2.dp).width(24.dp),
                        shape = CircleShape,
                        color = palette.content
                    ) {}
                }
            }
            
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(24.dp),
                    shape = CircleShape,
                    color = palette.content
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = palette.start,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

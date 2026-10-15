/*
 * Copyright (C) 2026 Anshuman _X (maxxcodebug)
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dolbyatmos.R
import org.dolbyatmos.ui.components.StyledParticles
import org.dolbyatmos.ui.components.ModernSettingSlider
import org.dolbyatmos.ui.components.ModernSettingsCard
import org.dolbyatmos.ui.components.verticalBouncyEdge
import org.dolbyatmos.utils.HapticFeedbackHelper
import org.dolbyatmos.utils.rememberHapticFeedback

private data class VolumeStreamInfo(
    val streamType: Int,
    val title: String,
    val icon: ImageVector
)

/** Media fraction that trips the safe-volume nudge. */
private const val SAFE_FRACTION = 0.7f
private const val KEY_SAFE_ACK = "safe_volume_ack"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VolumeControlScreen() {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    // Dismissible safe-volume acknowledgement (shared dolby_prefs).
    val prefs = remember {
        context.getSharedPreferences("dolby_prefs", Context.MODE_PRIVATE)
    }
    var safeAck by remember { mutableStateOf(prefs.getBoolean(KEY_SAFE_ACK, false)) }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val streams = remember {
        listOf(
            VolumeStreamInfo(
                AudioManager.STREAM_MUSIC,
                "Media",
                Icons.Default.MusicNote
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_RING,
                "Ringtone",
                Icons.Default.Notifications
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_NOTIFICATION,
                "Notification",
                Icons.Default.NotificationsActive
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_ALARM,
                "Alarm",
                Icons.Default.Alarm
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_VOICE_CALL,
                "Call",
                Icons.Default.Call
            )
        )
    }

    fun readVolumes(): Map<Int, Int> =
        streams.associate { stream ->
            stream.streamType to try {
                audioManager.getStreamVolume(stream.streamType)
            } catch (_: Exception) {
                0
            }
        }

    var volumes by remember { mutableStateOf(readVolumes()) }

    fun refreshVolumes() {
        volumes = readVolumes()
    }

    // Reflect hardware volume keys / system volume panel changes while visible.
    DisposableEffect(context) {
        refreshVolumes()
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.media.STREAM_DEVICES_CHANGED_ACTION")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshVolumes()
            }
        }
        // VOLUME_CHANGED_ACTION is protected on some builds; fall back gracefully.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {
            // No live sync; sliders still read fresh values on recompose.
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Keep action icons off the screen edge (M3 only insets 4.dp).
                modifier = Modifier.padding(end = 8.dp),
                title = {
                    Text(
                        stringResource(R.string.volume),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        StyledParticles(modifier = Modifier.padding(paddingValues))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .verticalBouncyEdge()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = streams,
                key = { it.streamType }
            ) { stream ->
                val max = remember(stream.streamType) {
                    try {
                        audioManager.getStreamMaxVolume(stream.streamType)
                    } catch (_: Exception) {
                        15
                    }
                }
                val min = remember(stream.streamType) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            audioManager.getStreamMinVolume(stream.streamType)
                        } catch (_: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                }
                val current = (volumes[stream.streamType]
                    ?: try {
                        audioManager.getStreamVolume(stream.streamType)
                    } catch (_: Exception) {
                        min
                    }).coerceIn(min, max)

                ModernSettingsCard(
                    title = stream.title,
                    icon = stream.icon
                ) {
                    val isMusic = stream.streamType == AudioManager.STREAM_MUSIC
                    val fraction = if (max > min) {
                        (current - min).toFloat() / (max - min).toFloat()
                    } else 0f
                    val showSafeNudge = isMusic && !safeAck && fraction >= SAFE_FRACTION

                    // Heavy buzz exactly once per crossing into the red zone.
                    LaunchedEffect(showSafeNudge) {
                        if (showSafeNudge) {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.HEAVY_CLICK)
                        }
                    }

                    ModernSettingSlider(
                        title = "Level",
                        value = current,
                        valueRange = min.toFloat()..max.toFloat(),
                        steps = (max - min - 1).coerceAtLeast(0),
                        valueLabel = { "$it / $max" },
                        icon = stream.icon,
                        warning = showSafeNudge,
                        onValueChange = { newValue ->
                            val target = newValue.toInt().coerceIn(min, max)
                            // Optimistic UI update; receiver will correct it if clamped.
                            volumes = volumes.toMutableMap().apply {
                                put(stream.streamType, target)
                            }
                            try {
                                audioManager.setStreamVolume(
                                    stream.streamType,
                                    target,
                                    0
                                )
                            } catch (_: SecurityException) {
                                // DND / policy restricted stream; re-read actual value.
                                refreshVolumes()
                            } catch (_: Exception) {
                                refreshVolumes()
                            }
                        }
                    )
                    if (showSafeNudge) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High volume — long sessions here can damage hearing",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                safeAck = true
                                prefs.edit().putBoolean(KEY_SAFE_ACK, true).apply()
                            }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
        }
    }
}

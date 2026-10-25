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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dolbyatmos.R
import org.dolbyatmos.ui.components.ModernSettingSlider
import org.dolbyatmos.ui.components.ModernSettingsCard
import org.dolbyatmos.ui.components.StudioAmbientBackground
import org.dolbyatmos.ui.components.StudioCard
import org.dolbyatmos.ui.components.StudioHeader
import org.dolbyatmos.ui.components.StudioSectionHeader
import org.dolbyatmos.ui.components.StyledParticles
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

    fun setVolume(streamType: Int, value: Int) {
        volumes = volumes.toMutableMap().apply { put(streamType, value) }
        try {
            audioManager.setStreamVolume(streamType, value, 0)
        } catch (_: SecurityException) {
            refreshVolumes()
        } catch (_: Exception) {
            refreshVolumes()
        }
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

    val mediaStream = streams.first()

    Scaffold(
        topBar = {
            StudioHeader(
                eyebrow = "Mixer",
                title = stringResource(R.string.volume),
                subtitle = "Independent levels for every stream"
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            StudioAmbientBackground()
            StyledParticles(modifier = Modifier.padding(paddingValues))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalBouncyEdge()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 124.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item(key = "media_header") {
                    StudioSectionHeader(
                        title = "Now playing",
                        eyebrow = "Media",
                        subtitle = "The level everything else is tuned around."
                    )
                }

                item(key = "media_hero") {
                    val max = remember(mediaStream.streamType) {
                        try {
                            audioManager.getStreamMaxVolume(mediaStream.streamType)
                        } catch (_: Exception) {
                            15
                        }
                    }
                    val min = remember(mediaStream.streamType) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            try {
                                audioManager.getStreamMinVolume(mediaStream.streamType)
                            } catch (_: Exception) {
                                0
                            }
                        } else {
                            0
                        }
                    }
                    val current = (volumes[mediaStream.streamType]
                        ?: min).coerceIn(min, max)
                    val fraction = if (max > min) {
                        (current - min).toFloat() / (max - min).toFloat()
                    } else 0f
                    val showSafeNudge = !safeAck && fraction >= SAFE_FRACTION
                    val percent = (fraction * 100f).toInt()

                    LaunchedEffect(showSafeNudge) {
                        if (showSafeNudge) {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.HEAVY_CLICK)
                        }
                    }

                    StudioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = mediaStream.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaStream.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (showSafeNudge) "Loud — watch your ears"
                                    else "System media stream",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        ModernSettingSlider(
                            title = "Level",
                            value = current,
                            valueRange = min.toFloat()..max.toFloat(),
                            steps = (max - min - 1).coerceAtLeast(0),
                            valueLabel = { "$it / $max" },
                            icon = mediaStream.icon,
                            warning = showSafeNudge,
                            onValueChange = { newValue ->
                                val target = newValue.toInt().coerceIn(min, max)
                                scope.launch {
                                    haptic.performHaptic(
                                        HapticFeedbackHelper.HapticIntensity.CLICK
                                    )
                                }
                                setVolume(mediaStream.streamType, target)
                            }
                        )

                        if (showSafeNudge) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "High volume — long sessions here can damage hearing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
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

                item(key = "other_header") {
                    StudioSectionHeader(
                        title = "Other streams",
                        eyebrow = "System"
                    )
                }

                items(
                    items = streams.drop(1),
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
                        ModernSettingSlider(
                            title = "Level",
                            value = current,
                            valueRange = min.toFloat()..max.toFloat(),
                            steps = (max - min - 1).coerceAtLeast(0),
                            valueLabel = { "$it / $max" },
                            icon = stream.icon,
                            onValueChange = { newValue ->
                                val target = newValue.toInt().coerceIn(min, max)
                                setVolume(stream.streamType, target)
                            }
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

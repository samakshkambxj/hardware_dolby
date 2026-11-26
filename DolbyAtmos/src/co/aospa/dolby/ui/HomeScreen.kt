/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.aospa.dolby.R

private data class IeqOption(
    val value: Int,
    val labelRes: Int,
    val iconRes: Int,
)

private val IEQ_OPTIONS =
    listOf(
        IeqOption(0, R.string.dolby_off, R.drawable.ic_ieq_off),
        IeqOption(1, R.string.dolby_balanced, R.drawable.ic_ieq_balanced),
        IeqOption(2, R.string.dolby_warm, R.drawable.ic_ieq_warm),
        IeqOption(3, R.string.dolby_detailed, R.drawable.ic_ieq_detailed),
    )

@Composable
fun HomeScreen(
    state: DolbyUiState,
    scrollState: ScrollState = rememberScrollState(),
    onToggle: (Boolean) -> Unit,
    onProfile: (Int) -> Unit,
    onIeq: (Int) -> Unit,
    onDialogue: (Int) -> Unit,
    onBass: (Boolean) -> Unit,
    onVolumeLeveler: (Boolean) -> Unit,
    onSpeakerVirtualizer: (Boolean) -> Unit,
    onHeadphoneVirtualizer: (Boolean) -> Unit,
    onStereo: (Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    onReset: () -> Unit,
) {
    var showReset by remember { mutableStateOf(false) }
    val profileLabel = state.profileName.ifBlank { stringResource(R.string.dolby_unknown) }
    val deviceLabel =
        state.deviceName.ifBlank {
            stringResource(
                if (state.isOnSpeaker) R.string.dolby_phone_speaker else R.string.dolby_headphones
            )
        }

    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().widthIn(max = 640.dp).fillMaxWidth(),
        ) {
            HomeHeader(
                onOpenEqualizer = onOpenEqualizer,
                onReset = { showReset = true },
                enabled = state.enabled,
            )

            Column(
                modifier =
                    Modifier.weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 104.dp),
            ) {
                DolbyMainCard(
                    enabled = state.dsOn,
                    onEnabledChange = onToggle,
                    statusText =
                        if (state.dsOn) {
                            stringResource(R.string.dolby_on_with_profile, profileLabel)
                        } else {
                            stringResource(R.string.dolby_off)
                        },
                    outputDeviceText = deviceLabel,
                    outputDeviceIcon =
                        if (state.isOnSpeaker) {
                            Icons.Rounded.Speaker
                        } else {
                            Icons.Rounded.Headphones
                        },
                )

                Column(Modifier.alpha(if (state.enabled) 1f else 0.38f)) {
                    Spacer(Modifier.height(20.dp))
                    ProfileCarousel(
                        currentProfile = state.profile,
                        onProfileChange = onProfile,
                        enabled = state.enabled,
                    )

                    SectionLabel(
                        title = stringResource(id = R.string.dolby_section_sound),
                        meta = state.profileName,
                    )
                    SoundCard(
                        state = state,
                        onIeq = onIeq,
                        onDialogue = onDialogue,
                        onBass = onBass,
                        onVolumeLeveler = onVolumeLeveler,
                        onOpenEqualizer = onOpenEqualizer,
                    )

                    SectionLabel(
                        title = stringResource(id = R.string.dolby_section_devices),
                        meta = state.deviceName,
                    )
                    DeviceCard(
                        state = state,
                        onSpeakerVirtualizer = onSpeakerVirtualizer,
                        onHeadphoneVirtualizer = onHeadphoneVirtualizer,
                        onStereo = onStereo,
                    )

                    ResetButton(enabled = state.enabled, onClick = { showReset = true })
                }
            }
        }
    }

    if (showReset) {
        ResetDialog(
            profileName = state.profileName,
            onConfirm = {
                showReset = false
                onReset()
            },
            onDismiss = { showReset = false },
        )
    }
}

@Composable
private fun HomeHeader(
    onOpenEqualizer: () -> Unit,
    onReset: () -> Unit,
    enabled: Boolean,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.dolby_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.dolby_more_options),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.dolby_preset)) },
                    enabled = enabled,
                    onClick = {
                        menuExpanded = false
                        onOpenEqualizer()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.dolby_reset_profile)) },
                    enabled = enabled,
                    onClick = {
                        menuExpanded = false
                        onReset()
                    },
                )
            }
        }
    }
}

@Composable
private fun SoundCard(
    state: DolbyUiState,
    onIeq: (Int) -> Unit,
    onDialogue: (Int) -> Unit,
    onBass: (Boolean) -> Unit,
    onVolumeLeveler: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    val unknown = stringResource(id = R.string.dolby_unknown)
    val ieqEntries = stringArrayResource(id = R.array.dolby_ieq_entries)
    val dialogueEntries = stringArrayResource(id = R.array.dolby_dialogue_entries)
    val dialogueValues = stringArrayResource(id = R.array.dolby_dialogue_values).map { it.toInt() }

    val ieqLabel = ieqEntries.getOrElse(state.ieq) { unknown }
    val dialogueIndex = dialogueValues.indexOf(state.dialogue)
    val dialogueLabel = if (dialogueIndex >= 0) dialogueEntries[dialogueIndex] else unknown

    var showDialogue by remember { mutableStateOf(false) }

    SettingsGroup {
        IeqGroup(state = state, label = ieqLabel, onIeq = onIeq)

        ActionRow(
            icon = R.drawable.ic_eq_curve,
            title = stringResource(id = R.string.dolby_preset),
            subtitle =
                stringResource(
                    id = R.string.dolby_graphic_equalizer_summary,
                    state.presetName.ifBlank { unknown },
                ),
            enabled = state.enabled,
            onClick = onOpenEqualizer,
            trailing = {
                Sparkline(gains = state.presetGains)
                Spacer(Modifier.width(4.dp))
                Chevron()
            },
        )

        SwitchRow(
            icon = R.drawable.ic_bass,
            title = stringResource(id = R.string.dolby_bass_enhancer),
            subtitle = stringResource(id = R.string.dolby_bass_enhancer_summary),
            checked = state.bass,
            enabled = state.enabled,
            onCheckedChange = onBass,
        )

        ActionRow(
            icon = R.drawable.ic_dialogue,
            title = stringResource(id = R.string.dolby_dialogue_enhancer),
            subtitle = stringResource(id = R.string.dolby_dialogue_enhancer_summary),
            enabled = state.enabled,
            onClick = { showDialogue = true },
            trailing = {
                ValuePill(text = dialogueLabel)
                Chevron()
            },
        )

        SwitchRow(
            icon = R.drawable.ic_volume_leveler,
            title = stringResource(id = R.string.dolby_volume_leveler),
            subtitle = stringResource(id = R.string.dolby_volume_leveler_summary),
            checked = state.volumeLeveler,
            enabled = state.enabled,
            onCheckedChange = onVolumeLeveler,
        )
    }

    if (showDialogue) {
        ChoiceDialog(
            title = stringResource(id = R.string.dolby_dialogue_enhancer),
            entries = dialogueEntries.toList(),
            selectedIndex = dialogueIndex,
            onSelect = { index -> onDialogue(dialogueValues[index]) },
            onDismiss = { showDialogue = false },
        )
    }
}

@Composable
private fun IeqGroup(state: DolbyUiState, label: String, onIeq: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth().background(colors.surfaceContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIcon(R.drawable.ic_equalizer)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.dolby_ieq),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            IEQ_OPTIONS.forEach { option ->
                val selected = state.ieq == option.value
                Surface(
                    modifier =
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .selectable(
                                selected = selected,
                                enabled = state.enabled,
                                role = Role.RadioButton,
                                onClick = { onIeq(option.value) },
                            ),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) colors.primaryContainer else colors.surface,
                    contentColor =
                        if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    border =
                        if (selected) {
                            BorderStroke(1.dp, colors.primary.copy(alpha = 0.34f))
                        } else null,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(id = option.iconRes),
                            contentDescription = null,
                            tint = if (selected) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(id = option.labelRes),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    state: DolbyUiState,
    onSpeakerVirtualizer: (Boolean) -> Unit,
    onHeadphoneVirtualizer: (Boolean) -> Unit,
    onStereo: (Int) -> Unit,
) {
    val connect = stringResource(id = R.string.dolby_connect_headphones)
    val unknown = stringResource(id = R.string.dolby_unknown)
    val stereoEntries = stringArrayResource(id = R.array.dolby_stereo_entries)
    val stereoValues = stringArrayResource(id = R.array.dolby_stereo_values).map { it.toInt() }
    val stereoIndex = stereoValues.indexOf(state.stereo)
    val stereoLabel = if (stereoIndex >= 0) stereoEntries[stereoIndex] else unknown

    var showStereo by remember { mutableStateOf(false) }

    SettingsGroup {
        SwitchRow(
            icon = R.drawable.ic_phone,
            title = stringResource(id = R.string.dolby_spk_virtualizer),
            subtitle = stringResource(id = R.string.dolby_spk_virtualizer_summary),
            checked = state.speakerVirtualizer,
            enabled = state.enabled,
            onCheckedChange = onSpeakerVirtualizer,
        )

        SwitchRow(
            icon = R.drawable.ic_headphones,
            title = stringResource(id = R.string.dolby_hp_virtualizer),
            subtitle =
                if (state.isOnSpeaker) {
                    connect
                } else {
                    stringResource(id = R.string.dolby_hp_virtualizer_summary)
                },
            checked = state.headphoneVirtualizer,
            enabled = state.enabled && !state.isOnSpeaker,
            onCheckedChange = onHeadphoneVirtualizer,
        )

        if (state.stereoSupported) {
            ActionRow(
                icon = R.drawable.ic_stereo,
                title = stringResource(id = R.string.dolby_stereo_widening),
                subtitle =
                    if (state.isOnSpeaker) {
                        connect
                    } else {
                        stringResource(id = R.string.dolby_stereo_widening_summary)
                    },
                enabled = state.enabled && !state.isOnSpeaker,
                onClick = { showStereo = true },
                trailing = {
                    ValuePill(text = stereoLabel)
                    Chevron()
                },
            )
        }
    }

    if (showStereo) {
        ChoiceDialog(
            title = stringResource(id = R.string.dolby_stereo_widening),
            entries = stereoEntries.toList(),
            selectedIndex = stereoIndex,
            onSelect = { index -> onStereo(stereoValues[index]) },
            onDismiss = { showStereo = false },
        )
    }
}

/**
 * Single-choice dialog, the way Settings renders a list of levels: radio rows, no confirm button,
 * the value applies as soon as it is picked.
 */
@Composable
private fun ChoiceDialog(
    title: String,
    entries: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                entries.forEachIndexed { index, label ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = index == selectedIndex,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelect(index)
                                        onDismiss()
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = null,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = label, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

@Composable
private fun SwitchRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.surfaceContainer)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ActionRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.surfaceContainer)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun RowIcon(@DrawableRes icon: Int) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            Modifier.size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ValuePill(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface,
        contentColor = colors.onSurface,
        shadowElevation = 1.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        painter = painterResource(id = R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(19.dp),
    )
}

@Composable
private fun Sparkline(gains: List<Int>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.width(58.dp).height(24.dp)) {
        if (gains.isEmpty()) return@Canvas
        val path = Path()
        val step = size.width / (gains.size - 1).coerceAtLeast(1)
        gains.forEachIndexed { index, gain ->
            val x = index * step
            val y = size.height / 2f - (gain / 100f) * (size.height / 2f - 2.dp.toPx())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun ResetButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = colors.error,
                disabledContentColor = colors.error.copy(alpha = 0.38f),
            ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.reset_settings_24px),
            contentDescription = null,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.dolby_reset_profile),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionLabel(title: String, meta: String) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = meta,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
    }
}

@Composable
private fun ResetDialog(profileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.reset_settings_24px),
                contentDescription = null,
                tint = colors.error,
            )
        },
        title = { Text(stringResource(id = R.string.dolby_reset_profile_title, profileName)) },
        text = { Text(stringResource(id = R.string.dolby_reset_profile_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.dolby_reset), color = colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
    )
}

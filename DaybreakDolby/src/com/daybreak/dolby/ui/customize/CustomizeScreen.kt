/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.components.DayCard
import com.daybreak.dolby.ui.components.DayOptionRow
import com.daybreak.dolby.ui.components.DayScreen
import com.daybreak.dolby.ui.components.DaySectionHeader
import com.daybreak.dolby.ui.components.DaySettingSlider
import com.daybreak.dolby.ui.components.DaySwitchRow
import com.daybreak.dolby.ui.theme.AccentChoice
import com.daybreak.dolby.ui.theme.previewColor

@Composable
fun CustomizeScreen() {
    val context = LocalContext.current
    val customize by rememberCustomize()
    val repo = rememberCustomizeRepo()

    DayScreen {
        item { DaySectionHeader(text = stringResource(R.string.custom_theme)) }

        item {
            DayCard {
                DaySwitchRow(
                    title = stringResource(R.string.custom_dynamic),
                    summary = stringResource(R.string.custom_dynamic_summary),
                    checked = customize.dynamicColor,
                    enabled = true,
                    onToggle = { repo.update { it.copy(dynamicColor = it) } },
                )
                DaySwitchRow(
                    title = stringResource(R.string.custom_amoled),
                    summary = stringResource(R.string.custom_amoled_summary),
                    checked = customize.amoled,
                    enabled = true,
                    onToggle = { repo.update { it.copy(amoled = it) } },
                )
            }
        }

        item { DaySectionHeader(text = stringResource(R.string.custom_accent)) }

        item {
            DayCard {
                Column(Modifier.padding(16.dp)) {
                    AccentGrid(
                        selected = customize.accent,
                        onSelect = { repo.update { it.copy(accent = it) } },
                    )
                }
            }
        }

        item { DaySectionHeader(text = stringResource(R.string.custom_cards)) }

        item {
            DayCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.custom_corners),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    DayOptionRow(
                        options = CornerStyle.entries.map { it.name.lowercase() },
                        selectedIndex = CornerStyle.entries.indexOf(customize.cornerStyle),
                        onSelect = { repo.update { it.copy(cornerStyle = CornerStyle.entries[it]) } },
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.custom_card_style),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    DayOptionRow(
                        options = CardStyle.entries.map { it.name.lowercase() },
                        selectedIndex = CardStyle.entries.indexOf(customize.cardStyle),
                        onSelect = { repo.update { it.copy(cardStyle = CardStyle.entries[it]) } },
                    )
                }
            }
        }

        item { DaySectionHeader(text = stringResource(R.string.custom_navbar)) }

        item {
            DayCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.custom_design),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    DayOptionRow(
                        options = BarDesign.entries.map { it.name.lowercase() },
                        selectedIndex = BarDesign.entries.indexOf(customize.barDesign),
                        onSelect = { repo.update { it.copy(barDesign = BarDesign.entries[it]) } },
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.custom_style),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    DayOptionRow(
                        options = BarStyle.entries.map { it.name.lowercase() },
                        selectedIndex = BarStyle.entries.indexOf(customize.barStyle),
                        onSelect = { repo.update { it.copy(barStyle = BarStyle.entries[it]) } },
                        itemsPerRow = 2,
                    )
                }
            }
        }

        item {
            DayCard {
                DaySwitchRow(
                    title = stringResource(R.string.custom_rainbow),
                    summary = stringResource(R.string.custom_rainbow_summary),
                    checked = customize.rainbow,
                    enabled = true,
                    onToggle = { repo.update { it.copy(rainbow = it) } },
                )
                DaySettingSlider(
                    title = stringResource(R.string.custom_intensity),
                    value = (customize.glassIntensity * 100).toInt(),
                    valueRange = 0f..100f,
                    steps = 19,
                    valueLabel = { "$it%" },
                    onCommit = { repo.update { it.copy(glassIntensity = it / 100f) } },
                )
                DaySettingSlider(
                    title = stringResource(R.string.custom_tint),
                    value = (customize.navTint * 100).toInt(),
                    valueRange = 0f..100f,
                    steps = 19,
                    valueLabel = { "$it%" },
                    onCommit = { repo.update { it.copy(navTint = it / 100f) } },
                )
                DaySettingSlider(
                    title = stringResource(R.string.custom_blur),
                    value = customize.navBlurRadius.toInt(),
                    valueRange = 0f..60f,
                    steps = 14,
                    valueLabel = { "$it" },
                    onCommit = { repo.update { it.copy(navBlurRadius = it.toFloat()) } },
                )
            }
        }
    }
}

@Composable
private fun rememberCustomizeRepo(): CustomizeRepository {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        CustomizeRepository.get(context.applicationContext)
    }
}

@Composable
private fun AccentGrid(
    selected: AccentChoice,
    onSelect: (AccentChoice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentChoice.entries.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { accent ->
                    val isSelected = accent == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(accent.previewColor)
                            .clickableOption(
                                selected = isSelected,
                                onClick = { onSelect(accent) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary),
                            )
                        }
                    }
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun Modifier.clickableOption(
    selected: Boolean,
    onClick: () -> Unit,
): Modifier = this.then(
    androidx.compose.foundation.border(
        width = if (selected) 3.dp else 0.dp,
        color = if (selected) MaterialTheme.colorScheme.primary
        else androidx.compose.ui.graphics.Color.Transparent,
        shape = MaterialTheme.shapes.medium,
    ),
).then(
    androidx.compose.foundation.clickable(
        role = androidx.compose.ui.semantics.Role.Button,
        onClick = onClick,
    ),
)

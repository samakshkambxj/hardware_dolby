/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.lunaris.dolby.ui.components.*
import org.lunaris.dolby.ui.theme.previewColor
import org.lunaris.dolby.utils.AppIconManager
import org.lunaris.dolby.utils.ToastHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageStyleScreen(navController: NavController) {
    val repo = rememberPageStyleRepository()
    val style by repo.style.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var currentIconId by remember { mutableStateOf(AppIconManager.current(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(end = 8.dp),
                title = {
                    Text(
                        "Customization",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live preview of the card + icon style below.
            ModernSettingsCard(title = "Preview", icon = Icons.Default.Palette) {
                ModernSettingSwitch(
                    title = "Sample switch",
                    subtitle = "Cards, icons and corners update live",
                    checked = true,
                    onCheckedChange = {}
                )
            }

            ModernSettingsCard(title = "Header", icon = Icons.Default.Edit) {
                OutlinedTextField(
                    value = style.headerTitle,
                    onValueChange = { v -> repo.update { it.copy(headerTitle = v) } },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = style.subtitleText,
                    onValueChange = { v -> repo.update { it.copy(subtitleText = v) } },
                    label = { Text("Subtitle") },
                    singleLine = true,
                    enabled = style.showSubtitle,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                ModernSettingSwitch(
                    title = "Show title",
                    subtitle = "Display the header title",
                    checked = style.showTitle,
                    onCheckedChange = { checked -> repo.update { it.copy(showTitle = checked) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModernSettingSwitch(
                    title = "Show subtitle",
                    subtitle = "Display the header subtitle",
                    checked = style.showSubtitle,
                    onCheckedChange = { checked -> repo.update { it.copy(showSubtitle = checked) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModernSettingSwitch(
                    title = "Center header",
                    subtitle = "Center the title on the home page",
                    checked = style.headerCentered,
                    onCheckedChange = { checked -> repo.update { it.copy(headerCentered = checked) } }
                )
            }

            ModernSettingsCard(title = "Header banner", icon = Icons.Default.Image) {
                ModernSettingSwitch(
                    title = "Waveform banner",
                    subtitle = "Animated visualizer behind the logo",
                    checked = style.showBannerWaveform,
                    onCheckedChange = { checked ->
                        repo.update { it.copy(showBannerWaveform = checked) }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModernSettingSwitch(
                    title = "Banner gradient",
                    subtitle = "Fade the banner into the card",
                    checked = style.bannerGradient,
                    onCheckedChange = { checked ->
                        repo.update { it.copy(bannerGradient = checked) }
                    }
                )
            }

            ModernSettingsCard(title = "Cards", icon = Icons.Default.Dashboard) {
                StyleOptionRow(
                    options = PageCardStyle.entries.map {
                        it.name.lowercase().replaceFirstChar(Char::uppercase)
                    },
                    selectedIndex = PageCardStyle.entries.indexOf(style.cardStyle),
                    onSelect = { index ->
                        repo.update { it.copy(cardStyle = PageCardStyle.entries[index]) }
                    }
                )
            }

            ModernSettingsCard(title = "Icons", icon = Icons.Default.Tune) {
                Text(
                    "Icon color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StyleOptionRow(
                    options = PageIconStyle.entries.map {
                        it.name.lowercase().replaceFirstChar(Char::uppercase)
                    },
                    selectedIndex = PageIconStyle.entries.indexOf(style.iconStyle),
                    onSelect = { index ->
                        repo.update { it.copy(iconStyle = PageIconStyle.entries[index]) }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Icon shape",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StyleOptionRow(
                    options = PageIconShape.entries.map {
                        it.name.lowercase().replaceFirstChar(Char::uppercase)
                    },
                    selectedIndex = PageIconShape.entries.indexOf(style.iconShape),
                    onSelect = { index ->
                        repo.update { it.copy(iconShape = PageIconShape.entries[index]) }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Icon size",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StyleOptionRow(
                    options = listOf("S", "M", "L"),
                    selectedIndex = PageIconSize.entries.indexOf(style.iconSize),
                    onSelect = { index ->
                        repo.update { it.copy(iconSize = PageIconSize.entries[index]) }
                    }
                )
            }

            ModernSettingsCard(title = "Corners", icon = Icons.Default.GraphicEq) {
                StyleOptionRow(
                    options = PageCornerStyle.entries.map {
                        it.name.lowercase().replaceFirstChar(Char::uppercase)
                    },
                    selectedIndex = PageCornerStyle.entries.indexOf(style.cornerStyle),
                    onSelect = { index ->
                        repo.update { it.copy(cornerStyle = PageCornerStyle.entries[index]) }
                    }
                )
            }

            ModernSettingsCard(title = "Navbar", icon = Icons.Default.Navigation) {
                Text(
                    "Style",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StyleOptionRow(
                    options = PageNavbarStyle.entries.map {
                        it.name.lowercase().replaceFirstChar(Char::uppercase)
                    },
                    selectedIndex = PageNavbarStyle.entries.indexOf(style.navbarStyle),
                    onSelect = { index ->
                        repo.update { it.copy(navbarStyle = PageNavbarStyle.entries[index]) }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ModernSettingSwitch(
                    title = "Navbar blur",
                    subtitle = "Live blur behind the navbar — pure blur when transparent, frosted pill otherwise",
                    checked = style.navBlur,
                    onCheckedChange = { checked -> repo.update { it.copy(navBlur = checked) } }
                )
            }

            ModernSettingsCard(title = "Background FX", icon = Icons.Default.BlurOn) {
                Text(
                    "Particles",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StyleOptionRow(
                    options = ParticleDensity.entries.map {
                        it.name.lowercase().replaceFirstChar(Char::uppercase)
                    },
                    selectedIndex = ParticleDensity.entries.indexOf(style.particleDensity),
                    onSelect = { index ->
                        repo.update { it.copy(particleDensity = ParticleDensity.entries[index]) }
                    }
                )
            }

            ModernSettingsCard(title = "Theme", icon = Icons.Default.Palette) {
                ModernSettingSwitch(
                    title = "Dynamic color",
                    subtitle = "Follow the system wallpaper colors",
                    checked = style.dynamicColor,
                    onCheckedChange = { checked ->
                        repo.update { it.copy(dynamicColor = checked) }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.alpha(if (style.dynamicColor) 0.5f else 1f)
                ) {
                    ModernSettingSwitch(
                        title = "AMOLED black",
                        subtitle = "True-black surfaces in dark mode",
                        checked = style.amoledDark,
                        enabled = !style.dynamicColor,
                        onCheckedChange = { checked ->
                            repo.update { it.copy(amoledDark = checked) }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Accent",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AccentSwatchGrid(
                        selected = style.accent,
                        enabled = !style.dynamicColor,
                        onSelect = { accent -> repo.update { it.copy(accent = accent) } }
                    )
                    if (style.dynamicColor) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Turn off dynamic color to use AMOLED and accents.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ModernSettingsCard(title = "App icon", icon = Icons.Default.Apps) {
                AppIconManager.OPTIONS.forEach { option ->
                    AppIconRow(
                        label = option.label,
                        selected = currentIconId == option.id,
                        onClick = {
                            AppIconManager.apply(context, option.id)
                            currentIconId = option.id
                            ToastHelper.showToast(
                                context,
                                "Icon applied — the launcher refreshes in a few seconds"
                            )
                        }
                    )
                    if (option != AppIconManager.OPTIONS.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    if (showResetDialog) {
        ModernConfirmDialog(
            title = "Reset customization",
            message = "This resets all customization options — header, banner, cards, icons, corners, navbar, background FX and theme — back to defaults.",
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                repo.resetAll()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun StyleOptionRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = if (selected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatchGrid(
    selected: AccentChoice,
    enabled: Boolean,
    onSelect: (AccentChoice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentChoice.entries.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { accent ->
                    val isSelected = accent == selected
                    Surface(
                        onClick = { if (enabled) onSelect(accent) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .alpha(if (enabled) 1f else 0.6f),
                        shape = if (isSelected) MaterialTheme.shapes.extraLarge
                        else MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(
                                1.1.dp,
                                MaterialTheme.colorScheme.primary
                            )
                        } else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(accent.previewColor, CircleShape)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
                // Pad an incomplete last row so tiles keep equal width.
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AppIconRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

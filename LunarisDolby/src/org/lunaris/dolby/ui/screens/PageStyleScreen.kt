/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.lunaris.dolby.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageStyleScreen(navController: NavController) {
    val repo = rememberPageStyleRepository()
    val style by repo.style.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(end = 8.dp),
                title = {
                    Text(
                        "Page style",
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
            }

            ModernSettingsCard(title = "Icons", icon = Icons.Default.Tune) {
                StyleOptionRow(
                    options = PageIconStyle.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    selectedIndex = PageIconStyle.entries.indexOf(style.iconStyle),
                    onSelect = { index -> repo.update { it.copy(iconStyle = PageIconStyle.entries[index]) } }
                )
            }

            ModernSettingsCard(title = "Corners", icon = Icons.Default.GraphicEq) {
                StyleOptionRow(
                    options = PageCornerStyle.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    selectedIndex = PageCornerStyle.entries.indexOf(style.cornerStyle),
                    onSelect = { index -> repo.update { it.copy(cornerStyle = PageCornerStyle.entries[index]) } }
                )
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    if (showResetDialog) {
        ModernConfirmDialog(
            title = "Reset page style",
            message = "This resets the title, subtitle, icon style and corner style back to defaults.",
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

/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.domain.models

import org.lunaris.dolby.data.AppInfo

sealed class AppProfileUiState {
    object Loading : AppProfileUiState()
    data class Success(
        val apps: List<AppInfo>,
        val appsWithProfiles: Map<String, Int>,
        val appsWithScenes: Map<String, String> = emptyMap(),
        val scenes: List<Scene> = emptyList()
    ) : AppProfileUiState()
    data class Error(val message: String) : AppProfileUiState()
}

/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.samples.socialite.ui.home.chatlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.window.core.layout.WindowWidthSizeClass
import com.google.android.samples.socialite.R

@Composable
fun shouldUseBottomSheet(adaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()): Boolean {
    return adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
}

@Composable
fun OptionMenu(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuItems: @Composable ColumnScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        if (enabled) {
            OptionMenuButton(menuItems)
        } else {
            OptionMenuIcon()
        }
    }
}

@Composable
private fun OptionMenuButton(
    menuItems: @Composable ColumnScope.() -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButton(
        onClick = { isExpanded = !isExpanded },
    ) {
        OptionMenuIcon()
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            content = menuItems,
        )
    }
}

@Composable
private fun OptionMenuIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.MoreVert,
    contentDescription: String = stringResource(R.string.option),
) {
    Icon(icon, contentDescription = contentDescription, modifier = modifier)
}

/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.samples.socialite.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination.Companion.isTopLevel
import kotlinx.serialization.Serializable

    @Serializable
    data object Timeline : NavKey()

    @Serializable
    data object ChatsList : NavKey()

    @Serializable
    data object Settings : NavKey()

    @Serializable
    data object Home : NavKey()

    @Serializable
    data class ChatThread(val chatId: Long, val text: String? = null) : NavKey()

    @Serializable
    data class Camera(val chatId: Long) : NavKey()

    @Serializable
    data class PhotoPicker(val chatId: Long) : NavKey()

    @Serializable
    data class VideoEdit(val chatId: Long, val uri: String) : NavKey()

    @Serializable
    data class VideoPlayer(val uri: String) : NavKey()

enum class TopLevelDestination(
    val navKey: NavKey,
    @StringRes val label: Int,
    val imageVector: ImageVector,
) {
    Timeline(
        navKey = com.google.android.samples.socialite.ui.navigation.Timeline,
        label = R.string.timeline,
        imageVector = Icons.Outlined.VideoLibrary,
    ),
    ChatsList(
        navKey = com.google.android.samples.socialite.ui.navigation.ChatsList,
        label = R.string.chats,
        imageVector = Icons.Outlined.ChatBubbleOutline,
    ),
    Settings(
        navKey = com.google.android.samples.socialite.ui.navigation.Settings,
        label = R.string.settings,
        imageVector = Icons.Outlined.Settings,
    ),
    ;

    companion object {
        val START_DESTINATION = ChatsList

        fun fromNavKey(navKey: NavKey?): TopLevelDestination {
            return entries.find { it.navKey::class == navKey?.let { r -> r::class } }
                ?: START_DESTINATION
        }

        fun NavKey.isTopLevel(): Boolean {
            return TopLevelDestination.entries.any { it.navKey::class == this::class }
        }
    }
}

private fun calculateNavigationLayoutType(
    navKey: NavKey?,
    defaultLayoutType: NavigationSuiteType,
): NavigationSuiteType {
    return when {
        navKey == null -> defaultLayoutType
        // Never show navigation UI on Camera.
        navKey::class == Camera::class -> NavigationSuiteType.None
        // Top level destinations can show any layout type.
        navKey.isTopLevel() -> defaultLayoutType
        // Every other destination goes through a ChatThread. Hide the bottom nav bar
        // since it interferes with composing chat messages.
        defaultLayoutType == NavigationSuiteType.NavigationBar -> NavigationSuiteType.None
        else -> defaultLayoutType
    }
}

@Composable
fun SocialiteNavSuite(
    backStack: MutableList<NavKey>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentNavKey = backStack.lastOrNull()
    val topLevelDestination = TopLevelDestination.fromNavKey(currentNavKey)

    val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo(),
    )
    val layoutType = calculateNavigationLayoutType(currentNavKey, defaultLayoutType)

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach {
                val isSelected = it == topLevelDestination
                item(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            backStack.add(it.navKey)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = it.imageVector,
                            contentDescription = stringResource(it.label),
                        )
                    },
                    label = {
                        Text(text = stringResource(it.label))
                    },
                    alwaysShowLabel = false,
                )
            }
        },
    ) {
        content()
    }
}

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

import android.os.Parcelable
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
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination.Companion.isTopLevel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

sealed interface Screen : Parcelable {
    @Parcelize
    @Serializable
    data object Timeline : Screen

    @Parcelize
    @Serializable
    data object ChatsList : Screen

    @Parcelize
    @Serializable
    data object Settings : Screen

    @Parcelize
    @Serializable
    data object Home : Screen

    @Parcelize
    @Serializable
    data class ChatThread(val chatId: Long, val text: String? = null) : Screen

    @Parcelize
    @Serializable
    data class Camera(val chatId: Long) : Screen

    @Parcelize
    @Serializable
    data class PhotoPicker(val chatId: Long) : Screen

    @Parcelize
    @Serializable
    data class VideoEdit(val chatId: Long, val uri: String) : Screen

    @Parcelize
    @Serializable
    data class VideoPlayer(val uri: String) : Screen
}

enum class TopLevelDestination(
    val screen: Screen,
    @StringRes val label: Int,
    val imageVector: ImageVector,
) {
    Timeline(
        screen = Screen.Timeline,
        label = R.string.timeline,
        imageVector = Icons.Outlined.VideoLibrary,
    ),
    ChatsList(
        screen = Screen.ChatsList,
        label = R.string.chats,
        imageVector = Icons.Outlined.ChatBubbleOutline,
    ),
    Settings(
        screen = Screen.Settings,
        label = R.string.settings,
        imageVector = Icons.Outlined.Settings,
    ),
    ;

    companion object {
        val START_DESTINATION = ChatsList

        fun fromScreen(screen: Screen?): TopLevelDestination {
            return entries.find { it.screen::class == screen?.let { r -> r::class } }
                ?: START_DESTINATION
        }

        fun Screen.isTopLevel(): Boolean {
            return TopLevelDestination.entries.any { it.screen::class == this::class }
        }
    }
}

private fun calculateNavigationLayoutType(
    screen: Screen?,
    defaultLayoutType: NavigationSuiteType,
): NavigationSuiteType {
    return when {
        screen == null -> defaultLayoutType
        // Never show navigation UI on Camera.
        screen::class == Screen.Camera::class -> NavigationSuiteType.None
        // Top level destinations can show any layout type.
        screen.isTopLevel() -> defaultLayoutType
        // Every other destination goes through a ChatThread. Hide the bottom nav bar
        // since it interferes with composing chat messages.
        defaultLayoutType == NavigationSuiteType.NavigationBar -> NavigationSuiteType.None
        else -> defaultLayoutType
    }
}

@Composable
fun SocialiteNavSuite(
    backStack: MutableList<Screen>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentScreen = backStack.lastOrNull()
    val topLevelDestination = TopLevelDestination.fromScreen(currentScreen)

    val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo(),
    )
    val layoutType = calculateNavigationLayoutType(currentScreen, defaultLayoutType)

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
                            backStack.add(it.screen)
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

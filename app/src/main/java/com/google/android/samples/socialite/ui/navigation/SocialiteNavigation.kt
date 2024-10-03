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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination.Companion.isTopLevel
import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Timeline : Route

    @Serializable
    data object ChatsList : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Home : Route

    @Serializable
    data class ChatThread(val chatId: Long, val text: String? = null) : Route

    @Serializable
    data class Camera(val chatId: Long) : Route

    @Serializable
    data class PhotoPicker(val chatId: Long) : Route

    @Serializable
    data class VideoEdit(val chatId: Long, val uri: String) : Route

    @Serializable
    data class VideoPlayer(val uri: String) : Route
}

enum class TopLevelDestination(
    val route: Route,
    @StringRes val label: Int,
    val imageVector: ImageVector,
) {
    Timeline(
        route = Route.Timeline,
        label = R.string.timeline,
        imageVector = Icons.Outlined.VideoLibrary,
    ),
    ChatsList(
        route = Route.ChatsList,
        label = R.string.chats,
        imageVector = Icons.Outlined.ChatBubbleOutline,
    ),
    Settings(
        route = Route.Settings,
        label = R.string.settings,
        imageVector = Icons.Outlined.Settings,
    ),
    ;

    companion object {
        val START_DESTINATION = ChatsList

        fun fromNavDestination(destination: NavDestination?): TopLevelDestination {
            return entries.find { dest ->
                destination?.hierarchy?.any {
                    it.hasRoute(dest.route::class)
                } == true
            } ?: START_DESTINATION
        }

        fun NavDestination.isTopLevel(): Boolean {
            return entries.any {
                hasRoute(it.route::class)
            }
        }
    }
}

private fun calculateNavigationLayoutType(
    destination: NavDestination?,
    defaultLayoutType: NavigationSuiteType,
): NavigationSuiteType {
    return when {
        destination == null -> defaultLayoutType
        // Never show navigation UI on Camera.
        destination.hasRoute<Route.Camera>() -> NavigationSuiteType.None
        // Top level destinations can show any layout type.
        destination.isTopLevel() -> defaultLayoutType
        // Every other destination goes through a ChatThread. Hide the bottom nav bar
        // since it interferes with composing chat messages.
        defaultLayoutType == NavigationSuiteType.NavigationBar -> NavigationSuiteType.None
        else -> defaultLayoutType
    }
}

@Composable
fun SocialiteNavSuite(
    navController: NavController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination

    val topLevelDestination = TopLevelDestination.fromNavDestination(destination)
    val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo(),
    )
    val layoutType = calculateNavigationLayoutType(destination, defaultLayoutType)

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
                            navController.navigate(it.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
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

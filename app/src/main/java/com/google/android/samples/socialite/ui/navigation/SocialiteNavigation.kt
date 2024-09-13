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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.google.android.samples.socialite.R
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
    data class ChatThread(val chatId: Long, val text: String? = null): Route

    @Serializable
    data class Camera(val chatId: Long): Route

    @Serializable
    data class PhotoPicker(val chatId: Long): Route

    @Serializable
    data class VideoEdit(val chatId: Long, val uri: String): Route

    @Serializable
    data class VideoPlayer(val uri: String): Route
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
    Chats(
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
        val START_DESTINATION = Chats

        fun fromNavBackStackEntry(nbse: NavBackStackEntry?): TopLevelDestination {
            return entries.find { dest ->
                nbse?.destination?.hierarchy?.any {
                    it.hasRoute(dest.route::class)
                } == true
            } ?: START_DESTINATION
        }
    }
}

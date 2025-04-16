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

package com.google.android.samples.socialite.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.android.samples.socialite.ui.chat.ChatScreen
import com.google.android.samples.socialite.ui.home.chatlist.ChatList
import com.google.android.samples.socialite.ui.navigation.Route
import com.google.android.samples.socialite.ui.photopicker.navigation.navigateToPhotoPicker

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChatsListDetail(
    navController: NavHostController,
    chatId: Long?,
    modifier: Modifier = Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()

    LaunchedEffect(chatId) {
        if (chatId != null) {
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                content = chatId
            )
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                ChatList(onChatClicked = { chatId ->
                    navigator.navigateTo(
                        pane = ListDetailPaneScaffoldRole.Detail,
                        content = chatId
                    )
                })
            }
        },
        detailPane = {
            val selectedChatId = navigator.currentDestination?.content as? Long ?: 1L
            AnimatedPane {
                ChatScreen(
                    chatId = selectedChatId,
                    foreground = true,
                    onBackPressed = { navigator.navigateBack() },
                    onCameraClick = { navController.navigate(Route.Camera(selectedChatId)) },
                    onPhotoPickerClick = { navController.navigateToPhotoPicker(selectedChatId) },
                    onVideoClick = { uri -> navController.navigate(Route.VideoPlayer(uri)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    )
}

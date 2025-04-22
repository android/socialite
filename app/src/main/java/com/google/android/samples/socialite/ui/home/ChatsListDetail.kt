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

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import com.google.android.samples.socialite.AppArgs
import com.google.android.samples.socialite.ui.chat.ChatScreen
import com.google.android.samples.socialite.ui.home.chatlist.ChatList
import com.google.android.samples.socialite.ui.home.chatlist.ChatOpenRequest
import com.google.android.samples.socialite.ui.navigation.Route
import com.google.android.samples.socialite.ui.photopicker.navigation.navigateToPhotoPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChatsListDetail(
    navController: NavHostController,
    chatId: Long?,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val activity = LocalActivity.current

    LaunchedEffect(chatId) {
        if (chatId != null) {
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                contentKey = chatId,
            )
        }
    }

    LaunchedEffect(LocalConfiguration.current) {
        val currentDestination = navigator.currentDestination
        if (currentDestination != null) {
            navigator.navigateTo(currentDestination.pane, currentDestination.contentKey)
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                ChatList(
                    onOpenChatRequest = { request ->
                        handleOChatOpenRequest(
                            request = request,
                            navigator = navigator,
                            activity = activity,
                            coroutineScope = coroutineScope,
                        )
                    },
                )
            }
        },
        detailPane = {
            val selectedChatId = navigator.currentDestination?.contentKey ?: 1L
            AnimatedPane {
                ChatScreen(
                    chatId = selectedChatId,
                    foreground = true,
                    onBackPressed = {
                        coroutineScope.launch {
                            navigator.navigateBack()
                        }
                    },
                    onCameraClick = { navController.navigate(Route.Camera(selectedChatId)) },
                    onPhotoPickerClick = { navController.navigateToPhotoPicker(selectedChatId) },
                    onVideoClick = { uri -> navController.navigate(Route.VideoPlayer(uri)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun handleOChatOpenRequest(
    request: ChatOpenRequest,
    navigator: ThreePaneScaffoldNavigator<Long>,
    activity: Activity?,
    coroutineScope: CoroutineScope,
) {
    when (request) {
        is ChatOpenRequest.SameWindow -> {
            coroutineScope.launch {
                navigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.Detail,
                    contentKey = request.chatId,
                )
            }
        }

        is ChatOpenRequest.NewWindow -> {
            activity?.launchAnotherInstance(chatId = request.chatId)
        }
    }
}

private fun Activity.launchAnotherInstance(chatId: Long) {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.apply {
        putExtra(AppArgs.LaunchParams.CHAT_ID_KEY, chatId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = flags or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
        }
    }
    if (intent?.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}

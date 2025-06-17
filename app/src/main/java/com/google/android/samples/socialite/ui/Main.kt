/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.google.android.samples.socialite.ui

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.google.android.samples.socialite.AppArgs
import com.google.android.samples.socialite.tryCreateIntentFrom
import com.google.android.samples.socialite.ui.camera.Camera
import com.google.android.samples.socialite.ui.camera.Media
import com.google.android.samples.socialite.ui.camera.MediaType
import com.google.android.samples.socialite.ui.chat.ChatScreen
import com.google.android.samples.socialite.ui.home.chatlist.ChatList
import com.google.android.samples.socialite.ui.home.chatlist.ChatOpenRequest
import com.google.android.samples.socialite.ui.home.settings.Settings
import com.google.android.samples.socialite.ui.home.timeline.Timeline
import com.google.android.samples.socialite.ui.navigation.Camera
import com.google.android.samples.socialite.ui.navigation.ChatThread
import com.google.android.samples.socialite.ui.navigation.ChatsList
import com.google.android.samples.socialite.ui.navigation.PhotoPicker
import com.google.android.samples.socialite.ui.navigation.Settings
import com.google.android.samples.socialite.ui.navigation.SocialiteNavSuite
import com.google.android.samples.socialite.ui.navigation.Timeline
import com.google.android.samples.socialite.ui.navigation.VideoEdit
import com.google.android.samples.socialite.ui.navigation.VideoPlayer
import com.google.android.samples.socialite.ui.photopicker.navigation.PhotoPickerRoute
import com.google.android.samples.socialite.ui.player.VideoPlayerScreen
import com.google.android.samples.socialite.ui.videoedit.VideoEditScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun Main(
    appArgs: AppArgs? = null,
) {
    // TODO: Checkout "codelab-adaptive-apps-step-1" git branch to start the codelab.
    val modifier = Modifier.fillMaxSize()
    SocialTheme {
        MainNavigation(modifier, appArgs)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavigation(
    modifier: Modifier,
    appArgs: AppArgs?,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val activity = LocalActivity.current
    val backStack = rememberNavBackStack(ChatsList)

    SharedTransitionLayout {
        SocialiteNavSuite(
            modifier = modifier,
            backStack = backStack,
        ) {
            NavDisplay(
                backStack = backStack,
                entryProvider = { backStackKey ->
                    when (backStackKey) {
                        is ChatsList -> NavEntry(backStackKey) {
                            ChatList(
                                onOpenChatRequest = { request ->
                                    handleOChatOpenRequest(
                                        request = request,
                                        onOpenInSameWindow = { chatId ->
                                            backStack.add(ChatThread(chatId = chatId))
                                        },
                                        activity = activity,
                                        coroutineScope = coroutineScope,
                                    )
                                },
                            )
                        }

                        is ChatThread -> NavEntry(backStackKey) {
                            ChatScreen(
                                chatId = backStackKey.chatId,
                                foreground = true,
                                onBackPressed = { backStack.removeLastOrNull() },
                                onCameraClick = { backStack.add(Camera(backStackKey.chatId)) },
                                onPhotoPickerClick = {
                                    backStack.add(
                                        PhotoPicker(
                                            backStackKey.chatId,
                                        ),
                                    )
                                },
                                onVideoClick = { uri -> backStack.add(VideoPlayer(uri)) },
                                prefilledText = backStackKey.text,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is Camera -> NavEntry(backStackKey) {
                            val chatId = backStackKey.chatId
                            Camera(
                                chatId = backStackKey.chatId,
                                onMediaCaptured = { capturedMedia: Media? ->
                                    when (capturedMedia?.mediaType) {
                                        MediaType.PHOTO -> {
                                            backStack.removeLastOrNull()
                                        }

                                        MediaType.VIDEO -> {
                                            backStack.add(
                                                VideoEdit(
                                                    chatId,
                                                    capturedMedia.uri.toString(),
                                                ),
                                            )
                                        }

                                        else -> {
                                            // No media to use.
                                            backStack.removeLastOrNull()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is PhotoPicker -> NavEntry(backStackKey) {
                            PhotoPickerRoute(
                                chatId = backStackKey.chatId,
                                onPhotoPicked = { backStack.removeLastOrNull() },
                            )
                        }

                        is VideoEdit -> NavEntry(backStackKey) {
                            VideoEditScreen(
                                chatId = backStackKey.chatId,
                                uri = backStackKey.uri,
                                onCloseButtonClicked = { backStack.removeLastOrNull() },
                                onFinishEditing = {
                                    var navKey = backStack.lastOrNull()
                                    while (navKey != null &&
                                        (
                                            navKey !is ChatThread ||
                                                navKey.chatId != backStackKey.chatId
                                            )
                                    ) {
                                        backStack.removeLastOrNull()
                                        navKey = backStack.lastOrNull()
                                    }
                                },
                            )
                        }

                        is VideoPlayer -> NavEntry(backStackKey) {
                            VideoPlayerScreen(
                                uri = backStackKey.uri,
                                onCloseButtonClicked = { backStack.removeLastOrNull() },
                            )
                        }

                        else -> NavEntry(backStackKey) {
                            Text("Unknown back stack key: $backStackKey")
                        }
                    }
                },
                onBack = { repeat(it) { backStack.removeLastOrNull() } },
                entryDecorators = listOf(
                    rememberSceneSetupNavEntryDecorator(),
                    rememberSavedStateNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            )
        }

        LaunchedEffect(appArgs) {
            if (appArgs != null) {
                backStack.add(appArgs.toNavKey())
            }
        }
    }
}

private fun handleOChatOpenRequest(
    request: ChatOpenRequest,
    onOpenInSameWindow: (chatId: Long) -> Unit,
    activity: Activity?,
    coroutineScope: CoroutineScope,
) {
    when (request) {
        is ChatOpenRequest.SameWindow -> {
            coroutineScope.launch {
                onOpenInSameWindow(request.chatId)
            }
        }

        is ChatOpenRequest.NewWindow -> {
            activity?.launchAnotherInstance(request.toAppArgs())
        }
    }
}

private fun Activity.launchAnotherInstance(params: AppArgs.LaunchParams) {
    val intent = tryCreateIntentFrom(params)
    if (intent?.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}

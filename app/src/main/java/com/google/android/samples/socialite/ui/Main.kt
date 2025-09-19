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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.google.android.samples.socialite.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.navEntryDecorator
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
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
import com.google.android.samples.socialite.ui.navigation.Pane
import com.google.android.samples.socialite.ui.navigation.SocialiteNavSuite
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination
import com.google.android.samples.socialite.ui.photopicker.navigation.PhotoPickerRoute
import com.google.android.samples.socialite.ui.player.VideoPlayerScreen
import com.google.android.samples.socialite.ui.videoedit.VideoEditScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun Main(
    appArgs: AppArgs? = null,
) {
    val modifier = Modifier.fillMaxSize()
    SocialTheme {
        MainNavigation(modifier, appArgs)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavigation(
    modifier: Modifier,
    appArgs: AppArgs?,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val activity = LocalActivity.current
    val backStack = rememberSavableMutableStateListOf(TopLevelDestination.START_DESTINATION.pane)

    /** The [SharedTransitionScope] of the [NavDisplay]. */
    val localNavSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope> =
        compositionLocalOf {
            throw IllegalStateException(
                "Unexpected access to LocalNavSharedTransitionScope. You must provide a " +
                    "SharedTransitionScope from a call to SharedTransitionLayout() or " +
                    "SharedTransitionScope()",
            )
        }

    /**
     * A [NavEntryDecorator] that wraps each entry in a shared element that is controlled by the
     * [Scene].
     */
    val sharedEntryInSceneNavEntryDecorator = navEntryDecorator { entry ->
        with(localNavSharedTransitionScope.current) {
            Box(
                Modifier.sharedElement(
                    rememberSharedContentState(entry.key),
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                ),
            ) {
                entry.content(entry.key)
            }
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(localNavSharedTransitionScope provides this) {
            SocialiteNavSuite(
                modifier = modifier,
                backStack = backStack,
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { repeat(it) { backStack.removeLastOrNull() } },
                    sceneStrategy = rememberListDetailSceneStrategy(),
                    entryDecorators = listOf(
                        sharedEntryInSceneNavEntryDecorator,
                        rememberSceneSetupNavEntryDecorator(),
                        rememberSavedStateNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = { backStackKey ->
                        when (backStackKey) {
                            is Pane.Timeline -> NavEntry(backStackKey) {
                                Timeline(Modifier.fillMaxSize())
                            }

                            is Pane.Settings -> NavEntry(backStackKey) {
                                Settings(Modifier.fillMaxSize())
                            }

                            is Pane.ChatsList -> NavEntry(
                                key = backStackKey,
                                metadata = ListDetailSceneStrategy.listPane(),
                            ) {
                                ChatList(
                                    onOpenChatRequest = { request ->
                                        handleOChatOpenRequest(
                                            request = request,
                                            onOpenInSameWindow = { chatId ->
                                                backStack.add(Pane.ChatThread(chatId = chatId))
                                            },
                                            activity = activity,
                                            coroutineScope = coroutineScope,
                                        )
                                    },
                                )
                            }

                            is Pane.ChatThread -> NavEntry(
                                key = backStackKey,
                                metadata = ListDetailSceneStrategy.detailPane(),
                            ) {
                                ChatScreen(
                                    chatId = backStackKey.chatId,
                                    foreground = true,
                                    onBackPressed = { backStack.removeLastOrNull() },
                                    onCameraClick = { backStack.add(Pane.Camera(backStackKey.chatId)) },
                                    onPhotoPickerClick = {
                                        backStack.add(
                                            Pane.PhotoPicker(
                                                backStackKey.chatId,
                                            ),
                                        )
                                    },
                                    onVideoClick = { uri -> backStack.add(Pane.VideoPlayer(uri)) },
                                    prefilledText = backStackKey.text,
                                    prefilledImageUri = backStackKey.imageUri,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            is Pane.Camera -> NavEntry(backStackKey) {
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
                                                    Pane.VideoEdit(
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

                            is Pane.PhotoPicker -> NavEntry(backStackKey) {
                                PhotoPickerRoute(
                                    chatId = backStackKey.chatId,
                                    onPhotoPicked = { backStack.removeLastOrNull() },
                                )
                            }

                            is Pane.VideoEdit -> NavEntry(backStackKey) {
                                VideoEditScreen(
                                    chatId = backStackKey.chatId,
                                    uri = backStackKey.uri,
                                    onCloseButtonClicked = { backStack.removeLastOrNull() },
                                    onFinishEditing = {
                                        var pane = backStack.lastOrNull()
                                        while (pane != null &&
                                            (
                                                pane !is Pane.ChatThread ||
                                                    pane.chatId != backStackKey.chatId
                                                )
                                        ) {
                                            backStack.removeLastOrNull()
                                            pane = backStack.lastOrNull()
                                        }
                                    },
                                )
                            }

                            is Pane.VideoPlayer -> NavEntry(backStackKey) {
                                VideoPlayerScreen(
                                    uri = backStackKey.uri,
                                    onCloseButtonClicked = { backStack.removeLastOrNull() },
                                )
                            }

                            else -> NavEntry(backStackKey) {
                                Text("Unknown pane: $backStackKey")
                            }
                        }
                    },
                )
            }

            LaunchedEffect(appArgs) {
                if (appArgs != null) {
                    backStack.add(appArgs.toPane())
                }
            }

            LaunchedEffect(backStack.lastOrNull()) {
                when (backStack.lastOrNull()) {
                    is Pane.Camera -> {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                    }
                    else -> {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            }
        }
    }
}

@Composable
fun <T : Any> rememberSavableMutableStateListOf(vararg elements: T): SnapshotStateList<T> {
    return rememberSaveable(saver = snapshotStateListSaver()) {
        elements.toList().toMutableStateList()
    }
}

private fun <T : Any> snapshotStateListSaver() =
    listSaver<SnapshotStateList<T>, T>(
        save = { stateList -> stateList.toList() },
        restore = { it.toMutableStateList() },
    )

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

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
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.google.android.samples.socialite.model.extractChatId
import com.google.android.samples.socialite.ui.camera.Camera
import com.google.android.samples.socialite.ui.camera.Media
import com.google.android.samples.socialite.ui.camera.MediaType
import com.google.android.samples.socialite.ui.chat.ChatScreen
import com.google.android.samples.socialite.ui.home.chatlist.ChatList
import com.google.android.samples.socialite.ui.home.settings.Settings
import com.google.android.samples.socialite.ui.home.timeline.Timeline
import com.google.android.samples.socialite.ui.navigation.Route
import com.google.android.samples.socialite.ui.navigation.SocialiteNavSuite
import com.google.android.samples.socialite.ui.photopicker.navigation.navigateToPhotoPicker
import com.google.android.samples.socialite.ui.photopicker.navigation.photoPickerScreen
import com.google.android.samples.socialite.ui.player.VideoPlayerScreen
import com.google.android.samples.socialite.ui.videoedit.VideoEditScreen

@Composable
fun Main(
    shortcutParams: ShortcutParams?,
) {
    val modifier = Modifier.fillMaxSize()
    SocialTheme {
        MainNavigation(modifier, shortcutParams)
    }
}

@Composable
fun MainNavigation(
    modifier: Modifier,
    shortcutParams: ShortcutParams?,
) {
    val activity = LocalContext.current as Activity
    val navController = rememberNavController()

    navController.addOnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
        // Lock the layout of the Camera screen to portrait so that the UI layout remains
        // constant, even on orientation changes. Note that the camera is still aware of
        // orientation, and will assign the correct edge as the bottom of the photo or video.
        if (destination.hasRoute<Route.Camera>()) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    SocialiteNavSuite(
        navController = navController,
        modifier = modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.ChatsList,
        ) {
            composable<Route.ChatsList> {
                ChatList(
                    onChatClicked = { chatId -> navController.navigate(Route.ChatThread(chatId)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            composable<Route.Timeline> {
                Timeline(Modifier.fillMaxSize())
            }

            composable<Route.Settings> {
                Settings(Modifier.fillMaxSize())
            }

            composable<Route.ChatThread>(
                deepLinks = listOf(
                    navDeepLink {
                        action = Intent.ACTION_VIEW
                        uriPattern = "https://socialite.google.com/chat/{chatId}"
                    },
                ),
            ) { backStackEntry ->
                val route: Route.ChatThread = backStackEntry.toRoute()
                val chatId = route.chatId
                ChatScreen(
                    chatId = chatId,
                    foreground = true,
                    onBackPressed = { navController.popBackStack() },
                    onCameraClick = { navController.navigate(Route.Camera(chatId)) },
                    onPhotoPickerClick = { navController.navigateToPhotoPicker(chatId) },
                    onVideoClick = { uri -> navController.navigate(Route.VideoPlayer(uri)) },
                    prefilledText = route.text,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            composable<Route.Camera> { backStackEntry ->
                val route: Route.Camera = backStackEntry.toRoute()
                val chatId = route.chatId
                Camera(
                    onMediaCaptured = { capturedMedia: Media? ->
                        when (capturedMedia?.mediaType) {
                            MediaType.PHOTO -> {
                                navController.popBackStack()
                            }

                            MediaType.VIDEO -> {
                                navController.navigate(
                                    Route.VideoEdit(
                                        chatId,
                                        capturedMedia.uri.toString(),
                                    ),
                                )
                            }

                            else -> {
                                // No media to use.
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Invoke PhotoPicker to select photo or video from device gallery
            photoPickerScreen(
                onPhotoPicked = navController::popBackStack,
            )

            composable<Route.VideoEdit> { backStackEntry ->
                val route: Route.VideoEdit = backStackEntry.toRoute()
                val chatId = route.chatId
                val videoUri = route.uri
                VideoEditScreen(
                    chatId = chatId,
                    uri = videoUri,
                    onCloseButtonClicked = { navController.popBackStack() },
                    navController = navController,
                )
            }

            composable<Route.VideoPlayer> { backStackEntry ->
                val route: Route.VideoPlayer = backStackEntry.toRoute()
                val videoUri = route.uri
                VideoPlayerScreen(
                    uri = videoUri,
                    onCloseButtonClicked = { navController.popBackStack() },
                )
            }
        }
    }

    if (shortcutParams != null) {
        val chatId = extractChatId(shortcutParams.shortcutId)
        val text = shortcutParams.text
        navController.navigate(Route.ChatThread(chatId, text))
    }
}

data class ShortcutParams(
    val shortcutId: String,
    val text: String,
)

object AnimationConstants {
    private const val ENTER_MILLIS = 250
    private const val EXIT_MILLIS = 250

    val enterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = ENTER_MILLIS,
            easing = FastOutLinearInEasing,
        ),
    )

    val exitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = EXIT_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    )
}

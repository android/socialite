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

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.google.android.samples.socialite.AppArgs
import com.google.android.samples.socialite.ui.camera.Camera
import com.google.android.samples.socialite.ui.camera.Media
import com.google.android.samples.socialite.ui.camera.MediaType
import com.google.android.samples.socialite.ui.home.ChatsListDetail
import com.google.android.samples.socialite.ui.home.settings.Settings
import com.google.android.samples.socialite.ui.home.timeline.Timeline
import com.google.android.samples.socialite.ui.navigation.Route
import com.google.android.samples.socialite.ui.navigation.SocialiteNavSuite
import com.google.android.samples.socialite.ui.photopicker.navigation.photoPickerScreen
import com.google.android.samples.socialite.ui.player.VideoPlayerScreen
import com.google.android.samples.socialite.ui.videoedit.VideoEditScreen

@Composable
fun Main(
    appArgs: AppArgs? = null,
) {
    val modifier = Modifier.fillMaxSize()
    SocialTheme {
        MainNavigation(modifier, appArgs)
    }
}

@Composable
fun MainNavigation(
    modifier: Modifier,
    appArgs: AppArgs?,
) {
    val activity = LocalActivity.current
    val navController = rememberNavController()

    navController.addOnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
        // Lock the layout of the Camera screen to portrait so that the UI layout remains
        // constant, even on orientation changes. Note that the camera is still aware of
        // orientation, and will assign the correct edge as the bottom of the photo or video.
        if (destination.hasRoute<Route.Camera>()) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    SocialiteNavSuite(
        navController = navController,
        modifier = modifier,
    ) {
        NavigationTree(
            navController = navController,
        )
    }

    LaunchedEffect(appArgs) {
        if (appArgs != null) {
            navController.navigate(appArgs.toRoute())
        }
    }
}

@Composable
private fun NavigationTree(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Chats(null, null),
        popExitTransition = {
            scaleOut(
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f),
            )
        },
        popEnterTransition = {
            EnterTransition.None
        },
        modifier = modifier,
    ) {
        composable<Route.Chats>(
            deepLinks = listOf(
                navDeepLink {
                    action = Intent.ACTION_VIEW
                    uriPattern = "https://socialite.google.com/chat/{chatId}"
                },
            ),
        ) { backStackEntry ->
            val route: Route.Chats = backStackEntry.toRoute()
            val chatId = route.chatId
            ChatsListDetail(
                navController = navController,
                chatId = chatId,
            )
        }

        composable<Route.Timeline> {
            Timeline(Modifier.fillMaxSize())
        }

        composable<Route.Settings> {
            Settings(Modifier.fillMaxSize())
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

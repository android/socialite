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

package com.google.android.samples.socialite.ui.camera.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.android.samples.socialite.ui.camera.Camera
import com.google.android.samples.socialite.ui.camera.Media
import com.google.android.samples.socialite.ui.camera.MediaType

const val CHAT_ID_ARG = "chatId"
const val CAMERA_ROUTE = "chat/{$CHAT_ID_ARG}/camera"

fun NavController.navigateToCamera(chatId: Long, navOptions: NavOptions? = null) {
    navigate(
        route = CAMERA_ROUTE.replace("{$CHAT_ID_ARG}", chatId.toString()),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.cameraScreen(
    onBackPressed: () -> Unit,
    onVideoEditClick: (String) -> Unit,
) {
    composable(
        route = CAMERA_ROUTE,
        arguments = listOf(
            navArgument(CHAT_ID_ARG) { type = NavType.LongType },
        ),
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getLong(CHAT_ID_ARG) ?: 0

        Camera(
            onBackPressed = onBackPressed,
            onMediaCaptured = { capturedMedia: Media? ->
                if (capturedMedia?.mediaType == MediaType.VIDEO) {
                    onVideoEditClick("videoEdit?uri=${capturedMedia.uri}&chatId=$chatId")
                } else {
                    onBackPressed()
                }
            },
        )
    }
}

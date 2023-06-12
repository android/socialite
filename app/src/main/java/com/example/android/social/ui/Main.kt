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

package com.example.android.social.ui

import android.content.Intent
import android.media.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.android.social.model.extractChatId
import com.example.android.social.ui.camera.Camera
import com.example.android.social.ui.camera.Media
import com.example.android.social.ui.chat.Chat
import com.example.android.social.ui.home.Home

@Composable
fun Main(
    shortcutParams: ShortcutParams?,
) {
    SocialTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = "home",
        ) {
            composable(
                route = "home",
            ) {
                Home(
                    modifier = Modifier.fillMaxSize(),
                    onChatClicked = { chatId -> navController.navigate("chat/$chatId") },
                )
            }
            composable(
                route = "chat/{chatId}?text={text}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.LongType },
                    navArgument("text") { defaultValue = "" },
                ),
                deepLinks = listOf(
                    navDeepLink {
                        action = Intent.ACTION_VIEW
                        uriPattern = "https://android.example.com/chat/{chatId}"
                    },
                ),
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                val text = backStackEntry.arguments?.getString("text")
                Chat(
                    chatId = chatId,
                    foreground = true,
                    onCameraClick = { navController.navigate("chat/$chatId/camera") },
                    prefilledText = text,
                )
            }
            composable(
                route = "chat/{chatId}/camera",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.LongType },
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                Camera(onMediaCaptured = { capturedMedia: Media ->
                        navController.navigate("chat/$chatId")
                    },
                )
            }
        }
        if (shortcutParams != null) {
            val chatId = extractChatId(shortcutParams.shortcutId)
            val text = shortcutParams.text
            navController.navigate("chat/$chatId?text=$text")
        }
    }
}

data class ShortcutParams(
    val shortcutId: String,
    val text: String,
)

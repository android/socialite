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

package com.google.android.samples.socialite.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.samples.socialite.ui.AnimationConstants
import com.google.android.samples.socialite.ui.home.chatlist.ChatList
import com.google.android.samples.socialite.ui.home.timeline.Timeline
import com.google.android.samples.socialite.ui.navigation.Route
import com.google.android.samples.socialite.ui.navigation.SocialiteNavSuite
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination

@Composable
fun Home(
    onChatClicked: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    SocialiteNavSuite(navController) {
        HomeContent(navController, modifier, onChatClicked)
    }
}

@Composable
private fun HomeContent(
    navController: NavHostController,
    modifier: Modifier,
    onChatClicked: (chatId: Long) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START_DESTINATION.route,
        modifier = modifier,
    ) {
        composable<Route.Timeline>(
            enterTransition = { AnimationConstants.enterTransition },
            exitTransition = { AnimationConstants.exitTransition },
        ) {
            Timeline()
        }
        composable<Route.ChatsList>(
            enterTransition = { AnimationConstants.enterTransition },
            exitTransition = { AnimationConstants.exitTransition },
        ) {
            ChatList(
                onChatClicked = onChatClicked,
                modifier = modifier,
            )
        }
        composable<Route.Settings>(
            enterTransition = { AnimationConstants.enterTransition },
            exitTransition = { AnimationConstants.exitTransition },
        ) {
            Settings(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
    )
}

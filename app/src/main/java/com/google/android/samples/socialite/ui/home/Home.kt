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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.AnimationConstants
import com.google.android.samples.socialite.ui.home.timeline.Timeline

@Composable
fun Home(
    onChatClicked: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.Chats) }
    Scaffold(
        modifier = modifier,
        topBar = { HomeAppBar(title = stringResource(destination.label)) },
        bottomBar = {
            HomeNavigationBar(
                currentDestination = destination.route,
                onDestinationChanged = { destination = it },
            )
        },
    ) { innerPadding ->
        val navController = rememberNavController()
        HomeBackground(modifier = Modifier.fillMaxSize())
        NavHost(
            navController = navController,
            startDestination = destination.route,
            modifier = modifier,
        ) {
            composable(
                route = Destination.Timeline.route,
                enterTransition = { AnimationConstants.enterTransition },
                exitTransition = { AnimationConstants.exitTransition },
            ) {
                Timeline(
                    contentPadding = innerPadding,
                    modifier = modifier,
                )
            }
            composable(
                route = Destination.Chats.route,
                enterTransition = { AnimationConstants.enterTransition },
                exitTransition = { AnimationConstants.exitTransition },
            ) {
                val viewModel: HomeViewModel = hiltViewModel()
                val chats by viewModel.chats.collectAsStateWithLifecycle()
                ChatList(
                    chats = chats,
                    contentPadding = innerPadding,
                    onChatClicked = onChatClicked,
                    modifier = modifier,
                )
            }
            composable(
                route = Destination.Settings.route,
                enterTransition = { AnimationConstants.enterTransition },
                exitTransition = { AnimationConstants.exitTransition },
            ) {
                Settings(
                    contentPadding = innerPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAppBar(
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

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val imageVector: ImageVector,
) {
    Timeline(
        route = "timeline",
        label = R.string.timeline,
        imageVector = Icons.Outlined.VideoLibrary,
    ),
    Chats(
        route = "chats",
        label = R.string.chats,
        imageVector = Icons.Outlined.ChatBubbleOutline,
    ),
    Settings(
        route = "settings",
        label = R.string.settings,
        imageVector = Icons.Outlined.Settings,
    ),
}

@Composable
private fun HomeNavigationBar(
    currentDestination: String,
    onDestinationChanged: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        for (destination in Destination.entries) {
            val selected = currentDestination == destination.route
            val label = stringResource(destination.label)
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationChanged(destination) },
                icon = {
                    Icon(
                        imageVector = destination.imageVector,
                        contentDescription = label,
                    )
                },
                label = {
                    Text(text = label)
                },
                alwaysShowLabel = false,
            )
        }
    }
}

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.samples.socialite.ui.AnimationConstants
import com.google.android.samples.socialite.ui.home.timeline.Timeline
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination
import com.google.android.samples.socialite.ui.navigation.Route

@Composable
fun Home(
    onChatClicked: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = TopLevelDestination.fromNavBackStackEntry(navBackStackEntry)

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach {
                val isSelected = it == currentDestination
                item(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(it.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = it.imageVector,
                            contentDescription = stringResource(it.label),
                        )
                    },
                    label = {
                        Text(text = stringResource(it.label))
                    },
                    alwaysShowLabel = false,
                )
            }
        },
    ) {
        HomeContent(navController, currentDestination, modifier, onChatClicked)
    }
}

@Composable
private fun HomeContent(
    navController: NavHostController,
    currentDestination: TopLevelDestination,
    modifier: Modifier,
    onChatClicked: (chatId: Long) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            HomeAppBar(title = stringResource(currentDestination.label))
        },
    ) { innerPadding ->
        HomeBackground(modifier = Modifier.fillMaxSize())
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START_DESTINATION.route,
            modifier = modifier,
        ) {
            composable<Route.TimelineRoute>(
                enterTransition = { AnimationConstants.enterTransition },
                exitTransition = { AnimationConstants.exitTransition },
            ) {
                Timeline(
                    contentPadding = innerPadding,
                    modifier = modifier,
                )
            }
            composable<Route.ChatsRoute>(
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
            composable<Route.SettingsRoute>(
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

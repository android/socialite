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

package com.google.android.samples.socialite.ui.player

import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.google.android.samples.socialite.R

private const val TAG = "VideoPlayerScreen"

@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerScreenViewModel = viewModel(),
    uri: String,
    onCloseButtonClicked: () -> Unit,
) {
    val player = viewModel.player.collectAsStateWithLifecycle()
    val context = LocalContext.current
    VideoPlayerScreen(
        player = player.value,
        onCloseButtonClicked = onCloseButtonClicked,
        initializePlayer = { viewModel.initializePlayer(uri, context) },
        releasePlayer = viewModel::releasePlayer,
    )
}

@Composable
private fun VideoPlayerScreen(
    player: Player? = null,
    onCloseButtonClicked: () -> Unit = {},
    initializePlayer: () -> Unit = {},
    releasePlayer: () -> Unit = {},
) {
    PlayerLifecycle(
        initialize = initializePlayer,
        release = releasePlayer,
    )

    Scaffold(
        topBar = { VideoPlayerTopAppBar(onCloseButtonClicked) },
    ) { innerPadding ->
        VideoPlayer(player, Modifier.padding(innerPadding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayerTopAppBar(
    onCloseButtonClicked: () -> Unit,
) {
    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            navigationIconContentColor = Color.White,
        ),
        navigationIcon = {
            IconButton(onClick = onCloseButtonClicked) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = {},
    )
}

@Composable
private fun VideoPlayer(
    player: Player?,
    modifier: Modifier = Modifier,
) {
    // When in preview, early return a Box with the received modifier preserving layout
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    AndroidView(
        factory = { PlayerView(it) },
        update = {
            it.player = player
        },
        modifier = modifier
            .fillMaxSize()
            .focusable(),
//            .onKeyEvent { playerView.dispatchKeyEvent(it.nativeKeyEvent) },
    )
}

@Composable
@Preview
fun VideoPlayerScreenPreview() {
    VideoPlayerScreen()
}

/**
 * Handle the lifecycle of the player, making sure it's initialized and released at the
 * right moments in the Android lifecycle.
 */
@Composable
private fun PlayerLifecycle(
    initialize: () -> Unit,
    release: () -> Unit,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val currentInitializePlayer by rememberUpdatedState(initialize)
    val currentReleasePlayer by rememberUpdatedState(release)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (Build.VERSION.SDK_INT > 23) {
                    currentInitializePlayer()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT <= 23) {
                    currentInitializePlayer()
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                if (Build.VERSION.SDK_INT <= 23) {
                    currentReleasePlayer()
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                if (Build.VERSION.SDK_INT > 23) {
                    currentReleasePlayer()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

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

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.graphics.toRect
import androidx.core.util.Consumer
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.google.android.samples.socialite.R

// constants for broadcast receiver
const val ACTION_BROADCAST_CONTROL = "broadcast_control"

// Intent extra for broadcast controls from Picture-in-Picture mode.
const val EXTRA_CONTROL_TYPE = "control_type"
const val EXTRA_CONTROL_PLAY = 1
const val EXTRA_CONTROL_PAUSE = 2
const val EXTRA_CONTROL_FF = 3
const val EXTRA_CONTROL_RW = 4
const val REQUEST_PLAY = 5
const val REQUEST_PAUSE = 6
const val REQUEST_FF = 7
const val REQUEST_RW = 8

private const val TAG = "VideoPlayerScreen"

@Composable
fun VideoPlayerScreen(
    uri: String,
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerScreenViewModel = viewModel(),
    onCloseButtonClicked: () -> Unit,
) {
    val player = viewModel.player.collectAsStateWithLifecycle()
    val context = LocalContext.current
    VideoPlayerScreen(
        shouldEnterPipMode = viewModel.shouldEnterPipMode,
        modifier = modifier,
        player = player.value,
        onCloseButtonClicked = onCloseButtonClicked,
        initializePlayer = { viewModel.initializePlayer(uri, context) },
        releasePlayer = viewModel::releasePlayer,
    )
}

@Composable
private fun VideoPlayerScreen(
    shouldEnterPipMode: Boolean,
    modifier: Modifier = Modifier,
    player: Player? = null,
    onCloseButtonClicked: () -> Unit = {},
    initializePlayer: () -> Unit = {},
    releasePlayer: () -> Unit = {},
) {
    PlayerLifecycle(
        initialize = initializePlayer,
        release = releasePlayer,
    )

    PipListenerPreAPI12(shouldEnterPipMode = shouldEnterPipMode)

    if (isInPipMode()) {
        VideoPlayer(player, shouldEnterPipMode, Modifier.fillMaxSize())
    } else {
        Scaffold(
            topBar = { VideoPlayerTopAppBar(onCloseButtonClicked) },
        ) { innerPadding ->
            Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                VideoPlayer(player, shouldEnterPipMode, Modifier.padding(innerPadding))
            }
        }
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
    shouldEnterPipMode: Boolean,
    modifier: Modifier = Modifier,
) {
    // When in preview, early return a Box with the received modifier preserving layout
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // create modifier that adds pip to video player
        val pipModifier = modifier.onGloballyPositioned { layoutCoordinates ->
            val builder = PictureInPictureParams.Builder()

            if (shouldEnterPipMode) {
                // set source rect hint, aspect ratio and remote actions
                val sourceRect = layoutCoordinates.boundsInWindow().toAndroidRectF().toRect()
                builder.setSourceRectHint(sourceRect)
                builder.setAspectRatio(Rational(sourceRect.width(), sourceRect.height()))
            }

            builder.setActions(
                listOfRemoteActions(shouldEnterPipMode, context),
            )

            // Add autoEnterEnabled for versions S and up
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(shouldEnterPipMode)
            }
            context.findActivity().setPictureInPictureParams(builder.build())
        }

        val isInPipMode = isInPipMode()
        AndroidView(
            factory = { PlayerView(it) },
            update = { playerView ->
                playerView.player = player
                playerView.useController = !isInPipMode
            },
            modifier = pipModifier
                .focusable(),
        )
    } else {
        AndroidView(
            factory = { PlayerView(it) },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier
                .focusable(),
        )
    }

    BroadcastReceiver(player = player)
}

@Composable
@Preview
fun VideoPlayerScreenPreview() {
    VideoPlayer(player = null, shouldEnterPipMode = false)
}

/**
 * Handle the lifecycle of the player, making sure it's initialized and released at the
 * right moments in the Android lifecycle.
 */
@Composable
private fun PlayerLifecycle(
    initialize: () -> Unit,
    release: () -> Unit,
) {
    val currentOnInitializePlayer by rememberUpdatedState(initialize)
    val currentOnReleasePlayer by rememberUpdatedState(release)

    /**
     * Android API level 24 and higher supports multiple windows. As your app can be visible, but
     * not active in split window mode, you need to initialize the player in onStart
     */
    if (Build.VERSION.SDK_INT >= 24) {
        LifecycleStartEffect(true) {
            currentOnInitializePlayer()
            onStopOrDispose {
                currentOnReleasePlayer()
            }
        }
    }

    /**
     * Android API level 23 and lower requires you to wait as long as possible until you grab
     * resources, so you wait until onResume before initializing the player.
     */
    if (Build.VERSION.SDK_INT <= 23) {
        LifecycleResumeEffect(true) {
            currentOnInitializePlayer()
            onPauseOrDispose {
                currentOnReleasePlayer()
            }
        }
    }
}

/**
 * Uses Disposable Effect to add a pip observer to check when app enters pip mode so UI can be
 * updated
 */

@Composable
fun isInPipMode(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val activity = LocalContext.current.findActivity()
        var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }

        // Uses Disposable Effect to add a pip observer to check when app enters pip mode
        DisposableEffect(activity) {
            val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
            activity.addOnPictureInPictureModeChangedListener(
                observer,
            )
            onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
        }

        return pipMode
    } else {
        return false
    }
}

/**
 * Uses Disposable Effect to add a listener for onUserLeaveHint - allowing us to add PiP pre
 * Android 12
 */
@Composable
fun PipListenerPreAPI12(shouldEnterPipMode: Boolean) {
    // Using the rememberUpdatedState ensures that the updated version of shouldEnterPipMode is
    // used by the DisposableEffect
    val currentShouldEnterPipMode by rememberUpdatedState(newValue = shouldEnterPipMode)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    ) {
        val activity = LocalContext.current.findActivity()
        DisposableEffect(activity) {
            val onUserLeaveBehavior = {
                if (currentShouldEnterPipMode) {
                    activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                }
            }
            activity.addOnUserLeaveHintListener(
                onUserLeaveBehavior,
            )
            onDispose {
                activity.removeOnUserLeaveHintListener(
                    onUserLeaveBehavior,
                )
            }
        }
    }
}

/**
 * Adds a Broadcast Receiver for controls while in pip mode. We are demonstrating how to add custom
 * controls - if you use a MediaSession these controls come with it.
 */
@Composable
fun BroadcastReceiver(player: Player?) {
    if (isInPipMode() && player != null) {
        val context = LocalContext.current
        DisposableEffect(key1 = player, key2 = context) {
            val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if ((intent == null) || (intent.action != ACTION_BROADCAST_CONTROL)) {
                        return
                    }

                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        EXTRA_CONTROL_PAUSE -> player.pause()
                        EXTRA_CONTROL_PLAY -> player.play()
                        EXTRA_CONTROL_FF -> player.seekForward()
                        EXTRA_CONTROL_RW -> player.seekBack()
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                broadcastReceiver,
                IntentFilter(ACTION_BROADCAST_CONTROL),
                RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                context.unregisterReceiver(broadcastReceiver)
            }
        }
    }
}

// Find the closest Activity in a given Context.
internal fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Picture in picture should be called in the context of an Activity")
}

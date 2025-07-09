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

package com.google.android.samples.socialite.ui.home.timeline

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.android.samples.socialite.ui.home.timeline.component.EmptyTimeline
import com.google.android.samples.socialite.ui.home.timeline.component.TimelineFormat
import com.google.android.samples.socialite.ui.home.timeline.component.TimelineGrid
import com.google.android.samples.socialite.ui.home.timeline.component.TimelineScaffold
import com.google.android.samples.socialite.ui.home.timeline.component.TimelineVerticalPager
import com.google.android.samples.socialite.ui.home.timeline.component.rememberTimelineFormat

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun Timeline(
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel(),
    format: TimelineFormat = rememberTimelineFormat(),
) {
    val mediaItems by viewModel.media.collectAsStateWithLifecycle(emptyList())

    val player = viewModel.player
    val videoRatio = viewModel.videoRatio

    when {
        mediaItems.isEmpty() -> {
            TimelineScaffold(modifier = modifier) { contentPadding ->
                EmptyTimeline(contentPadding, modifier)
            }
        }
        else -> {
            Timeline(
                player = player,
                mediaItems = mediaItems,
                videoRatio = videoRatio,
                onChangePlayerItem = viewModel::changePlayerItem,
                onInitializePlayer = viewModel::initializePlayer,
                onReleasePlayer = viewModel::releasePlayer,
                format = format,
            )
        }
    }
}

@Composable
fun Timeline(
    mediaItems: List<TimelineMediaItem>,
    player: Player?,
    videoRatio: Float?,
    modifier: Modifier = Modifier,
    format: TimelineFormat = rememberTimelineFormat(),
    onChangePlayerItem: (uri: Uri?, page: Int) -> Unit = { uri: Uri?, i: Int -> },
    onInitializePlayer: () -> Unit = {},
    onReleasePlayer: () -> Unit = {},
) {
    val currentOnInitializePlayer by rememberUpdatedState(onInitializePlayer)
    val currentOnReleasePlayer by rememberUpdatedState(onReleasePlayer)
    if (Build.VERSION.SDK_INT > 23) {
        LifecycleStartEffect(true) {
            currentOnInitializePlayer()
            onStopOrDispose {
                currentOnReleasePlayer()
            }
        }
    } else {
        LifecycleResumeEffect(true) {
            currentOnInitializePlayer()
            onPauseOrDispose {
                currentOnReleasePlayer()
            }
        }
    }

    when (format) {
        TimelineFormat.Pager -> {
            TimelineScaffold(modifier = modifier) { contentPadding ->
                TimelineVerticalPager(
                    mediaItems = mediaItems,
                    player = player,
                    videoRatio = videoRatio,
                    onChangePlayerItem = onChangePlayerItem,
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                )
            }
        }

        TimelineFormat.Grid -> {
            TimelineGrid(
                mediaItems = mediaItems,
                player = player,
                onChangePlayerItem = onChangePlayerItem,
                modifier = modifier,
            )
        }
    }
}

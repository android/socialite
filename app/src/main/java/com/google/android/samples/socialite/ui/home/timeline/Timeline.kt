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

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.home.HomeAppBar
import com.google.android.samples.socialite.ui.home.HomeBackground
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination
import com.google.android.samples.socialite.ui.rememberIconPainter
import kotlin.math.absoluteValue

@Composable
fun Timeline(
    modifier: Modifier = Modifier,
) {
    val viewModel: TimelineViewModel = hiltViewModel()
    val media = viewModel.media
    val player = viewModel.player
    val videoRatio = viewModel.videoRatio
    Scaffold(
        modifier = modifier,
        topBar = {
            HomeAppBar(title = stringResource(TopLevelDestination.Timeline.label))
        },
    ) { contentPadding ->
        HomeBackground(modifier = Modifier.fillMaxSize())
        if (media.isEmpty()) {
            EmptyTimeline(contentPadding, modifier)
        } else {
            TimelineVerticalPager(
                contentPadding,
                Modifier,
                media,
                player,
                viewModel::initializePlayer,
                viewModel::releasePlayer,
                viewModel::changePlayerItem,
                videoRatio,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineVerticalPager(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    mediaItems: List<TimelineMediaItem>,
    player: Player?,
    onInitializePlayer: () -> Unit = {},
    onReleasePlayer: () -> Unit = {},
    onChangePlayerItem: (uri: Uri?) -> Unit = {},
    videoRatio: Float?,
) {
    val pagerState = rememberPagerState(pageCount = { mediaItems.count() })
    LaunchedEffect(pagerState) {
        // Collect from the a snapshotFlow reading the settledPage
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (mediaItems[page].type == TimelineMediaType.VIDEO) {
                onChangePlayerItem(Uri.parse(mediaItems[page].uri))
            } else {
                onChangePlayerItem(null)
            }
        }
    }

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

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) { page ->
        if (player != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .graphicsLayer {
                        // Calculate the absolute offset for the current page from the
                        // scroll position. We use the absolute value which allows us to mirror
                        // any effects for both directions
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState
                                .currentPageOffsetFraction
                            ).absoluteValue

                        // We animate the alpha, between 0% and 100%
                        alpha = lerp(
                            start = 0f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f),
                        )
                    },
            ) {
                TimelinePage(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    media = mediaItems[page],
                    player = player,
                    page,
                    pagerState,
                    videoRatio,
                )

                MetadataOverlay(modifier = Modifier.padding(16.dp), mediaItem = mediaItems[page])
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelinePage(
    modifier: Modifier = Modifier,
    media: TimelineMediaItem,
    player: Player,
    page: Int,
    state: PagerState,
    videoRatio: Float?,
) {
    when (media.type) {
        TimelineMediaType.VIDEO -> {
            if (page == state.settledPage) {
                // Use a default 1:1 ratio if the video size is unknown
                val sanitizedRatio = videoRatio ?: 1f
                AndroidExternalSurface(
                    modifier = modifier
                        .aspectRatio(sanitizedRatio, sanitizedRatio < 1f)
                        .background(Color.White),
                ) {
                    onSurface { surface, _, _ ->
                        player.setVideoSurface(surface)

                        // Cleanup when surface is destroyed
                        surface.onDestroyed {
                            player.clearVideoSurface(this)
                            release()
                        }
                    }
                }
            }
        }
        TimelineMediaType.PHOTO -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(media.uri)
                    .build(),
                contentDescription = null,
                modifier = modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun MetadataOverlay(modifier: Modifier, mediaItem: TimelineMediaItem) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(999f),
    ) {
        if (mediaItem.type == TimelineMediaType.VIDEO) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(
                LocalContext.current,
                Uri.parse(mediaItem.uri),
            )

            val duration =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong()
            duration?.let {
                val seconds = it / 1000L
                val minutes = seconds / 60L
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "%d:%02d".format(minutes, seconds % 60),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            mediaItem.chatIconUri?.let {
                Image(
                    painter = rememberIconPainter(contentUri = it),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                )
            }
            Text(modifier = Modifier.padding(end = 16.dp), text = mediaItem.chatName)
        }
    }
}

@Composable
fun EmptyTimeline(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.empty_timeline),
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.timeline_empty_title),
            modifier = Modifier.padding(top = 64.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.timeline_empty_message),
            textAlign = TextAlign.Center,
        )
    }
}

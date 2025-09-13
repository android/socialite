/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.samples.socialite.ui.home.timeline.component

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaType
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimelineVerticalPager(
    mediaItems: List<TimelineMediaItem>,
    player: Player?,
    videoRatio: Float?,
    modifier: Modifier = Modifier,
    onChangePlayerItem: (uri: Uri?, page: Int) -> Unit = { uri: Uri?, i: Int -> },
    onInspectClicked: (uri: String) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { mediaItems.count() })
    LaunchedEffect(pagerState) {
        // Collect from the a snapshotFlow reading the settledPage
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (mediaItems[page].type == TimelineMediaType.VIDEO) {
                onChangePlayerItem(mediaItems[page].uri.toUri(), pagerState.currentPage)
            } else {
                onChangePlayerItem(null, pagerState.currentPage)
            }
        }
    }

    if (player != null) {
        TimelineVerticalPager(
            mediaItems = mediaItems,
            player = player,
            pagerState = pagerState,
            videoRatio = videoRatio,
            modifier = modifier,
            onInspectClicked = onInspectClicked,
        )
    }
}

@Composable
private fun TimelineVerticalPager(
    mediaItems: List<TimelineMediaItem>,
    player: Player,
    pagerState: PagerState,
    videoRatio: Float?,
    modifier: Modifier = Modifier,
    onInspectClicked: (uri: String) -> Unit = {},
) {
    VerticalPager(
        state = pagerState,
        modifier = modifier,
    ) { page ->
        val mediaItem = mediaItems[page]

        TimelinePage(
            mediaItem = mediaItem,
            player = player,
            page = page,
            pagerState = pagerState,
            videoRatio = videoRatio,
            modifier = Modifier
                .combinedClickable(
                    enabled = true,
                    onClick = {},
                    onLongClick = {},
                )
                .fillMaxSize()
                .padding(8.dp)
                .graphicsLayer {
                    // Calculate the absolute offset for the current page from the
                    // scroll position. We use the absolute value which allows us to mirror
                    // any effects for both directions
                    val pageOffset =
                        ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    // We animate the alpha, between 0% and 100%
                    alpha = lerp(
                        start = 0f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                    )
                },
            onInspectClicked = onInspectClicked,
        )
    }
}

@Composable
private fun TimelinePage(
    mediaItem: TimelineMediaItem,
    player: Player,
    page: Int,
    pagerState: PagerState,
    videoRatio: Float?,
    modifier: Modifier = Modifier,
    onInspectClicked: (uri: String) -> Unit = {},
) {
    TimelineCard(
        modifier = modifier,
    ) {
        ContextMenuArea(mediaItem) {
            MediaItem(
                modifier = Modifier.Companion
                    .align(Alignment.Center)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .draggableMediaItem(mediaItem),
                media = mediaItem,
                player = player,
                page,
                pagerState,
                videoRatio,
                onInspectClicked = onInspectClicked,
            )
        }
        MetadataOverlay(
            modifier = Modifier.padding(16.dp),
            mediaItem = mediaItem,
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaItem(
    modifier: Modifier = Modifier,
    media: TimelineMediaItem,
    player: Player,
    page: Int,
    state: PagerState,
    videoRatio: Float?,
    onInspectClicked: (uri: String) -> Unit = {},
) {
    when (media.type) {
        TimelineMediaType.VIDEO -> {
            if (page == state.settledPage) {
                when {
                    // When in preview, place a Box with the received modifier preserving layout
                    LocalInspectionMode.current -> {
                        Box(modifier = modifier)
                    }

                    else -> {
                        // Add a small i button to the top left
                        IconButton(
                            onClick = {
                                onInspectClicked(media.uri)
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .zIndex(1f), // Ensure the button is on top of other content
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Inspect video metadata",
                            )
                        }
                        PlayerSurface(
                            player = player,
                            modifier = modifier.resizeWithContentScale(
                                ContentScale.Fit,
                                null,
                            ),
                        )
                    }
                }
            }
        }

        TimelineMediaType.PHOTO -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(media.uri).build(),
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

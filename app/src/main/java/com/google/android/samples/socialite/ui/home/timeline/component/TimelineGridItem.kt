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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.samples.socialite.ui.components.PlayArrowIcon
import com.google.android.samples.socialite.ui.components.VideoPreview
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaType

@Composable
internal fun TimelineGridItem(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit = {},
) {
    TimelineCard(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
    ) {
        Thumbnail(
            mediaItem = mediaItem,
            modifier = Modifier.draggableMediaItem(mediaItem),
        )
        MetadataOverlay(
            mediaItem = mediaItem,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun Thumbnail(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
) {
    ContextMenuArea(mediaItem = mediaItem) {
        when (mediaItem.type) {
            TimelineMediaType.PHOTO -> {
                Photo(mediaItem = mediaItem, modifier = modifier)
            }

            TimelineMediaType.VIDEO -> {
                Video(mediaItem = mediaItem, modifier = modifier)
            }
        }
    }
}

@Composable
private fun Photo(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(mediaItem.uri)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun Video(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
    ) {
        VideoPreview(
            videoUri = mediaItem.uri,
            contentScale = ContentScale.Crop,
        ) {
            PlayArrowIcon(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

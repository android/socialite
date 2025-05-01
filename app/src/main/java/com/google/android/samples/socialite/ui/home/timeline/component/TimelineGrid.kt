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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.components.PlayArrowIcon
import com.google.android.samples.socialite.ui.components.VideoPreview
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaType

private const val INVALID_MEDIA_ITEM_INDEX = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineGrid(
    mediaItems: List<TimelineMediaItem>,
    player: Player?,
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Adaptive(minSize = 240.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onChangePlayerItem: (uri: Uri?, page: Int) -> Unit = { uri: Uri?, i: Int -> },
) {
    var selectedMediaItemIndex by rememberSaveable { mutableIntStateOf(INVALID_MEDIA_ITEM_INDEX) }
    val shouldShowCarousel = selectedMediaItemIndex != INVALID_MEDIA_ITEM_INDEX

    var carouselWidth by remember { mutableStateOf(0.dp) }

    TimelineFrame(
        shouldShowModalContent = shouldShowCarousel,
        onDismissRequest = {
            selectedMediaItemIndex = INVALID_MEDIA_ITEM_INDEX
        },
        modalContent = {
            MediaItemCarousel(
                mediaItems = mediaItems,
                player = player,
                itemWidth = carouselWidth,
                initialIndex = selectedMediaItemIndex,
                onChangePlayerItem = onChangePlayerItem,
                modifier = Modifier.safeDrawingPadding(),
            )
        },
        closeCarousel = {
            IconButton(
                onClick = {
                    selectedMediaItemIndex = INVALID_MEDIA_ITEM_INDEX
                },
            ) {
                Icon(
                    Icons.Default.Close,
                    tint = Color.White,
                    contentDescription = stringResource(R.string.close_carousel),
                )
            }
        },
        modifier = modifier.onPlaced {
            carouselWidth = it.size.width.dp * 0.9f
        },
    ) {
        LazyVerticalGrid(
            columns = columns,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding,
        ) {
            itemsIndexed(mediaItems) { index, mediaItem ->
                TimelineGridItem(
                    mediaItem = mediaItem,
                    modifier = Modifier.aspectRatio(0.7072f),
                    onClick = {
                        selectedMediaItemIndex = index
                    },
                )
            }
        }
    }
}

@Composable
private fun TimelineFrame(
    shouldShowModalContent: Boolean,
    modifier: Modifier = Modifier,
    scrimBrush: Brush = SolidColor(Color.Black.copy(alpha = 0.8f)),
    onDismissRequest: () -> Unit = {},
    modalContent: @Composable BoxScope.() -> Unit = {},
    closeCarousel: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.clickable(onClick = onDismissRequest),
    ) {
        TimelineScaffold { contentPadding ->
            Box(
                modifier = Modifier.padding(contentPadding),
            ) {
                content()
            }
        }
        AnimatedVisibility(
            shouldShowModalContent,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Scrim(brush = scrimBrush, modifier = Modifier.fillMaxSize())
            modalContent()
            Box(
                modifier = Modifier.padding(vertical = 48.dp, horizontal = 32.dp),
            ) {
                closeCarousel()
            }
        }
    }
}

@Composable
private fun Scrim(
    modifier: Modifier = Modifier,
    brush: Brush = SolidColor(Color.Black.copy(alpha = 0.8f)),
) {
    Box(
        modifier = Modifier
            .background(brush)
            .then(modifier),
    )
}

@Composable
fun TimelineGridItem(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit = {},
) {
    TimelineCard(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick,
        ),
    ) {
        when (mediaItem.type) {
            TimelineMediaType.PHOTO -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaItem.uri)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            TimelineMediaType.VIDEO -> {
                Box(
                    modifier = Modifier
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
        }
        MetadataOverlay(
            mediaItem = mediaItem,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaItemCarousel(
    mediaItems: List<TimelineMediaItem>,
    itemWidth: Dp,
    player: Player?,
    onChangePlayerItem: (uri: Uri?, page: Int) -> Unit = { uri: Uri?, i: Int -> },
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
) {
    val carouselState = rememberCarouselState(initialItem = initialIndex) {
        mediaItems.size
    }

    HorizontalUncontainedCarousel(
        state = carouselState,
        itemWidth = itemWidth,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(48.dp),
        flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState),
        modifier = modifier,
    ) { index ->
        val mediaItem = mediaItems[index]

        when {
            mediaItem.type == TimelineMediaType.PHOTO -> {
                PhotoCarouselSlide(
                    mediaItem = mediaItem,
                )
            }

            mediaItem.type == TimelineMediaType.VIDEO && player != null -> {
                VideoCarouselSlide(
                    mediaItem = mediaItem,
                    index = index,
                    player = player,
                    onChangePlayerItem = onChangePlayerItem,
                )
            }
        }
    }
}

@Composable
private fun PhotoCarouselSlide(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
) {
    CarouselSlide(
        mediaItem = mediaItem,
        modifier = modifier,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaItem.uri)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun VideoCarouselSlide(
    mediaItem: TimelineMediaItem,
    index: Int,
    player: Player,
    modifier: Modifier = Modifier,
    onChangePlayerItem: (uri: Uri?, page: Int) -> Unit = { uri: Uri?, i: Int -> },
) {
    val uri = mediaItem.uri.toUri()
    DisposableEffect(uri) {
        onChangePlayerItem(uri, index)
        onDispose {
            onChangePlayerItem(null, index)
        }
    }

    CarouselSlide(
        mediaItem = mediaItem,
        modifier = modifier,
    ) {
        PlayerSurface(
            player = player,
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Fit,
                sourceSizeDp = null,
            ),
        )
    }
}

@Composable
private fun CarouselSlide(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit = {},
) {
    Box(
        contentAlignment = contentAlignment,
        modifier = modifier.fillMaxSize(),
    ) {
        content()
        MetadataOverlay(
            mediaItem = mediaItem,
            modifier = Modifier.padding(16.dp),
        )
    }
}

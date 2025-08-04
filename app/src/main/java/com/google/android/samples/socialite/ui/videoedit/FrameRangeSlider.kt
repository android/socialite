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

package com.google.android.samples.socialite.ui.videoedit

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "FrameRangeSlider"

@OptIn(UnstableApi::class)
@Composable
fun FrameRangeSlider(
    frames: List<Bitmap>,
    startFrameIndex: Int,
    endFrameIndex: Int,
    onRangeChanged: (Int, Int) -> Unit,
    onFrameSelected: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val frameHeight = 64.dp

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(frameHeight),  // Use frame height for container
        ) {
            val availableWidth = LocalConfiguration.current.screenWidthDp.dp - 64.dp // Subtract padding
            val frameWidth = if (frames.isNotEmpty()) availableWidth / frames.size else 0.dp

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(frameHeight),
                horizontalArrangement = Arrangement.Start,
            ) {
                itemsIndexed(frames) { index, frame ->
                    val isSelected = index >= startFrameIndex && index <= endFrameIndex

                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .width(frameWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                alpha = if (isSelected) 1f else 0.4f
                            }
                            .clickable { onFrameSelected(index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Start trim boundary thumb (left edge)
            if (frames.isNotEmpty()) {
                var startThumbOffset by remember("start_thumb_$startFrameIndex") {
                    mutableFloatStateOf(
                        0f,
                    )
                }
                val frameWidthPx = with(density) { frameWidth.toPx() }
                val startThumbPosition = with(density) {
                    (startFrameIndex * frameWidthPx - 12).toDp()
                }

                // Start thumb - White with flat right edge, curved left edge
                Box(
                    modifier = Modifier
                        .offset(x = startThumbPosition + with(density) { startThumbOffset.toDp() })
                        .width(16.dp)
                        .height(frameHeight)
                        .background(
                            Color.White,
                            RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp,
                                topEnd = 0.dp,
                                bottomEnd = 0.dp,
                            ),
                        )
                        .border(
                            2.dp,
                            Color.Black,
                            RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp,
                                topEnd = 0.dp,
                                bottomEnd = 0.dp,
                            ),
                        )
                        .pointerInput("start_thumb_drag_$startFrameIndex") {
                            detectDragGestures(
                                onDragStart = { startThumbOffset = 0f },
                                onDragEnd = {
                                    val newPosition = startThumbPosition.toPx() + startThumbOffset
                                    val frameIndex = ((newPosition + 12) / frameWidthPx).toInt()
                                        .coerceIn(0, endFrameIndex - 1)
                                    onRangeChanged(frameIndex, endFrameIndex)

                                    startThumbOffset = 0f
                                },
                            ) { _, dragAmount ->
                                startThumbOffset += dragAmount.x
                            }
                        }
                        .align(Alignment.TopStart),
                )
            }

            // End trim boundary thumb (right edge)
            if (frames.isNotEmpty()) {
                var endThumbOffset by remember("end_thumb_$endFrameIndex") { mutableFloatStateOf(0f) }
                val frameWidthPx = with(density) { frameWidth.toPx() }

                val endThumbPosition = with(density) {
                    (endFrameIndex * frameWidthPx + frameWidthPx - 4).toDp()
                }

                // End thumb - White with flat left edge, curved right edge
                Box(
                    modifier = Modifier
                        .offset(x = endThumbPosition + with(density) { endThumbOffset.toDp() })
                        .width(16.dp)
                        .height(frameHeight)
                        .background(
                            Color.White,
                            RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp,
                            ),
                        )
                        .border(
                            2.dp,
                            Color.Black,
                            RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp,
                            ),
                        )
                        .pointerInput("end_thumb_drag_$endFrameIndex") {
                            detectDragGestures(
                                onDragStart = { endThumbOffset = 0f },
                                onDragEnd = {
                                    val newPosition = endThumbPosition.toPx() + endThumbOffset
                                    val frameIndex =
                                        ((newPosition - frameWidthPx + 4) / frameWidthPx).toInt()
                                            .coerceIn(startFrameIndex + 1, frames.size - 1)
                                    onRangeChanged(startFrameIndex, frameIndex)
                                    endThumbOffset = 0f
                                },
                            ) { _, dragAmount ->
                                endThumbOffset += dragAmount.x
                            }
                        }
                        .align(Alignment.TopStart),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Trim range: Frame ${startFrameIndex + 1} to ${endFrameIndex + 1} (${endFrameIndex - startFrameIndex + 1} frames)",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@UnstableApi
suspend fun extractFrames(
    context: android.content.Context,
    videoUri: String,
    frameCount: Int,
): Pair<Long, List<Bitmap>> {
    return withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, videoUri.toUri())
        val keyCode = MediaMetadataRetriever.METADATA_KEY_DURATION
        val time: String? = mediaMetadataRetriever.extractMetadata(keyCode)
        val duration = time?.toLong() ?: 0L
        if (duration > 0) {
            val frameDuration = duration * 1000 / frameCount
            for (i in 0 until frameCount) {
                try {
                    val frameTime = i * frameDuration
                    val frame = mediaMetadataRetriever.getFrameAtTime(frameTime)
                    frame?.let { frames.add(it) }
                    Log.d(TAG, "Extracted frame $i of $frameCount")
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting frame at index $i", e)
                }
            }
        }
        Pair(duration, frames)
    }
}

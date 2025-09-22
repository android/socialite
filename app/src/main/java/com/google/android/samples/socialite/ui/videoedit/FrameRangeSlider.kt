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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "FrameRangeSlider"

@Composable
fun FrameRangeSlider(
    frames: List<Bitmap>,
    state: TrimState,
    onTrimChanged: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 6.dp,
    handleVisualWidth: Dp = 12.dp,
    handleHitRadius: Dp = 20.dp,
    railThickness: Dp = 2.dp,
) {
    val density = LocalDensity.current
    val frameHeight = 56.dp
    val handleColor = Color(0xFFDDDDDD)

    val haptics = LocalHapticFeedback.current

    val edgePadPx = with(density) { edgePadding.toPx() }
    val handleVisualPx = with(density) { handleVisualWidth.toPx() }
    val hitRadiusPx = with(density) { handleHitRadius.toPx() }

    var laneWidthPx by remember { mutableFloatStateOf(0f) }
    var startXPx by remember { mutableFloatStateOf(0f) }
    var endXPx by remember { mutableFloatStateOf(0f) }

    fun msToX(ms: Long): Float =
        if (state.durationMs <= 0) edgePadPx else edgePadPx + (ms.toFloat() / state.durationMs) * laneWidthPx

    fun xToMs(x: Float): Long {
        val clamped = x.coerceIn(edgePadPx, edgePadPx + laneWidthPx)
        val rel = (clamped - edgePadPx) / laneWidthPx
        return (rel * state.durationMs).toLong().coerceIn(0, state.durationMs)
    }

    fun clampAndNotify() {
        val minGapPx = (state.minTrimMs / state.durationMs.toFloat()).coerceAtMost(1f) * laneWidthPx
        val minGap = max(minGapPx, handleVisualPx * 1.2f)

        startXPx = startXPx.coerceIn(edgePadPx, endXPx - minGap)
        endXPx = endXPx.coerceIn(startXPx + minGap, edgePadPx + laneWidthPx)
        onTrimChanged(xToMs(startXPx), xToMs(endXPx))
    }

    LaunchedEffect(laneWidthPx, state.startMs, state.endMs, state.durationMs) {
        if (laneWidthPx > 0f) {
            startXPx = msToX(state.startMs)
            endXPx = msToX(state.endMs)
            clampAndNotify()
        }
    }

    var dragging: HandlePosition? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(frameHeight + 24.dp)
            .onGloballyPositioned {
                val total = it.size.width.toFloat()
                laneWidthPx = (total - edgePadPx * 2f).coerceAtLeast(1f)
            }
            .pointerInput(state.durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val x = offset.x
                        val dStart = kotlin.math.abs(x - startXPx)
                        val dEnd = kotlin.math.abs(x - endXPx)
                        dragging = when {
                            dStart <= dEnd && dStart < hitRadiusPx -> HandlePosition.START
                            dEnd < hitRadiusPx -> HandlePosition.END
                            else -> null
                        }
                        if (dragging != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDrag = { _, drag ->
                        when (dragging) {
                            HandlePosition.START -> {
                                startXPx += drag.x
                                clampAndNotify()
                            }

                            HandlePosition.END -> {
                                endXPx += drag.x
                                clampAndNotify()
                            }

                            null -> Unit
                        }
                    },
                    onDragEnd = {
                        if (dragging != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        dragging = null
                    },
                    onDragCancel = { dragging = null },
                )
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(frameHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111314)),
        ) {
            if (frames.isEmpty()) {
                Spacer(Modifier.fillMaxSize())
            } else {
                val weight = 1f / frames.size.toFloat()
                frames.forEach { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .border(0.5.dp, Color.Black.copy(alpha = 0.35f)),
                    )
                }
            }
        }

        Box(
            Modifier
                .offset { IntOffset(0, 0) }
                .width(edgePadding)
                .height(frameHeight)
                .background(Color.Transparent),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(edgePadding)
                .height(frameHeight)
                .background(Color.Transparent),
        )

        Box(
            Modifier
                .offset { IntOffset(0, 0) }
                .width(with(density) { (startXPx).toDp() })
                .height(frameHeight)
                .background(Color.Black.copy(alpha = 0.45f)),
        )
        Box(
            Modifier
                .offset { IntOffset(endXPx.toInt(), 0) }
                .width(with(density) { (edgePadPx + laneWidthPx - (endXPx - edgePadPx)).toDp() })
                .height(frameHeight)
                .background(Color.Black.copy(alpha = 0.45f)),
        )

        Box(
            Modifier
                .offset { IntOffset(startXPx.toInt(), 0) }
                .width(with(density) { (endXPx - startXPx).toDp() })
                .height(railThickness)
                .background(handleColor),
        )
        Box(
            Modifier
                .offset {
                    IntOffset(
                        startXPx.toInt(),
                        frameHeight.roundToPx() - railThickness.roundToPx(),
                    )
                }
                .width(with(density) { (endXPx - startXPx).toDp() })
                .height(railThickness)
                .background(handleColor),
        )
        Handle(
            xPx = startXPx,
            height = frameHeight,
            color = handleColor,
            visualWidth = handleVisualWidth,
            hitRadius = handleHitRadius,
            isLeftSide = true,
        )
        Handle(
            xPx = endXPx,
            height = frameHeight,
            color = handleColor,
            visualWidth = handleVisualWidth,
            hitRadius = handleHitRadius,
            isLeftSide = false,
        )
    }
}

private enum class HandlePosition { START, END }

@Stable
data class TrimState(
    val durationMs: Long,
    val minTrimMs: Long = 300L,
    val startMs: Long = 0L,
    val endMs: Long = durationMs,
)

@Composable
private fun Handle(
    xPx: Float,
    height: Dp,
    color: Color,
    visualWidth: Dp,
    hitRadius: Dp,
    isLeftSide: Boolean,
) {
    val flatShape = if (isLeftSide) {
        RoundedCornerShape(
            topStart = 8.dp,
            bottomStart = 8.dp,
            topEnd = 0.dp,
            bottomEnd = 0.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = 8.dp,
            bottomEnd = 8.dp,
        )
    }

    Box(
        Modifier
            .offset { IntOffset(xPx.toInt() - (hitRadius / 2).roundToPx(), 0) }
            .width(hitRadius)
            .height(height),
    )

    Box(
        Modifier
            .offset { IntOffset(xPx.toInt() - (visualWidth / 2).roundToPx(), 0) }
            .width(visualWidth)
            .height(height)
            .shadow(4.dp, flatShape, clip = false)
            .background(color, flatShape),
    )
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
        var duration = 0L
        try {
            mediaMetadataRetriever.setDataSource(context, videoUri.toUri())
            val keyCode = MediaMetadataRetriever.METADATA_KEY_DURATION
            val time: String? = try {
                mediaMetadataRetriever.extractMetadata(keyCode)
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting video metadata: ${e.message}")
                null
            }
            duration = time?.toLong() ?: 0L
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract frames for $videoUri", e)
        } finally {
            mediaMetadataRetriever.release()
        }
        Pair(duration, frames)
    }
}

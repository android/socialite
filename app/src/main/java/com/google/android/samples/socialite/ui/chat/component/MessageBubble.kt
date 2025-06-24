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

package com.google.android.samples.socialite.ui.chat.component

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.samples.socialite.ui.chat.ChatMessage
import com.google.android.samples.socialite.ui.components.PlayArrowIcon
import com.google.android.samples.socialite.ui.components.VideoPreview

private const val TAG = "ChatUI"

@Composable
internal fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onVideoClick: () -> Unit = {},
) {
    var rightClickOffset by remember { mutableStateOf<DpOffset>(DpOffset.Zero) }
    var isMenuVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                awaitEachGesture { // Start listening for pointer gestures
                    val event = awaitPointerEvent()

                    if (
                        event.type == PointerEventType.Press
                        && !event.buttons.isPrimaryPressed
                        && event.buttons.isSecondaryPressed
                        && !event.buttons.isTertiaryPressed
                        // all pointer inputs just went down
                        && event.changes.fastAll { it.changedToDown() }
                    ) {
                        // Get the pressed pointer info
                        val press = event.changes.find { it.pressed }
                        if (press != null) {
                            // Convert raw press coordinates (px) to dp for positioning the menu
                            rightClickOffset = with(density) {
                                isMenuVisible = true // Show the context menu
                                DpOffset(
                                    press.position.x.toDp(),
                                    press.position.y.toDp()
                                )
                            }
                        }
                        // Consume the press event so it doesn't propagate further
                        event.changes.forEach {
                            it.consume()
                        }
                        // Wait for the release and consume it as well
                        waitForUpOrCancellation()?.consume()
                    }
                }
            }
            .then(modifier),
    ) {
        AnimatedVisibility(isMenuVisible) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { isMenuVisible = false },
                offset = rightClickOffset,
            ) {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = {
                        // Custom Reply functionality
                    },
                )
            }
        }
        MessageBubbleSurface(
            isVideoContentAttached = message.isVideoContentAttached,
            isIncoming = message.isIncoming,
            onVideoClick = onVideoClick,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                SelectionContainer {
                    Text(text = message.text)
                }
                AttachedMedia(
                    message = message,
                    modifier = Modifier.draggableMediaItem(message),
                )
            }
        }
    }
}

@Composable
private fun MessageBubbleSurface(
    isVideoContentAttached: Boolean,
    isIncoming: Boolean,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = remember { ripple() }

    Surface(
        modifier = modifier
            .then(
                if (isVideoContentAttached) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = indication,
                        onClick = onVideoClick,
                    )
                } else {
                    Modifier.focusable(interactionSource = interactionSource)
                },
            )
            .indication(interactionSource = interactionSource, indication = indication),
        color = if (isIncoming) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        shape = MaterialTheme.shapes.large,
        content = content,
    )
}

@Composable
private fun AttachedMedia(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val uri = message.mediaUri
    if (uri != null) {
        when {
            message.isImageContentAttached -> {
                Photo(
                    uri = uri,
                    modifier = modifier,
                )
            }

            message.isVideoContentAttached -> {
                Video(
                    uri = uri,
                    modifier = modifier,
                )
            }

            else -> {
                Log.e(TAG, "Unrecognized media type")
            }
        }
    }
}

@Composable
private fun Photo(
    uri: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(uri)
            .build(),
        contentDescription = null,
        modifier = modifier
            .height(250.dp)
            .padding(10.dp),
    )
}

@Composable
private fun Video(
    uri: String,
    modifier: Modifier = Modifier,
) {
    VideoPreview(
        videoUri = uri,
        modifier = modifier.padding(10.dp),
        overlay = {
            PlayArrowIcon(
                modifier = Modifier.Companion
                    .size(50.dp)
                    .align(Alignment.Center),
            )
        },
    )
}

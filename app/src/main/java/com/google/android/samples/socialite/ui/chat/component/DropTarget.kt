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

import android.app.Activity
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import com.google.android.samples.socialite.ui.chat.MediaItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
internal fun Modifier.mediaItemDropTarget(
    onMediaItemAttached: (MediaItem) -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier {
    return dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            val shouldStart = supportedMediaTypes.any { mediaType ->
                event.mimeTypes().any { mimeType ->
                    isMimeTypeMatched(mediaType.representation, mimeType)
                }
            }
            shouldStart
        },
        target = rememberMediaItemDropTarget(
            onMediaItemAttached = onMediaItemAttached,
            interactionSource = interactionSource,
        ),
    ).mediaItemDropTargetIndication(interactionSource)
}

private fun isMimeTypeMatched(expected: String, actual: String): Boolean {
    val expectedParts = expected.split("/")
    val actualParts = actual.split("/")

    return when {
        expectedParts.size != 2 || actualParts.size != 2 -> false
        expectedParts[1] == "*" -> expectedParts[0] == actualParts[0]
        else -> expectedParts == actualParts
    }
}

@Composable
private fun rememberMediaItemDropTarget(
    onMediaItemAttached: (MediaItem) -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    activity: Activity? = LocalActivity.current,
): DragAndDropTarget {
    return remember(onMediaItemAttached, interactionSource) {
        object : DragAndDropTarget {
            private var dragStart: DragInteraction.Start? = null

            override fun onStarted(event: DragAndDropEvent) {
                val start = DragInteraction.Start()
                interactionSource.tryEmit(start)
                dragStart = start
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData

                val mimeTypes = event.mimeTypes().filter { mimeType ->
                    supportedMediaTypes.any { supported ->
                        isMimeTypeMatched(supported.representation, mimeType)
                    }
                }

                return if (
                    // Check if the app can request the permission to use the dropped data
                    activity === null ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
                    // Check if clipData has at least 1 item with MIME type
                    clipData.itemCount < 1 || mimeTypes.isEmpty()
                ) {
                    false
                } else {
                    val dropPermission =
                        activity.requestDragAndDropPermissions(event.toAndroidDragEvent())

                    val item = clipData.getItemAt(0)
                    onMediaItemAttached(MediaItem(item.uri.toString(), mimeTypes.first()))
                    dropPermission?.release()
                    true
                }
            }

            override fun onEnded(event: DragAndDropEvent) {
                notifyDragStop()
            }

            override fun onEntered(event: DragAndDropEvent) {
                interactionSource.tryEmit(DragEnterInteraction())
            }

            override fun onExited(event: DragAndDropEvent) {
                interactionSource.tryEmit(DragExitInteraction())
            }

            private fun notifyDragStop() {
                val start = dragStart
                if (start != null) {
                    interactionSource.tryEmit(DragInteraction.Stop(start))
                    dragStart = null
                }
            }
        }
    }
}

@Composable
private fun Modifier.mediaItemDropTargetIndication(
    interactionSource: InteractionSource,
    foreground: Brush = SolidColor(MaterialTheme.colorScheme.inverseSurface),
    background: Brush = SolidColor(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.4f)),
): Modifier {
    return indication(
        interactionSource = interactionSource,
        indication = MediaItemDropTargetIndication(
            foreground = foreground,
            background = background,
        ),
    )
}

private class MediaItemDropTargetIndication(
    private val foreground: Brush,
    private val background: Brush,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return MediaItemDropTargetIndicationNode(
            interactionSource = interactionSource,
            foreground = foreground,
            background = background,
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is MediaItemDropTargetIndication &&
            other.foreground == foreground &&
            other.background == background
    }

    override fun hashCode(): Int {
        return foreground.hashCode() * 31 + background.hashCode()
    }
}

private class MediaItemDropTargetIndicationNode(
    interactionSource: InteractionSource,
    private val foreground: Brush,
    private val background: Brush,
) : Modifier.Node(), DrawModifierNode {
    private val events = interactionSource.interactions.filter {
        it is DragInteraction
    }

    private var isVisible = false
    private var isEntered = false

    override fun onAttach() {
        coroutineScope.launch {
            events.collectLatest { event ->
                when (event) {
                    is DragInteraction.Start -> {
                        isVisible = true
                    }

                    is DragEnterInteraction -> {
                        isEntered = true
                    }

                    is DragExitInteraction -> {
                        isEntered = false
                    }

                    else -> {
                        isEntered = false
                        isVisible = false
                    }
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (isVisible) {
            if (isEntered) {
                drawRect(
                    brush = background,
                    size = size,
                )
            }

            val outline = RectangleShape.createOutline(
                size = size,
                layoutDirection = layoutDirection,
                density = this,
            )
            drawOutline(
                outline = outline,
                brush = foreground,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(10.dp.toPx(), 10.dp.toPx()),
                    ),
                ),
            )
        }
    }
}

private class DragEnterInteraction : DragInteraction
private class DragExitInteraction : DragInteraction

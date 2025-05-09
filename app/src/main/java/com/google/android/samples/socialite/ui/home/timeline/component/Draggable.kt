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

import android.os.Build
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.draggableMediaItem(
    mediaItem: TimelineMediaItem,
): Modifier {
    val clipData = tryIntoClipData(mediaItem)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && clipData != null) {
        dragAndDropSource(
            block = {
                detectDragGesturesAfterLongPress { _, _ ->
                    val data = DragAndDropTransferData(
                        clipData = clipData,
                        flags = View.DRAG_FLAG_GLOBAL
                            or View.DRAG_FLAG_GLOBAL_URI_READ
                            or View.DRAG_FLAG_GLOBAL_PERSISTABLE_URI_PERMISSION,
                    )
                    startTransfer(data)
                }
            },
        )
    } else {
        this
    }
}

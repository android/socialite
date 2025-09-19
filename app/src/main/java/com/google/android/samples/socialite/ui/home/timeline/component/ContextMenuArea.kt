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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.samples.socialite.ui.components.ContextMenuArea
import com.google.android.samples.socialite.ui.components.ContextMenuItem
import com.google.android.samples.socialite.ui.components.rememberContextMenuToCopyMediaItem
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem

@Composable
internal fun ContextMenuArea(
    mediaItem: TimelineMediaItem,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    ContextMenuArea(
        items = rememberContextMenuItem(mediaItem),
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun rememberContextMenuItem(
    mediaItem: TimelineMediaItem,
): List<ContextMenuItem> {
    val clipData = tryIntoClipData(mediaItem)

    return rememberContextMenuToCopyMediaItem(clipData)
}

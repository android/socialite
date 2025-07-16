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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import com.google.android.samples.socialite.ui.chat.MediaItem

@OptIn(ExperimentalFoundationApi::class)
internal fun TransferableContent.tryCreateMediaItem(
    mediaType: MediaType,
    onMediaItemAttached: (MediaItem) -> Unit,
): TransferableContent? {
    val mimeTypes = clipMetadata.clipDescription.filterMimeTypes(mediaType.representation)
    return if (mimeTypes.isEmpty()) {
        this
    } else {
        consume { item ->
            onMediaItemAttached(MediaItem(item.uri.toString(), mimeTypes.first()))
            true
        }
    }
}

internal val MediaType.Companion.Video get() = MediaType("video/*")

internal val supportedMediaTypes = listOf(MediaType.Image, MediaType.Video)

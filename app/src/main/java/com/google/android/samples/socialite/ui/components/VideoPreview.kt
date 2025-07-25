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

package com.google.android.samples.socialite.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun VideoPreview(
    videoUri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    colorFilter: ColorFilter = ColorFilter.tint(Color.Gray, BlendMode.Darken),
    overlay: @Composable (BoxScope.() -> Unit)? = null,
) {
    when (overlay) {
        null -> VideoPreview(
            state = rememberVideoPreviewBitmap(videoUri),
            modifier = modifier,
            contentScale = contentScale,
            contentDescription = contentDescription,
            colorFilter = colorFilter,
        )
        else -> {
            VideoPreview(
                state = rememberVideoPreviewBitmap(videoUri),
                overlay = overlay,
                modifier = modifier,
                contentScale = contentScale,
                contentDescription = contentDescription,
                colorFilter = colorFilter,
            )
        }
    }
}

@Composable
private fun VideoPreview(
    state: State<Bitmap?>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    colorFilter: ColorFilter = ColorFilter.tint(Color.Gray, BlendMode.Darken),
) {
    val bitmap = state.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            colorFilter = colorFilter,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}

@Composable
private fun VideoPreview(
    state: State<Bitmap?>,
    overlay: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    colorFilter: ColorFilter = ColorFilter.tint(Color.Gray, BlendMode.Darken),
) {
    val bitmap = state.value
    if (bitmap != null) {
        Box(
            modifier = modifier,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                colorFilter = colorFilter,
                contentScale = contentScale,
            )
            overlay()
        }
    }
}

private suspend fun createVideoPreviewBitmap(videoUri: String, context: Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        val mediaMetadataRetriever = MediaMetadataRetriever()

        // Remote url
        if (videoUri.contains("https://")) {
            mediaMetadataRetriever.setDataSource(videoUri, HashMap<String, String>())
        } else { // Locally saved files
            mediaMetadataRetriever.setDataSource(context, videoUri.toUri())
        }
        // Return any frame that the framework considers representative of a valid frame
        mediaMetadataRetriever.frameAtTime
    }
}

@Composable
private fun rememberVideoPreviewBitmap(videoUri: String): State<Bitmap?> {
    val context = LocalContext.current
    val state = remember(videoUri) {
        val state = mutableStateOf<Bitmap?>(null)
        state
    }.also {
        LaunchedEffect(videoUri) {
            it.value = createVideoPreviewBitmap(videoUri, context)
        }
    }
    return state
}

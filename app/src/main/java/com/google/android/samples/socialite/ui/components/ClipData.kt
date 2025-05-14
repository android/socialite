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

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

private const val CLIP_DATA_LABEL = "media_item"

internal fun Context.tryCreateClipData(
    uri: Uri,
): Result<ClipData> {
    return tryCreateContentUriWithFileProvider(uri).recover {
        uri
    }.map { sharableUri ->
        Log.d("tryCreateClipData", "sharableUri: $sharableUri")
        ClipData.newUri(contentResolver, CLIP_DATA_LABEL, sharableUri)
    }
}

// Create a shareable URI for the pasted/dropped images and videos.
private fun Context.tryCreateContentUriWithFileProvider(
    mediaItem: Uri,
    authority: String = "com.google.android.samples.socialite.file_provider",
): Result<Uri> {
    return runCatching {
        val file = File(mediaItem.path!!)
        FileProvider.getUriForFile(this, authority, file)
    }
}

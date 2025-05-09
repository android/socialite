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
import androidx.core.content.FileProvider
import java.io.File

private const val CLIP_DATA_LABEL = "media_item"

internal fun Context.tryCreateClipData(
    uri: Uri,
): Result<ClipData> {
    return tryCreateContentUriWithFileProvider(uri).fold(
        onSuccess = {
            Result.success(it)
        },
        onFailure = {
            // Asset files can not be shared with other apps.
            if (uri.host == "com.google.android.samples.socialite") {
                Result.failure(UnsupportedOperationException("Unsupported media item"))
            } else {
                // Images from PhotoPicker can be shared with other apps.
                Result.success(uri)
            }
        },
    ).map { uri ->
        ClipData.newUri(contentResolver, CLIP_DATA_LABEL, uri)
    }
}

// Create a shareable URI for the pasted/dropped images and videos.
private fun Context.tryCreateContentUriWithFileProvider(
    mediaItem: Uri,
    authority: String = "com.google.android.samples.socialite.file_provider",
): Result<Uri> {
    val file = File(mediaItem.path!!)
    return tryCreateContentUriWithFileProvider(file, authority)
}

private fun Context.tryCreateContentUriWithFileProvider(
    file: File,
    authority: String = "com.google.android.samples.socialite.file_provider",
): Result<Uri> {
    return try {
        val uri = FileProvider.getUriForFile(this, authority, file)
        Result.success(uri)
    } catch (e: IllegalArgumentException) {
        Result.failure(e)
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.google.android.samples.socialite.ui

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.IconCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Creates and remembers a [Painter] from a bitmap icon specified by the [contentUri].
 */
@Composable
fun rememberIconPainter(contentUri: Uri): Painter {
    val context = LocalContext.current

    val loadDrawable by produceState<Drawable?>(
        initialValue = null,
        key1 = contentUri,
    ) {
        value = withContext(Dispatchers.IO) {
            val icon = IconCompat.createWithAdaptiveBitmapContentUri(contentUri)
            icon.loadDrawable(context)
        }
    }

    return rememberDrawablePainter(drawable = loadDrawable)
}

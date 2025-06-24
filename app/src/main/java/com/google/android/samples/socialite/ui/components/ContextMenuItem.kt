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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import com.google.android.samples.socialite.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun rememberContextMenuToCopyMediaItem(
    clipData: ClipData?,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): List<ContextMenuItem> {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    return remember(clipData, clipboard, coroutineScope, context) {
        if (clipData != null) {
            listOf(
                ContextMenuItem(
                    label = context.getString(R.string.copy),
                    action = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(clipData),
                            )
                        }
                    },
                ),
            )
        } else {
            emptyList()
        }
    }
}

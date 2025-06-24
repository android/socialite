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

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.google.android.samples.socialite.ui.chat.ChatMessage
import com.google.android.samples.socialite.ui.components.tryCreateClipData

@Composable
internal fun tryIntoClipData(chatMessage: ChatMessage): ClipData? {
    val context = LocalContext.current
    return remember(chatMessage.mediaUri, context) {
        if (chatMessage.mediaUri == null) {
            null
        } else {
            context.tryCreateClipData(chatMessage.mediaUri.toUri()).getOrNull()
        }
    }
}

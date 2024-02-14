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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.samples.socialite.ui.chat.ChatScreen

@Composable
fun Bubble(chatId: Long) {
    SocialTheme {
        ChatScreen(
            chatId = chatId,
            foreground = false,
            onBackPressed = null,
            // TODO (donovanfm): Hook up camera button in the Bubble composable
            onCameraClick = {},
            // TODO (jolandaverhoef): Hook up play video button in the Bubble composable
            onVideoClick = {},
            // TODO (mayurikhin): Hook up camera button in the Bubble composable
            onPhotoPickerClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

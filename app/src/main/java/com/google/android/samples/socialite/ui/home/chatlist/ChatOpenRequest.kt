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

package com.google.android.samples.socialite.ui.home.chatlist

import com.google.android.samples.socialite.AppArgs
import com.google.android.samples.socialite.model.ChatDetail

sealed interface ChatOpenRequest {
    val chatId: Long

    data class SameWindow(override val chatId: Long) : ChatOpenRequest
    data class NewWindow(override val chatId: Long) : ChatOpenRequest {
        fun toAppArgs(): AppArgs.LaunchParams {
            return AppArgs.LaunchParams(chatId)
        }
    }

    companion object {
        fun openInNewWindow(chatDetail: ChatDetail) = NewWindow(chatDetail.chatWithLastMessage.id)
        fun openInSameWindow(chatDetail: ChatDetail) = SameWindow(chatDetail.chatWithLastMessage.id)
    }
}

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

import android.app.Activity
import android.content.Intent
import android.os.Build
import com.google.android.samples.socialite.AppArgs
import com.google.android.samples.socialite.model.ChatDetail

internal fun Activity.openChatInNewInstance(request: OpenChatRequest.NewInstance) {
    val intent = packageManager.getLaunchIntentForPackage(packageName)

    if (intent != null) {
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }
        intent.putExtra(OpenChatRequest.NewInstance.KEY, request.chatId)
        startActivity(intent)
    }
}

sealed interface OpenChatRequest {
    val chatId: Long

    data class SameInstance(override val chatId: Long) : OpenChatRequest {
        companion object {
            fun from(chat: ChatDetail): SameInstance {
                return SameInstance(chat.chatWithLastMessage.id)
            }
        }
    }

    data class NewInstance(override val chatId: Long) : OpenChatRequest {
        companion object {
            const val KEY = AppArgs.ChatParams.KEY

            fun from(chat: ChatDetail): NewInstance {
                return NewInstance(chat.chatWithLastMessage.id)
            }
        }
    }
}

/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.samples.socialite.repository

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.samples.socialite.model.Message
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatReplyHelper @Inject constructor(
    private val repository: ChatRepository,
) {

    fun start(lifecycle: Lifecycle) {
        lifecycle.coroutineScope.launch {
            var first = true
            // Prevents the same outgoing message to be processed twice.
            var previousMessageId = 0L
            // The Flow from Room is collected every time the table is modified. The same outgoing
            // message can be passed multiple times.
            repository.getLatestOutgoingMessage().collect { message ->
                if (first) {
                    // Ignore the first message because it is an existing latest message.
                    first = false
                } else if (previousMessageId != message.id) {
                    handleNewMessage(message)
                    previousMessageId = message.id
                }
            }
        }
    }

    /**
     * message: A [Message] sent by the user.
     */
    private suspend fun handleNewMessage(message: Message) {
        // The person is typing...
        delay(5000L)
        val replier = repository.findReplier(message) ?: return
        repository.receiveMessage(
            replier.reply(message.text).apply { chatId = message.chatId }.build(),
        )
    }
}

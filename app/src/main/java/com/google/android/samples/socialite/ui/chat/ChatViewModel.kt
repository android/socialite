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

package com.google.android.samples.socialite.ui.chat

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.android.samples.socialite.ui.stateInUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    private val chatId = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val chatDetail = chatId.flatMapLatest { id -> repository.findChat(id) }

    private val attendees =
        chatDetail.map { c -> (c?.attendees ?: emptyList()).associateBy { it.id } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val messagesInChat = chatId.flatMapLatest { id -> repository.findMessages(id) }

    val chat = chatDetail.stateInUi(null)

    val messages = combine(messagesInChat, attendees) { messages, attendees ->

        // List of senders, which is referred to the message list to show or not the icon.
        val senderList = messages.fold(emptyList<Long?>()) { list, message ->
            val senderId = if (list.isEmpty() || list.last() != message.senderId) {
                message.senderId
            } else {
                null
            }
            list + senderId
        }

        messages.zip(senderList).map { (message, senderId) ->
            // Show the sender's icon only for the first message in a row from the same sender.
            val senderIconUri = if (senderId != null) {
                attendees[senderId]?.iconUri
            } else {
                null
            }

            ChatMessage(
                text = message.text,
                mediaUri = message.mediaUri,
                mediaMimeType = message.mediaMimeType,
                timestamp = message.timestamp,
                isIncoming = message.isIncoming,
                senderIconUri = senderIconUri,
            )
        }
    }.stateInUi(emptyList())

    val textFieldState = TextFieldState()
    private val attachedMediaItem = MutableStateFlow<MediaItem?>(null)
    val attachedMedia = attachedMediaItem

    private val isInputValidFlow = snapshotFlow {
        isInputValid(textFieldState)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val sendEnabled = combine(
        isInputValidFlow,
        attachedMediaItem,
    ) { isInputValid, attachedMediaItem ->
        isInputValid || attachedMediaItem != null
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )

    private var inputPrefilled = false

    /**
     * We want to update the notification when the corresponding chat screen is open. Setting this
     * to `true` updates the current notification, removing the unread message(s) badge icon and
     * suppressing further notifications.
     */
    fun setForeground(foreground: Boolean) {
        val chatId = chatId.value
        if (chatId != 0L) {
            if (foreground) {
                repository.activateChat(chatId)
            } else {
                repository.deactivateChat(chatId)
            }
        }
    }

    fun setChatId(chatId: Long) {
        this.chatId.value = chatId
    }

    fun updateInput(input: String) {
        textFieldState.setTextAndPlaceCursorAtEnd(input)
    }

    fun prefillInput(input: String) {
        if (inputPrefilled) return
        inputPrefilled = true
        updateInput(input)
    }

    fun attachMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.saveAttachedMediaItem(mediaItem)
                .onSuccess {
                    attachedMediaItem.emit(it)
                }
                .onFailure {
                    attachedMediaItem.emit(null)
                }
        }
    }

    fun removeAttachedMedia() {
        viewModelScope.launch {
            val mediaItem = attachedMediaItem.value
            if (mediaItem != null) {
                repository.removeAttachedMediaItem(mediaItem)
                    .onSuccess {
                        attachedMediaItem.emit(null)
                    }
                    .onFailure {}
            }
        }
    }

    fun send() {
        val chatId = chatId.value
        if (chatId <= 0) return
        if (!sendEnabled.value) return

        val mediaItem = attachedMediaItem.value
        val input = textFieldState.text.toString()
        viewModelScope.launch {
            if (mediaItem != null) {
                repository.sendMessage(chatId, input, mediaItem.uri, mediaItem.mimeType)
            } else {
                repository.sendMessage(chatId, input, null, null)
            }
            textFieldState.clearText()
            attachedMediaItem.emit(null)
        }
    }
}

private fun isInputValid(textFieldState: TextFieldState): Boolean {
    return textFieldState.text.isNotBlank()
}

data class MediaItem(val uri: String, val mimeType: String)

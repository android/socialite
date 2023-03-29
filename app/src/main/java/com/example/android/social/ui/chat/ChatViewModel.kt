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

package com.example.android.social.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.social.repository.ChatRepository
import com.example.android.social.ui.stateInUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ChatRepository = ChatRepository.getInstance(application),
) : AndroidViewModel(application) {

    private val _chatId = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val chat = _chatId
        .flatMapLatest { id -> repository.findChat(id) }
        .stateInUi(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _chatId
        .flatMapLatest { id -> repository.findMessages(id) }
        .stateInUi(emptyList())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    /**
     * We want to update the notification when the corresponding chat screen is open. Setting this
     * to `true` updates the current notification, removing the unread message(s) badge icon and
     * suppressing further notifications.
     */
    fun setForeground(foreground: Boolean) {
        val chatId = _chatId.value
        if (chatId != 0L) {
            if (foreground) {
                repository.activateChat(chatId)
            } else {
                repository.deactivateChat(chatId)
            }
        }
    }

    fun setChatId(chatId: Long) {
        _chatId.value = chatId
    }

    fun updateInput(input: String) {
        _input.value = input
    }

    fun send() {
        viewModelScope.launch {
            repository.sendMessage(_chatId.value, _input.value, null, null)
            _input.value = ""
        }
    }
}

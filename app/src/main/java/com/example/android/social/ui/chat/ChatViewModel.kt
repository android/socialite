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
import com.example.android.social.data.ChatRepository
import com.example.android.social.data.DefaultChatRepository
import com.example.android.social.ui.stateInUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ChatRepository = DefaultChatRepository.getInstance(application),
) : AndroidViewModel(application) {

    private val _chatId = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val contact = _chatId
        .flatMapLatest { id -> repository.findContact(id) }
        .stateInUi(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _chatId
        .flatMapLatest { id -> repository.findMessages(id) }
        .stateInUi(emptyList())

    fun setChatId(chatId: Long) {
        _chatId.value = chatId
    }
}

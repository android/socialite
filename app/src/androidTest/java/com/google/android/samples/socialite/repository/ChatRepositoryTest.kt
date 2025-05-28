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

package com.google.android.samples.socialite.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.android.samples.socialite.data.ChatDao
import com.google.android.samples.socialite.data.ContactDao
import com.google.android.samples.socialite.data.MessageDao
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.model.Message
import com.google.android.samples.socialite.widget.model.WidgetModelRepository
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class ChatRepositoryTest {
    private lateinit var repository: ChatRepository
    private lateinit var chatDao: ChatDao
    private lateinit var messageDao: MessageDao
    private lateinit var contactDao: ContactDao
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var widgetModelRepository: WidgetModelRepository
    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        chatDao = mock()
        messageDao = mock()
        contactDao = mock()
        notificationHelper = mock()
        widgetModelRepository = mock()
        context = mock()
        dataStore = mock()
        scope = mock()
        repository = ChatRepository(chatDao, messageDao, contactDao, notificationHelper, widgetModelRepository, scope, context)

    }
    @Test
    fun getChats() = runTest {
        val repository = createTestRepository()
        repository.getChats().test {
            assertThat(awaitItem()).isNotEmpty()
        }
    }

    @Test
    fun findChat() = runTest {
        val repository = createTestRepository()
        repository.findChat(1L).test {
            assertThat(awaitItem()!!.firstContact.name).isEqualTo("Cat")
        }
    }

    @Test
    fun findMessages() = runTest {
        val repository = createTestRepository()
        repository.findMessages(1L).test {
            assertThat(awaitItem()).hasSize(2)
        }
    }
    @Test
    fun sendMessage_regular() = runTest {
        val repository = createTestRepository()
        val chatId = 1L
        val text = "Hello"
        repository.sendMessage(chatId, text, null, null)
        repository.findMessages(chatId).test {
            val messages = awaitItem()
            assertThat(messages).hasSize(3)
            assertThat(messages.first().text).isEqualTo(text)
        }

    }
    @Test
    fun sendMessage_withMedia() = runTest {
        val repository = createTestRepository()
        val chatId = 1L
        val text = "Hello"
        val mediaUri = "content://media"
        val mediaMimeType = "image/png"
        repository.sendMessage(chatId, text, mediaUri, mediaMimeType)
        repository.findMessages(chatId).test {
            val messages = awaitItem()
            assertThat(messages).hasSize(3)
            assertThat(messages.first().text).isEqualTo(text)
            assertThat(messages.first().mediaUri).isEqualTo(mediaUri)
            assertThat(messages.first().mediaMimeType).isEqualTo(mediaMimeType)
        }

    }

    @Test
    fun sendMessage_chatbot() = runTest {
        val repository = createTestRepository()
        val chatId = 1L
        val text = "Hello"
        val mediaUri = "content://media"
        val mediaMimeType = "image/png"
        val isBotEnabled = true
        val responseText = "Hi there, this is a response"
        whenever(repository.isBotEnabled.first()).thenReturn(isBotEnabled)
        repository.sendMessage(chatId, text, mediaUri, mediaMimeType)
        repository.findMessages(chatId).test {
            val messages = awaitItem()
            assertThat(messages).hasSize(4)
            assertThat(messages[0].text).isEqualTo(text)
            assertThat(messages[1].text).isEqualTo(responseText)
        }

    }
    @Test
    fun preloadShortVideos() = runTest {
        val repository = createTestRepository()
        val chatId = 1L
        val text = "preload"
        val detail = ChatDetail(null, null, null)
        whenever(chatDao.loadDetailById(chatId)).thenReturn(detail)
        repository.sendMessage(chatId, text, null, null)
        repository.findMessages(chatId).test {
            val messages = awaitItem()
            assertThat(messages).hasSize(ShortsVideoList.mediaUris.size + 2)
        }
    }

     @Test
    fun toggleChatbotSetting() = runTest {
        val isBotEnabled = false
        whenever(context.dataStore.data).thenReturn(mock())
        whenever(context.dataStore.edit(any())).thenAnswer { invocation ->
            val preferences = invocation.arguments[0] as MutableMap<*,*>
             val key = booleanPreferencesKey("enable_chatbot")
            if (preferences[key] == isBotEnabled){
                preferences[key] = !isBotEnabled
            } else {
                preferences[key] = isBotEnabled
            }

        }

        repository.toggleChatbotSetting()
        Truth.assertThat(repository.isBotEnabled.first()).isEqualTo(!isBotEnabled)

    }
}

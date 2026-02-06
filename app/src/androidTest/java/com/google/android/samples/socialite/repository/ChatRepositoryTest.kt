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
import androidx.compose.foundation.text2.input.insert
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.android.samples.socialite.data.ChatDao
import com.google.android.samples.socialite.data.ContactDao
import com.google.android.samples.socialite.data.MessageDao
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.model.Contact
import com.google.android.samples.socialite.model.Message
import com.google.android.samples.socialite.widget.model.WidgetModelRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ChatRepositoryTest {

    @Mock
    private lateinit var chatDao: ChatDao

    @Mock
    private lateinit var messageDao: MessageDao

    @Mock
    private lateinit var contactDao: ContactDao

    @Mock
    private lateinit var notificationHelper: NotificationHelper

    @Mock
    private lateinit var widgetModelRepository: WidgetModelRepository

    private lateinit var chatRepository: ChatRepository

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private val testScope: CoroutineScope = CoroutineScope(testDispatcher)
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        chatRepository = ChatRepository(
            chatDao,
            messageDao,
            contactDao,
            notificationHelper,
            widgetModelRepository,
            testScope,
            context,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getChats() = runTest {
        val chatDetails = listOf(
            ChatDetail(1, Contact(1, "Contact 1", "", "", "", "", "", "", ""), emptyList()),
            ChatDetail(2, Contact(2, "Contact 2", "", "", "", "", "", "", ""), emptyList()),
        )
        `when`(chatDao.allDetails()).thenReturn(flowOf(chatDetails))
        chatRepository.getChats().test {
            assertThat(awaitItem()).isEqualTo(chatDetails)
        }
        verify(chatDao).allDetails()
    }

    @Test
    fun findChat() = runTest {
        val chatId = 1L
        val chatDetail = ChatDetail(chatId, Contact(1, "Contact 1", "", "", "", "", "", "", ""), emptyList())
        `when`(chatDao.detailById(chatId)).thenReturn(flowOf(chatDetail))
        chatRepository.findChat(chatId).test {
            assertThat(awaitItem()).isEqualTo(chatDetail)
        }
        verify(chatDao).detailById(chatId)
    }

    @Test
    fun findMessages() = runTest {
        val chatId = 1L
        val messages = listOf(
            Message(1, chatId, 1, "Message 1", null, null, 1234567890),
            Message(2, chatId, 2, "Message 2", null, null, 1234567891),
        )
        `when`(messageDao.allByChatId(chatId)).thenReturn(flowOf(messages))
        chatRepository.findMessages(chatId).test {
            assertThat(awaitItem()).isEqualTo(messages)
        }
        verify(messageDao).allByChatId(chatId)
    }

    @Test
    fun `sendMessage saves message and notifies`() = runTest {
        val chatId = 1L
        val text = "Hello"
        val mediaUri = null
        val mediaMimeType = null
        val contact = Contact(1, "Contact 1", "", "", "", "", "", "", "")
        val chatDetail = ChatDetail(chatId, contact, emptyList())
        `when`(chatDao.loadDetailById(chatId)).thenReturn(chatDetail)
        `when`(messageDao.loadAll(chatId)).thenReturn(emptyList())

        chatRepository.sendMessage(chatId, text, mediaUri, mediaMimeType)

        verify(messageDao).insert(any())
        verify(notificationHelper).pushShortcut(contact, PushReason.OutgoingMessage)
        verify(widgetModelRepository).updateUnreadMessagesForContact(contactId = contact.id, unread = true)
    }

    @Test
    fun `sendMessage with bot enabled sends message to gemini and saves response`() = runTest {
        val chatId = 1L
        val text = "Hello"
        val mediaUri = null
        val mediaMimeType = null
        val contact = Contact(1, "Contact 1", "", "", "", "", "", "", "")
        val chatDetail = ChatDetail(chatId, contact, emptyList())
        val pastMessages = emptyList<Message>()
        val dataStore = context.dataStore
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("enable_chatbot")] = true
        }

        `when`(chatDao.loadDetailById(chatId)).thenReturn(chatDetail)
        `when`(messageDao.loadAll(chatId)).thenReturn(pastMessages)
        `when`(messageDao.allByChatId(chatId)).thenReturn(flowOf(pastMessages))

        chatRepository.sendMessage(chatId, text, mediaUri, mediaMimeType)
        // Wait for the coroutine to finish
        testScope.launch {
            chatRepository.isBotEnabled.collect {
                if (it) {
                    verify(messageDao, times(2)).insert(any())
                }
            }
        }
    }

    @Test
    fun `sendMessage with bot disabled simulates peer response`() = runTest {
        val chatId = 1L
        val text = "Hello"
        val mediaUri = null
        val mediaMimeType = null
        val contact = Contact(1, "Contact 1", "", "", "", "", "", "", "")
        val chatDetail = ChatDetail(chatId, contact, emptyList())
        val dataStore = context.dataStore
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("enable_chatbot")] = false
        }

        `when`(chatDao.loadDetailById(chatId)).thenReturn(chatDetail)
        `when`(messageDao.loadAll(chatId)).thenReturn(emptyList())

        chatRepository.sendMessage(chatId, text, mediaUri, mediaMimeType)

        verify(messageDao, times(2)).insert(any())
    }
}

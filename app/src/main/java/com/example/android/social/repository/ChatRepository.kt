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

package com.example.android.social.repository

import android.content.Context
import androidx.room.Room
import com.example.android.social.data.AppDatabase
import com.example.android.social.model.ChatDetail
import com.example.android.social.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class ChatRepository internal constructor(
    private val database: AppDatabase,
    private val notificationHelper: NotificationHelper,
    private val executor: Executor,
) {

    companion object {
        private var instance: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return instance ?: synchronized(this) {
                instance ?: ChatRepository(
                    Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
                        .build(),
                    NotificationHelper(context),
                    Executors.newFixedThreadPool(4),
                ).also {
                    instance = it
                }
            }
        }
    }

    private var currentChat: Long = 0L

    init {
        notificationHelper.setUpNotificationChannels()
    }

    suspend fun initialize() {
        database.populateInitialData()
    }

    fun getChats(): Flow<List<ChatDetail>> {
        return database.chat().allDetails()
    }

    fun findChat(chatId: Long): Flow<ChatDetail?> {
        return database.chat().detailById(chatId)
    }

    fun findMessages(chatId: Long): Flow<List<Message>> {
        return database.message().allByChatId(chatId)
    }

    suspend fun sendMessage(
        chatId: Long,
        text: String,
        mediaUri: String?,
        mediaMimeType: String?,
    ) {
        val detail = database.chat().loadDetailById(chatId) ?: return
        database.message().insert(
            Message(
                id = 0L,
                chatId = chatId,
                senderId = 0L, // User
                text = text,
                mediaUri = mediaUri,
                mediaMimeType = mediaMimeType,
                timestamp = System.currentTimeMillis(),
            ),
        )
        notificationHelper.pushShortcut(detail.firstContact, PushReason.OutgoingMessage)
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            // The person is typing...
            delay(5000L)
            // Receive a reply.
            database.message().insert(
                detail.firstContact.reply(text).apply { this.chatId = chatId }.build(),
            )
            notificationHelper.pushShortcut(detail.firstContact, PushReason.IncomingMessage)
            // Show notification if the chat is not on the foreground.
            if (chatId != currentChat) {
                notificationHelper.showNotification(
                    detail.firstContact,
                    database.message().loadAll(chatId),
                    false,
                )
            }
        }
    }

    suspend fun updateNotification(chatId: Long) {
        val detail = database.chat().loadDetailById(chatId) ?: return
        val messages = database.message().loadAll(chatId)
        notificationHelper.showNotification(
            detail.firstContact,
            messages,
            fromUser = false,
            update = true,
        )
    }

    fun activateChat(chatId: Long) {
        currentChat = chatId
        notificationHelper.dismissNotification(chatId)
    }

    fun deactivateChat(chatId: Long) {
        if (currentChat == chatId) {
            currentChat = 0
        }
    }

    suspend fun showAsBubble(chatId: Long) {
        val detail = database.chat().loadDetailById(chatId) ?: return
        val messages = database.message().loadAll(chatId)
        notificationHelper.showNotification(detail.firstContact, messages, true)
    }

    suspend fun canBubble(chatId: Long): Boolean {
        val detail = database.chat().loadDetailById(chatId) ?: return false
        return notificationHelper.canBubble(detail.firstContact)
    }
}

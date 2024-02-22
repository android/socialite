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

import com.google.android.samples.socialite.data.ChatDao
import com.google.android.samples.socialite.data.MessageDao
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.model.Contact
import com.google.android.samples.socialite.model.Message
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ChatRepository @Inject internal constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val notificationHelper: NotificationHelper,
) {
    private var currentChat: Long = 0L

    init {
        notificationHelper.setUpNotificationChannels()
    }

    fun getChats(): Flow<List<ChatDetail>> {
        return chatDao.allDetails()
    }

    fun findChat(chatId: Long): Flow<ChatDetail?> {
        return chatDao.detailById(chatId)
    }

    fun findMessages(chatId: Long): Flow<List<Message>> {
        return messageDao.allByChatId(chatId)
    }

    fun getLatestOutgoingMessage(): Flow<Message> {
        return messageDao.latestOutgoingMessage()
    }

    suspend fun findReplier(message: Message): Contact? {
        // Find the chat room.
        val detail = chatDao.loadDetailById(message.chatId) ?: return null
        // Take the first contact in the chat room to reply.
        // TODO: Take group chat into account.
        return detail.firstContact
    }

    suspend fun sendMessage(
        chatId: Long,
        text: String,
        mediaUri: String?,
        mediaMimeType: String?,
    ) {
        val detail = chatDao.loadDetailById(chatId) ?: return
        messageDao.insert(
            Message(
                id = 0L,
                chatId = chatId,
                // User
                senderId = 0L,
                text = text,
                mediaUri = mediaUri,
                mediaMimeType = mediaMimeType,
                timestamp = System.currentTimeMillis(),
            ),
        )
        notificationHelper.pushShortcut(detail.firstContact, PushReason.OutgoingMessage)
    }

    suspend fun receiveMessage(
        message: Message,
    ) {
        val detail = chatDao.loadDetailById(message.chatId) ?: return
        // Receive a reply.
        messageDao.insert(message)
        notificationHelper.pushShortcut(detail.firstContact, PushReason.IncomingMessage)
        // Show notification if the chat is not on the foreground.
        if (message.chatId != currentChat) {
            notificationHelper.showNotification(
                detail.firstContact,
                messageDao.loadAll(message.chatId),
                false,
            )
        }
    }

    suspend fun clearMessages() {
        messageDao.clearAll()
    }

    suspend fun updateNotification(chatId: Long) {
        val detail = chatDao.loadDetailById(chatId) ?: return
        val messages = messageDao.loadAll(chatId)
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
        val detail = chatDao.loadDetailById(chatId) ?: return
        val messages = messageDao.loadAll(chatId)
        notificationHelper.showNotification(detail.firstContact, messages, true)
    }

    suspend fun canBubble(chatId: Long): Boolean {
        val detail = chatDao.loadDetailById(chatId) ?: return false
        return notificationHelper.canBubble(detail.firstContact)
    }
}

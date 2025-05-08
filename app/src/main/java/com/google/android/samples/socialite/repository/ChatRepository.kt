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
import android.widget.Toast
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.android.samples.socialite.BuildConfig
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.data.ChatDao
import com.google.android.samples.socialite.data.ContactDao
import com.google.android.samples.socialite.data.MessageDao
import com.google.android.samples.socialite.data.utils.ShortsVideoList
import com.google.android.samples.socialite.di.AppCoroutineScope
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.model.Message
import com.google.android.samples.socialite.ui.chat.MediaItem
import com.google.android.samples.socialite.widget.model.WidgetModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class ChatRepository @Inject internal constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val notificationHelper: NotificationHelper,
    private val widgetModelRepository: WidgetModelRepository,
    @AppCoroutineScope
    private val coroutineScope: CoroutineScope,
    @ApplicationContext private val appContext: Context,
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val enableChatbotKey = booleanPreferencesKey("enable_chatbot")
    val isBotEnabled = appContext.dataStore.data.map {
            preference ->
        preference[enableChatbotKey] ?: false
    }

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

    suspend fun sendMessage(
        chatId: Long,
        text: String,
        mediaUri: String?,
        mediaMimeType: String?,
    ) {
        val detail = chatDao.loadDetailById(chatId) ?: return
        // Save the message to the database
        saveMessageAndNotify(chatId, text, 0L, mediaUri, mediaMimeType, detail, PushReason.OutgoingMessage)

        // Create a generative AI Model to interact with the Gemini API.
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-pro-latest",
            // Set your Gemini API in as an `API_KEY` variable in your local.properties file
            apiKey = BuildConfig.API_KEY,
            // Set a system instruction to set the behavior of the model.
            systemInstruction = content {
                text("Please respond to this chat conversation like a friendly ${detail.firstContact.replyModel}.")
            },
        )

        coroutineScope.launch {
            // Special incoming message indicating to add shorts videos to try preload in exoplayer
            if (text == "preload") {
                preloadShortVideos(chatId, detail, PushReason.IncomingMessage)
                return@launch
            }

            if (isBotEnabled.firstOrNull() == true) {
                // Get the previous messages and them generative model chat
                val pastMessages = getMessageHistory(chatId)
                val chat = generativeModel.startChat(
                    history = pastMessages,
                )

                // Send a message prompt to the model to generate a response
                var response = try {
                    if (mediaMimeType?.contains("image") == true) {
                        appContext.contentResolver.openInputStream(
                            Uri.parse(mediaUri),
                        ).use {
                            if (it != null) {
                                chat.sendMessage(BitmapFactory.decodeStream(it)).text?.trim() ?: "..."
                            } else {
                                appContext.getString(
                                    R.string.image_error,
                                )
                            }
                        }
                    } else {
                        chat.sendMessage(text).text?.trim() ?: "..."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    appContext.getString(
                        R.string.gemini_error,
                        e.message ?: appContext.getString(R.string.unknown_error),
                    )
                }

                // Save the generated response to the database
                saveMessageAndNotify(chatId, response, detail.firstContact.id, null, null, detail, PushReason.IncomingMessage)
            } else {
                // Simulate a response from the peer.
                // The code here is just for demonstration purpose in this sample.
                // Real apps will use their server backend and Firebase Cloud Messaging to deliver messages.

                // The person is typing...
                delay(5000L)
                // Receive a reply.
                val message = detail.firstContact.reply(text).apply { this.chatId = chatId }.build()
                saveMessageAndNotify(message.chatId, message.text, detail.firstContact.id, message.mediaUri, message.mediaMimeType, detail, PushReason.IncomingMessage)
            }

            // Show notification if the chat is not on the foreground.
            if (chatId != currentChat) {
                notificationHelper.showNotification(
                    detail.firstContact,
                    messageDao.loadAll(chatId),
                    false,
                )
            }

            widgetModelRepository.updateUnreadMessagesForContact(contactId = detail.firstContact.id, unread = true)
        }
    }

    fun saveAttachedMediaItem(mediaItem: MediaItem): Result<MediaItem?> {
        try {
            var createdUri: String? = null
            appContext.contentResolver.openInputStream(mediaItem.uri.toUri())?.use {
                val filename = "pasted_media_${System.currentTimeMillis()}"
                val file = File(appContext.filesDir, filename)
                file.createNewFile()
                FileOutputStream(file).use { outputStream ->
                    it.copyTo(outputStream)
                }
                createdUri = file.toUri().toString()
            }
            val newMediaItem = if (createdUri != null) {
                MediaItem(createdUri, mediaItem.mimeType)
            } else {
                null
            }
            return Result.success(newMediaItem)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun removeAttachedMediaItem(mediaItem: MediaItem): Result<Unit> {
        try {
            File(mediaItem.uri).delete()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Add list of short form videos as sent messages to current chat history.This is used to test the preload manager of exoplayer
     */
    private suspend fun preloadShortVideos(
        chatId: Long,
        detail: ChatDetail,
        incomingMessage: PushReason,
    ) {
        for ((index, uri) in ShortsVideoList.mediaUris.withIndex())
            saveMessageAndNotify(
                chatId,
                "Shorts $index",
                0L,
                uri,
                "video/mp4",
                detail,
                incomingMessage,
            )
    }

    private suspend fun saveMessageAndNotify(
        chatId: Long,
        text: String,
        senderId: Long,
        mediaUri: String?,
        mediaMimeType: String?,
        detail: ChatDetail,
        pushReason: PushReason,
    ) {
        messageDao.insert(
            Message(
                id = 0L,
                chatId = chatId,
                senderId = senderId,
                text = text,
                mediaUri = mediaUri,
                mediaMimeType = mediaMimeType,
                timestamp = System.currentTimeMillis(),
            ),
        )
        notificationHelper.pushShortcut(detail.firstContact, PushReason.OutgoingMessage)
    }

    private suspend fun getMessageHistory(chatId: Long): List<Content> {
        val pastMessages = findMessages(chatId).first().filter { message ->
            message.text.isNotEmpty()
        }.sortedBy { message ->
            message.timestamp
        }.fold(initial = mutableListOf<Message>()) { acc, message ->
            if (acc.isEmpty()) {
                acc.add(message)
            } else {
                if (acc.last().isIncoming == message.isIncoming) {
                    val lastMessage = acc.removeLast()
                    val combinedMessage = Message(
                        id = lastMessage.id,
                        chatId = chatId,
                        // User
                        senderId = lastMessage.senderId,
                        text = lastMessage.text + " " + message.text,
                        mediaUri = null,
                        mediaMimeType = null,
                        timestamp = System.currentTimeMillis(),
                    )
                    acc.add(combinedMessage)
                } else {
                    acc.add(message)
                }
            }
            return@fold acc
        }

        val lastUserMessage = pastMessages.removeLast()

        val pastContents = pastMessages.mapNotNull { message: Message ->
            val role = if (message.isIncoming) "model" else "user"
            return@mapNotNull content(role = role) { text(message.text) }
        }
        return pastContents
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
        coroutineScope.launch {
            chatDao.detailById(currentChat).filterNotNull().collect { detail ->
                widgetModelRepository.updateUnreadMessagesForContact(detail.firstContact.id, false)
            }
        }
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

    fun toggleChatbotSetting() {
        if (BuildConfig.API_KEY == "DUMMY_API_KEY") {
            Toast.makeText(
                appContext,
                appContext.getString(R.string.set_api_key_toast),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        coroutineScope.launch {
            appContext.dataStore.edit { preferences ->
                preferences[enableChatbotKey] = (preferences[enableChatbotKey]?.not()) ?: false
            }
        }
    }
}

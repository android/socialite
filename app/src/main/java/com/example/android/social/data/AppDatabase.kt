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

package com.example.android.social.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.android.social.model.Chat
import com.example.android.social.model.ChatAttendee
import com.example.android.social.model.Contact
import com.example.android.social.model.Message

@Database(
    entities = [
        Contact::class,
        Chat::class,
        ChatAttendee::class,
        Message::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contact(): ContactDao
    abstract fun chat(): ChatDao
    abstract fun message(): MessageDao

    suspend fun populateInitialData() {
        withTransaction {
            if (contact().count() != 0) return@withTransaction
            val contacts = Contact.CONTACTS
            val currentTimeMillis = System.currentTimeMillis()
            contact().insert(Contact(0L, "You", "you.jpg", ""))
            for (contact in contacts) {
                contact().insert(contact)
                val chatId = chat().createDirectChat(contact.id)
                message().insert(
                    Message(
                        id = 0L,
                        chatId = chatId,
                        senderId = contact.id,
                        text = "Send me a message",
                        mediaUri = null,
                        mediaMimeType = null,
                        timestamp = currentTimeMillis,
                    ),
                )
                message().insert(
                    Message(
                        id = 0L,
                        chatId = chatId,
                        senderId = contact.id,
                        text = "I will reply in 5 seconds",
                        mediaUri = null,
                        mediaMimeType = null,
                        timestamp = currentTimeMillis,
                    ),
                )
            }
        }
    }
}

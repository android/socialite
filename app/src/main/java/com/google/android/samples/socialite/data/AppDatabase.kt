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

package com.google.android.samples.socialite.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.samples.socialite.model.Chat
import com.google.android.samples.socialite.model.ChatAttendee
import com.google.android.samples.socialite.model.Contact
import com.google.android.samples.socialite.model.Message
import com.google.android.samples.socialite.widget.model.WidgetModel
import com.google.android.samples.socialite.widget.model.WidgetModelDao

@Database(
    entities = [
        Contact::class,
        Chat::class,
        ChatAttendee::class,
        Message::class,
        WidgetModel::class,
    ],
    views = [ChatWithLastMessage::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun widgetDao(): WidgetModelDao
}

fun RoomDatabase.wipeAndReinitializeData() = runInTransaction {
    clearAllTables()
    openHelper.writableDatabase.populateInitialData()
}

// Initialization for pre-populating the database
fun SupportSQLiteDatabase.populateInitialData() {
    // Insert self as contact
    insert(
        table = "contact",
        conflictAlgorithm = SQLiteDatabase.CONFLICT_NONE,
        values = ContentValues().apply {
            put("id", 0L)
            put("icon", "you.jpg")
            put("name", "You")
            put("replyModel", "")
        },
    )

    // Populate data for other contacts
    val contacts = Contact.CONTACTS
    val chatIds = contacts.map { it.id }

    contacts.forEachIndexed { index, contact ->
        // Insert contact
        insert(
            table = "Contact",
            conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
            values = ContentValues().apply {
                put("id", contact.id)
                put("icon", contact.icon)
                put("name", contact.name)
                put("replyModel", contact.replyModel)
            },
        )

        // Insert chat id
        insert(
            table = "Chat",
            conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
            values = ContentValues().apply {
                put("id", chatIds[index])
            },
        )

        // Insert chat attendee
        insert(
            table = "ChatAttendee",
            conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
            values = ContentValues().apply {
                put("chatId", chatIds[index])
                put("attendeeId", contact.id)
            },
        )

        val now = System.currentTimeMillis()

        // Add first message
        insert(
            table = "Message",
            conflictAlgorithm = SQLiteDatabase.CONFLICT_NONE,
            values = ContentValues().apply {
                // Use index * 2, since per contact two chats are pre populated
                put("id", (index * 2).toLong())
                put("chatId", chatIds[index])
                put("senderId", contact.id)
                put("text", "Send me a message")
                put("timestamp", now + chatIds[index])
            },
        )

        // Add second message
        insert(
            table = "Message",
            conflictAlgorithm = SQLiteDatabase.CONFLICT_NONE,
            values = ContentValues().apply {
                put("id", (index * 2).toLong() + 1L)
                put("chatId", chatIds[index])
                put("senderId", contact.id)
                put("text", "I will reply in 5 seconds")
                put("timestamp", now + chatIds[index])
            },
        )
    }
}

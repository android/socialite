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

package com.google.android.samples.socialite.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.PrimaryKey

private val replyModels = mapOf<String, Contact.(String) -> Message.Builder>(
    "cat" to { _ -> buildReply { this.text = "Meow" } },
    "dog" to { _ ->
        buildReply {
            this.text = "Check out the Top 3 Modern Android Development Announcements from I/O '23!"
            mediaUri = "content://com.google.android.samples.socialite/video/mad_io23_recap.mp4"
            mediaMimeType = "video/mp4"
        }
    },
    "parrot" to { text -> buildReply { this.text = text } },
    "sheep" to { _ ->
        buildReply {
            this.text = "Look at me!"
            mediaUri = "content://com.google.android.samples.socialite/photo/sheep_full.jpg"
            mediaMimeType = "image/jpeg"
        }
    },
)

private const val SHORTCUT_PREFIX = "contact_"

@Entity
data class Contact(
    @PrimaryKey
    val id: Long,
    val name: String,
    val icon: String,
    val replyModel: String,
) {
    companion object {
        val CONTACTS = listOf(
            Contact(1L, "Cat", "cat.jpg", "cat"),
            Contact(2L, "Dog", "dog.jpg", "dog"),
            Contact(3L, "Parrot", "parrot.jpg", "parrot"),
            Contact(4L, "Sheep", "sheep.jpg", "sheep"),
        )
    }

    val iconUri: Uri
        get() = "content://com.google.android.samples.socialite/icon/$id".toUri()

    val contentUri: Uri
        get() = "https://socialite.google.com/chat/$id".toUri()

    val shortcutId: String
        get() = "$SHORTCUT_PREFIX$id"

    fun buildReply(body: Message.Builder.() -> Unit) = Message.Builder().apply {
        senderId = this@Contact.id
        timestamp = System.currentTimeMillis()
        body()
    }

    fun reply(text: String): Message.Builder {
        val model = replyModels[replyModel] ?: { _ -> buildReply { this.text = "Hello" } }
        return model(this, text)
    }
}

fun extractChatId(shortcutId: String): Long {
    if (!shortcutId.startsWith(SHORTCUT_PREFIX)) return 0L
    return try {
        shortcutId.substring(SHORTCUT_PREFIX.length).toLong()
    } catch (e: NumberFormatException) {
        0L
    }
}

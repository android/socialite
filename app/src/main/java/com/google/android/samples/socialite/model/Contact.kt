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
import com.google.android.samples.socialite.R

/**
 * Represents a reply model with its associated text and optional media.
 *
 * @property textResId The resource ID of the string for the reply text.
 * @property mediaUri The URI of the media associated with the reply (optional).
 * @property mediaMimeType The MIME type of the media (optional).
 */
data class ReplyModel(
    val textResId: Int,
    val mediaUri: String? = null,
    val mediaMimeType: String? = null,
)

/**
 * A map of reply models keyed by a unique identifier.
 */
private val replyModels = mapOf(
    "cat" to ReplyModel(R.string.cat_reply),
    "dog" to ReplyModel(
        R.string.dog_reply,
        "content://com.google.android.samples.socialite/video/mad_io23_recap.mp4",
        "video/mp4",
    ),
    "parrot" to ReplyModel(R.string.parrot_reply),
    "sheep" to ReplyModel(
        R.string.sheep_reply,
        "content://com.google.android.samples.socialite/photo/sheep_full.jpg",
        "image/jpeg",
    ),
)

private const val SHORTCUT_PREFIX = "contact_"

/**
 * Represents a contact in the socialite application.
 *
 * @property id The unique identifier for the contact.
 * @property name The name of the contact.
 * @property icon The filename of the contact's icon.
 * @property replyModel The key to look up the reply model in [replyModels].
 */
@Entity
data class Contact(
    @PrimaryKey
    val id: Long,
    val name: String,
    val icon: String,
    val replyModel: String,
) {
    companion object {
        /**
         * A list of predefined contacts.
         */
        val CONTACTS = listOf(
            Contact(1L, "Cat", "cat.jpg", "cat"),
            Contact(2L, "Dog", "dog.jpg", "dog"),
            Contact(3L, "Parrot", "parrot.jpg", "parrot"),
            Contact(4L, "Sheep", "sheep.jpg", "sheep"),
        )
    }

    /**
     * The URI for the contact's icon.
     */
    val iconUri: Uri
        get() = "content://com.google.android.samples.socialite/icon/$id".toUri()

    /**
     * The URI for the contact's content.
     */
    val contentUri: Uri
        get() = "https://socialite.google.com/chat/$id".toUri()

    /**
     * The shortcut ID for the contact.
     */
    val shortcutId: String
        get() = "$SHORTCUT_PREFIX$id"

    /**
     * Builds a reply message.
     *
     * @param body A lambda to configure the message builder.
     * @return A [Message.Builder] for the reply.
     */
    fun buildReply(body: Message.Builder.() -> Unit) = Message.Builder().apply {
        senderId = this@Contact.id
        timestamp = System.currentTimeMillis()
        body()
    }

    /**
     * Generates a reply message based on the contact's reply model.
     *
     * @param text The text of the incoming message (used for parrot).
     * @param context The application context to access string resources.
     * @return A [Message.Builder] for the reply.
     */
    fun reply(text: String, context: android.content.Context): Message.Builder {
        val model = replyModels[replyModel] ?: ReplyModel(R.string.default_reply)
        return buildReply {
            this.text = when (replyModel) {
                "parrot" -> text
                else -> context.getString(model.textResId)
            }
            this.mediaUri = model.mediaUri
            this.mediaMimeType = model.mediaMimeType
        }
    }
}

/**
 * Extracts the chat ID from a shortcut ID.
 *
 * @param shortcutId The shortcut ID to extract from.
 * @return The chat ID, or 0L if the shortcut ID is invalid.
 */
fun extractChatId(shortcutId: String): Long {
    if (!shortcutId.startsWith(SHORTCUT_PREFIX)) return 0L
    return try {
        shortcutId.substring(SHORTCUT_PREFIX.length).toLong()
    } catch (e: NumberFormatException) {
        0L
    }
}

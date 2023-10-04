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

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("chatId"),
        Index("senderId"),
    ],
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String,
    val mediaUri: String?,
    val mediaMimeType: String?,
    val timestamp: Long,
) {

    val isIncoming: Boolean
        get() = senderId != 0L

    class Builder {
        var id: Long = 0L
        var chatId: Long? = null
        var senderId: Long? = null
        var text: String? = null
        var mediaUri: String? = null
        var mediaMimeType: String? = null
        var timestamp: Long? = null
        fun build(): Message {
            requireNotNull(chatId)
            requireNotNull(senderId)
            requireNotNull(text)
            requireNotNull(timestamp)
            return Message(
                id!!,
                chatId!!,
                senderId!!,
                text!!,
                mediaUri,
                mediaMimeType,
                timestamp!!,
            )
        }
    }
}

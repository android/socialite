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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.google.android.samples.socialite.model.ChatAttendee
import com.google.android.samples.socialite.model.ChatDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM ChatWithLastMessage ORDER BY timestamp DESC")
    fun allDetails(): Flow<List<ChatDetail>>

    @Query("SELECT * FROM ChatWithLastMessage WHERE id = :id")
    suspend fun loadDetailById(id: Long): ChatDetail?

    @Query("SELECT * FROM ChatWithLastMessage WHERE id = :id")
    fun detailById(id: Long): Flow<ChatDetail?>

    @Query("SELECT * FROM ChatWithLastMessage")
    suspend fun loadAllDetails(): List<ChatDetail>

    @Query("INSERT INTO Chat (id) VALUES (NULL)")
    suspend fun createChat(): Long

    @Insert
    suspend fun insert(chatAttendee: ChatAttendee): Long

    @Transaction
    suspend fun createDirectChat(contactId: Long): Long {
        val chatId = createChat()
        return insert(ChatAttendee(chatId = chatId, attendeeId = contactId))
    }
}

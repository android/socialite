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
import com.google.android.samples.socialite.model.Contact

@Dao
interface ContactDao {

    @Query("SELECT COUNT(id) FROM Contact")
    suspend fun count(): Int

    @Insert
    suspend fun insert(contact: Contact)

    @Query("SELECT * FROM Contact")
    suspend fun loadAll(): List<Contact>
}

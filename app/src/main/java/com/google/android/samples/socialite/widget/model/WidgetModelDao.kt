/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.samples.socialite.widget.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetModelDao {

    @Query("SELECT * FROM WidgetModel where widgetId = :widgetId")
    fun loadWidgetModel(widgetId: Int): Flow<WidgetModel?>

    @Insert
    suspend fun insert(model: WidgetModel)

    @Update
    suspend fun update(model: WidgetModel)

    @Delete
    suspend fun delete(model: WidgetModel)

    @Query("SELECT * FROM WidgetModel where widgetId NOT IN (:widgetIds)")
    fun findOrphanModels(widgetIds: List<Int>): List<WidgetModel>

    @Query("SELECT * FROM WidgetModel where contactId = :contactId")
    fun modelsForContact(contactId: Long): List<WidgetModel?>
}

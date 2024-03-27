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
package com.google.android.samples.socialite.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.samples.socialite.data.AppDatabase
import com.google.android.samples.socialite.data.ChatDao
import com.google.android.samples.socialite.data.ContactDao
import com.google.android.samples.socialite.data.DatabaseManager
import com.google.android.samples.socialite.data.MessageDao
import com.google.android.samples.socialite.data.RoomDatabaseManager
import com.google.android.samples.socialite.data.populateInitialData
import com.google.android.samples.socialite.widget.model.WidgetModelDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher

@Qualifier
annotation class AppCoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.populateInitialData()
                    }
                },
            ).build()

    @Provides
    fun providesChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    fun providesMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun providesContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun providesWidgetModelDao(database: AppDatabase): WidgetModelDao = database.widgetDao()

    @Provides
    @Singleton
    @AppCoroutineScope
    fun providesApplicationCoroutineScope(): CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
    )
}

@Module
@InstallIn(SingletonComponent::class)
interface DatabaseBindingModule {

    @Binds
    fun bindDatabaseManager(manager: RoomDatabaseManager): DatabaseManager
}

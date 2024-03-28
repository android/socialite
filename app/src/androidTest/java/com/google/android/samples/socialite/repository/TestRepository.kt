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

import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.samples.socialite.data.createTestDatabase
import com.google.android.samples.socialite.widget.model.WidgetModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun createTestRepository(): ChatRepository {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val database = createTestDatabase()
    return ChatRepository(
        chatDao = database.chatDao(),
        contactDao = database.contactDao(),
        messageDao = database.messageDao(),
        notificationHelper = NotificationHelper(context),
        widgetModelRepository = WidgetModelRepository(database.widgetDao(), CoroutineScope(SupervisorJob() + Dispatchers.Default), context),
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )
}

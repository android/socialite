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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.samples.socialite.model.Contact
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private var contactsSize = Contact.CONTACTS.size + 1

    @Before
    fun setup() {
        db = createTestDatabase()
    }

    @Test
    fun populateInitialData_AssureDataIsInitialized() = runBlocking {
        val contacts = db.contactDao().loadAll()
        val chatDetails = db.chatDao().loadAllDetails()

        // Assert
        assertThat(contacts).hasSize(contactsSize)
        assertThat(chatDetails).hasSize(4)
        for (detail in chatDetails) {
            assertThat(detail.attendees).hasSize(1)
            val messages = db.messageDao().loadAll(detail.chatWithLastMessage.id)
            assertThat(messages).hasSize(2)
        }
    }
}

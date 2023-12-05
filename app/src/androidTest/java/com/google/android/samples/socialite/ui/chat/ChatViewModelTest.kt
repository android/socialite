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

package com.google.android.samples.socialite.ui.chat

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.android.samples.socialite.awaitNotEmpty
import com.google.android.samples.socialite.awaitNotNull
import com.google.android.samples.socialite.repository.createTestRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatViewModelTest {

    @Test
    fun setChatId() = runTest {
        val repository = createTestRepository()
        val viewModel = ChatViewModel(repository)
        viewModel.setChatId(1L)
        viewModel.chat.test {
            assertThat(awaitNotNull().firstContact.name).isEqualTo("Cat")
        }
        viewModel.messages.test {
            assertThat(awaitNotEmpty()).hasSize(2)
        }
    }
}

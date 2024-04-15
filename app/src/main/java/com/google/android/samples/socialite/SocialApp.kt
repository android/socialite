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
package com.google.android.samples.socialite

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.samples.socialite.repository.ChatReplyHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SocialApp : Application() {

    @Inject
    lateinit var chatReplyHelper: ChatReplyHelper

    override fun onCreate() {
        super.onCreate()
        chatReplyHelper.start(ProcessLifecycleOwner.get().lifecycle)
    }
}

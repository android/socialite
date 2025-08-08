/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.core.content.pm.ShortcutManagerCompat
import com.google.android.samples.socialite.model.extractChatId
import com.google.android.samples.socialite.ui.navigation.Pane

sealed interface AppArgs {
    fun toPane(): Pane

    data class ShortcutParams(val shortcutId: String, val text: String, val mediaUri: String) :
        AppArgs {
        override fun toPane(): Pane {
            val chatId = extractChatId(shortcutId)
            return Pane.ChatThread(chatId, text, mediaUri)
        }

        companion object {
            fun tryFrom(intent: Intent): ShortcutParams? {
                var mediaUri: Uri? = null
                if (intent.action != Intent.ACTION_SEND) return null

                if (intent.type?.startsWith("image/")!!) {
                    mediaUri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                }

                val shortcutId = intent.getStringExtra(
                    ShortcutManagerCompat.EXTRA_SHORTCUT_ID,
                )
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)

                return if (shortcutId != null && text != null) {
                    ShortcutParams(shortcutId, text, mediaUri.toString())
                } else {
                    null
                }
            }
        }
    }
    data class LaunchParams(val chatId: Long) : AppArgs {
        override fun toPane() = Pane.ChatThread(chatId, null)

        companion object {
            const val CHAT_ID_KEY = "chatId"
            const val INTENT_KEY = "AppArgs.LaunchParams"
            const val REQUEST_CODE = 54321

            fun tryFrom(intent: Intent): LaunchParams? {
                val chatId = intent.getLongExtra(CHAT_ID_KEY, -1)
                return if (chatId != -1L) {
                    LaunchParams(chatId)
                } else {
                    null
                }
            }
        }
    }
}

internal fun Activity.tryCreateIntentFrom(params: AppArgs.LaunchParams): Intent? {
    return Intent(Intent.ACTION_VIEW).apply {
        component = componentName
        putExtra(AppArgs.LaunchParams.CHAT_ID_KEY, params.chatId)
        flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
        } else {
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }
    }
}

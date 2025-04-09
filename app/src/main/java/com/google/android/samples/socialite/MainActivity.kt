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

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.updateAll
import com.google.android.samples.socialite.ui.Main
import com.google.android.samples.socialite.widget.SociaLiteAppWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        runBlocking { SociaLiteAppWidget().updateAll(this@MainActivity) }
        setContent {
            Main(
                appArgs = extractAppArgs(intent),
            )
        }
    }

    private fun extractAppArgs(intent: Intent?): AppArgs? {
        return when {
            intent == null -> null
            intent.action == Intent.ACTION_SEND -> AppArgs.ShortcutParams.tryFrom(intent)
            else -> AppArgs.ChatParams.tryFrom(intent)
        }
    }
}

sealed interface AppArgs {
    // Parameters passed from app shortcuts
    data class ShortcutParams(
        val shortcutId: String,
        val text: String,
    ) : AppArgs {
        companion object {
            fun tryFrom(intent: Intent): ShortcutParams? {
                val shortcutId = intent.getStringExtra(
                    ShortcutManagerCompat.EXTRA_SHORTCUT_ID,
                ) ?: return null
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
                return ShortcutParams(shortcutId, text)
            }
        }
    }

    // Parameters passed from the intent to open chat in a new instance
    data class ChatParams(
        val chatId: Long,
    ) : AppArgs {
        companion object {
            const val KEY = "ChatParams"
            const val INVALID_CHAT_ID = -1L

            fun tryFrom(intent: Intent): ChatParams? {
                val chatId = intent.getLongExtra(KEY, INVALID_CHAT_ID)
                return when (chatId) {
                    INVALID_CHAT_ID -> null
                    else -> ChatParams(chatId)
                }
            }
        }
    }
}

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
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.updateAll
import com.google.android.samples.socialite.model.extractChatId
import com.google.android.samples.socialite.ui.Main
import com.google.android.samples.socialite.ui.navigation.Screen
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
        if (intent == null) return null
        return AppArgs.ShortcutParams.tryFrom(intent) ?: AppArgs.LaunchParams.tryFrom(intent)
    }

    override fun onProvideKeyboardShortcuts(
        data: MutableList<KeyboardShortcutGroup?>?,
        menu: Menu?,
        deviceId: Int,
    ) {
        data?.add(provideChatShortcuts())
    }

    private fun provideChatShortcuts(): KeyboardShortcutGroup? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            KeyboardShortcutGroup(
                resources.getString(R.string.chat),
                listOf(
                    KeyboardShortcutInfo(
                        resources.getString(R.string.send_message),
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.META_SHIFT_ON,
                    ),
                    KeyboardShortcutInfo(
                        resources.getString(R.string.page_up),
                        KeyEvent.KEYCODE_PAGE_UP,
                        0,
                    ),
                    KeyboardShortcutInfo(
                        resources.getString(R.string.page_up),
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.META_SHIFT_ON,
                    ),
                    KeyboardShortcutInfo(
                        resources.getString(R.string.page_down),
                        KeyEvent.KEYCODE_PAGE_DOWN,
                        0,
                    ),
                    KeyboardShortcutInfo(
                        resources.getString(R.string.page_down),
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.META_SHIFT_ON,
                    ),
                ),
            )
        } else {
            null
        }
    }
}

sealed interface AppArgs {
    fun toScreen(): Screen

    data class ShortcutParams(val shortcutId: String, val text: String) : AppArgs {
        override fun toScreen(): Screen {
            val chatId = extractChatId(shortcutId)
            return Screen.ChatThread(chatId, text)
        }

        companion object {
            fun tryFrom(intent: Intent): ShortcutParams? {
                if (intent.action != Intent.ACTION_SEND) return null

                val shortcutId = intent.getStringExtra(
                    ShortcutManagerCompat.EXTRA_SHORTCUT_ID,
                )
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)

                return if (shortcutId != null && text != null) {
                    ShortcutParams(shortcutId, text)
                } else {
                    null
                }
            }
        }
    }
    data class LaunchParams(val chatId: Long) : AppArgs {
        override fun toScreen() = Screen.ChatThread(chatId, null)

        companion object {
            const val CHAT_ID_KEY = "chatId"

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

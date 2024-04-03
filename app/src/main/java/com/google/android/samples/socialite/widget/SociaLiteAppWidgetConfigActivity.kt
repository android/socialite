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

package com.google.android.samples.socialite.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.updateAll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.samples.socialite.data.ChatWithLastMessage
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.android.samples.socialite.ui.ChatRow
import com.google.android.samples.socialite.ui.SocialTheme
import com.google.android.samples.socialite.ui.home.HomeAppBar
import com.google.android.samples.socialite.ui.home.HomeBackground
import com.google.android.samples.socialite.ui.home.HomeViewModel
import com.google.android.samples.socialite.widget.model.WidgetModel
import com.google.android.samples.socialite.widget.model.WidgetModelRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class SociaLiteAppWidgetConfigActivity : ComponentActivity() {
    @Inject
    lateinit var widgetModelRepository: WidgetModelRepository

    @Inject
    lateinit var chatRepository: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modifier = Modifier.fillMaxSize()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        enableEdgeToEdge()
        setContent {
            SocialTheme {
                Scaffold(
                    modifier = modifier,
                    topBar = { HomeAppBar(title = "Select a favorite contact") },
                ) { innerPadding ->

                    HomeBackground()
                    val viewModel: HomeViewModel = hiltViewModel()
                    val chats by viewModel.chats.collectAsStateWithLifecycle()
                    LazyColumn(
                        modifier = modifier,
                        contentPadding = innerPadding,
                    ) {
                        items(items = chats) { chat ->

                            ChatRow(
                                chat = chat.copy(
                                    chatWithLastMessage = ChatWithLastMessage(0),
                                    attendees = chat.attendees
                                ),
                                onClick = TODO("Replace with code from codelab"),
                            )
                        }
                    }
                }
            }
        }
    }
}

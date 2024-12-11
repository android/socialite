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

package com.google.android.samples.socialite.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.samples.socialite.R
import kotlinx.coroutines.flow.map

@Composable
fun Settings(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        item {
            Box(modifier = Modifier.padding(32.dp)) {
                Button(
                    onClick = { viewModel.clearMessages() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(text = stringResource(R.string.clear_message_history))
                }
            }

            val chatbotStatusResource = viewModel.isBotEnabledFlow.map {
                if (it) {
                    R.string.ai_chatbot_setting_enabled
                } else {
                    R.string.ai_chatbot_setting_disabled
                }
            }.collectAsState(initial = R.string.ai_chatbot_setting_enabled).value

            Box(modifier = Modifier.padding(32.dp)) {
                Button(
                    onClick = { viewModel.toggleChatbot() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(text = "${stringResource(id = R.string.ai_chatbot_setting)}: ${stringResource(chatbotStatusResource)}")
                }
            }
        }
        item {
            Box(modifier = Modifier.padding(32.dp)) {
                Text(
                    text = stringResource(
                        R.string.performance_class_level,
                        viewModel.mediaPerformanceClass,
                    ),
                )
            }
        }
    }
}

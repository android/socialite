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

package com.google.android.samples.socialite.ui.home.chatlist

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.ui.home.HomeAppBar
import com.google.android.samples.socialite.ui.home.HomeBackground
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination

@Composable
fun ChatList(
    onChatClicked: (request: OpenChatRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val chatList by viewModel.chats.collectAsStateWithLifecycle()
    ChatList(
        chats = chatList,
        onChatClicked = onChatClicked,
        modifier = modifier,
    )
}

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun ChatList(
    chats: List<ChatDetail>,
    onChatClicked: (request: OpenChatRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    @SuppressLint("InlinedApi") // Granted at install time on API <33.
    val notificationPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    val shouldUseBottomSheet = shouldUseBottomSheet()
    var selectedChatId by remember { mutableLongStateOf(NO_CHAT_IS_SELECTED) }
    val isBottomSheetOpen = selectedChatId != NO_CHAT_IS_SELECTED && shouldUseBottomSheet
    val bottomSheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeAppBar(title = stringResource(TopLevelDestination.ChatsList.label))
        },
    ) { contentPadding ->
        HomeBackground(modifier = Modifier.fillMaxSize())
        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!notificationPermissionState.status.isGranted) {
                item {
                    NotificationPermissionCard(
                        shouldShowRationale = notificationPermissionState.status.shouldShowRationale,
                        onGrantClick = {
                            notificationPermissionState.launchPermissionRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
            items(items = chats) { chat ->
                ChatRow(
                    chat = chat,
                    isOptionMenuEnabled = !shouldUseBottomSheet,
                    menuItems = {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.open_in_new_window))
                            },
                            onClick = {
                                onChatClicked(OpenChatRequest.NewInstance.from(chat))
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onLongClick = {
                                selectedChatId = chat.chatWithLastMessage.id
                            },
                            onClick = {
                                onChatClicked(OpenChatRequest.SameInstance.from(chat))
                            },
                        ),
                )
            }
        }
        AnimatedVisibility(isBottomSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { selectedChatId = NO_CHAT_IS_SELECTED },
                sheetState = bottomSheetState,
            ) {
                TextButton(
                    onClick = {
                        onChatClicked(OpenChatRequest.NewInstance(selectedChatId))
                        selectedChatId = NO_CHAT_IS_SELECTED
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.open_in_new_window))
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionCard(
    shouldShowRationale: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.permission_message),
            modifier = Modifier.padding(16.dp),
        )
        if (shouldShowRationale) {
            Text(
                text = stringResource(R.string.permission_rationale),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Button(onClick = onGrantClick) {
                Text(text = stringResource(R.string.permission_grant))
            }
        }
    }
}

const val NO_CHAT_IS_SELECTED: Long = -1

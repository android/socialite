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
import android.app.Activity
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.samples.socialite.AppArgs
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.tryCreateIntentFrom
import com.google.android.samples.socialite.ui.home.HomeAppBar
import com.google.android.samples.socialite.ui.home.HomeBackground
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination

@Composable
fun ChatList(
    onOpenChatRequest: (request: ChatOpenRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val chatList by viewModel.chats.collectAsStateWithLifecycle()
    val shouldUseBottomSheet =
        !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    ChatList(
        chats = chatList,
        onOpenChatRequest = onOpenChatRequest,
        modifier = modifier,
        shouldUseBottomSheet = shouldUseBottomSheet,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatList(
    chats: List<ChatDetail>,
    onOpenChatRequest: (request: ChatOpenRequest) -> Unit,
    modifier: Modifier = Modifier,
    shouldUseBottomSheet: Boolean = false,
) {
    @SuppressLint("InlinedApi") // Granted at install time on API <33.
    val notificationPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS,
    )

    var selectedChatId by remember { mutableLongStateOf(0L) }
    val isBottomSheetVisible = selectedChatId != 0L && shouldUseBottomSheet

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeAppBar(title = stringResource(TopLevelDestination.ChatsList.label))
        },
    ) { contentPadding ->
        HomeBackground(modifier = Modifier.fillMaxSize())
        LazyColumn(
            contentPadding = contentPadding,
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
                ChatListItem(
                    chat = chat,
                    onOpenChatRequest = onOpenChatRequest,
                    onLongClick = {
                        selectedChatId = chat.chatWithLastMessage.id
                    },
                    shouldUseMenu = !shouldUseBottomSheet,
                    modifier = Modifier.draggableWithIntentToOpenChat(chatId = chat.chatWithLastMessage.id),
                )
            }
        }
        AnimatedVisibility(isBottomSheetVisible) {
            ChatListBottomSheet(
                chatId = selectedChatId,
                onOpenChatRequest = onOpenChatRequest,
                onDismissRequest = { selectedChatId = 0L },
            )
        }
    }
}

@Composable
private fun Modifier.draggableWithIntentToOpenChat(
    chatId: Long,
    activity: Activity? = LocalActivity.current,
): Modifier {
    return if (activity != null) {
        draggableWithIntentToOpenChat(
            params = AppArgs.LaunchParams(chatId),
            activity = activity,
        )
    } else {
        this
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.draggableWithIntentToOpenChat(
    params: AppArgs.LaunchParams,
    activity: Activity,
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // TODO: This is a workaround to ensure the items are drawn properly
        val graphicsLayer = rememberGraphicsLayer()
        return drawWithCache {
            graphicsLayer.record { drawContent() }
            onDrawWithContent { drawLayer(graphicsLayer) }
        }
            .dragAndDropSource(
                drawDragDecoration = {
                    drawLayer(graphicsLayer)
                },
                block = {
                    detectDragGesturesAfterLongPress { _, _ ->
                        val intent = activity.tryCreateIntentFrom(params)

                        if (intent != null) {
                            val pendingIntent = PendingIntent.getActivity(
                                activity,
                                AppArgs.LaunchParams.REQUEST_CODE,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE,
                            )
                            val item = ClipData.Item.Builder()
                                .setIntentSender(pendingIntent.intentSender)
                                .build()

                            val clipData = ClipData(
                                AppArgs.LaunchParams.INTENT_KEY,
                                arrayOf(ClipDescription.MIMETYPE_TEXT_INTENT),
                                item,
                            )

                            val data = DragAndDropTransferData(
                                clipData = clipData,
                                flags =
                                View.DRAG_FLAG_GLOBAL or
                                    View.DRAG_FLAG_START_INTENT_SENDER_ON_UNHANDLED_DRAG,
                            )
                            startTransfer(data)
                        }
                    }
                },
            )
    } else {
        this
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListBottomSheet(
    chatId: Long,
    onOpenChatRequest: (request: ChatOpenRequest) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        TextButton(
            onClick = { onOpenChatRequest(ChatOpenRequest.NewWindow(chatId)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.open_in_new_window))
        }
    }
}

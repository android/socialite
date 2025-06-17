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

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.data.ChatWithLastMessage
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.model.Contact
import com.google.android.samples.socialite.ui.SocialTheme
import com.google.android.samples.socialite.ui.chat.component.InputBar
import com.google.android.samples.socialite.ui.chat.component.MessageBubble
import com.google.android.samples.socialite.ui.components.tryRequestFocus
import com.google.android.samples.socialite.ui.rememberIconPainter

@Composable
fun ChatScreen(
    chatId: Long,
    foreground: Boolean,
    onBackPressed: (() -> Unit)?,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onVideoClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    prefilledText: String? = null,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(chatId) {
        viewModel.setChatId(chatId)
        if (prefilledText != null) {
            viewModel.prefillInput(prefilledText)
        }
    }
    val chat by viewModel.chat.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val textFieldState = viewModel.textFieldState
    val attachedMedia by viewModel.attachedMedia.collectAsStateWithLifecycle()
    val sendEnabled by viewModel.sendEnabled.collectAsStateWithLifecycle()
    chat?.let { c ->
        ChatContent(
            chat = c,
            messages = messages,
            textFieldState = textFieldState,
            attachedMedia = attachedMedia,
            sendEnabled = sendEnabled,
            onBackPressed = onBackPressed,
            onSendClick = { viewModel.send() },
            onCameraClick = onCameraClick,
            onPhotoPickerClick = onPhotoPickerClick,
            onVideoClick = onVideoClick,
            onMediaItemAttached = viewModel::attachMedia,
            onRemoveAttachedMediaItem = viewModel::removeAttachedMedia,
            modifier = modifier
                .clip(RoundedCornerShape(5)),
        )
    }
    LifecycleEffect(
        onResume = { viewModel.setForeground(foreground) },
        onPause = { viewModel.setForeground(false) },
    )
}

@Composable
private fun LifecycleEffect(
    onResume: () -> Unit = {},
    onPause: () -> Unit = {},
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val listener = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                onPause()
            }
        }
        lifecycle.addObserver(listener)
        onDispose {
            lifecycle.removeObserver(listener)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContent(
    chat: ChatDetail,
    messages: List<ChatMessage>,
    textFieldState: TextFieldState,
    attachedMedia: MediaItem?,
    sendEnabled: Boolean,
    onBackPressed: (() -> Unit)?,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onVideoClick: (uri: String) -> Unit,
    onMediaItemAttached: (MediaItem) -> Unit,
    onRemoveAttachedMediaItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val scrollState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .focusProperties {
                onEnter = {
                    focusRequester.tryRequestFocus().onFailure { }
                }
            }
            .focusGroup(),
        topBar = {
            ChatAppBar(
                chat = chat,
                scrollBehavior = scrollBehavior,
                onBackPressed = onBackPressed,
            )
        },
    ) { innerPadding ->
        Column {
            val context = LocalContext.current
            val activity = context as Activity

            // Step 1 - uncomment line 218
            /*var isDraggedOver by remember { mutableStateOf(false) }*/

            val layoutDirection = LocalLayoutDirection.current

            MessageList(
                messages = messages,
                contentPadding = innerPadding.copy(layoutDirection, bottom = 16.dp),
                state = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                    // TODO: Implement drag and drop
                onVideoClick = onVideoClick,
            )
            InputBar(
                textFieldState = textFieldState,
                attachedMedia = attachedMedia,
                onSendClick = onSendClick,
                onCameraClick = onCameraClick,
                onPhotoPickerClick = onPhotoPickerClick,
                onMediaItemAttached = onMediaItemAttached,
                onRemoveAttachedMediaItem = onRemoveAttachedMediaItem,
                contentPadding = innerPadding.copy(layoutDirection, top = 0.dp),
                sendEnabled = sendEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                    .focusRequester(focusRequester),
            )
        }
    }
}

private fun PaddingValues.copy(
    layoutDirection: LayoutDirection,
    start: Dp? = null,
    top: Dp? = null,
    end: Dp? = null,
    bottom: Dp? = null,
) = PaddingValues(
    start = start ?: calculateStartPadding(layoutDirection),
    top = top ?: calculateTopPadding(),
    end = end ?: calculateEndPadding(layoutDirection),
    bottom = bottom ?: calculateBottomPadding(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatAppBar(
    chat: ChatDetail,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackPressed: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // This only supports DM for now.
                val contact = chat.attendees.first()
                SmallContactIcon(iconUri = contact.iconUri, size = 32.dp)
                Text(text = contact.name)
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (onBackPressed != null) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
    )
}

@Composable
private fun SmallContactIcon(iconUri: Uri, size: Dp) {
    Image(
        painter = rememberIconPainter(contentUri = iconUri),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.LightGray),
    )
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    onVideoClick: (uri: String) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        state = state,
    ) {
        items(items = messages) { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    16.dp,
                    if (message.isIncoming) Alignment.Start else Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconSize = 48.dp
                if (message.senderIconUri != null) {
                    SmallContactIcon(iconUri = message.senderIconUri, size = iconSize)
                } else {
                    Spacer(modifier = Modifier.size(iconSize))
                }
                MessageBubble(
                    message = message,
                    onVideoClick = { message.mediaUri?.let { onVideoClick(it) } },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewChatContent() {
    SocialTheme {
        ChatContent(
            chat = ChatDetail(ChatWithLastMessage(0L), listOf(Contact.CONTACTS[0])),
            messages = listOf(
                ChatMessage("Hi!", null, null, 0L, false, null),
                ChatMessage("Hello", null, null, 0L, true, null),
                ChatMessage("world", null, null, 0L, true, null),
                ChatMessage("!", null, null, 0L, true, null),
                ChatMessage("Hello, world!", null, null, 0L, true, null),
            ),
            textFieldState = TextFieldState("Hello"),
            attachedMedia = null,
            sendEnabled = true,
            onBackPressed = {},
            onSendClick = {},
            onCameraClick = {},
            onPhotoPickerClick = {},
            onVideoClick = {},
            onMediaItemAttached = {},
            onRemoveAttachedMediaItem = {},
        )
    }
}

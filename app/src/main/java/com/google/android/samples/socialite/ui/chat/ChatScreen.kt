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

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.data.ChatWithLastMessage
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.model.Contact
import com.google.android.samples.socialite.ui.SocialTheme
import com.google.android.samples.socialite.ui.chat.component.isKeyPressed
import com.google.android.samples.socialite.ui.components.PlayArrowIcon
import com.google.android.samples.socialite.ui.components.VideoPreview
import com.google.android.samples.socialite.ui.rememberIconPainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "ChatUI"

@Composable
fun ChatScreen(
    chatId: Long,
    foreground: Boolean,
    modifier: Modifier = Modifier,
    onBackPressed: (() -> Unit)?,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onVideoClick: (uri: String) -> Unit,
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
    val input by viewModel.input.collectAsStateWithLifecycle()
    val sendEnabled by viewModel.sendEnabled.collectAsStateWithLifecycle()
    chat?.let { c ->
        ChatContent(
            chat = c,
            messages = messages,
            input = input,
            sendEnabled = sendEnabled,
            onBackPressed = onBackPressed,
            onInputChanged = { viewModel.updateInput(it) },
            onSendClick = { viewModel.send() },
            onCameraClick = onCameraClick,
            onPhotoPickerClick = onPhotoPickerClick,
            onVideoClick = onVideoClick,
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
    input: String,
    sendEnabled: Boolean,
    onBackPressed: (() -> Unit)?,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onVideoClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val scrollState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollWithKeyboards(
                scrollState = scrollState,
                coroutineScope = rememberCoroutineScope(),
            )
            .focusProperties {
                onEnter = {
                    focusRequester.tryRequestFocus()
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
            val layoutDirection = LocalLayoutDirection.current
            MessageList(
                messages = messages,
                contentPadding = innerPadding.copy(layoutDirection, bottom = 16.dp),
                state = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onVideoClick = onVideoClick,
            )
            InputBar(
                input = input,
                onInputChanged = onInputChanged,
                onSendClick = onSendClick,
                onCameraClick = onCameraClick,
                onPhotoPickerClick = onPhotoPickerClick,
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

fun FocusRequester.tryRequestFocus(): Result<Unit> {
    return try {
        requestFocus()
        Result.success(Unit)
    } catch (e: IllegalArgumentException) {
        Result.failure(e)
    }
}

private fun Modifier.scrollWithKeyboards(
    scrollState: LazyListState,
    coroutineScope: CoroutineScope,
): Modifier {
    return onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp) {
            when {
                event.isKeyPressed(Key.DirectionDown, shouldShiftBePressed = true) -> {
                    scrollState.pageDown(coroutineScope)
                    true
                }

                event.isKeyPressed(Key.PageDown) -> {
                    scrollState.pageDown(coroutineScope)
                    true
                }

                event.isKeyPressed(Key.DirectionUp, shouldShiftBePressed = true) -> {
                    scrollState.pageUp(coroutineScope)
                    true
                }

                event.isKeyPressed(Key.PageUp) -> {
                    scrollState.pageUp(coroutineScope)
                    true
                }

                else -> false
            }
        } else {
            false
        }
    }
}

private fun LazyListState.pageUp(
    coroutineScope: CoroutineScope,
    fraction: Float = 0.8f,
) {
    val amount = layoutInfo.viewportSize.height * fraction
    coroutineScope.launch {
        animateScrollBy(amount)
    }
}

private fun LazyListState.pageDown(
    coroutineScope: CoroutineScope,
    fraction: Float = 0.8f,
) {
    val amount = -layoutInfo.viewportSize.height * fraction
    coroutineScope.launch {
        animateScrollBy(amount)
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
                        imageVector = Icons.Default.ArrowBack,
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

@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onVideoClick: () -> Unit = {},
) {
    val mimeType = message.mediaMimeType
    val interactionSource = remember { MutableInteractionSource() }
    val indication = remember { ripple() }

    Surface(
        modifier = modifier
            .then(
                if (mimeType != null && mimeType.contains("video")) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = indication,
                        onClick = onVideoClick,
                    )
                } else {
                    Modifier.focusable(interactionSource = interactionSource)
                },
            )
            .indication(interactionSource = interactionSource, indication = indication),
        color = if (message.isIncoming) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = message.text)
            if (message.mediaUri != null) {
                if (mimeType != null) {
                    if (mimeType.contains("image")) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(message.mediaUri)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .height(250.dp)
                                .padding(10.dp),
                        )
                    } else if (mimeType.contains("video")) {
                        VideoMessagePreview(
                            videoUri = message.mediaUri,
                            onClick = onVideoClick,
                        )
                    } else {
                        Log.e(TAG, "Unrecognized media type")
                    }
                } else {
                    Log.e(TAG, "No MIME type associated with media object")
                }
            }
        }
    }
}

@Composable
private fun VideoMessagePreview(
    videoUri: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VideoPreview(
        videoUri = videoUri,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(10.dp),
        overlay = {
            PlayArrowIcon(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center),
            )
        },
    )
}

@Composable
private fun InputBar(
    input: String,
    contentPadding: PaddingValues,
    sendEnabled: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = modifier
            .focusProperties {
                onEnter = {
                    focusRequester.tryRequestFocus()
                }
            }
            .focusGroup(),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onCameraClick) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onPhotoPickerClick) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Select Photo or video",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            TextField(
                value = input,
                onValueChange = onInputChanged,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                placeholder = { Text(stringResource(R.string.message)) },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        when {
                            event.isKeyPressed(Key.Enter, shouldShiftBePressed = true) -> {
                                onSendClick()
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    },
            )
            FilledIconButton(
                onClick = onSendClick,
                modifier = Modifier.size(56.dp),
                enabled = sendEnabled,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewInputBar() {
    SocialTheme {
        InputBar(
            input = "Hello, world",
            contentPadding = PaddingValues(0.dp),
            onInputChanged = {},
            onSendClick = {},
            onCameraClick = {},
            onPhotoPickerClick = {},
            sendEnabled = true,
        )
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
            input = "Hello",
            sendEnabled = true,
            onBackPressed = {},
            onInputChanged = {},
            onSendClick = {},
            onCameraClick = {},
            onPhotoPickerClick = {},
            onVideoClick = {},
        )
    }
}

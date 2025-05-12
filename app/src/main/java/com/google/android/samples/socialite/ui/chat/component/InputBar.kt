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

package com.google.android.samples.socialite.ui.chat.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.SocialTheme
import com.google.android.samples.socialite.ui.chat.MediaItem
import com.google.android.samples.socialite.ui.components.AttachmentRemovalIcon
import com.google.android.samples.socialite.ui.components.VideoPreview
import com.google.android.samples.socialite.ui.components.tryRequestFocus

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun InputBar(
    textFieldState: TextFieldState,
    contentPadding: PaddingValues,
    sendEnabled: Boolean,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onMediaItemAttached: (MediaItem) -> Unit,
    onRemoveAttachedMediaItem: () -> Unit,
    modifier: Modifier = Modifier,
    attachedMedia: MediaItem? = null,
) {
    val focusRequester = remember { FocusRequester() }
    rememberReceiveContentListener(onMediaItemAttached)

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
            verticalAlignment = Alignment.Bottom,
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
            ChatMessageTextField(
                state = textFieldState,
                attachedMedia = attachedMedia,
                onSendClick = onSendClick,
                onMediaItemAttached = onMediaItemAttached,
                onRemoveAttachedMedia = onRemoveAttachedMediaItem,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMessageTextField(
    state: TextFieldState,
    onSendClick: () -> Unit,
    onMediaItemAttached: (MediaItem) -> Unit,
    onRemoveAttachedMedia: () -> Unit,
    modifier: Modifier = Modifier,
    attachedMedia: MediaItem? = null,
    placeholder: @Composable () -> Unit = { Text(stringResource(R.string.message)) },
) {
    val receiveContentListener = rememberReceiveContentListener(onMediaItemAttached)

    Column(
        modifier = modifier.focusGroup(),
    ) {
        if (attachedMedia != null) {
            AttachedMediaItem(
                mediaItem = attachedMedia,
                onRemove = onRemoveAttachedMedia,
                modifier = Modifier
                    .padding(vertical = 8.dp),
            )
        }
        TextField(
            state = state,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            onKeyboardAction = KeyboardActionHandler { onSendClick() },
            placeholder = placeholder,
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .contentReceiver(receiveContentListener)
                .onPreviewKeyEvent { event ->
                    when {
                        event.isKeyPressed(Key.Enter) -> {
                            onSendClick()
                            true
                        }

                        else -> {
                            false
                        }
                    }
                },
        )
    }
}

@Composable
private fun AttachedMediaItem(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit = {},
) {
    Box(
        modifier = modifier,
    ) {
        Thumbnail(
            mediaItem = mediaItem,
            modifier = Modifier.height(height = 128.dp).padding(24.dp),
        )
        IconButton(
            onClick = onRemove,
        ) {
            AttachmentRemovalIcon()
        }
    }
}

@Composable
private fun Thumbnail(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    when {
        mediaItem.isImage() -> {
            AsyncImage(
                model = mediaItem.uri,
                contentDescription = null,
                contentScale = contentScale,
                modifier = modifier,
            )
        }

        mediaItem.isVideo() -> {
            VideoPreview(
                videoUri = mediaItem.uri,
                contentScale = contentScale,
                modifier = Modifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberReceiveContentListener(
    onMediaItemAttached: (MediaItem) -> Unit,
): ReceiveContentListener {
    return remember(onMediaItemAttached) {
        ReceiveContentListener { transferableContent ->
            when {
                transferableContent.hasMediaType(MediaType.Image) -> {
                    transferableContent.tryCreateMediaItem(MediaType.Image, onMediaItemAttached)
                }

                transferableContent.hasMediaType(MediaType.Video) -> {
                    transferableContent.tryCreateMediaItem(MediaType.Video, onMediaItemAttached)
                }

                else -> transferableContent
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun TransferableContent.tryCreateMediaItem(
    mediaType: MediaType,
    onMediaItemAttached: (MediaItem) -> Unit,
): TransferableContent? {
    val mimeTypes = clipMetadata.clipDescription.filterMimeTypes(mediaType.representation)
    return if (mimeTypes.isEmpty()) {
        this
    } else {
        consume { item ->
            onMediaItemAttached(MediaItem(item.uri.toString(), mimeTypes.first()))
            true
        }
    }
}

private val MediaType.Companion.Video get() = MediaType("video/*")

private fun MediaItem.isImage(): Boolean {
    return mimeType.startsWith("image/")
}

private fun MediaItem.isVideo(): Boolean {
    return mimeType.startsWith("video/")
}

@Preview(showBackground = true)
@Composable
private fun PreviewInputBar() {
    SocialTheme {
        InputBar(
            textFieldState = TextFieldState("Hello, world"),
            contentPadding = PaddingValues(0.dp),
            onSendClick = {},
            onCameraClick = {},
            onPhotoPickerClick = {},
            onMediaItemAttached = {},
            onRemoveAttachedMediaItem = {},
            sendEnabled = true,
        )
    }
}

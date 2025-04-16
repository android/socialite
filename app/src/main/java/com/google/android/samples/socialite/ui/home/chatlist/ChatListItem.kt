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

package com.google.android.samples.socialite.ui.home.chatlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.data.utils.toReadableString
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.ui.rememberIconPainter

@Composable
fun ChatListItem(
    chat: ChatDetail,
    onOpenChatRequest: (ChatOpenRequest) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    shouldUseTooltip: Boolean = false,
) {
    var isTooltipVisible by remember { mutableStateOf(false) }

    Box {
        ChatListItem(
            chat = chat,
            modifier = modifier,
            onClick = {
                onOpenChatRequest(ChatOpenRequest.openInSameWindow(chat))
            },
            onLongClick = {
                if (shouldUseTooltip) {
                    isTooltipVisible = true
                } else {
                    onLongClick()
                }
            },
        )
        ChatListToolTip(
            chatDetail = chat,
            onOpenChatRequest = {
                onOpenChatRequest(it)
                isTooltipVisible = false
            },
            onDismissRequest = { isTooltipVisible = false },
            expanded = isTooltipVisible,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: ChatDetail,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // This only supports DM for now.
        val contact = chat.attendees.first()
        Image(
            painter = rememberIconPainter(contentUri = contact.iconUri),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
            )
            Text(
                text = chat.chatWithLastMessage.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
            )
        }
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chat.chatWithLastMessage.timestamp.toReadableString(),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun ChatListToolTip(
    chatDetail: ChatDetail,
    onOpenChatRequest: (ChatOpenRequest) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.open_in_new_window)) },
            onClick = { onOpenChatRequest(ChatOpenRequest.openInNewWindow(chatDetail)) },
        )
    }
}

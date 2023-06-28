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

package com.example.android.social.ui.media

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.util.UnstableApi
import com.example.android.social.R

private const val TAG = "VideoEditScreen"

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditScreen(uri: String) {
    var removeAudioEnabled by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VideoMessagePreview(uri)
        Spacer(modifier = Modifier.height(20.dp))
        CheckBoxWithText(
            text = stringResource(R.string.remove_audio),
            checkedState = removeAudioEnabled,
            onCheckChange = { removeAudioEnabled = it })
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Button(modifier = Modifier.padding(10.dp), onClick = {
                // TODO Implement saving transformed video
            }) {
                Text(text = stringResource(R.string.save_edited_video))
            }

            Button(modifier = Modifier.padding(10.dp), onClick = {
                // TODO Implement saving transformed video and sending video in chat
            }) {
                Text(text = stringResource(R.string.send_video))
            }
        }
    }
}

@Composable
private fun VideoMessagePreview(videoUri: String) {
    // Render yellow box instead of frame of captured video for Preview purposes
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(266.dp)
                .background(color = Color.Yellow)
        )
        return
    }

    val mediaMetadataRetriever = MediaMetadataRetriever()
    mediaMetadataRetriever.setDataSource(LocalContext.current, Uri.parse(videoUri))

    // Return any frame that the framework considers representative of a valid frame
    val bitmap = mediaMetadataRetriever.frameAtTime

    if (bitmap != null) {
        Box(
            modifier = Modifier
                .padding(10.dp)
        ) {
            Image(
                modifier = Modifier.width(200.dp),
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
            )

            Icon(
                Icons.Filled.Movie,
                tint = Color.White,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .padding(10.dp)
            )
        }
    } else {
        Log.e(TAG, "Error rendering preview of video")
    }
}

@Composable
fun CheckBoxWithText(
    text: String,
    checkedState: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .toggleable(
                value = checkedState,
                onValueChange = { onCheckChange(!checkedState) },
                role = Role.Checkbox
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checkedState,
            onCheckedChange = null // null recommended for accessibility with screenreaders
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
@Preview
fun VideoEditScreenPreview() {
    VideoEditScreen(uri = "")
}
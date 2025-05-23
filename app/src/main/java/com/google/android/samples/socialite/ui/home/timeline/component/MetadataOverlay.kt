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

package com.google.android.samples.socialite.ui.home.timeline.component

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaType
import com.google.android.samples.socialite.ui.rememberIconPainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MetadataOverlay(modifier: Modifier, mediaItem: TimelineMediaItem) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(999f),
    ) {
        if (mediaItem.type == TimelineMediaType.VIDEO) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            val context = LocalContext.current.applicationContext

            // Running on an IO thread for loading metadata from remote urls to reduce lag time
            val duration: State<Long?> = produceState<Long?>(initialValue = null) {
                withContext(Dispatchers.IO) {
                    // Remote url
                    if (mediaItem.uri.contains("https://")) {
                        mediaMetadataRetriever.setDataSource(
                            mediaItem.uri,
                            HashMap<String, String>(),
                        )
                    } else { // Locally saved files
                        mediaMetadataRetriever.setDataSource(context, mediaItem.uri.toUri())
                    }
                    value =
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLong()
                }
            }
            duration.value?.let {
                val seconds = it / 1000L
                val minutes = seconds / 60L
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "%d:%02d".format(minutes, seconds % 60),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomStart)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            mediaItem.chatIconUri?.let {
                Image(
                    painter = rememberIconPainter(contentUri = it),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                )
            }
            Text(modifier = Modifier.padding(end = 16.dp), text = mediaItem.chatName)
        }
    }
}

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

package com.google.android.samples.socialite.ui.metadata.screens

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.ui.metadata.AttributeUtil
import com.google.android.samples.socialite.ui.metadata.MediaMetadata
import com.google.android.samples.socialite.ui.metadata.MetadataInspectorViewModel
import com.google.android.samples.socialite.ui.metadata.TrackAttributes
import com.google.android.samples.socialite.ui.metadata.components.MetadataCard
import com.google.android.samples.socialite.ui.metadata.components.ShowLoading

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@Composable
fun MetadataInspector(
    mediaPath: String,
    viewModel: MetadataInspectorViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val isLoaded = viewModel.isLoaded
        val mediaMetadata = viewModel.mediaMetadata

        Spacer(modifier = Modifier.height(50.dp))
        Text(
            stringResource(R.string.metadata_inspector_title),
            modifier = Modifier
                .padding(15.dp)
                .align(Alignment.Start),
            style = MaterialTheme.typography.titleLarge.copy(),
        )

        LaunchedEffect(mediaPath) {
            viewModel.processMedia(mediaPath)
        }

        if (isLoaded) {
            ShowMetadataCardList(mediaPath, mediaMetadata!!)
        } else {
            ShowLoading()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShowMetadataCardList(
    mediaPath: String,
    mediaMetadata: MediaMetadata,
) {
    MetadataCard(
        iconId = R.drawable.box,
        title = stringResource(R.string.container_metadata_title),
        items = AttributeUtil.getContainerAttributes(mediaPath, mediaMetadata),
    )

    ShowTrackGroup(
        R.drawable.video,
        stringResource(R.string.video_tracks_title),
        mediaMetadata.trackAttributesList.filter { MimeTypes.isVideo(it.trackMimeType) },
    )
    ShowTrackGroup(
        R.drawable.audio,
        stringResource(R.string.audio_tracks_title),
        mediaMetadata.trackAttributesList.filter { MimeTypes.isAudio(it.trackMimeType) },
    )
    ShowTrackGroup(
        R.drawable.text,
        stringResource(R.string.subtitle_tracks_title),
        mediaMetadata.trackAttributesList.filter { MimeTypes.isText(it.trackMimeType) },
    )
}

@Composable
@OptIn(UnstableApi::class)
private fun ShowTrackGroup(
    iconId: Int,
    trackGroupHeading: String,
    trackMetadataList: List<TrackAttributes>,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(all = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .padding(2.dp),
            )
            Spacer(modifier = Modifier.width(15.dp))
            Text(
                text = "$trackGroupHeading (${trackMetadataList.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                trackMetadataList.forEach { trackMetadata ->
                    MetadataCard(
                        iconId = iconId,
                        title = "Track ${trackMetadata.trackId} Metadata",
                        items = AttributeUtil.getTrackAttributes(trackMetadata),
                    )
                }
            }
        }
    }
}

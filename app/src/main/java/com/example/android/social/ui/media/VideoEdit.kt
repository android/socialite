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

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import com.example.android.social.R
import com.example.android.social.ui.camera.CameraViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "VideoEditScreen"

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditScreen(uri: String) {
    val context = LocalContext.current

    var removeAudioEnabled by rememberSaveable { mutableStateOf(false) }
    var overlayText by rememberSaveable { mutableStateOf("") }
    var redOverlayTextEnabled by rememberSaveable { mutableStateOf(false) }
    var largeOverlayTextEnabled by rememberSaveable { mutableStateOf(false) }

    val transformerListener: Transformer.Listener =
        object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                Toast.makeText(context, "Edited video saved", Toast.LENGTH_LONG).show()
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                exportException.printStackTrace()
                Toast.makeText(context, "Error applying edits on video", Toast.LENGTH_LONG).show()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, bottom = 100.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VideoMessagePreview(uri)
        Spacer(modifier = Modifier.height(20.dp))
        CheckBoxWithText(
            text = stringResource(R.string.remove_audio),
            checkedState = removeAudioEnabled,
            onCheckChange = { removeAudioEnabled = it },
            fillRow = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        TextOverlayOption(
            inputtedText = overlayText,
            inputtedTextChange = {
                // Limit character count to 20
                if (it.length <= 20) {
                    overlayText = it
                }
            },
            redTextCheckedState = redOverlayTextEnabled,
            redTextCheckedStateChange = { redOverlayTextEnabled = it },
            largeTextCheckedState = largeOverlayTextEnabled,
            largeTextCheckedStateChange = { largeOverlayTextEnabled = it }
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Button(modifier = Modifier.padding(10.dp), onClick = {
                applyVideoTransformation(
                    context = context,
                    videoUri = uri,
                    removeAudio = removeAudioEnabled,
                    textOverlayText = overlayText,
                    textOverlayRedSelected = redOverlayTextEnabled,
                    textOverlayLargeSelected = largeOverlayTextEnabled,
                    transformerListener
                )
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

@androidx.annotation.OptIn(UnstableApi::class)
fun applyVideoTransformation(
    context: Context,
    videoUri: String,
    removeAudio: Boolean,
    textOverlayText: String,
    textOverlayRedSelected: Boolean,
    textOverlayLargeSelected: Boolean,
    transformerListener: Transformer.Listener
) {
    val transformer = Transformer.Builder(context)
        .setTransformationRequest(
            // TODO In a future release of media3-transformer, we will be able to call
            //  setVideoMimeType on Transformer directly.
            TransformationRequest.Builder()
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .build()
        )
        .addListener(transformerListener)
        .build()

    // TODO Apply textOverlayText, textOverlayRedSelected, textOverlayLargeSelected options

    val editedMediaItem =
        EditedMediaItem.Builder(MediaItem.fromUri(videoUri)).setRemoveAudio(removeAudio).build()

    val editedVideoFileName = "Socialite-edited-recording-" +
            SimpleDateFormat(CameraViewModel.FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"

    // TODO: Investigate using MediaStoreOutputOptions instead of external cache file for saving
    //  edited video https://github.com/androidx/media/issues/504
    transformer.start(editedMediaItem, createNewVideoFilePath(context, editedVideoFileName))
}

fun createNewVideoFilePath(context: Context, fileName: String): String {
    val externalCacheFile = createExternalCacheFile(context, fileName)
    return externalCacheFile.absolutePath
}

/** Creates a cache file, resetting it if it already exists.  */
@Throws(IOException::class)
private fun createExternalCacheFile(context: Context, fileName: String): File {
    val file = File(context.externalCacheDir, fileName)
    check(!(file.exists() && !file.delete())) { "Could not delete the previous transformer output file" }
    check(file.createNewFile()) { "Could not create the transformer output file" }
    return file
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
fun TextOverlayOption(
    inputtedText: String,
    inputtedTextChange: (String) -> Unit,
    redTextCheckedState: Boolean,
    redTextCheckedStateChange: (Boolean) -> Unit,
    largeTextCheckedState: Boolean,
    largeTextCheckedStateChange: (Boolean) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        TextField(
            value = inputtedText,
            onValueChange = inputtedTextChange,
            placeholder = { Text(stringResource(R.string.add_text_overlay_placeholder)) },
            modifier = Modifier.width(200.dp)
        )
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            CheckBoxWithText(
                text = stringResource(R.string.red_text_option),
                checkedState = redTextCheckedState,
                onCheckChange = redTextCheckedStateChange,
                fillRow = false
            )
            CheckBoxWithText(
                text = stringResource(R.string.large_text_option),
                checkedState = largeTextCheckedState,
                onCheckChange = largeTextCheckedStateChange,
                fillRow = false
            )
        }
    }
}

@Composable
fun CheckBoxWithText(
    text: String,
    checkedState: Boolean,
    onCheckChange: (Boolean) -> Unit,
    fillRow: Boolean
) {
    Row(
        Modifier
            .then(if (fillRow) Modifier.fillMaxWidth() else Modifier)
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
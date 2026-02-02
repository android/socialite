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

package com.google.android.samples.socialite.ui.videoedit

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.ui.compose.PlayerSurface
import com.google.android.samples.socialite.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configuration options for video preview.
 */
@Immutable
private data class VideoPreviewConfig(
    val uri: String,
    val removeAudioEnabled: Boolean,
    val rgbAdjustmentEffectEnabled: Boolean,
    val periodicVignetteEffectEnabled: Boolean,
    val styleTransferEffectEnabled: Boolean,
    val overlayText: String,
    val redOverlayTextEnabled: Boolean,
    val largeOverlayTextEnabled: Boolean,
)

@ExperimentalMaterial3Api
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditScreen(
    chatId: Long,
    uri: String,
    onCloseButtonClicked: () -> Unit,
    onFinishEditing: (chatId: Long) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var frames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val viewModel: VideoEditScreenViewModel = hiltViewModel()
    viewModel.setChatId(chatId)

    val isFinishedEditing = viewModel.isFinishedEditing.collectAsStateWithLifecycle()
    if (isFinishedEditing.value) {
        onFinishEditing(chatId)
    }

    val isProcessing = viewModel.isProcessing.collectAsState()

    var removeAudioEnabled by rememberSaveable { mutableStateOf(false) }
    var rgbAdjustmentEffectEnabled by rememberSaveable { mutableStateOf(false) }
    var periodicVignetteEffectEnabled by rememberSaveable { mutableStateOf(false) }
    var styleTransferEffectEnabled by rememberSaveable { mutableStateOf(false) }
    var overlayText by rememberSaveable { mutableStateOf("") }
    var redOverlayTextEnabled by rememberSaveable { mutableStateOf(false) }
    var largeOverlayTextEnabled by rememberSaveable { mutableStateOf(false) }
    var videoTrimStart by rememberSaveable { mutableLongStateOf(0L) }
    var videoTrimEnd by rememberSaveable { mutableLongStateOf(0L) }
    val duration = rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(uri) {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            val keyCode = MediaMetadataRetriever.METADATA_KEY_DURATION
            mediaMetadataRetriever.setDataSource(context, uri.toUri())
            val time: String? = mediaMetadataRetriever.extractMetadata(keyCode)
            duration.longValue = time?.toLong() ?: 0L
            videoTrimEnd = duration.longValue
        } catch (e: Exception) {
            Log.d("VideoEditScreen", "Error extracting video metadata: ${e.message}")
        } finally {
            mediaMetadataRetriever.release()
        }

        coroutineScope.launch {
            val extractedFrames = withContext(Dispatchers.IO) {
                extractFrames(context, uri, 10)
            }
            frames = extractedFrames.second
        }
    }

    // Create a VideoPreviewConfig based on the current state of the editing options
    val previewConfig = remember(
        removeAudioEnabled,
        rgbAdjustmentEffectEnabled,
        periodicVignetteEffectEnabled,
        styleTransferEffectEnabled,
        overlayText,
        redOverlayTextEnabled,
        largeOverlayTextEnabled,
    ) {
        VideoPreviewConfig(
            uri = uri,
            removeAudioEnabled = removeAudioEnabled,
            rgbAdjustmentEffectEnabled = rgbAdjustmentEffectEnabled,
            periodicVignetteEffectEnabled = periodicVignetteEffectEnabled,
            styleTransferEffectEnabled = styleTransferEffectEnabled,
            overlayText = overlayText,
            redOverlayTextEnabled = redOverlayTextEnabled,
            largeOverlayTextEnabled = largeOverlayTextEnabled,
        )
    }

    Scaffold(
        topBar = {
            VideoEditTopAppBar(
                onSendButtonClicked = {
                    // Trigger the video transformation process in the ViewModel
                    viewModel.applyVideoTransformation(
                        context = context,
                        videoUri = uri,
                        removeAudio = removeAudioEnabled,
                        rgbAdjustmentEffectSelected = rgbAdjustmentEffectEnabled,
                        periodicVignetteEffectSelected = periodicVignetteEffectEnabled,
                        styleTransferEffectSelected = styleTransferEffectEnabled,
                        textOverlayText = overlayText,
                        textOverlayRedSelected = redOverlayTextEnabled,
                        textOverlayLargeSelected = largeOverlayTextEnabled,
                        videoTrimStart = videoTrimStart,
                        videoTrimEnd = videoTrimEnd,
                    )
                },
                onCloseButtonClicked = onCloseButtonClicked,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(Color.Black)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            // Display the video preview with applied effects.
            VideoMessagePreview(
                context,
                previewConfig,
            ) { context, previewConfig ->
                // Trigger composition preparation process in the viewModel
                viewModel.prepareComposition(
                    context = context,
                    videoUri = previewConfig.uri,
                    removeAudio = previewConfig.removeAudioEnabled,
                    rgbAdjustmentEffectSelected = previewConfig.rgbAdjustmentEffectEnabled,
                    periodicVignetteEffectSelected = previewConfig.periodicVignetteEffectEnabled,
                    styleTransferEffectSelected = previewConfig.styleTransferEffectEnabled,
                    textOverlayText = previewConfig.overlayText,
                    textOverlayRedSelected = previewConfig.redOverlayTextEnabled,
                    textOverlayLargeSelected = previewConfig.largeOverlayTextEnabled,
                    videoTrimStart = videoTrimStart,
                    videoTrimEnd = videoTrimEnd,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Tabbed Controls Area
            VideoEditTabs(
                removeAudioEnabled = removeAudioEnabled,
                onRemoveAudioToggle = { removeAudioEnabled = !removeAudioEnabled },
                rgbAdjustmentEffectEnabled = rgbAdjustmentEffectEnabled,
                onRgbAdjustmentEffectToggle = {
                    rgbAdjustmentEffectEnabled = !rgbAdjustmentEffectEnabled
                },
                periodicVignetteEffectEnabled = periodicVignetteEffectEnabled,
                onPeriodicVignetteEffectToggle = {
                    periodicVignetteEffectEnabled = !periodicVignetteEffectEnabled
                },
                styleTransferEffectEnabled = styleTransferEffectEnabled,
                onStyleTransferEffectToggle = {
                    styleTransferEffectEnabled = !styleTransferEffectEnabled
                },
                overlayText = overlayText,
                onOverlayTextChange = { if (it.length <= 20) overlayText = it },
                redOverlayTextEnabled = redOverlayTextEnabled,
                onRedOverlayTextToggle = { redOverlayTextEnabled = !redOverlayTextEnabled },
                largeOverlayTextEnabled = largeOverlayTextEnabled,
                onLargeOverlayTextToggle = { largeOverlayTextEnabled = !largeOverlayTextEnabled },
                videoTrimStart = videoTrimStart,
                videoTrimEnd = videoTrimEnd,
                onTrimChanged = { startMs, endMs ->
                    videoTrimStart = startMs
                    videoTrimEnd = endMs
                },
                frames = frames,
                durationMs = duration,
            )
        }
    }

    // Show a loading indicator while the video is being processed.
    CenteredCircularProgressIndicator(isProcessing.value)
}

@Composable
fun VideoEditTabs(
    removeAudioEnabled: Boolean,
    onRemoveAudioToggle: () -> Unit,
    rgbAdjustmentEffectEnabled: Boolean,
    onRgbAdjustmentEffectToggle: () -> Unit,
    periodicVignetteEffectEnabled: Boolean,
    onPeriodicVignetteEffectToggle: () -> Unit,
    styleTransferEffectEnabled: Boolean,
    onStyleTransferEffectToggle: () -> Unit,
    overlayText: String,
    onOverlayTextChange: (String) -> Unit,
    redOverlayTextEnabled: Boolean,
    onRedOverlayTextToggle: () -> Unit,
    largeOverlayTextEnabled: Boolean,
    onLargeOverlayTextToggle: () -> Unit,
    videoTrimStart: Long,
    videoTrimEnd: Long,
    onTrimChanged: (startMs: Long, endMs: Long) -> Unit,
    frames: List<Bitmap>,
    durationMs: MutableLongState,
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Edit", "Overlay", "Trim")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 32.dp,
            )
            .background(
                color = colorResource(R.color.dark_gray),
                shape = RoundedCornerShape(size = 28.dp),
            ),
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colorResource(R.color.dark_gray),
            contentColor = Color.White,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = colorResource(id = R.color.aqua),
                )
            },
            divider = {},
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) },
                    selectedContentColor = colorResource(id = R.color.aqua),
                    unselectedContentColor = Color.LightGray,
                )
            }
        }

        Box(modifier = Modifier.padding(15.dp)) {
            when (selectedTabIndex) {
                0 -> VideoEditControls(
                    removeAudioEnabled = removeAudioEnabled,
                    onRemoveAudioToggle = onRemoveAudioToggle,
                    rgbAdjustmentEffectEnabled = rgbAdjustmentEffectEnabled,
                    onRgbAdjustmentEffectToggle = onRgbAdjustmentEffectToggle,
                    periodicVignetteEffectEnabled = periodicVignetteEffectEnabled,
                    onPeriodicVignetteEffectToggle = onPeriodicVignetteEffectToggle,
                    styleTransferEffectEnabled = styleTransferEffectEnabled,
                    onStyleTransferEffectToggle = onStyleTransferEffectToggle,
                )

                1 -> VideoOverlayControls(
                    overlayText = overlayText,
                    onOverlayTextChange = onOverlayTextChange,
                    redOverlayTextEnabled = redOverlayTextEnabled,
                    onRedOverlayTextToggle = onRedOverlayTextToggle,
                    largeOverlayTextEnabled = largeOverlayTextEnabled,
                    onLargeOverlayTextToggle = onLargeOverlayTextToggle,
                )

                2 -> VideoTrimControls(
                    videoTrimStart = videoTrimStart,
                    videoTrimEnd = videoTrimEnd,
                    onTrimChanged = onTrimChanged,
                    frames = frames,
                    durationMs = durationMs,
                )
            }
        }
    }
}

@Composable
fun VideoEditControls(
    removeAudioEnabled: Boolean,
    onRemoveAudioToggle: () -> Unit,
    rgbAdjustmentEffectEnabled: Boolean,
    onRgbAdjustmentEffectToggle: () -> Unit,
    periodicVignetteEffectEnabled: Boolean,
    onPeriodicVignetteEffectToggle: () -> Unit,
    styleTransferEffectEnabled: Boolean,
    onStyleTransferEffectToggle: () -> Unit,
) {
    Column {
        VideoEditFilterChip(
            icon = Icons.AutoMirrored.Filled.VolumeMute,
            selected = removeAudioEnabled,
            onClick = onRemoveAudioToggle,
            label = stringResource(id = R.string.remove_audio),
        )
        VideoEditFilterChip(
            icon = Icons.Filled.ColorLens,
            selected = rgbAdjustmentEffectEnabled,
            onClick = onRgbAdjustmentEffectToggle,
            label = stringResource(id = R.string.add_rgb_adjustment_effect),
        )
        VideoEditFilterChip(
            icon = Icons.Filled.Brightness1,
            selected = periodicVignetteEffectEnabled,
            onClick = onPeriodicVignetteEffectToggle,
            label = stringResource(id = R.string.add_periodic_vignette_effect),
        )
        VideoEditFilterChip(
            icon = Icons.Filled.Style,
            selected = styleTransferEffectEnabled,
            onClick = onStyleTransferEffectToggle,
            label = stringResource(id = R.string.add_style_transfer_effect),
        )
    }
}

@Composable
fun VideoOverlayControls(
    overlayText: String,
    onOverlayTextChange: (String) -> Unit,
    redOverlayTextEnabled: Boolean,
    onRedOverlayTextToggle: () -> Unit,
    largeOverlayTextEnabled: Boolean,
    onLargeOverlayTextToggle: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextOverlayOption(
            inputtedText = overlayText,
            inputtedTextChange = onOverlayTextChange,
            redTextCheckedState = redOverlayTextEnabled,
            redTextCheckedStateChange = onRedOverlayTextToggle,
            largeTextCheckedState = largeOverlayTextEnabled,
            largeTextCheckedStateChange = onLargeOverlayTextToggle,
        )
    }
}

@Composable
fun VideoTrimControls(
    videoTrimStart: Long,
    videoTrimEnd: Long,
    onTrimChanged: (startMs: Long, endMs: Long) -> Unit,
    frames: List<Bitmap>,
    durationMs: MutableLongState,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val rangeStart = "%.2f".format(videoTrimStart / 1000.0)
        val rangeEnd = "%.2f".format(videoTrimEnd / 1000.0)
        Text(
            text = "Video segment: $rangeStart s .. $rangeEnd s",
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (frames.isNotEmpty() && durationMs.longValue > 0) {
            FrameRangeSlider(
                frames = frames,
                state = TrimState(
                    durationMs = durationMs.longValue,
                    startMs = videoTrimStart,
                    endMs = videoTrimEnd,
                ),
                onTrimChanged = onTrimChanged,
            )
        } else if (durationMs.longValue == 0L && videoTrimStart == 0L && videoTrimEnd == 0L) {
            // Still loading duration and frames
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
            )
        } else if (frames.isEmpty() && durationMs.longValue > 0) {
            // Duration loaded but frames not yet (or failed)
            Text("Loading frames...", color = Color.White)
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
            )
        } else {
            Text("Video too short or unable to load trim controls.", color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoEditTopAppBar(
    onSendButtonClicked: () -> Unit,
    onCloseButtonClicked: () -> Unit,
) {
    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            navigationIconContentColor = Color.White,
        ),
        navigationIcon = {
            IconButton(onClick = onCloseButtonClicked) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.aqua),
                    contentColor = Color.Black,
                ),
                onClick = onSendButtonClicked,
                modifier = Modifier.padding(8.dp),
            ) {
                Text(text = stringResource(id = R.string.send))
            }
        },
    )
}

/**
 * Preview of the video with applied effects. This composable uses Media3's [CompositionPlayer] to
 * render the video with the specified edits.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoMessagePreview(
    context: Context,
    previewConfig: VideoPreviewConfig,
    prepareComposition: (Context, VideoPreviewConfig) -> Composition,
) {
    // Render yellow box instead of frame of captured video for Preview purposes
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(266.dp)
                .background(color = Color.Yellow),
        )
        return
    }

    // CompositionPlayer is still under active development
    var compositionPlayer by remember { mutableStateOf<CompositionPlayer?>(null) }

    PlayerSurface(
        compositionPlayer,
        modifier = Modifier
            .width(250.dp)
            .height(450.dp),
        )

    LaunchedEffect(previewConfig) {
        // Release the previous player instance if it exists
        compositionPlayer?.release()
        // Create a new CompositionPlayer
        compositionPlayer = CompositionPlayer.Builder(context).build()

        val composition = prepareComposition(context, previewConfig)

        // Set the composition to the player and start playback
        compositionPlayer?.setComposition(composition)
        compositionPlayer?.prepare()
        compositionPlayer?.play()
    }
}

@Composable
fun TextOverlayOption(
    inputtedText: String,
    inputtedTextChange: (String) -> Unit,
    redTextCheckedState: Boolean,
    redTextCheckedStateChange: () -> Unit,
    largeTextCheckedState: Boolean,
    largeTextCheckedStateChange: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        TextField(
            value = inputtedText,
            onValueChange = inputtedTextChange,
            placeholder = { Text(stringResource(R.string.add_text_overlay_placeholder)) },
            modifier = Modifier.width(200.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.DarkGray,
                unfocusedPlaceholderColor = Color.LightGray,
            ),
        )
        Spacer(modifier = Modifier.padding(5.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            VideoEditFilterChip(
                icon = Icons.Filled.DonutLarge,
                selected = redTextCheckedState,
                onClick = redTextCheckedStateChange,
                label = stringResource(id = R.string.red_text_option),
                iconColor = Color.Red,
            )
            Spacer(modifier = Modifier.padding(10.dp))

            VideoEditFilterChip(
                icon = Icons.Filled.FormatSize,
                selected = largeTextCheckedState,
                onClick = largeTextCheckedStateChange,
                label = stringResource(id = R.string.large_text_option),
            )
        }
    }
}

@Composable
private fun VideoEditFilterChip(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    iconColor: Color = Color.White,
    selectedIconColor: Color = Color.Black,
) {
    FilterChip(
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = Color.White,
            selectedContainerColor = colorResource(id = R.color.light_purple),
            selectedLabelColor = Color.Black,
            iconColor = iconColor,
            selectedLeadingIconColor = selectedIconColor,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Circular progress indicator displayed in the center of the screen when the video is being
 * processed. This provides visual feedback to the user that an operation is in progress.
 */
@Composable
private fun CenteredCircularProgressIndicator(isProcessing: Boolean) {
    if (isProcessing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun VideoEditScreenPreview() {
    VideoEditScreen(
        chatId = 0L,
        uri = "",
        onCloseButtonClicked = {},
        onFinishEditing = {},
    )
}

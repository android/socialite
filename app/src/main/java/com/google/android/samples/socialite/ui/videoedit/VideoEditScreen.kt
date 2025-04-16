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
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VolumeMute
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.samples.socialite.R

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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditScreen(
    chatId: Long,
    uri: String,
    onCloseButtonClicked: () -> Unit,
    navController: NavController,
) {
    val context = LocalContext.current

    val viewModel: VideoEditScreenViewModel = hiltViewModel()
    viewModel.setChatId(chatId)

    val isFinishedEditing = viewModel.isFinishedEditing.collectAsStateWithLifecycle()
    if (isFinishedEditing.value) {
        navController.popBackStack("chat/$chatId", false)
    }

    val isProcessing = viewModel.isProcessing.collectAsState()

    var removeAudioEnabled by rememberSaveable { mutableStateOf(false) }
    var rgbAdjustmentEffectEnabled by rememberSaveable { mutableStateOf(false) }
    var periodicVignetteEffectEnabled by rememberSaveable { mutableStateOf(false) }
    var styleTransferEffectEnabled by rememberSaveable { mutableStateOf(false) }
    var overlayText by rememberSaveable { mutableStateOf("") }
    var redOverlayTextEnabled by rememberSaveable { mutableStateOf(false) }
    var largeOverlayTextEnabled by rememberSaveable { mutableStateOf(false) }

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
            VideoMessagePreview(
                context,
                previewConfig,
            ) { context, previewConfig ->
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
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .padding(15.dp)
                    .background(
                        color = colorResource(R.color.dark_gray),
                        shape = RoundedCornerShape(size = 28.dp),
                    )
                    .padding(15.dp),
            ) {
                VideoEditFilterChip(
                    icon = Icons.Filled.VolumeMute,
                    selected = removeAudioEnabled,
                    onClick = { removeAudioEnabled = !removeAudioEnabled },
                    label = stringResource(id = R.string.remove_audio),
                )
                VideoEditFilterChip(
                    icon = Icons.Filled.ColorLens,
                    selected = rgbAdjustmentEffectEnabled,
                    onClick = { rgbAdjustmentEffectEnabled = !rgbAdjustmentEffectEnabled },
                    label = stringResource(id = R.string.add_rgb_adjustment_effect),
                )
                VideoEditFilterChip(
                    icon = Icons.Filled.Brightness1,
                    selected = periodicVignetteEffectEnabled,
                    onClick = { periodicVignetteEffectEnabled = !periodicVignetteEffectEnabled },
                    label = stringResource(id = R.string.add_periodic_vignette_effect),
                )
                VideoEditFilterChip(
                    icon = Icons.Filled.Style,
                    selected = styleTransferEffectEnabled,
                    onClick = { styleTransferEffectEnabled = !styleTransferEffectEnabled },
                    label = stringResource(id = R.string.add_style_transfer_effect),
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
                    redTextCheckedStateChange = {
                        redOverlayTextEnabled = !redOverlayTextEnabled
                    },
                    largeTextCheckedState = largeOverlayTextEnabled,
                    largeTextCheckedStateChange = {
                        largeOverlayTextEnabled = !largeOverlayTextEnabled
                    },
                )
            }
        }
    }

    CenteredCircularProgressIndicator(isProcessing.value)
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

    val playerView = remember(context) { PlayerView(context) }
    var compositionPlayer by remember { mutableStateOf<CompositionPlayer?>(null) }

    AndroidView(
        factory = {
            playerView.apply {
                player = compositionPlayer
                controllerAutoShow = false
            }
        },
        modifier = Modifier
            .width(250.dp)
            .height(450.dp),
    )

    LaunchedEffect(previewConfig) {
        compositionPlayer?.release()
        compositionPlayer = CompositionPlayer.Builder(context).build()

        playerView.player = compositionPlayer

        val composition = prepareComposition(context, previewConfig)

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

@OptIn(ExperimentalMaterial3Api::class)
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

@Composable
@Preview
fun VideoEditScreenPreview() {
    VideoEditScreen(
        chatId = 0L,
        uri = "",
        onCloseButtonClicked = {},
        navController = rememberNavController(),
    )
}

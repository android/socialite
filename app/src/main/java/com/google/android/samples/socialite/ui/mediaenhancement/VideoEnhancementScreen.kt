package com.google.android.samples.socialite.ui.mediaenhancement

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.samples.socialite.R

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoEnhancementScreen(
    messageId: Long,
    uri: String,
    onCloseButtonClicked: () -> Unit,
    onFinishEditing: () -> Unit,
    videoEnhancementViewModel: VideoEnhancementViewModel = hiltViewModel(),
) {
    val uiState by videoEnhancementViewModel.uiState.collectAsStateWithLifecycle()

    val handleClose = {
        if (uiState.isEnhancing) {
            videoEnhancementViewModel.cancelEnhancement()
        }
        onCloseButtonClicked()
    }

    BackHandler(enabled = uiState.isEnhancing) {
        handleClose()
    }

    LaunchedEffect(uri) {
        videoEnhancementViewModel.setVideo(uri)
    }

    val isEnhanced = uiState.enhancedVideoUri != null
    val canEnhance = uiState.isModuleReady &&
        !uiState.isEnhancing &&
        !uiState.isSaving &&
        !uiState.isSourceTooLarge &&
        uiState.sourceUri != null

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    R.string.video_enhancement_module_status,
                    stringResource(uiState.moduleStatus),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // FlowRow wraps all effect chips onto the visible width without horizontal scrolling.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                VideoEnhancementEffect.entries.forEach { effect ->
                    EffectChip(
                        effect = effect,
                        isSelected = effect in uiState.selectedEffects,
                        isAvailable = effect in uiState.availableEffects,
                        isSourceTooLarge = uiState.isSourceTooLarge,
                        onClick = { videoEnhancementViewModel.onEffectToggled(effect) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { videoEnhancementViewModel.enhanceVideo() },
                enabled = canEnhance,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val buttonText = when {
                    uiState.isModuleInstalling -> R.string.video_enhancement_action_installing
                    uiState.isEnhancing -> R.string.video_enhancement_action_enhancing
                    else -> R.string.video_enhancement_action_enhance
                }
                Text(stringResource(buttonText))
            }

            if (uiState.isModuleInstalling) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { uiState.moduleInstallProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(
                            R.string.video_enhancement_module_download,
                            uiState.moduleInstallProgress,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (uiState.isEnhancing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LinearProgressIndicator(
                            progress = { uiState.transcodeProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(
                                R.string.video_enhancement_transcode_progress,
                                uiState.transcodeProgress,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { videoEnhancementViewModel.cancelEnhancement() }) {
                        Text(stringResource(R.string.video_enhancement_action_cancel_run))
                    }
                }
            }

            if (uiState.isSourceTooLarge) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.video_enhancement_source_too_large),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            if (uiState.moduleInstallError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.moduleInstallError
                        ?: stringResource(R.string.unknown_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (uiState.enhancementError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.enhancementError
                        ?: stringResource(R.string.unknown_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // A single player: once the enhanced video is ready it replaces the source in place.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                VideoEnhancementPlayer(
                    uri = uiState.enhancedVideoUri ?: uri,
                    modifier = Modifier.fillMaxSize(),
                )
                PreviewTag(
                    label = if (isEnhanced) {
                        R.string.video_enhancement_preview_enhanced
                    } else {
                        R.string.video_enhancement_preview_original
                    },
                    width = uiState.sourceWidth,
                    height = uiState.sourceHeight,
                    isUpscaled = isEnhanced &&
                        VideoEnhancementEffect.UPSCALE in uiState.selectedEffects,
                    latency = uiState.enhancementLatencyMs.takeIf { isEnhanced },
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedButton(
                    onClick = handleClose,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(stringResource(R.string.video_enhancement_action_close))
                }
                Button(
                    onClick = {
                        videoEnhancementViewModel.saveEnhancedVideo(
                            messageId,
                            uri,
                            onFinishEditing,
                        )
                    },
                    enabled = isEnhanced && !uiState.isEnhancing && !uiState.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.video_enhancement_action_confirm))
                }
            }
        }
    }
}

@Composable
private fun EffectChip(
    effect: VideoEnhancementEffect,
    isSelected: Boolean,
    isAvailable: Boolean,
    isSourceTooLarge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilterChip(
            selected = isSelected && isAvailable,
            onClick = onClick,
            enabled = isAvailable,
            label = { Text(stringResource(effect.labelResId())) },
        )
        // Only show per-effect unavailability when the source itself is within the 2K limit.
        if (!isAvailable && !isSourceTooLarge) {
            Text(
                text = stringResource(R.string.video_enhancement_effect_unavailable_output),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoEnhancementPlayer(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    // Swapping the media item in place keeps a single player for both the source and the result.
    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { playerContext ->
            PlayerView(playerContext).apply {
                player = exoPlayer
                setControllerAutoShow(false)
            }
        },
        onRelease = { playerView -> playerView.player = null },
        modifier = modifier,
    )
}

@Composable
private fun PreviewTag(
    @StringRes label: Int,
    width: Int,
    height: Int,
    isUpscaled: Boolean,
    latency: Long?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = stringResource(label),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        if (width > 0 && height > 0) {
            val scale = if (isUpscaled) UPSCALE_FACTOR else 1
            Text(
                text = stringResource(
                    R.string.video_enhancement_resolution,
                    width * scale,
                    height * scale,
                ),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (latency != null) {
            Text(
                text = stringResource(R.string.video_enhancement_latency, latency),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@StringRes
private fun VideoEnhancementEffect.labelResId(): Int = when (this) {
    VideoEnhancementEffect.TONEMAP -> R.string.video_enhancement_effect_tonemap
    VideoEnhancementEffect.DEBLUR_AND_DENOISE -> R.string.video_enhancement_effect_deblur
    VideoEnhancementEffect.UPSCALE -> R.string.video_enhancement_effect_upscale
}

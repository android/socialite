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

package com.google.android.samples.socialite.ui.camera

import android.Manifest
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.domain.AspectRatioType
import com.google.android.samples.socialite.domain.CameraSettings
import com.google.android.samples.socialite.domain.FoldingState
import com.google.android.samples.socialite.ui.DevicePreview
import com.google.android.samples.socialite.ui.LocalFoldingState
import com.google.android.samples.socialite.ui.SocialTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun Camera(
    onBackPressed: () -> Unit,
    onMediaCaptured: (Media?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val foldingState = LocalFoldingState.current

    viewModel.setCameraOrientation(
        foldingState,
        LocalConfiguration.current.orientation == ORIENTATION_PORTRAIT,
    )

    val cameraSettings by viewModel.cameraSettings.collectAsStateWithLifecycle()

    LifecycleResumeEffect(key1 = Unit) {
        val job = viewModel.mediaCapture
            .onEach { onMediaCaptured(it) }
            .launchIn(viewModel.viewModelScope)

        onPauseOrDispose {
            job.cancel()
        }
    }

    CameraPermissionHandle(
        onBackPressed = onBackPressed,
        modifier = modifier,
    ) {
        CameraContent(
            cameraSettings = cameraSettings,
            onCameraEvent = viewModel::setUserEvent,
            onBackPressed = onBackPressed,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraPermissionHandle(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val cameraAndRecordAudioPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ),
    )

    if (cameraAndRecordAudioPermissionState.allPermissionsGranted) {
        content()
    } else {
        CameraAndRecordAudioPermission(
            permissionsState = cameraAndRecordAudioPermissionState,
            onBackClicked = onBackPressed,
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraContent(
    cameraSettings: CameraSettings,
    onCameraEvent: (CameraEvent) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        when (cameraSettings.foldingState) {
            FoldingState.HALF_OPEN -> {
                TwoPaneCameraLayout(
                    cameraSettings = cameraSettings,
                    onCameraEvent = onCameraEvent,
                )
            }

            FoldingState.FLAT, FoldingState.CLOSE -> {
                FlatCameraLayout(
                    cameraSettings = cameraSettings,
                    onCameraEvent = onCameraEvent,
                )
            }
        }

        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.TopStart)
                .padding(start = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun TwoPaneCameraLayout(
    cameraSettings: CameraSettings,
    onCameraEvent: (CameraEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        ORIENTATION_LANDSCAPE -> {
            TwoPaneLandScapeCameraLayout(
                cameraSettings = cameraSettings,
                onCameraEvent = onCameraEvent,
            )
        }

        else -> {
            TwoPanePortraitCameraLayout(
                cameraSettings = cameraSettings,
                onCameraEvent = onCameraEvent,
            )
        }
    }
}

@Composable
private fun TwoPaneLandScapeCameraLayout(
    cameraSettings: CameraSettings,
    onCameraEvent: (CameraEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 100.dp),
            ) {
                CameraControls(
                    captureMode = cameraSettings.captureMode,
                    onCameraEvent = onCameraEvent,
                )
            }

            ShutterButton(
                captureMode = cameraSettings.captureMode,
                onCameraEvent = onCameraEvent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 50.dp),
            )

            CameraSwitcher(
                captureMode = cameraSettings.captureMode,
                onCameraSelector = { onCameraEvent(CameraEvent.ToggleCameraFacing) },
                modifier = Modifier
                    .padding(start = 200.dp, top = 50.dp)
                    .align(Alignment.Center),
            )
        }

        ViewFinder(
            onSurfaceProviderReady = { onCameraEvent(CameraEvent.SurfaceProviderReady(it)) },
            onZoomChange = { onCameraEvent(CameraEvent.ZoomChange(it)) },
            modifier = Modifier
                .weight(1f)
                .aspectRatio(cameraSettings.aspectRatioType.ratio.toFloat()),
        )
    }
}

@Composable
private fun TwoPanePortraitCameraLayout(
    cameraSettings: CameraSettings,
    onCameraEvent: (CameraEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        ViewFinder(
            onSurfaceProviderReady = { onCameraEvent(CameraEvent.SurfaceProviderReady(it)) },
            onZoomChange = { onCameraEvent(CameraEvent.ZoomChange(it)) },
            modifier = Modifier
                .weight(1f)
                .aspectRatio(cameraSettings.aspectRatioType.ratio.toFloat())
                .align(Alignment.CenterHorizontally),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row {
                    CameraControls(
                        captureMode = cameraSettings.captureMode,
                        onCameraEvent = onCameraEvent,
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Box {
                    ShutterButton(
                        captureMode = cameraSettings.captureMode,
                        onCameraEvent = onCameraEvent,
                        modifier = modifier.align(Alignment.Center),
                    )

                    CameraSwitcher(
                        captureMode = cameraSettings.captureMode,
                        onCameraSelector = { onCameraEvent(CameraEvent.ToggleCameraFacing) },
                        modifier = modifier
                            .padding(start = 200.dp)
                            .align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FlatCameraLayout(
    cameraSettings: CameraSettings,
    onCameraEvent: (CameraEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ViewFinder(
        modifier = Modifier
            .aspectRatio(cameraSettings.aspectRatioType.ratio.toFloat())
            .align(Alignment.Center),
        onSurfaceProviderReady = { onCameraEvent(CameraEvent.SurfaceProviderReady(it)) },
        onZoomChange = { onCameraEvent(CameraEvent.ZoomChange(it)) },
    )

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 100.dp),
    ) {
        CameraControls(
            captureMode = cameraSettings.captureMode,
            onCameraEvent = onCameraEvent,
        )
    }

    ShutterButton(
        captureMode = cameraSettings.captureMode,
        onCameraEvent = onCameraEvent,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.5.dp),
    )

    CameraSwitcher(
        captureMode = cameraSettings.captureMode,
        onCameraSelector = { onCameraEvent(CameraEvent.ToggleCameraFacing) },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 200.dp, bottom = 30.dp),
    )
}

@Composable
private fun CameraControls(
    captureMode: CaptureMode,
    onCameraEvent: (CameraEvent) -> Unit,
) {
    val activeButtonColor =
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    val inactiveButtonColor =
        ButtonDefaults.buttonColors(containerColor = Color.LightGray)
    if (captureMode != CaptureMode.VIDEO_RECORDING) {
        Button(
            modifier = Modifier.padding(5.dp),
            onClick = { onCameraEvent(CameraEvent.CaptureModeChange(CaptureMode.PHOTO)) },
            colors = if (captureMode == CaptureMode.PHOTO) activeButtonColor else inactiveButtonColor,
        ) {
            Text(stringResource(id = R.string.photo))
        }
        Button(
            modifier = Modifier.padding(5.dp),
            onClick = { onCameraEvent(CameraEvent.CaptureModeChange(CaptureMode.VIDEO_READY)) },
            colors = if (captureMode != CaptureMode.PHOTO) activeButtonColor else inactiveButtonColor,
        ) {
            Text(stringResource(id = R.string.video))
        }
    }
}

@Composable
private fun ShutterButton(
    captureMode: CaptureMode,
    onCameraEvent: (CameraEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = 25.dp),
    ) {
        when (captureMode) {
            CaptureMode.PHOTO -> {
                Button(
                    onClick = { onCameraEvent(CameraEvent.CapturePhoto) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.size(75.dp),
                    content = { Unit },
                )
            }

            CaptureMode.VIDEO_READY -> {
                Button(
                    onClick = {
                        onCameraEvent(CameraEvent.CaptureModeChange(CaptureMode.VIDEO_RECORDING))
                        onCameraEvent(CameraEvent.StartVideoRecording)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.size(75.dp),
                    content = { Unit },
                )
            }

            CaptureMode.VIDEO_RECORDING -> {
                Button(
                    onClick = {
                        onCameraEvent(CameraEvent.CaptureModeChange(CaptureMode.VIDEO_READY))
                        onCameraEvent(CameraEvent.StopVideoRecording)
                    },
                    shape = RoundedCornerShape(10),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.size(50.dp),
                    content = { Unit },
                )
            }
        }
    }
}

@Composable
private fun CameraSwitcher(
    captureMode: CaptureMode,
    onCameraSelector: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (captureMode != CaptureMode.VIDEO_RECORDING) {
        IconButton(
            onClick = onCameraSelector,
            modifier = modifier,
        ) {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(75.dp),
            )
        }
    }
}

@Preview(
    showSystemUi = true,
    device = "spec:width=673dp,height=841dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape",
)
@Composable
private fun HalfLandScapeCameraLayoutPreView() {
    SocialTheme {
        CameraContent(
            cameraSettings = CameraSettings(
                foldingState = FoldingState.HALF_OPEN,
                aspectRatioType = AspectRatioType.RATIO_16_9,
            ),
            onCameraEvent = {},
            onBackPressed = {},
        )
    }
}

@Preview(
    showSystemUi = true,
    device = "spec:width=673dp,height=841dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait",
)
@Composable
private fun HalfPortraitCameraLayoutPreView() {
    SocialTheme {
        CameraContent(
            cameraSettings = CameraSettings(
                foldingState = FoldingState.HALF_OPEN,
                aspectRatioType = AspectRatioType.RATIO_9_16,
            ),
            onCameraEvent = {},
            onBackPressed = {},
        )
    }
}

@DevicePreview
@Composable
private fun FlatCameraLayoutPreView() {
    SocialTheme {
        CameraContent(
            cameraSettings = CameraSettings(
                foldingState = FoldingState.FLAT,
                aspectRatioType = AspectRatioType.RATIO_4_3,
            ),
            onCameraEvent = {},
            onBackPressed = {},
        )
    }
}

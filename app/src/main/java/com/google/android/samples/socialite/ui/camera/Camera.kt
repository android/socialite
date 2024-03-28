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
import android.annotation.SuppressLint
import android.view.Display
import android.view.Surface
import androidx.camera.core.Preview.SurfaceProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.domain.CameraSettings
import com.google.android.samples.socialite.ui.DevicePreview
import com.google.android.samples.socialite.ui.LocalFoldingState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Camera(
    onBackPressed: () -> Unit,
    onMediaCaptured: (Media?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val foldState = LocalFoldingState.current
    val rotationState by viewModel.rotationState.collectAsStateWithLifecycle()
    val cameraSettings by viewModel.cameraSettings.collectAsStateWithLifecycle()

    val cameraAndRecordAudioPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ),
    )

    @SuppressLint("MissingPermission")
    if (cameraAndRecordAudioPermissionState.allPermissionsGranted) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = Color.Black)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            when (foldState) {
                FoldingState.HALF_OPEN -> {
                    TwoPaneCameraLayout(
                        cameraSettings = cameraSettings,
                        rotationState = rotationState,
                        onCameraSelector = viewModel::toggleCameraFacing,
                        onCaptureMode = viewModel::setCaptureMode,
                        onPhotoCapture = {
                            viewModel.capturePhoto(onMediaCaptured)
                        },
                        onPreviewSurfaceProviderReady = viewModel::setSurfaceProvider,
                        onVideoRecordingStart = {
                            viewModel.setCaptureMode(CaptureMode.VIDEO_RECORDING)
                            viewModel.startVideoCapture(onMediaCaptured)
                        },
                        onVideoRecordingFinish = {
                            viewModel.setCaptureMode(CaptureMode.VIDEO_READY)
                            viewModel.stopVideoRecording()
                        },
                        onTapToFocus = viewModel::tapToFocus,
                        onZoomChange = viewModel::setZoomScale,
                    )
                }

                FoldingState.FLAT, FoldingState.CLOSE -> {
                    FlatCameraLayout(
                        cameraSettings = cameraSettings,
                        onCameraSelector = viewModel::toggleCameraFacing,
                        onCaptureMode = viewModel::setCaptureMode,
                        onPhotoCapture = {
                            viewModel.capturePhoto(onMediaCaptured)
                        },
                        onPreviewSurfaceProviderReady = viewModel::setSurfaceProvider,
                        onVideoRecordingStart = {
                            viewModel.setCaptureMode(CaptureMode.VIDEO_RECORDING)
                            viewModel.startVideoCapture(onMediaCaptured)
                        },
                        onVideoRecordingFinish = {
                            viewModel.setCaptureMode(CaptureMode.VIDEO_READY)
                            viewModel.stopVideoRecording()
                        },
                        onTapToFocus = viewModel::tapToFocus,
                        onZoomChange = viewModel::setZoomScale,
                    )
                }
            }

            IconButton(
                onClick = onBackPressed,
                modifier = modifier
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
    } else {
        CameraAndRecordAudioPermission(
            permissionsState = cameraAndRecordAudioPermissionState,
            onBackClicked = onBackPressed,
        )
    }
}

@Composable
private fun TwoPaneCameraLayout(
    cameraSettings: CameraSettings,
    rotationState: Int,
    onCameraSelector: () -> Unit,
    onCaptureMode: (CaptureMode) -> Unit,
    onPhotoCapture: () -> Unit,
    onPreviewSurfaceProviderReady: (SurfaceProvider) -> Unit,
    onVideoRecordingStart: () -> Unit,
    onVideoRecordingFinish: () -> Unit,
    onTapToFocus: (Display, Int, Int, Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (rotationState) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> {
            TwoPaneVerticalCameraLayout(
                cameraSettings = cameraSettings,
                onCameraSelector = onCameraSelector,
                onCaptureMode = onCaptureMode,
                onPhotoCapture = onPhotoCapture,
                onPreviewSurfaceProviderReady = onPreviewSurfaceProviderReady,
                onVideoRecordingStart = onVideoRecordingStart,
                onVideoRecordingFinish = onVideoRecordingFinish,
                onTapToFocus = onTapToFocus,
                onZoomChange = onZoomChange,
                modifier = modifier,
            )
        }

        else -> {
            TwoPaneHorizontalCameraLayout(
                cameraSettings = cameraSettings,
                onCameraSelector = onCameraSelector,
                onCaptureMode = onCaptureMode,
                onPhotoCapture = onPhotoCapture,
                onPreviewSurfaceProviderReady = onPreviewSurfaceProviderReady,
                onVideoRecordingStart = onVideoRecordingStart,
                onVideoRecordingFinish = onVideoRecordingFinish,
                onTapToFocus = onTapToFocus,
                onZoomChange = onZoomChange,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TwoPaneVerticalCameraLayout(
    cameraSettings: CameraSettings,
    onCameraSelector: () -> Unit,
    onCaptureMode: (CaptureMode) -> Unit,
    onPhotoCapture: () -> Unit,
    onPreviewSurfaceProviderReady: (SurfaceProvider) -> Unit,
    onVideoRecordingStart: () -> Unit,
    onVideoRecordingFinish: () -> Unit,
    onTapToFocus: (Display, Int, Int, Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
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
                    onPhotoButtonClick = { onCaptureMode(CaptureMode.PHOTO) },
                    onVideoButtonClick = { onCaptureMode(CaptureMode.VIDEO_READY) },
                )
            }

            ShutterButton(
                captureMode = cameraSettings.captureMode,
                onPhotoCapture = onPhotoCapture,
                onVideoRecordingStart = onVideoRecordingStart,
                onVideoRecordingFinish = onVideoRecordingFinish,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 50.dp),
            )

            CameraSwitcher(
                captureMode = cameraSettings.captureMode,
                onCameraSelector = onCameraSelector,
                modifier = Modifier
                    .padding(start = 200.dp, top = 50.dp)
                    .align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            ViewFinder(
                onSurfaceProviderReady = onPreviewSurfaceProviderReady,
                onTapToFocus = onTapToFocus,
                onZoomChange = onZoomChange,
            )
        }
    }
}

@Composable
private fun TwoPaneHorizontalCameraLayout(
    cameraSettings: CameraSettings,
    onCameraSelector: () -> Unit,
    onCaptureMode: (CaptureMode) -> Unit,
    onPhotoCapture: () -> Unit,
    onPreviewSurfaceProviderReady: (SurfaceProvider) -> Unit,
    onVideoRecordingStart: () -> Unit,
    onVideoRecordingFinish: () -> Unit,
    onTapToFocus: (Display, Int, Int, Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            ViewFinder(
                onSurfaceProviderReady = onPreviewSurfaceProviderReady,
                onTapToFocus = onTapToFocus,
                onZoomChange = onZoomChange,
            )
        }

        Row(
            modifier = modifier
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
                        onPhotoButtonClick = { onCaptureMode(CaptureMode.PHOTO) },
                        onVideoButtonClick = { onCaptureMode(CaptureMode.VIDEO_READY) },
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Box {
                    ShutterButton(
                        captureMode = cameraSettings.captureMode,
                        onPhotoCapture = onPhotoCapture,
                        onVideoRecordingStart = onVideoRecordingStart,
                        onVideoRecordingFinish = onVideoRecordingFinish,
                        modifier = modifier.align(Alignment.Center),
                    )

                    CameraSwitcher(
                        captureMode = cameraSettings.captureMode,
                        onCameraSelector = onCameraSelector,
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
    onCameraSelector: () -> Unit,
    onCaptureMode: (CaptureMode) -> Unit,
    onPhotoCapture: () -> Unit,
    onPreviewSurfaceProviderReady: (SurfaceProvider) -> Unit,
    onVideoRecordingStart: () -> Unit,
    onVideoRecordingFinish: () -> Unit,
    onTapToFocus: (Display, Int, Int, Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    ViewFinder(
        modifier = modifier.fillMaxSize(),
        onSurfaceProviderReady = onPreviewSurfaceProviderReady,
        onTapToFocus = onTapToFocus,
        onZoomChange = onZoomChange,
    )

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 100.dp),
    ) {
        CameraControls(
            captureMode = cameraSettings.captureMode,
            onPhotoButtonClick = { onCaptureMode(CaptureMode.PHOTO) },
            onVideoButtonClick = { onCaptureMode(CaptureMode.VIDEO_READY) },
        )
    }

    ShutterButton(
        captureMode = cameraSettings.captureMode,
        onPhotoCapture = onPhotoCapture,
        onVideoRecordingStart = onVideoRecordingStart,
        onVideoRecordingFinish = onVideoRecordingFinish,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.5.dp),
    )

    CameraSwitcher(
        captureMode = cameraSettings.captureMode,
        onCameraSelector = onCameraSelector,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 200.dp, bottom = 30.dp),
    )
}

@Composable
fun CameraControls(
    captureMode: CaptureMode,
    onPhotoButtonClick: () -> Unit,
    onVideoButtonClick: () -> Unit,
) {
    val activeButtonColor =
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    val inactiveButtonColor =
        ButtonDefaults.buttonColors(containerColor = Color.LightGray)
    if (captureMode != CaptureMode.VIDEO_RECORDING) {
        Button(
            modifier = Modifier.padding(5.dp),
            onClick = onPhotoButtonClick,
            colors = if (captureMode == CaptureMode.PHOTO) activeButtonColor else inactiveButtonColor,
        ) {
            Text(stringResource(id = R.string.photo))
        }
        Button(
            modifier = Modifier.padding(5.dp),
            onClick = onVideoButtonClick,
            colors = if (captureMode != CaptureMode.PHOTO) activeButtonColor else inactiveButtonColor,
        ) {
            Text(stringResource(id = R.string.video))
        }
    }
}

@Composable
fun ShutterButton(
    captureMode: CaptureMode,
    onPhotoCapture: () -> Unit,
    onVideoRecordingStart: () -> Unit,
    onVideoRecordingFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = 25.dp),
    ) {
        when (captureMode) {
            CaptureMode.PHOTO -> {
                Button(
                    onClick = onPhotoCapture,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.size(75.dp),
                    content = { Unit },
                )
            }

            CaptureMode.VIDEO_READY -> {
                Button(
                    onClick = onVideoRecordingStart,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.size(75.dp),
                    content = { Unit },
                )
            }

            CaptureMode.VIDEO_RECORDING -> {
                Button(
                    onClick = onVideoRecordingFinish,
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
fun CameraSwitcher(
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

@Preview(device = Devices.PIXEL_FOLD)
@Composable
private fun HalfOpenHorizontalCameraLayoutPreView() {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        TwoPaneCameraLayout(
            cameraSettings = CameraSettings(),
            rotationState = Surface.ROTATION_270,
            onCameraSelector = {},
            onCaptureMode = {},
            onPhotoCapture = {},
            onPreviewSurfaceProviderReady = {},
            onVideoRecordingStart = {},
            onVideoRecordingFinish = {},
            onTapToFocus = { _, _, _, _, _ -> },
            onZoomChange = {},
        )
    }
}

@Preview(device = Devices.PIXEL_FOLD)
@Composable
private fun HalfOpenVerticalCameraLayoutPreView() {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        TwoPaneCameraLayout(
            cameraSettings = CameraSettings(),
            rotationState = Surface.ROTATION_0,
            onCameraSelector = {},
            onCaptureMode = {},
            onPhotoCapture = {},
            onPreviewSurfaceProviderReady = {},
            onVideoRecordingStart = {},
            onVideoRecordingFinish = {},
            onTapToFocus = { _, _, _, _, _ -> },
            onZoomChange = {},
        )
    }
}

@DevicePreview
@Composable
private fun FlatCameraLayoutPreView() {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        FlatCameraLayout(
            cameraSettings = CameraSettings(captureMode = CaptureMode.VIDEO_READY),
            onCameraSelector = {},
            onCaptureMode = {},
            onPhotoCapture = {},
            onPreviewSurfaceProviderReady = {},
            onVideoRecordingStart = {},
            onVideoRecordingFinish = {},
            onTapToFocus = { _, _, _, _, _ -> },
            onZoomChange = {},
        )
    }
}

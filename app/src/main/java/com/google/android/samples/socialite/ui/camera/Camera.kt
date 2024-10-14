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
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.RotationProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlin.reflect.KFunction1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Camera(
    onMediaCaptured: (Media?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    var surfaceProvider by remember { mutableStateOf<Preview.SurfaceProvider?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    val cameraAndRecordAudioPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ),
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var isLayoutUnfolded by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(lifecycleOwner, context) {
        val windowInfoTracker = WindowInfoTracker.getOrCreate(context)
        windowInfoTracker.windowLayoutInfo(context).collect { newLayoutInfo ->
            try {
                val foldingFeature = newLayoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>().firstOrNull()
                isLayoutUnfolded = (foldingFeature != null)
            } catch (e: Exception) {
                // If there was an issue detecting a foldable in the open position, default
                // to isLayoutUnfolded being false.
                isLayoutUnfolded = false
            }
        }
    }

    val viewFinderState by viewModel.viewFinderState.collectAsStateWithLifecycle()
    var rotation by remember { mutableStateOf(Surface.ROTATION_0) }

    DisposableEffect(lifecycleOwner, context) {
        val rotationProvider = RotationProvider(context)
        val rotationListener: (Int) -> Unit = { rotationValue: Int ->
            if (rotationValue != rotation) {
                surfaceProvider?.let { provider ->
                    viewModel.startPreview(
                        lifecycleOwner,
                        provider,
                        captureMode,
                        cameraSelector,
                        rotationValue,
                    )
                }
            }
            rotation = rotationValue
        }

        rotationProvider.addListener(Dispatchers.Main.asExecutor(), rotationListener)

        onDispose {
            rotationProvider.removeListener(rotationListener)
        }
    }

    val onPreviewSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit = {
        surfaceProvider = it
        viewModel.startPreview(lifecycleOwner, it, captureMode, cameraSelector, rotation)
    }

    fun setCaptureMode(mode: CaptureMode) {
        captureMode = mode
        surfaceProvider?.let { provider ->
            viewModel.startPreview(
                lifecycleOwner,
                provider,
                captureMode,
                cameraSelector,
                rotation,
            )
        }
    }

    fun setCameraSelector(selector: CameraSelector) {
        cameraSelector = selector
        surfaceProvider?.let { provider ->
            viewModel.startPreview(
                lifecycleOwner,
                provider,
                captureMode,
                cameraSelector,
                rotation,
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun onVideoRecordingStart() {
        captureMode = CaptureMode.VIDEO_RECORDING
        viewModel.startVideoCapture(onMediaCaptured)
    }

    fun onVideoRecordingFinish() {
        captureMode = CaptureMode.VIDEO_READY
        viewModel.saveVideo()
    }

    if (cameraAndRecordAudioPermissionState.allPermissionsGranted) {
        Box(modifier = modifier.background(color = Color.Black)) {
            Column(verticalArrangement = Arrangement.Bottom) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 25.dp, 0.dp, 0.dp)
                        .background(Color.Black)
                        .height(50.dp),
                ) {
                    IconButton(onClick = {
                        onMediaCaptured(null)
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
                if (isLayoutUnfolded != null) {
                    if (isLayoutUnfolded as Boolean) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CameraControls(
                                        captureMode,
                                        { setCaptureMode(CaptureMode.PHOTO) },
                                        { setCaptureMode(CaptureMode.VIDEO_READY) },
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp, 5.dp, 0.dp, 50.dp)
                                        .background(Color.Black)
                                        .height(100.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    ShutterButton(
                                        captureMode,
                                        { viewModel.capturePhoto(onMediaCaptured) },
                                        { onVideoRecordingStart() },
                                        { onVideoRecordingFinish() },
                                    )
                                }
                                Row(
                                    modifier = Modifier,
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CameraSwitcher(captureMode, cameraSelector, ::setCameraSelector)
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight(0.9F)
                                    .weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ViewFinder(
                                    viewFinderState.cameraState,
                                    onPreviewSurfaceProviderReady,
                                    viewModel::tapToFocus,
                                    viewModel::setZoomScale,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            ViewFinder(
                                viewFinderState.cameraState,
                                onPreviewSurfaceProviderReady,
                                viewModel::tapToFocus,
                                viewModel::setZoomScale,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp, 5.dp, 0.dp, 5.dp)
                                .background(Color.Black)
                                .height(50.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CameraControls(
                                captureMode,
                                { setCaptureMode(CaptureMode.PHOTO) },
                                { setCaptureMode(CaptureMode.VIDEO_READY) },
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp, 5.dp, 0.dp, 50.dp)
                                .background(Color.Black)
                                .height(100.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Spacer(modifier = Modifier.size(50.dp))
                            ShutterButton(
                                captureMode,
                                { viewModel.capturePhoto(onMediaCaptured) },
                                { onVideoRecordingStart() },
                                { onVideoRecordingFinish() },
                            )
                            CameraSwitcher(captureMode, cameraSelector, ::setCameraSelector)
                        }
                    }
                }
            }
        }
    } else {
        CameraAndRecordAudioPermission(cameraAndRecordAudioPermissionState) {
            onMediaCaptured(null)
        }
    }
}

@Composable
fun CameraControls(captureMode: CaptureMode, onPhotoButtonClick: () -> Unit, onVideoButtonClick: () -> Unit) {
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
            Text("Photo")
        }
        Button(
            modifier = Modifier.padding(5.dp),
            onClick = onVideoButtonClick,
            colors = if (captureMode != CaptureMode.PHOTO) activeButtonColor else inactiveButtonColor,
        ) {
            Text("Video")
        }
    }
}

@Composable
fun ShutterButton(captureMode: CaptureMode, onPhotoCapture: () -> Unit, onVideoRecordingStart: () -> Unit, onVideoRecordingFinish: () -> Unit) {
    Box(modifier = Modifier.padding(25.dp, 0.dp)) {
        if (captureMode == CaptureMode.PHOTO) {
            Button(
                onClick = onPhotoCapture,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .height(75.dp)
                    .width(75.dp),
            ) {}
        } else if (captureMode == CaptureMode.VIDEO_READY) {
            Button(
                onClick = onVideoRecordingStart,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .height(75.dp)
                    .width(75.dp),
            ) {}
        } else if (captureMode == CaptureMode.VIDEO_RECORDING) {
            Button(
                onClick = onVideoRecordingFinish,
                shape = RoundedCornerShape(10),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .height(50.dp)
                    .width(50.dp),
            ) {}
            Spacer(modifier = Modifier.width(100.dp))
        }
    }
}

@Composable
fun CameraSwitcher(captureMode: CaptureMode, cameraSelector: CameraSelector, setCameraSelector: KFunction1<CameraSelector, Unit>) {
    if (captureMode != CaptureMode.VIDEO_RECORDING) {
        IconButton(onClick = {
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)
            } else {
                setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
            }
        }) {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .height(75.dp)
                    .width(75.dp),
            )
        }
    }
}

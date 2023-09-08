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

package com.example.android.social.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Camera(chatId: Long, onMediaCaptured: (Media?) -> Unit) {
    var surfaceProvider = remember { mutableStateOf<Preview.SurfaceProvider?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    val cameraAndRecordAudioPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ),
    )
    val viewModel: CameraViewModel = viewModel()

    viewModel.initialize()
    viewModel.setChatId(chatId)

    val lifecycleOwner = LocalLifecycleOwner.current

    val onPreviewSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit = {
        surfaceProvider.value = it
        viewModel.startPreview(lifecycleOwner, it, captureMode, cameraSelector)
    }

    fun setCaptureMode(mode: CaptureMode) {
        captureMode = mode
        if (surfaceProvider.value != null) {
            viewModel.startPreview(
                lifecycleOwner,
                surfaceProvider.value!!,
                captureMode,
                cameraSelector,
            )
        }
    }

    fun setCameraSelector(selector: CameraSelector) {
        cameraSelector = selector
        if (surfaceProvider.value != null) {
            viewModel.startPreview(
                lifecycleOwner,
                surfaceProvider.value!!,
                captureMode,
                cameraSelector,
            )
        }
    }

    if (cameraAndRecordAudioPermissionState.allPermissionsGranted) {
        Box(modifier = Modifier.background(color = Color.Black)) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val viewFinderState by viewModel.viewFinderState.collectAsStateWithLifecycle()
                    ViewFinder(
                        viewFinderState.cameraState,
                        onPreviewSurfaceProviderReady,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 5.dp, 0.dp, 5.dp)
                        .background(Color.Black)
                        .height(100.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val activeButtonColor =
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    val inactiveButtonColor =
                        ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    if (captureMode != CaptureMode.VIDEO_RECORDING) {
                        Button(
                            modifier = Modifier.padding(5.dp),
                            onClick = { setCaptureMode(CaptureMode.PHOTO) },
                            colors = if (captureMode == CaptureMode.PHOTO) activeButtonColor else inactiveButtonColor,
                        ) {
                            Text("Photo")
                        }
                        Button(
                            modifier = Modifier.padding(5.dp),
                            onClick = { setCaptureMode(CaptureMode.VIDEO_READY) },
                            colors = if (captureMode != CaptureMode.PHOTO) activeButtonColor else inactiveButtonColor,
                        ) {
                            Text("Video")
                        }
                    }
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
                    Box(modifier = Modifier.padding(25.dp, 0.dp)) {
                        if (captureMode == CaptureMode.PHOTO) {
                            Button(
                                onClick = { viewModel.capturePhoto(onMediaCaptured) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier
                                    .height(75.dp)
                                    .width(75.dp),
                            ) {}
                        } else if (captureMode == CaptureMode.VIDEO_READY) {
                            Button(
                                onClick =
                                @SuppressLint("MissingPermission") {
                                    captureMode = CaptureMode.VIDEO_RECORDING
                                    viewModel.startVideoCapture(onMediaCaptured)
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier
                                    .height(75.dp)
                                    .width(75.dp),
                            ) {}
                        } else if (captureMode == CaptureMode.VIDEO_RECORDING) {
                            Button(
                                onClick =
                                {
                                    captureMode = CaptureMode.VIDEO_READY
                                    viewModel.saveVideo()
                                },
                                shape = RoundedCornerShape(10),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier
                                    .height(50.dp)
                                    .width(50.dp),
                            ) {}
                        }
                    }
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
        }
    } else {
        CameraAndRecordAudioPermission(cameraAndRecordAudioPermissionState)
    }
}

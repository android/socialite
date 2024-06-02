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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.android.samples.socialite.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraAndRecordAudioPermission(
    permissionsState: MultiplePermissionsState,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
) {
    var alreadyRequestedCameraPermissions by remember { mutableStateOf(false) }
    fun onRequestPermissionsClicked() {
        permissionsState.launchMultiplePermissionRequest()
        alreadyRequestedCameraPermissions = true
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(45.dp),
                )
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(45.dp),
                )
            }

            Row {
                Text(stringResource(R.string.camera_permission_rationale))
            }

            if (alreadyRequestedCameraPermissions) {
                Row {
                    Text(stringResource(R.string.camera_permission_settings))
                }
            } else {
                if (permissionsState.shouldShowRationale) {
                    Row {
                        Button(onClick = { onRequestPermissionsClicked() }) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                } else {
                    LaunchedEffect(permissionsState) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                    alreadyRequestedCameraPermissions = true
                }
            }

            Button(onClick = { onBackClicked() }) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

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

import androidx.camera.core.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.android.social.ui.camera.viewfinder.CameraPreview

@Composable
fun ViewFinder(cameraState: CameraState, onSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit = {}) {
    val context = LocalContext.current

//    if (cameraState == CameraState.NOT_READY) {
//        Text(text = stringResource(R.string.camera_not_ready))
//    } else if (cameraState == CameraState.READY) {
    Box {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onSurfaceProviderReady = onSurfaceProviderReady,
            onRequestBitmapReady = {
                val bitmap = it.invoke()
            },
        )
    }
}
// }

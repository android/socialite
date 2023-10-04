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

import android.view.View
import androidx.camera.core.AspectRatio
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.samples.socialite.ui.camera.viewfinder.CameraPreview

@Composable
fun ViewFinder(cameraState: CameraState, onSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit = {}) {
    lateinit var viewInfo: View

//    if (cameraState == CameraState.NOT_READY) {
//        Text(text = stringResource(R.string.camera_not_ready))
//    } else if (cameraState == CameraState.READY) {
    BoxWithConstraints(
        Modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val maxAspectRatio: Float = maxWidth / maxHeight
        val aspectRatio: Float = CameraViewModel.aspectRatios.getValue(AspectRatio.RATIO_16_9)
        val shouldUseMaxWidth = maxAspectRatio >= aspectRatio
        val width = if (shouldUseMaxWidth) maxWidth else maxHeight * aspectRatio
        val height = if (!shouldUseMaxWidth) maxHeight else maxWidth / aspectRatio
        Box(
            modifier = Modifier
                .width(width)
                .height(height),
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE,
                onSurfaceProviderReady = onSurfaceProviderReady,
                onRequestBitmapReady = {
                    val bitmap = it.invoke()
                },
                setSurfaceView = { s: View ->
                    viewInfo = s
                },
            )
        }
    }
    // }
}

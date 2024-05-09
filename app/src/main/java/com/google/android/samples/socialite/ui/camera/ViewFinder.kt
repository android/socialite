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

import androidx.camera.core.Preview
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ViewFinder(
    cameraState: CameraState,
    onSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit = {},
    onZoomChange: (Float) -> Unit,
) {
    val transformableState = rememberTransformableState(
        onTransformation = { zoomChange, _, _ ->
            onZoomChange(zoomChange)
        },
    )
    Box(
        Modifier
            .background(Color.Black)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .transformable(state = transformableState),
        ) {
            CameraXViewfinder(
                modifier = Modifier.fillMaxSize(),
                implementationMode = ImplementationMode.PERFORMANCE,
                onSurfaceProviderReady = onSurfaceProviderReady,
            )
        }
    }
}

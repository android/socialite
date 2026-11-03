/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.video.FrameProcessor
import androidx.media3.effect.DefaultGlFrameProcessor
import androidx.media3.effect.playservices.EnhancementSessionDecorator
import com.google.android.samples.socialite.ui.mediaenhancement.EnhancementSupportManager

/** Google Play services AI enhancements selected in the video edit screen. */
data class AiEnhancementOptions(
    val tonemap: Boolean = false,
    val deblurAndDenoise: Boolean = false,
    val upscale: Boolean = false,
) {
    val isEnabled: Boolean get() = tonemap || deblurAndDenoise || upscale
}

/** Whether this device can run Media3's [EnhancementSessionDecorator], which needs API 33. */
suspend fun isAiEnhancementSupported(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        EnhancementSupportManager.checkSupport(context)

/**
 * Returns a [FrameProcessor.Factory] that applies the Socialite edits on the GPU and then sends
 * each frame through a Google Play services enhancement session.
 *
 * Each [FrameProcessor] created by the factory opens its own enhancement session, and an app can
 * only have one active session, so release the preview player before starting an export.
 */
@OptIn(ExperimentalApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun AiEnhancementOptions.createFrameProcessorFactory(context: Context): FrameProcessor.Factory =
    EnhancementSessionDecorator.Builder(
        context,
        DefaultGlFrameProcessor.Factory.Builder(context).build(),
    )
        .setTonemappingEnabled(tonemap)
        .setDeblurAndDenoiseVideoEnabled(deblurAndDenoise)
        .setUpscaleVideoEnabled(upscale)
        .build()

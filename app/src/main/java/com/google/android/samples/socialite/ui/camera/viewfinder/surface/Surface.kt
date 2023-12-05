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

package com.google.android.samples.socialite.ui.camera.viewfinder.surface

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.samples.socialite.ui.camera.viewfinder.surface.SurfaceHolderEvent.SurfaceChanged
import com.google.android.samples.socialite.ui.camera.viewfinder.surface.SurfaceHolderEvent.SurfaceCreated
import com.google.android.samples.socialite.ui.camera.viewfinder.surface.SurfaceHolderEvent.SurfaceDestroyed

private const val TAG = "Surface"

@Composable
fun Surface(
    onSurfaceHolderEvent: (SurfaceHolderEvent) -> Unit = { _ -> }
) {
    Log.d(TAG, "Surface")

    AndroidView(factory = { context ->
        SurfaceView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            holder.addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        onSurfaceHolderEvent(SurfaceCreated(holder))
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        onSurfaceHolderEvent(SurfaceChanged(holder, width, height))
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        onSurfaceHolderEvent(SurfaceDestroyed(holder))
                    }
                }
            )
        }
    })
}


sealed interface SurfaceHolderEvent {
    data class SurfaceCreated(
        val holder: SurfaceHolder
    ) : SurfaceHolderEvent

    data class SurfaceChanged(
        val holder: SurfaceHolder,
        val width: Int,
        val height: Int
    ) : SurfaceHolderEvent

    data class SurfaceDestroyed(
        val holder: SurfaceHolder
    ) : SurfaceHolderEvent
}

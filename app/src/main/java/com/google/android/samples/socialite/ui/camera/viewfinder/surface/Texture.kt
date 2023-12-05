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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

private const val TAG = "Texture"

@SuppressLint("RestrictedApi")
@Composable
fun Texture(
    modifier: Modifier,
    onSurfaceTextureEvent: (SurfaceTextureEvent) -> Boolean = { _ -> true },
    onRequestBitmapReady: (() -> Bitmap?) -> Unit,
    setView: (View) -> Unit,
    surfaceRequest: SurfaceRequest?,
) {
    Log.d(TAG, "Texture")

    val resolution = surfaceRequest?.resolution
    var textureView: TextureView? by remember { mutableStateOf(null) }
    var parentView: FrameLayout? by remember { mutableStateOf(null) }
    if (parentView != null && surfaceRequest != null && resolution != null) {
        surfaceRequest.setTransformationInfoListener(Dispatchers.Main.asExecutor())
        { transformationInfo ->
            val parentViewSize = Size(parentView!!.width, parentView!!.height)
            if (parentViewSize.height == 0 || parentViewSize.width == 0) {
                return@setTransformationInfoListener
            }
            val viewFinder = textureView!!
            val surfaceRectInViewFinder: RectF =
                SurfaceTransformationUtil.getTransformedSurfaceRect(
                    resolution,
                    transformationInfo,
                    parentViewSize,
                    surfaceRequest.camera.isFrontFacing
                )
            if (!transformationInfo.hasCameraTransform()) {
                viewFinder.layoutParams = FrameLayout.LayoutParams(
                    surfaceRectInViewFinder.width().toInt(),
                    surfaceRectInViewFinder.height().toInt()
                )
            } else {
                viewFinder.layoutParams = FrameLayout.LayoutParams(
                    resolution.width,
                    resolution.height
                )
            }
            // For TextureView, correct the orientation to match the target rotation.
            val correctionMatrix = SurfaceTransformationUtil.getTextureViewCorrectionMatrix(
                transformationInfo,
                resolution
            )
            viewFinder.setTransform(correctionMatrix)

            viewFinder.pivotX = 0f
            viewFinder.pivotY = 0f
            viewFinder.scaleX = surfaceRectInViewFinder.width() / resolution.width
            viewFinder.scaleY = surfaceRectInViewFinder.height() / resolution.height
            viewFinder.translationX = surfaceRectInViewFinder.left - viewFinder.left
            viewFinder.translationY = surfaceRectInViewFinder.top - viewFinder.top
        }
    }

    if (resolution != null) {
        AndroidView(
            modifier = modifier.clipToBounds(),
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    addView(TextureView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            resolution.width,
                            resolution.height
                        )
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                onSurfaceTextureEvent(
                                    SurfaceTextureEvent.SurfaceTextureAvailable(
                                        surface,
                                        width,
                                        height
                                    )
                                )
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                onSurfaceTextureEvent(
                                    SurfaceTextureEvent.SurfaceTextureSizeChanged(
                                        surface,
                                        width,
                                        height
                                    )
                                )
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                return onSurfaceTextureEvent(
                                    SurfaceTextureEvent.SurfaceTextureDestroyed(
                                        surface
                                    )
                                )
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                                onSurfaceTextureEvent(
                                    SurfaceTextureEvent.SurfaceTextureUpdated(
                                        surface
                                    )
                                )
                            }
                        }
                    })
                }
            }, update = {
                parentView = it
                textureView = it.getChildAt(0) as TextureView?
                setView(it)
                onRequestBitmapReady { textureView!!.bitmap }
            }
        )
    }

}

sealed interface SurfaceTextureEvent {
    data class SurfaceTextureAvailable(
        val surface: SurfaceTexture,
        val width: Int,
        val height: Int
    ) : SurfaceTextureEvent

    data class SurfaceTextureSizeChanged(
        val surface: SurfaceTexture,
        val width: Int,
        val height: Int
    ) : SurfaceTextureEvent

    data class SurfaceTextureDestroyed(
        val surface: SurfaceTexture
    ) : SurfaceTextureEvent

    data class SurfaceTextureUpdated(
        val surface: SurfaceTexture
    ) : SurfaceTextureEvent
}
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
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Size
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.TransformUtils

/**
 * A util class with methods that transform the input viewFinder surface so that its preview fits
 * the given aspect ratio of its parent view.
 *
 * The goal is to transform it in a way so that the entire area of
 * [SurfaceRequest.TransformationInfo.getCropRect] is 1) visible to end users, and 2)
 * displayed as large as possible.
 *
 * The inputs for the calculation are 1) the dimension of the Surface, 2) the crop rect, 3) the
 * dimension of the Viewfinder and 4) rotation degrees
 */
object SurfaceTransformationUtil {
    @SuppressLint("RestrictedApi", "WrongConstant")
    private fun getRemainingRotationDegrees(transformationInfo: SurfaceRequest.TransformationInfo): Int {
        return if (!transformationInfo.hasCameraTransform()) {
            // If the Surface is not connected to the camera, then the SurfaceView/TextureView will
            // not apply any transformation. In that case, we need to apply the rotation
            // calculated by CameraX.
            transformationInfo.rotationDegrees
        } else if (transformationInfo.targetRotation == -1) {
            0
        } else {
            // If the Surface is connected to the camera, then the SurfaceView/TextureView
            // will be the one to apply the camera orientation. In that case, only the Surface
            // rotation needs to be applied.
            -CameraOrientationUtil.surfaceRotationToDegrees(transformationInfo.targetRotation)
        }
    }

    @SuppressLint("RestrictedApi")
    fun getTextureViewCorrectionMatrix(
        transformationInfo: SurfaceRequest.TransformationInfo,
        resolution: Size
    ): Matrix {
        val surfaceRect =
            RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())
        val rotationDegrees: Int = getRemainingRotationDegrees(transformationInfo)
        return TransformUtils.getRectToRect(surfaceRect, surfaceRect, rotationDegrees)
    }

    @SuppressLint("RestrictedApi")
    private fun getRotatedViewportSize(transformationInfo: SurfaceRequest.TransformationInfo): Size {
        return if (TransformUtils.is90or270(transformationInfo.rotationDegrees)) {
            Size(transformationInfo.cropRect.height(), transformationInfo.cropRect.width())
        } else Size(transformationInfo.cropRect.width(), transformationInfo.cropRect.height())
    }

    @SuppressLint("RestrictedApi")
    fun isViewportAspectRatioMatchViewFinder(
        transformationInfo: SurfaceRequest.TransformationInfo,
        viewFinderSize: Size
    ): Boolean {
        // Using viewport rect to check if the viewport is based on the view finder.
        val rotatedViewportSize: Size = getRotatedViewportSize(transformationInfo)
        return TransformUtils.isAspectRatioMatchingWithRoundingError(
            viewFinderSize,  /* isAccurate1= */true,
            rotatedViewportSize,  /* isAccurate2= */false
        )
    }

    private fun setMatrixRectToRect(
        matrix: Matrix, source: RectF, destination: RectF,
    ) {
        val matrixScaleType = Matrix.ScaleToFit.CENTER
        // android.graphics.Matrix doesn't support fill scale types. The workaround is
        // mapping inversely from destination to source, then invert the matrix.
        matrix.setRectToRect(destination, source, matrixScaleType)
        matrix.invert(matrix)
    }

    private fun getViewFinderViewportRectForMismatchedAspectRatios(
        transformationInfo: SurfaceRequest.TransformationInfo,
        viewFinderSize: Size,
    ): RectF {
        val viewFinderRect = RectF(
            0f, 0f, viewFinderSize.width.toFloat(),
            viewFinderSize.height.toFloat()
        )
        val rotatedViewportSize = getRotatedViewportSize(transformationInfo)
        val rotatedViewportRect = RectF(
            0f, 0f, rotatedViewportSize.width.toFloat(),
            rotatedViewportSize.height.toFloat()
        )
        val matrix = Matrix()
        setMatrixRectToRect(
            matrix,
            rotatedViewportRect,
            viewFinderRect,
        )
        matrix.mapRect(rotatedViewportRect)
        return rotatedViewportRect
    }

    @SuppressLint("RestrictedApi")
    fun getSurfaceToViewFinderMatrix(
        viewFinderSize: Size,
        transformationInfo: SurfaceRequest.TransformationInfo,
        isFrontCamera: Boolean
    ): Matrix {
        // Get the target of the mapping, the coordinates of the crop rect in view finder.
        val viewFinderCropRect: RectF =
            if (isViewportAspectRatioMatchViewFinder(transformationInfo, viewFinderSize)) {
                // If crop rect has the same aspect ratio as view finder, scale the crop rect to
                // fill the entire view finder. This happens if the scale type is FILL_* AND a
                // view-finder-based viewport is used.
                RectF(
                    0f, 0f, viewFinderSize.width.toFloat(),
                    viewFinderSize.height.toFloat()
                )
            } else {
                // If the aspect ratios don't match, it could be 1) scale type is FIT_*, 2) the
                // Viewport is not based on the view finder or 3) both.
                getViewFinderViewportRectForMismatchedAspectRatios(
                    transformationInfo, viewFinderSize
                )
            }
        val matrix = TransformUtils.getRectToRect(
            RectF(transformationInfo.cropRect), viewFinderCropRect,
            transformationInfo.rotationDegrees
        )
        if (isFrontCamera && transformationInfo.hasCameraTransform()) {
            // SurfaceView/TextureView automatically mirrors the Surface for front camera, which
            // needs to be compensated by mirroring the Surface around the upright direction of the
            // output image. This is only necessary if the stream has camera transform.
            // Otherwise, an internal GL processor would have mirrored it already.
            if (TransformUtils.is90or270(transformationInfo.rotationDegrees)) {
                // If the rotation is 90/270, the Surface should be flipped vertically.
                //   +---+     90 +---+  270 +---+
                //   | ^ | -->    | < |      | > |
                //   +---+        +---+      +---+
                matrix.preScale(
                    1f,
                    -1f,
                    transformationInfo.cropRect.centerX().toFloat(),
                    transformationInfo.cropRect.centerY().toFloat()
                )
            } else {
                // If the rotation is 0/180, the Surface should be flipped horizontally.
                //   +---+      0 +---+  180 +---+
                //   | ^ | -->    | ^ |      | v |
                //   +---+        +---+      +---+
                matrix.preScale(
                    -1f,
                    1f,
                    transformationInfo.cropRect.centerX().toFloat(),
                    transformationInfo.cropRect.centerY().toFloat()
                )
            }
        }
        return matrix
    }

    fun getTransformedSurfaceRect(
        resolution: Size,
        transformationInfo: SurfaceRequest.TransformationInfo,
        viewFinderSize: Size,
        isFrontCamera: Boolean
    ): RectF {
        val surfaceToViewFinder: Matrix =
            getSurfaceToViewFinderMatrix(
                viewFinderSize,
                transformationInfo,
                isFrontCamera
            )
        val rect = RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())
        surfaceToViewFinder.mapRect(rect)
        return rect
    }
}
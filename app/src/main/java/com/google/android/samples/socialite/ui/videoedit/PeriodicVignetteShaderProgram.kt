/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Assertions.checkArgument
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.GlUtil.GlException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.BaseGlShaderProgram
import java.io.IOException
import kotlin.math.cos


/**
 * A [GlShaderProgram] that periodically dims the frames such that pixels are darker the
 * further they are away from the frame center.
 *
 * The inner radius of the vignette effect oscillates smoothly between `minInnerRadius` and
 * `maxInnerRadius`.
 *
 * The pixels between the inner radius and the `outerRadius` are darkened linearly based on their
 * distance from `innerRadius`. All pixels outside `outerRadius` are black.
 *
 * The parameters are given in normalized texture coordinates from 0 to 1.
 */
@UnstableApi
internal class PeriodicVignetteShaderProgram(
    context: Context?,
    useHdr: Boolean,
    centerX: Float,
    centerY: Float,
    minInnerRadius: Float,
    maxInnerRadius: Float,
    outerRadius: Float,
) : BaseGlShaderProgram(
    /* useHighPrecisionColorComponents= */
    useHdr,
    /* texturePoolCapacity= */
    1,
) {
    private var glProgram: GlProgram
    private val minInnerRadius: Float
    private val deltaInnerRadius: Float

    init {
        checkArgument(minInnerRadius <= maxInnerRadius)
        checkArgument(maxInnerRadius <= outerRadius)
        this.minInnerRadius = minInnerRadius
        this.deltaInnerRadius = maxInnerRadius - minInnerRadius
        try {
            glProgram = GlProgram(context!!, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH)
        } catch (e: IOException) {
            throw VideoFrameProcessingException(e)
        } catch (e: GlException) {
            throw VideoFrameProcessingException(e)
        }
        glProgram.setFloatsUniform("uCenter", floatArrayOf(centerX, centerY))
        glProgram.setFloatsUniform("uOuterRadius", floatArrayOf(outerRadius))
        // Draw the frame on the entire normalized device coordinate space, from -1 to 1, for x and y.
        glProgram.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
        )
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
    }

    @Throws(VideoFrameProcessingException::class)
    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform(
                "uTexSampler",
                inputTexId,
                /* texUnitIndex= */
                0
            )
            val theta = presentationTimeUs * 2 * Math.PI / DIMMING_PERIOD_US
            val innerRadius =
                minInnerRadius + deltaInnerRadius * (0.5f - 0.5f * cos(theta).toFloat())
            glProgram.setFloatsUniform("uInnerRadius", floatArrayOf(innerRadius))
            glProgram.bindAttributesAndUniforms()
            // The four-vertex triangle strip forms a quad.
            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_STRIP,
                /* first= */
                0,
                /* count= */
                4
            )
        } catch (e: GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    @Throws(VideoFrameProcessingException::class)
    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    companion object {
        private const val VERTEX_SHADER_PATH = "vertex_shader_copy_es2.glsl"
        private const val FRAGMENT_SHADER_PATH = "fragment_shader_vignette_es2.glsl"
        private const val DIMMING_PERIOD_US = 5_600_000f
    }
}

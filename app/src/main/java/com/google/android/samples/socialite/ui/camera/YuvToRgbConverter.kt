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

package com.google.android.samples.socialite.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import androidx.annotation.IntDef
import java.nio.ByteBuffer

/**
 * Helper class used to convert a [Image] object from
 * [ImageFormat.YUV_420_888] format to an RGB [Bitmap] object, it has equivalent
 * functionality to https://github
 * .com/androidx/androidx/blob/androidx-main/camera/camera-core/src/main/java/androidx/camera/core/ImageYuvToRgbConverter.java
 *
 * NOTE: This has been tested in a limited number of devices and is not
 * considered production-ready code. It was created for illustration purposes,
 * since this is not an efficient camera pipeline due to the multiple copies
 * required to convert each frame. For example, this
 * implementation
 * (https://stackoverflow.com/questions/52726002/camera2-captured-picture-conversion-from-yuv-420-888-to-nv21/52740776#52740776)
 * might have better performance.
 */
class YuvToRgbConverter(context: Context) {
    private val rs = RenderScript.create(context)
    private val scriptYuvToRgb =
        ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    // Do not add getters/setters functions to these private variables
    // because yuvToRgb() assume they won't be modified elsewhere
    private var yuvBits: ByteBuffer? = null
    private var bytes: ByteArray = ByteArray(0)
    private var inputAllocation: Allocation? = null
    private var outputAllocation: Allocation? = null

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        val yuvBuffer = YuvByteBuffer(image, yuvBits)
        yuvBits = yuvBuffer.buffer

        if (needCreateAllocations(image, yuvBuffer)) {
            val yuvType = Type.Builder(rs, Element.U8(rs))
                .setX(image.width)
                .setY(image.height)
                .setYuvFormat(yuvBuffer.type)
            inputAllocation = Allocation.createTyped(
                rs,
                yuvType.create(),
                Allocation.USAGE_SCRIPT,
            )
            bytes = ByteArray(yuvBuffer.buffer.capacity())
            val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(image.width)
                .setY(image.height)
            outputAllocation = Allocation.createTyped(
                rs,
                rgbaType.create(),
                Allocation.USAGE_SCRIPT,
            )
        }

        yuvBuffer.buffer.get(bytes)
        inputAllocation!!.copyFrom(bytes)

        // Convert NV21 or YUV_420_888 format to RGB
        inputAllocation!!.copyFrom(bytes)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation!!.copyTo(output)
    }

    private fun needCreateAllocations(image: Image, yuvBuffer: YuvByteBuffer): Boolean {
        return (inputAllocation == null ||               // the very 1st call
            inputAllocation!!.type.x != image.width ||   // image size changed
            inputAllocation!!.type.y != image.height ||
            inputAllocation!!.type.yuv != yuvBuffer.type || // image format changed
            bytes.size == yuvBuffer.buffer.capacity())
    }
}

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(ImageFormat.NV21, ImageFormat.YUV_420_888)
annotation class YuvType

class YuvByteBuffer(image: Image, dstBuffer: ByteBuffer? = null) {
    @YuvType
    val type: Int
    val buffer: ByteBuffer

    init {
        val wrappedImage = ImageWrapper(image)

        type = if (wrappedImage.u.pixelStride == 1) {
            ImageFormat.YUV_420_888
        } else {
            ImageFormat.NV21
        }
        val size = image.width * image.height * 3 / 2
        buffer = if (
            dstBuffer == null || dstBuffer.capacity() < size ||
            dstBuffer.isReadOnly || !dstBuffer.isDirect
        ) {
            ByteBuffer.allocateDirect(size) }
        else {
            dstBuffer
        }
        buffer.rewind()

        removePadding(wrappedImage)
    }

    // Input buffers are always direct as described in
    // https://developer.android.com/reference/android/media/Image.Plane#getBuffer()
    private fun removePadding(image: ImageWrapper) {
        val sizeLuma = image.y.width * image.y.height
        val sizeChroma = image.u.width * image.u.height
        if (image.y.rowStride > image.y.width) {
            removePaddingCompact(image.y, buffer, 0)
        } else {
            buffer.position(0)
            buffer.put(image.y.buffer)
        }
        if (type == ImageFormat.YUV_420_888) {
            if (image.u.rowStride > image.u.width) {
                removePaddingCompact(image.u, buffer, sizeLuma)
                removePaddingCompact(image.v, buffer, sizeLuma + sizeChroma)
            } else {
                buffer.position(sizeLuma)
                buffer.put(image.u.buffer)
                buffer.position(sizeLuma + sizeChroma)
                buffer.put(image.v.buffer)
            }
        } else {
            if (image.u.rowStride > image.u.width * 2) {
                removePaddingNotCompact(image, buffer, sizeLuma)
            } else {
                buffer.position(sizeLuma)
                var uv = image.v.buffer
                val properUVSize = image.v.height * image.v.rowStride - 1
                if (uv.capacity() > properUVSize) {
                    uv = clipBuffer(image.v.buffer, 0, properUVSize)
                }
                buffer.put(uv)
                val lastOne = image.u.buffer[image.u.buffer.capacity() - 1]
                buffer.put(buffer.capacity() - 1, lastOne)
            }
        }
        buffer.rewind()
    }

    private fun removePaddingCompact(
        plane: PlaneWrapper,
        dst: ByteBuffer,
        offset: Int,
    ) {
        require(plane.pixelStride == 1) {
            "use removePaddingCompact with pixelStride == 1"
        }

        val src = plane.buffer
        val rowStride = plane.rowStride
        var row: ByteBuffer
        dst.position(offset)
        for (i in 0 until plane.height) {
            row = clipBuffer(src, i * rowStride, plane.width)
            dst.put(row)
        }
    }

    private fun removePaddingNotCompact(
        image: ImageWrapper,
        dst: ByteBuffer,
        offset: Int,
    ) {
        require(image.u.pixelStride == 2) {
            "use removePaddingNotCompact pixelStride == 2"
        }
        val width = image.u.width
        val height = image.u.height
        val rowStride = image.u.rowStride
        var row: ByteBuffer
        dst.position(offset)
        for (i in 0 until height - 1) {
            row = clipBuffer(image.v.buffer, i * rowStride, width * 2)
            dst.put(row)
        }
        row = clipBuffer(image.u.buffer, (height - 1) * rowStride - 1, width * 2)
        dst.put(row)
    }

    private fun clipBuffer(buffer: ByteBuffer, start: Int, size: Int): ByteBuffer {
        val duplicate = buffer.duplicate()
        duplicate.position(start)
        duplicate.limit(start + size)
        return duplicate.slice()
    }

    private class ImageWrapper(image: Image) {
        val width= image.width
        val height = image.height
        val y = PlaneWrapper(width, height, image.planes[0])
        val u = PlaneWrapper(width / 2, height / 2, image.planes[1])
        val v = PlaneWrapper(width / 2, height / 2, image.planes[2])

        // Check this is a supported image format
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
        init {
            require(y.pixelStride == 1) {
                "Pixel stride for Y plane must be 1 but got ${y.pixelStride} instead."
            }
            require(u.pixelStride == v.pixelStride && u.rowStride == v.rowStride) {
                "U and V planes must have the same pixel and row strides " +
                    "but got pixel=${u.pixelStride} row=${u.rowStride} for U " +
                    "and pixel=${v.pixelStride} and row=${v.rowStride} for V"
            }
            require(u.pixelStride == 1 || u.pixelStride == 2) {
                "Supported" + " pixel strides for U and V planes are 1 and 2"
            }
        }
    }

    private class PlaneWrapper(width: Int, height: Int, plane: Image.Plane) {
        val width = width
        val height = height
        val buffer: ByteBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
    }
}


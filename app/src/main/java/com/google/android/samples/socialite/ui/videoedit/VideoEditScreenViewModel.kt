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

package com.google.android.samples.socialite.ui.videoedit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ByteBufferGlEffect
import androidx.media3.effect.GlEffect
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.android.samples.socialite.ui.camera.CameraViewModel
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "VideoEditViewModel"

@UnstableApi
@HiltViewModel
class VideoEditScreenViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val repository: ChatRepository,
) : ViewModel() {

    private val chatId = MutableStateFlow(0L)
    private var transformedVideoFilePath = ""

    private val _isFinishedEditing = MutableStateFlow(false)
    val isFinishedEditing: StateFlow<Boolean> = _isFinishedEditing

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun setChatId(chatId: Long) {
        this.chatId.value = chatId
    }

    private val transformerListener: Transformer.Listener =
        @UnstableApi object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                Toast.makeText(application, "Edited video saved", Toast.LENGTH_LONG).show()

                sendVideo()

                _isFinishedEditing.value = true
                _isProcessing.value = false
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                exportException.printStackTrace()
                Toast.makeText(application, "Error applying edits on video", Toast.LENGTH_LONG)
                    .show()
                _isProcessing.value = false
            }
        }

    @OptIn(UnstableApi::class)
    fun applyVideoTransformation(
        context: Context,
        videoUri: String,
        removeAudio: Boolean,
        rgbAdjustmentEffectSelected: Boolean,
        periodicVignetteEffectSelected: Boolean,
        styleTransferEffectSelected: Boolean,
        textOverlayText: String,
        textOverlayRedSelected: Boolean,
        textOverlayLargeSelected: Boolean,
        videoTrimStart: Long,
        videoTrimEnd: Long,
    ) {
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(transformerListener)
            .build()

        val composition = prepareComposition(
            context = context,
            videoUri = videoUri,
            removeAudio = removeAudio,
            rgbAdjustmentEffectSelected = rgbAdjustmentEffectSelected,
            periodicVignetteEffectSelected = periodicVignetteEffectSelected,
            styleTransferEffectSelected = styleTransferEffectSelected,
            textOverlayText = textOverlayText,
            textOverlayRedSelected = textOverlayRedSelected,
            textOverlayLargeSelected = textOverlayLargeSelected,
            videoTrimStart = videoTrimStart,
            videoTrimEnd = videoTrimEnd,
        )

        val editedVideoFileName = "Socialite-edited-recording-" +
            SimpleDateFormat(CameraViewModel.FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"

        transformedVideoFilePath = createNewVideoFilePath(context, editedVideoFileName)

        // TODO: Investigate using MediaStoreOutputOptions instead of external cache file for saving
        //  edited video https://github.com/androidx/media/issues/504
        transformer.start(composition, transformedVideoFilePath)
        _isProcessing.value = true
    }

    /**
     * Prepares the video composition with the specified edits. This method is used to configure the
     * video for preview.
     */
    @OptIn(UnstableApi::class)
    fun prepareComposition(
        context: Context,
        videoUri: String,
        removeAudio: Boolean,
        rgbAdjustmentEffectSelected: Boolean,
        periodicVignetteEffectSelected: Boolean,
        styleTransferEffectSelected: Boolean,
        textOverlayText: String,
        textOverlayRedSelected: Boolean,
        textOverlayLargeSelected: Boolean,
        videoTrimStart: Long,
        videoTrimEnd: Long,
    ): Composition {
        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(videoTrimStart)
                    .setEndPositionMs(videoTrimEnd)
                    .build(),
            )
            .build()
        // Try to retrieve the video duration
        val retriever = MediaMetadataRetriever()
        val durationUs = try {
            retriever.setDataSource(context, videoUri.toUri())
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull()?.times(1000) ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving duration of video")
            0L
        } finally {
            retriever.release()
        }
        // Build the list of video effects to apply
        val videoEffects =
            buildVideoEffectsList(
                context = context,
                rgbAdjustmentEffectSelected = rgbAdjustmentEffectSelected,
                periodicVignetteEffectSelected = periodicVignetteEffectSelected,
                styleTransferEffectSelected = styleTransferEffectSelected,
                textOverlayText = textOverlayText,
                textOverlayRedSelected = textOverlayRedSelected,
                textOverlayLargeSelected = textOverlayLargeSelected,
            )

        val editedMediaItem =
            EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(removeAudio).setDurationUs(durationUs).build()
        val videoImageSequence = EditedMediaItemSequence(editedMediaItem)

        val compositionBuilder = Composition.Builder(videoImageSequence)
        // Tone-map to SDR if style transfer is selected since it can only be applied for SDR videos
        if (styleTransferEffectSelected) {
            compositionBuilder.setHdrMode(HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
        }
        compositionBuilder.setEffects(Effects(listOf(), videoEffects))

        return compositionBuilder.build()
    }

    /**
     * Builds a list of video effects based on the selected options.
     */
    @OptIn(UnstableApi::class)
    private fun buildVideoEffectsList(
        context: Context,
        rgbAdjustmentEffectSelected: Boolean,
        periodicVignetteEffectSelected: Boolean,
        styleTransferEffectSelected: Boolean,
        textOverlayText: String,
        textOverlayRedSelected: Boolean,
        textOverlayLargeSelected: Boolean,
    ): List<Effect> {
        val videoEffects: MutableList<GlEffect> = mutableListOf()
        val overlaysBuilder = ImmutableList.Builder<TextureOverlay>()

        // Add text overlay effect if text is provided
        if (textOverlayText.isNotEmpty()) {
            val spannableStringBuilder = SpannableStringBuilder(textOverlayText)

            val spanStart = 0
            val spanEnd = textOverlayText.length

            val redTextSpan = ForegroundColorSpan(Color.RED)
            val doubleTextSpan = RelativeSizeSpan(2f)

            if (textOverlayRedSelected) {
                spannableStringBuilder.setSpan(
                    redTextSpan,
                    spanStart,
                    spanEnd,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                )
            }
            if (textOverlayLargeSelected) {
                spannableStringBuilder.setSpan(
                    doubleTextSpan,
                    spanStart,
                    spanEnd,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                )
            }

            val textOverlay = TextOverlay.createStaticTextOverlay(
                SpannableString.valueOf(spannableStringBuilder),
            )

            overlaysBuilder.add(textOverlay)
        }
        videoEffects.add(OverlayEffect(overlaysBuilder.build()))

        // Add RGB adjustment effect if selected
        if (rgbAdjustmentEffectSelected) {
            // Create an RgbAdjustment effect to modify the color balance
            videoEffects.add(
                RgbAdjustment.Builder()
                    .setRedScale(1.5f)
                    .setGreenScale(1.2f)
                    .setBlueScale(0.8f).build(),
            )
        }
        // Add periodic vignette effect if selected
        if (periodicVignetteEffectSelected) {
            // Create a GlEffect that applies a vignette effect that changes over time
            // The shader program for this effect is defined in PeriodicVignetteShaderProgram
            videoEffects.add(
                GlEffect { context: Context?, useHdr: Boolean ->
                    PeriodicVignetteShaderProgram(
                        context = context,
                        useHdr = useHdr,
                        centerX = 0.5f,
                        centerY = 0.5f,
                        minInnerRadius = 0f,
                        maxInnerRadius = 0.7f,
                        outerRadius = 0.7f,
                    )
                },
            )
        }
        // Add style transfer effect if selected
        if (styleTransferEffectSelected) {
            // Apply a style transfer effect using a TensorFlow Lite model
            videoEffects.add(ByteBufferGlEffect<Bitmap>(StyleTransferEffect(context, "style.jpg")))
        }

        return videoEffects
    }

    private fun createNewVideoFilePath(context: Context, fileName: String): String {
        val externalCacheFile = createExternalCacheFile(context, fileName)
        return externalCacheFile.absolutePath
    }

    /** Creates a cache file, resetting it if it already exists.  */
    @Throws(IOException::class)
    private fun createExternalCacheFile(context: Context, fileName: String): File {
        val file = File(context.externalCacheDir, fileName)
        check(!(file.exists() && !file.delete())) {
            "Could not delete the previous transformer output file"
        }
        check(file.createNewFile()) { "Could not create the transformer output file" }
        return file
    }

    fun sendVideo() {
        viewModelScope.launch {
            repository.sendMessage(chatId.value, "", transformedVideoFilePath, "video/mp4")
        }
    }
}

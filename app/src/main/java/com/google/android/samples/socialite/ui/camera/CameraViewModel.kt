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

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Display
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.effects.OverlayEffect
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbFilter
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CameraViewModel"

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val cameraProviderManager: CameraXProcessCameraProviderManager,
    private val repository: ChatRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private lateinit var camera: Camera
    private lateinit var extensionsManager: ExtensionsManager

    val chatId: Long? = savedStateHandle.get("chatId")
    var viewFinderState = MutableStateFlow(ViewFinderState())

    val aspectRatioStrategy =
        AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_NONE)
    var resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(aspectRatioStrategy)
        .build()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest

    private val previewUseCase = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build().apply {
            setSurfaceProvider { newSurfaceRequest ->
                _surfaceRequest.update { newSurfaceRequest }
            }
        }

    private val imageCaptureUseCase = ImageCapture.Builder()
        .setResolutionSelector(resolutionSelector)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    private val recorder = Recorder.Builder()
        .setAspectRatio(AspectRatio.RATIO_16_9)
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()

    private val videoCaptureBuilder = VideoCapture.Builder(recorder)
    private lateinit var videoCaptureUseCase: VideoCapture<Recorder>

    private val imageAnalysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    lateinit var greenScreenEffect: OverlayEffect
    val backgroundRemovalThreshold = 0.8

    lateinit var mask: Bitmap
    lateinit var bitmap: Bitmap

    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    init {
        val videoCaptureBuilder = VideoCapture.Builder(recorder)
        viewModelScope.launch {
            greenScreenEffect = OverlayEffect(
                PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                5,
                Handler(Looper.getMainLooper()),
                {},
            )

            imageAnalysisUseCase.setAnalyzer(
                ContextCompat.getMainExecutor(application),
                SelfieSegmentationAnalyzer(),
            )

            // Create a Paint object to draw the mask layer.
            val paint = Paint()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            paint.colorFilter = ColorMatrixColorFilter(
                floatArrayOf(
                    0f, 0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )

            greenScreenEffect.setOnDrawListener { frame ->
                if (!::mask.isInitialized || !::bitmap.isInitialized) {
                    // Do not change the drawing if the frame doesn’t match the analysis
                    // result.
                    return@setOnDrawListener true
                }

                // Clear the previously drawn frame.
                frame.overlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                // Draw the bitmap and mask, positioning the overlay in the bottom right corner.
                val rect = Rect(2 * bitmap.width, 0, 3 * bitmap.width, bitmap.height)
                frame.overlayCanvas.drawBitmap(bitmap, null, rect, null)
                frame.overlayCanvas.drawBitmap(mask, null, rect, paint)

                true
            }
        }
    }

    private suspend fun getHdrCameraInfo(): DynamicRange? {
        var supportedHdrEncoding: DynamicRange? = null

        cameraProviderManager.getCameraProvider().availableCameraInfos
            .first { cameraInfo ->
                val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
                val supportedDynamicRanges =
                    videoCapabilities.supportedDynamicRanges

                supportedHdrEncoding = supportedDynamicRanges.firstOrNull {
                    // To ensure consistency between the multiple dynamic range profiles, chose
                    // HLG 10-bit, which is supported by all devices that supports HDR.
                    it == DynamicRange.HLG_10_BIT
                }
                return@first true
            }

        return supportedHdrEncoding
    }

    private suspend fun setVideoCaptureDynamicRange(effectMode: EffectMode, videoCaptureBuilder: VideoCapture.Builder<Recorder>) {
        // Note: there is currently an issue with androidx.camera.media3.effect not being
        // compatible with Media3 version 1.6. Media3 1.6 is needed to tonemap videos down
        // to SDR to perform certain effects, so for not, we are disabling HDR video. A fix
        // is currently in development, so we'll re-enable HDR video recording when that fix lands.
        videoCaptureBuilder.setDynamicRange(DynamicRange.SDR)

        // Determine whether we can capture HDR video. Currently, concurrent camera
        // does not support HDR video, so we can't enable HDR if the green screen effect
        // is being applied.
        // val hdrCameraInfo = getHdrCameraInfo()
        // if (hdrCameraInfo != null && effectMode != EffectMode.GREEN_SCREEN) {
        //    Log.i(TAG, "Capturing HDR video")
        //    videoCaptureBuilder.setDynamicRange(hdrCameraInfo)
        // } else {
        //    videoCaptureBuilder.setDynamicRange(DynamicRange.SDR)
        // }
    }

    fun setChatId(chatId: Long) {
        savedStateHandle.set("chatId", chatId)
    }

    @UnstableApi
    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        captureMode: CaptureMode,
        effectMode: EffectMode,
        cameraSelector: CameraSelector,
        rotation: Int,
    ) {
        viewModelScope.launch {
            val cameraProvider = cameraProviderManager.getCameraProvider()
            val extensionManagerJob = viewModelScope.launch {
                extensionsManager = ExtensionsManager.getInstanceAsync(
                    application,
                    cameraProvider,
                ).await()
            }
            var extensionsCameraSelector: CameraSelector? = null

            setVideoCaptureDynamicRange(effectMode, videoCaptureBuilder)
            videoCaptureUseCase = videoCaptureBuilder.build()

            val useCaseGroupBuilder = UseCaseGroup.Builder()
                .addUseCase(previewUseCase)

            if (captureMode == CaptureMode.PHOTO) {
                try {
                    extensionManagerJob.join()

                    // Query if we should use Night Mode and if the extension is available.
                    if (effectMode == EffectMode.NIGHT_MODE &&
                        extensionsManager.isExtensionAvailable(
                            cameraSelector,
                            ExtensionMode.NIGHT,
                        )
                    ) {
                        // Retrieve extension enabled camera selector
                        extensionsCameraSelector =
                            extensionsManager.getExtensionEnabledCameraSelector(
                                cameraSelector,
                                ExtensionMode.NIGHT,
                            )
                    }
                } catch (e: InterruptedException) {
                    // This should not happen unless the future is cancelled or the thread is
                    // interrupted by applications.
                }

                imageCaptureUseCase.targetRotation = rotation
                useCaseGroupBuilder.addUseCase(imageCaptureUseCase)
            } else if (captureMode == CaptureMode.VIDEO_READY || captureMode == CaptureMode.VIDEO_RECORDING) {
                videoCaptureUseCase.targetRotation = rotation
                useCaseGroupBuilder.addUseCase(videoCaptureUseCase)
            }

            if (effectMode == EffectMode.BLACK_AND_WHITE) {
                // Use the grayscale effect with the Media3Effect connector.
                val media3Effect =
                    Media3Effect(
                        application,
                        PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE,
                        ContextCompat.getMainExecutor(application),
                        {},
                    )
                media3Effect.setEffects(listOf(RgbFilter.createGrayscaleFilter()))
                useCaseGroupBuilder.addEffect(media3Effect)
            }

            if (effectMode == EffectMode.GREEN_SCREEN) {
                // Concurrent camera setup for green screen effect
                var primaryCameraSelector: CameraSelector? = null
                var secondaryCameraSelector: CameraSelector? = null

                // Iterate through available concurrent camera infos to find suitable primary
                // (front-facing) and secondary (back-facing) cameras.
                for (cameraInfos in cameraProvider.availableConcurrentCameraInfos) {
                    primaryCameraSelector = cameraInfos.first {
                        it.lensFacing == CameraSelector.LENS_FACING_FRONT
                    }.cameraSelector
                    secondaryCameraSelector = cameraInfos.first {
                        it.lensFacing == CameraSelector.LENS_FACING_BACK
                    }.cameraSelector

                    if (primaryCameraSelector == null || secondaryCameraSelector == null) {
                        // If either a primary or secondary selector wasn't found, reset both
                        // to move on to the next list of CameraInfos.
                        primaryCameraSelector = null
                        secondaryCameraSelector = null
                    } else {
                        // If both primary and secondary camera selectors were found, we can
                        // conclude the search.
                        break
                    }
                }

                if (primaryCameraSelector != null && secondaryCameraSelector != null) {
                    useCaseGroupBuilder.addEffect(greenScreenEffect)

                    val segmentedSelfieUseCaseGroupBuilder = UseCaseGroup.Builder()
                        .addUseCase(imageAnalysisUseCase)

                    val primary = ConcurrentCamera.SingleCameraConfig(
                        primaryCameraSelector,
                        segmentedSelfieUseCaseGroupBuilder.build(),
                        lifecycleOwner,
                    )

                    val secondary = ConcurrentCamera.SingleCameraConfig(
                        secondaryCameraSelector,
                        useCaseGroupBuilder.build(),
                        lifecycleOwner,
                    )

                    cameraProvider.unbindAll()
                    val concurrentCamera = cameraProvider.bindToLifecycle(
                        listOf(primary, secondary),
                    )

                    return@launch
                }
            }

            cameraProvider.unbindAll()
            val activeCameraSelector = extensionsCameraSelector ?: cameraSelector
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                activeCameraSelector,
                useCaseGroupBuilder.build(),
            )
            viewFinderState.value.cameraState = CameraState.READY
        }
    }

    fun setTargetRotation(rotation: Int) {
        imageCaptureUseCase.targetRotation = rotation
        videoCaptureUseCase.targetRotation = rotation
    }

    fun capturePhoto(onMediaCaptured: (Media) -> Unit) {
        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SociaLite")
            }
        }

        val context: Context = application
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
            .build()
        imageCaptureUseCase.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    val msg = "Photo capture failed."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        sendPhotoMessage(savedUri.toString())
                        onMediaCaptured(Media(savedUri, MediaType.PHOTO))
                    } else {
                        val msg = "Photo capture failed."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startVideoCapture(onMediaCaptured: (Media) -> Unit) {
        val name = "Socialite-recording-" +
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SociaLite")
            }
        }
        val context: Context = application
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
            .setContentValues(contentValues)
            .build()

        val captureListener = Consumer<VideoRecordEvent> { event ->
            recordingState = event
            if (event is VideoRecordEvent.Finalize) {
                onMediaCaptured(Media(event.outputResults.outputUri, MediaType.VIDEO))
            }
        }

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording = videoCaptureUseCase.output
            .prepareRecording(context, mediaStoreOutput)
            .apply { withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(context), captureListener)
    }

    fun sendPhotoMessage(photoUri: String) {
        viewModelScope.launch {
            if (chatId != null) {
                repository.sendMessage(
                    chatId = chatId,
                    text = "",
                    mediaUri = photoUri,
                    mediaMimeType = "image/jpeg",
                )
            }
        }
    }

    fun saveVideo() {
        if (currentRecording == null || recordingState is VideoRecordEvent.Finalize) {
            return
        }

        val recording = currentRecording
        if (recording != null) {
            recording.stop()
            currentRecording = null
        }
    }

    companion object {
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        val aspectRatios = mapOf(AspectRatio.RATIO_16_9 to (9.0 / 16.0).toFloat())
    }

    fun tapToFocus(
        display: Display,
        surfaceWidth: Int,
        surfaceHeight: Int,
        x: Float,
        y: Float,
    ) {
        camera.let { camera ->
            val meteringPoint =
                DisplayOrientedMeteringPointFactory(
                    display,
                    camera.cameraInfo,
                    surfaceWidth.toFloat(),
                    surfaceHeight.toFloat(),
                ).createPoint(x, y)

            val action = FocusMeteringAction.Builder(meteringPoint).build()

            camera.cameraControl.startFocusAndMetering(action)
        }
    }

    fun setZoomScale(scale: Float) {
        val zoomState = camera.cameraInfo.zoomState.value
        if (zoomState == null) return
        val finalScale =
            (zoomState.zoomRatio * scale).coerceIn(
                zoomState.minZoomRatio,
                zoomState.maxZoomRatio,
            )
        camera.cameraControl.setZoomRatio(finalScale)
    }

    inner class SelfieSegmentationAnalyzer : ImageAnalysis.Analyzer {

        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
        val selfieSegmenter = Segmentation.getClient(options)
        lateinit var maskBuffer: ByteBuffer
        lateinit var maskBitmap: Bitmap

        @androidx.annotation.OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                selfieSegmenter.process(image)
                    .addOnSuccessListener { results ->
                        // Get foreground probabilities for each pixel. Since ML Kit returns this
                        // in a byte buffer with each 4 bytes representing a float, convert it to
                        // a FloatBuffer for easier use.
                        val maskProbabilities = results.buffer.asFloatBuffer()

                        // Initialize our mask buffer and intermediate mask bitmap
                        if (!::maskBuffer.isInitialized) {
                            maskBitmap = createBitmap(
                                results.width,
                                results.height,
                                Bitmap.Config.ALPHA_8,
                            )
                            maskBuffer = ByteBuffer.allocateDirect(
                                maskBitmap.allocationByteCount,
                            )
                        }
                        maskBuffer.rewind()

                        // Convert the mask to an A8 image from the mask probabilities.
                        // We use a line buffer hear to optimize reads from the FloatBuffer.
                        val lineBuffer = FloatArray(results.width)
                        for (y in 0..<results.height) {
                            maskProbabilities.get(lineBuffer)
                            for (point in lineBuffer) {
                                maskBuffer.put(
                                    if (point > backgroundRemovalThreshold) {
                                        255.toByte()
                                    } else {
                                        0
                                    },
                                )
                            }
                        }
                        maskBuffer.rewind()
                        // Convert the mask buffer to a Bitmap so we can easily rotate and
                        // mirror.
                        maskBitmap.copyPixelsFromBuffer(maskBuffer)

                        // Transformation matrix to mirror and rotate our bitmaps
                        val matrix = Matrix().apply {
                            setScale(-1f, 1f)
                        }

                        // Mirror the ImageProxy
                        bitmap = Bitmap.createBitmap(
                            imageProxy.toBitmap(),
                            0,
                            0,
                            imageProxy.width,
                            imageProxy.height,
                            matrix,
                            false,
                        )

                        // Rotate and mirror the mask. When the rotation is 90 or 270, we need
                        // to swap the width and height.
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val (rotWidth, rotHeight) = when (rotation) {
                            90, 270 ->
                                Pair(maskBitmap.height, maskBitmap.width)

                            else ->
                                Pair(maskBitmap.width, maskBitmap.height)
                        }
                        mask = Bitmap.createBitmap(
                            maskBitmap,
                            0,
                            0,
                            rotWidth,
                            rotHeight,
                            matrix.apply { preRotate(-rotation.toFloat()) },
                            false,
                        )
                    }
                    .addOnCompleteListener {
                        // Final cleanup. Close imageProxy for next analysis frame.
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}

data class ViewFinderState(
    var cameraState: CameraState = CameraState.NOT_READY,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
)

/**
 * Defines the current state of the camera.
 */
enum class CameraState {
    /**
     * Camera hasn't been initialized.
     */
    NOT_READY,

    /**
     * Camera is open and presenting a preview stream.
     */
    READY,

    /**
     * Camera is initialized but the preview has been stopped.
     */
    PREVIEW_STOPPED,
}

enum class CaptureMode {
    PHOTO,
    VIDEO_READY,
    VIDEO_RECORDING,
}

enum class EffectMode {
    NONE,
    BLACK_AND_WHITE,
    GREEN_SCREEN,
    NIGHT_MODE,
}

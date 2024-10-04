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
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import androidx.annotation.RequiresPermission
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.samples.socialite.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private lateinit var videoCaptureUseCase: VideoCapture<Recorder>

    val chatId: Long? = savedStateHandle.get("chatId")
    var viewFinderState = MutableStateFlow(ViewFinderState())
    private val _imageCaptureState = MutableStateFlow(ImageCaptureState.PENDING)
    val imageCaptureState: StateFlow<ImageCaptureState> = _imageCaptureState

    val aspectRatioStrategy =
        AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_NONE)
    var resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(aspectRatioStrategy)
        .build()

    private val previewUseCase = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()

    private val imageCaptureUseCase = ImageCapture.Builder()
        .setResolutionSelector(resolutionSelector)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    private val recorder = Recorder.Builder()
        .setAspectRatio(AspectRatio.RATIO_16_9)
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()

    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    init {
        val videoCaptureBuilder = VideoCapture.Builder(recorder)
        viewModelScope.launch {
            val hdrCameraInfo = getHdrCameraInfo()

            if (hdrCameraInfo != null) {
                Log.i(TAG, "Capturing HDR video")
                videoCaptureBuilder.setDynamicRange(hdrCameraInfo)
            }

            videoCaptureUseCase = videoCaptureBuilder.build()
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

    fun setChatId(chatId: Long) {
        savedStateHandle.set("chatId", chatId)
    }

    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        captureMode: CaptureMode,
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
            val useCaseGroupBuilder = UseCaseGroup.Builder()

            previewUseCase.setSurfaceProvider(surfaceProvider)
            useCaseGroupBuilder.addUseCase(previewUseCase)

            if (captureMode == CaptureMode.PHOTO) {
                try {
                    extensionManagerJob.join()

                    // Query if extension is available.
                    if (extensionsManager.isExtensionAvailable(
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
            .format(System.currentTimeMillis())
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
                    _imageCaptureState.value = ImageCaptureState.IMAGE_CAPTURE_FAIL
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val state: ImageCaptureState
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        state = ImageCaptureState.IMAGE_CAPTURE_SUCCESS
                        sendPhotoMessage(savedUri.toString())
                        onMediaCaptured(Media(savedUri, MediaType.PHOTO))
                    } else {
                        state = ImageCaptureState.IMAGE_CAPTURE_FAIL
                    }
                    _imageCaptureState.value = state
                }
            },
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startVideoCapture(onMediaCaptured: (Media) -> Unit) {
        val name = "Socialite-recording-" +
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"
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
        camera?.let { camera ->
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
        val zoomState = camera?.cameraInfo?.zoomState?.value
        if (zoomState == null) return
        val finalScale =
            (zoomState.zoomRatio * scale).coerceIn(
                zoomState.minZoomRatio,
                zoomState.maxZoomRatio,
            )
        camera?.cameraControl?.setZoomRatio(finalScale)
    }
}

data class ViewFinderState(
    var cameraState: CameraState = CameraState.NOT_READY,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
)

enum class ImageCaptureState {
    PENDING,
    IMAGE_CAPTURE_SUCCESS,
    IMAGE_CAPTURE_FAIL,
}

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

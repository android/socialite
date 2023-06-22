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

package com.example.android.social.ui.camera

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class CameraViewModel @JvmOverloads constructor(
    application: Application,
) : AndroidViewModel(application) {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var initializeJob: Job

    private lateinit var context: Context
    var viewFinderState = MutableStateFlow(ViewFinderState())

    private val previewUseCase = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()

    private val imageCaptureUseCase = ImageCapture.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()

    private val recorder = Recorder.Builder()
        .setAspectRatio(AspectRatio.RATIO_16_9)
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()

    private var currentRecording: Recording? = null
    private var audioEnabled = false
    private lateinit var recordingState:VideoRecordEvent

    private val videoCaptureUseCase = VideoCapture.Builder(recorder)
        .build()

    fun initialize() {
        initializeJob = viewModelScope.launch {
            context = getApplication()
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        }
    }

    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
    ) {
        viewModelScope.launch {
            initializeJob.join()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            previewUseCase.setSurfaceProvider(surfaceProvider)

            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCaptureUseCase,
                videoCaptureUseCase
            )
            viewFinderState.value.cameraState = CameraState.READY
        }
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
                    if (output.savedUri != null) {
                        onMediaCaptured(Media(output.savedUri!!, MediaType.PHOTO))
                    } else {
                        val msg = "Photo capture failed."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    fun startVideoCapture(onMediaCaptured: (Media) -> Unit) {
        val name = "Socialite-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
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
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(context), captureListener)
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

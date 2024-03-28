/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.samples.socialite.domain

import android.content.Context
import android.net.Uri
import android.util.Rational
import androidx.annotation.RequiresPermission
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.media3.common.util.UnstableApi
import com.google.android.samples.socialite.ui.camera.CaptureMode
import com.google.android.samples.socialite.ui.camera.Media
import com.google.android.samples.socialite.ui.camera.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

interface CameraUseCase {
    suspend fun initializeCamera()
    fun createCameraUseCaseGroup(cameraSettings: CameraSettings): UseCaseGroup
    suspend fun capturePhoto(): Uri?
    suspend fun startVideoRecording(): Media
    fun stopVideoRecording()
}

@ViewModelScoped
class CameraXUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageOutputFileOptions: ImageCapture.OutputFileOptions,
    private val mediaStoreOutputOptions: MediaStoreOutputOptions,
) : CameraUseCase {

    private lateinit var previewUseCase: Preview
    private lateinit var imageCaptureUseCase: ImageCapture

    private lateinit var videoCaptureUseCase: VideoCapture<Recorder>
    private var recording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    override suspend fun initializeCamera() {
        val aspectRatioStrategy = AspectRatioStrategy(
            AspectRatio.RATIO_16_9,
            AspectRatioStrategy.FALLBACK_RULE_NONE,
        )

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy)
            .build()

        previewUseCase = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        imageCaptureUseCase = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        val recorder = Recorder.Builder()
            .setAspectRatio(AspectRatio.RATIO_16_9)
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()

        videoCaptureUseCase = VideoCapture.Builder(recorder).build()
    }

    override suspend fun capturePhoto(): Uri? = suspendCancellableCoroutine { continuation ->
        imageCaptureUseCase.takePicture(
            imageOutputFileOptions,
            Dispatchers.Default.asExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveUri = outputFileResults.savedUri
                    continuation.resume(saveUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            },
        )
    }

    @UnstableApi
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override suspend fun startVideoRecording(): Media = suspendCoroutine { continuation ->
        recording = videoCaptureUseCase.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply { withAudioEnabled() }
            .start(Dispatchers.Default.asExecutor()) { event ->
                recordingState = event

                if (event is VideoRecordEvent.Finalize) {
                    val media = Media(event.outputResults.outputUri, MediaType.VIDEO)
                    continuation.resume(media)
                }
            }
    }

    override fun stopVideoRecording() {
        val recording = checkNotNull(recording) { "Recording is not started" }

        recording.stop()
        this.recording = null
    }

    override fun createCameraUseCaseGroup(cameraSettings: CameraSettings): UseCaseGroup {
        val useCaseGroupBuilder = UseCaseGroup.Builder()

        previewUseCase.setSurfaceProvider(cameraSettings.surfaceProvider)
        videoCaptureUseCase.targetRotation = previewUseCase.targetRotation

        useCaseGroupBuilder.setViewPort(
            ViewPort.Builder(
                Rational(9, 16),
                previewUseCase.targetRotation,
            ).build(),
        )
        useCaseGroupBuilder.addUseCase(previewUseCase)
        useCaseGroupBuilder.addUseCase(imageCaptureUseCase)
        useCaseGroupBuilder.addUseCase(videoCaptureUseCase)

        return useCaseGroupBuilder.build()
    }
}

data class CameraSettings(
    val cameraLensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val aspectRatio: Int = AspectRatio.RATIO_16_9,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val surfaceProvider: Preview.SurfaceProvider? = null,
    val zoomScale: Float = 1f,
    val focusMetringAction: FocusMeteringAction? = null,
)

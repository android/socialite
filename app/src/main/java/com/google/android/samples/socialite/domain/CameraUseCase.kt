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
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
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

    override suspend fun initializeCamera() {
        previewUseCase = Preview.Builder().build()
        imageCaptureUseCase = ImageCapture.Builder().build()

        val recorder = Recorder.Builder()
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
    override suspend fun startVideoRecording(): Media =
        suspendCancellableCoroutine { continuation ->
            recording = videoCaptureUseCase.output
                .prepareRecording(context, mediaStoreOutputOptions)
                .apply { withAudioEnabled() }
                .start(Dispatchers.Default.asExecutor()) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        if (event.outputResults.outputUri.toString().isBlank()) {
                            continuation.resumeWithException(IllegalStateException("Video recording failed"))
                        }

                        val media = Media(event.outputResults.outputUri, MediaType.VIDEO)
                        continuation.resume(media)
                    }
                }
        }

    override fun stopVideoRecording() {
        recording?.stop() ?: return
        this.recording = null
    }

    override fun createCameraUseCaseGroup(cameraSettings: CameraSettings): UseCaseGroup {
        val useCaseGroupBuilder = UseCaseGroup.Builder()

        previewUseCase.setSurfaceProvider(cameraSettings.surfaceProvider)

        useCaseGroupBuilder.setViewPort(
            ViewPort.Builder(
                cameraSettings.aspectRatioType.ratio,
                cameraSettings.rotation,
            )
                .build(),
        )

        previewUseCase.targetRotation = cameraSettings.rotation
        imageCaptureUseCase.targetRotation = cameraSettings.rotation
        videoCaptureUseCase.targetRotation = cameraSettings.rotation

        useCaseGroupBuilder.addUseCase(previewUseCase)
        useCaseGroupBuilder.addUseCase(imageCaptureUseCase)
        useCaseGroupBuilder.addUseCase(videoCaptureUseCase)

        return useCaseGroupBuilder.build()
    }
}

data class CameraSettings(
    val cameraLensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val zoomScale: Float = 1f,
    val aspectRatioType: AspectRatioType = AspectRatioType.RATIO_9_16,
    val rotation: Int = Surface.ROTATION_0,
    val foldingState: FoldingState = FoldingState.CLOSE,
    val surfaceProvider: Preview.SurfaceProvider? = null,
)

enum class FoldingState {
    CLOSE,
    HALF_OPEN,
    FLAT,
}

enum class AspectRatioType(val ratio: Rational) {
    RATIO_4_3(Rational(4, 3)),
    RATIO_9_16(Rational(9, 16)),
    RATIO_16_9(Rational(16, 9)),
    RATIO_1_1(Rational(1, 1)),
}

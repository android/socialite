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
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.samples.socialite.domain.CameraSettings
import com.google.android.samples.socialite.domain.CameraUseCase
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.android.samples.socialite.util.CoroutineLifecycleOwner
import com.google.android.samples.socialite.util.RotationStateMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraProviderManager: CameraProviderManager,
    private val cameraXUseCase: CameraUseCase,
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    rotationStateMonitor: RotationStateMonitor,
) : ViewModel() {

    val chatId: StateFlow<Long> = savedStateHandle.getStateFlow("chatId", 0L)

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera

    private val _cameraSettings = MutableStateFlow(CameraSettings())
    val cameraSettings: StateFlow<CameraSettings> = _cameraSettings
        .filter { cameraSettings ->
            cameraSettings.surfaceProvider != null
        }
        .onStart {
            cameraProvider = cameraProviderManager.getCameraProvider()
            cameraXUseCase.initializeCamera()
        }
        .onEach { cameraSettings ->
            val useCaseGroup = cameraXUseCase.createCameraUseCaseGroup(cameraSettings)

            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                CoroutineLifecycleOwner(viewModelScope.coroutineContext),
                CameraSelector.Builder()
                    .requireLensFacing(cameraSettings.cameraLensFacing)
                    .build(),
                useCaseGroup,
            )
        }
        .catch {
            Log.e("CameraViewModel", "Error camera", it)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CameraSettings(),
        )

    val rotationState: StateFlow<Int> = rotationStateMonitor.currentRotation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Surface.ROTATION_0,
        )

    fun toggleCameraFacing() {
        _cameraSettings.update { settings ->
            settings.copy(
                cameraLensFacing = if (settings.cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                },
            )
        }
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        _cameraSettings.update { settings ->
            settings.copy(captureMode = captureMode)
        }
    }

    fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
        _cameraSettings.update { settings ->
            settings.copy(surfaceProvider = surfaceProvider)
        }
    }

    fun tapToFocus(
        display: Display,
        surfaceWidth: Int,
        surfaceHeight: Int,
        x: Float,
        y: Float,
    ) {
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

    fun setZoomScale(scale: Float) {
        val zoomState = camera.cameraInfo.zoomState.value ?: return
        val finalScale = (zoomState.zoomRatio * scale).coerceIn(
            zoomState.minZoomRatio,
            zoomState.maxZoomRatio,
        )

        camera.cameraControl.setZoomRatio(finalScale)
    }

    fun capturePhoto(onMediaCaptured: (Media) -> Unit) {
        viewModelScope.launch {
            val uri = cameraXUseCase.capturePhoto() ?: return@launch

            repository.sendMessage(
                chatId = chatId.value,
                text = "",
                mediaUri = uri.toString(),
                mediaMimeType = "image/jpeg",
            )
            onMediaCaptured(Media(uri, MediaType.PHOTO))
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startVideoCapture(onMediaCaptured: (Media) -> Unit) {
        viewModelScope.launch {
            val media = cameraXUseCase.startVideoRecording()
            onMediaCaptured(media)
        }
    }

    fun stopVideoRecording() {
        cameraXUseCase.stopVideoRecording()
    }
}

enum class CaptureMode {
    PHOTO,
    VIDEO_READY,
    VIDEO_RECORDING,
}

enum class FoldingState {
    CLOSE,
    HALF_OPEN,
    FLAT,
}

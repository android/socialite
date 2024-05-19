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

import android.util.Log
import android.view.Display
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.samples.socialite.domain.AspectRatioType
import com.google.android.samples.socialite.domain.CameraSettings
import com.google.android.samples.socialite.domain.CameraUseCase
import com.google.android.samples.socialite.domain.FoldingState
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.android.samples.socialite.util.CoroutineLifecycleOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraProviderManager: CameraProviderManager,
    private val cameraUseCase: CameraUseCase,
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val chatId: StateFlow<Long?> = savedStateHandle.getStateFlow("chatId", null)

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera

    private val _mediaCapture = MutableSharedFlow<Media>(replay = 0)
    val mediaCapture: SharedFlow<Media> = _mediaCapture

    private val _cameraSettings = MutableStateFlow(CameraSettings())
    val cameraSettings: StateFlow<CameraSettings> = _cameraSettings
        .onStart {
            cameraProvider = cameraProviderManager.getCameraProvider()
            cameraUseCase.initializeCamera()
        }
        .onEach { cameraSettings ->
            val useCaseGroup = cameraUseCase.createUseCaseGroup(cameraSettings)

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

    fun setUserEvent(cameraEvent: CameraEvent) {
        when (cameraEvent) {
            CameraEvent.CapturePhoto -> capturePhoto()
            CameraEvent.StartVideoRecording -> startVideoCapture()
            CameraEvent.StopVideoRecording -> stopVideoRecording()
            CameraEvent.ToggleCameraFacing -> toggleCameraFacing()
            is CameraEvent.CaptureModeChange -> setCaptureMode(cameraEvent.mode)
            is CameraEvent.TapToFocus -> tapToFocus(
                cameraEvent.display,
                cameraEvent.surfaceWidth,
                cameraEvent.surfaceHeight,
                cameraEvent.x,
                cameraEvent.y,
            )

            is CameraEvent.ZoomChange -> setZoomScale(cameraEvent.scale)
            is CameraEvent.SurfaceProviderReady -> setSurfaceProvider(cameraEvent.surfaceProvider)
        }
    }

    fun setCameraOrientation(
        foldingState: FoldingState,
        isPortrait: Boolean,
    ) {
        _cameraSettings.update { settings ->
            val ratio = when (foldingState) {
                FoldingState.CLOSE -> {
                    AspectRatioType.RATIO_9_16
                }

                FoldingState.HALF_OPEN -> {
                    if (isPortrait) {
                        AspectRatioType.RATIO_16_9
                    } else {
                        AspectRatioType.RATIO_9_16
                    }
                }

                FoldingState.FLAT -> {
                    if (isPortrait) {
                        AspectRatioType.RATIO_1_1
                    } else {
                        AspectRatioType.RATIO_4_3
                    }
                }
            }

            settings.copy(
                foldingState = foldingState,
                aspectRatioType = ratio,
            )
        }
    }

    private fun toggleCameraFacing() {
        _cameraSettings.update { settings ->
            settings.copy(
                cameraLensFacing =
                if (settings.cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                },
            )
        }
    }

    private fun setCaptureMode(captureMode: CaptureMode) {
        _cameraSettings.update { settings ->
            settings.copy(captureMode = captureMode)
        }
    }

    private fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
        _cameraSettings.update { settings ->
            settings.copy(surfaceProvider = surfaceProvider)
        }
    }

    private fun tapToFocus(
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

    private fun setZoomScale(scale: Float) {
        val zoomState = camera.cameraInfo.zoomState.value ?: return
        val finalScale = (zoomState.zoomRatio * scale).coerceIn(
            zoomState.minZoomRatio,
            zoomState.maxZoomRatio,
        )

        camera.cameraControl.setZoomRatio(finalScale)
    }

    private fun capturePhoto() {
        viewModelScope.launch {
            val uri = cameraUseCase.capturePhoto() ?: return@launch
            val chaId = chatId.value ?: return@launch

            repository.sendMessage(
                chatId = chaId,
                text = "",
                mediaUri = uri.toString(),
                mediaMimeType = "image/jpeg",
            )
            _mediaCapture.emit(Media(uri, MediaType.PHOTO))
        }
    }

    private fun startVideoCapture() {
        viewModelScope.launch {
            val media = cameraUseCase.startVideoRecording()
            _mediaCapture.emit(media)
        }
    }

    private fun stopVideoRecording() {
        cameraUseCase.stopVideoRecording()
    }
}

enum class CaptureMode {
    PHOTO,
    VIDEO_READY,
    VIDEO_RECORDING,
}

sealed interface CameraEvent {
    data object ToggleCameraFacing : CameraEvent
    data object CapturePhoto : CameraEvent
    data object StartVideoRecording : CameraEvent
    data object StopVideoRecording : CameraEvent
    data class ZoomChange(val scale: Float) : CameraEvent
    data class CaptureModeChange(val mode: CaptureMode) : CameraEvent
    data class SurfaceProviderReady(val surfaceProvider: Preview.SurfaceProvider) : CameraEvent
    data class TapToFocus(
        val display: Display,
        val surfaceWidth: Int,
        val surfaceHeight: Int,
        val x: Float,
        val y: Float,
    ) : CameraEvent
}

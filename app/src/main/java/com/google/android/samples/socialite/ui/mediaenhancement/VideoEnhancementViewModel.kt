package com.google.android.samples.socialite.ui.mediaenhancement

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.media.effect.enhancement.Enhancement
import com.google.android.gms.media.effect.enhancement.EnhancementClient
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "VideoEnhancementVM"

/**
 * Debounce applied before re-creating the session when the selected effects change: the SDK binds
 * the effect flags at session creation, so every change means a full recreate.
 */
private const val OPTIONS_DEBOUNCE_MS = 400L

private const val MEDIA_DIRECTORY = "media"
private const val ENHANCED_FILE_PREFIX = "enhanced_"
private const val PREVIEW_FILE_PREFIX = "enhanced_preview_"
private const val MP4_EXTENSION = ".mp4"

/** The video effects offered by the SDK. */
enum class VideoEnhancementEffect {
    TONEMAP,
    DEBLUR_AND_DENOISE,
    UPSCALE,
}

/** Defines the state of the video enhancement UI. */
data class VideoEnhancementUiState(
    @StringRes val moduleStatus: Int = R.string.video_enhancement_status_unknown,
    val isDeviceSupported: Boolean = true,
    val isModuleInstalling: Boolean = false,
    val moduleInstallProgress: Int = 0,
    val moduleInstallError: String? = null,
    val isModuleReady: Boolean = false,
    val selectedEffects: Set<VideoEnhancementEffect> = setOf(VideoEnhancementEffect.TONEMAP),
    val availableEffects: Set<VideoEnhancementEffect> = emptySet(),
    val sourceUri: String? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val isSourceTooLarge: Boolean = false,
    val isPreparingSession: Boolean = false,
    val isEnhancing: Boolean = false,
    val isSaving: Boolean = false,
    val transcodeProgress: Int = 0,
    val enhancedVideoUri: String? = null,
    val enhancementLatencyMs: Long? = null,
    val enhancementTimings: PhaseTimings? = null,
    val enhancementError: String? = null,
)

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.R)
class VideoEnhancementViewModel @Inject constructor(
    application: Application,
    private val chatRepository: ChatRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoEnhancementUiState())
    val uiState: StateFlow<VideoEnhancementUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.R)
    private val enhancementClient: EnhancementClient = Enhancement.getClient(application)
    private val enhancementExecutor = Executors.newSingleThreadExecutor()
    private val videoEnhancementRepository = VideoEnhancementRepository(
        context = application,
        enhancementClient = enhancementClient,
        enhancementExecutor = enhancementExecutor,
    )

    private var sourceInfo: VideoInfo? = null
    private var warmUpJob: Job? = null
    private var enhancementJob: Job? = null
    private var previewFile: File? = null

    init {
        checkAndInstallModule()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkAndInstallModule() {
        viewModelScope.launch {
            _uiState.update { it.copy(moduleStatus = R.string.video_enhancement_status_checking) }
            try {
                if (!enhancementClient.isDeviceSupportedAsync()) {
                    _uiState.update {
                        it.copy(
                            isDeviceSupported = false,
                            moduleStatus = R.string.video_enhancement_status_not_supported,
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isDeviceSupported = true) }

                if (!enhancementClient.isModuleInstalledAsync()) {
                    _uiState.update {
                        it.copy(
                            isModuleInstalling = true,
                            moduleInstallError = null,
                            moduleStatus = R.string.video_enhancement_status_installing,
                        )
                    }

                    val installed = enhancementClient.installModuleAsync { progress ->
                        _uiState.update { it.copy(moduleInstallProgress = progress) }
                    }
                    if (!installed) {
                        _uiState.update {
                            it.copy(
                                isModuleInstalling = false,
                                moduleInstallError = getString(
                                    R.string.video_enhancement_module_install_failed,
                                ),
                                moduleStatus = R.string.video_enhancement_status_install_failed,
                            )
                        }
                        return@launch
                    }
                }

                _uiState.update {
                    it.copy(
                        isModuleInstalling = false,
                        moduleInstallProgress = 100,
                        isModuleReady = true,
                        moduleStatus = R.string.video_enhancement_status_installed,
                    )
                }
                // The video may have been probed before the module was ready.
                warmUpSession(debounce = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check or install the enhancement module", e)
                _uiState.update {
                    it.copy(
                        isModuleInstalling = false,
                        moduleInstallError = getString(
                            R.string.video_enhancement_module_error,
                            e.message ?: getString(R.string.unknown_error),
                        ),
                        moduleStatus = R.string.video_enhancement_status_error,
                    )
                }
            }
        }
    }

    /** Probes [uri], applies the resolution policy, and pre-warms the session. */
    fun setVideo(uri: String) {
        if (_uiState.value.sourceUri == uri) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sourceUri = uri,
                    enhancedVideoUri = null,
                    enhancementError = null,
                    transcodeProgress = 0,
                )
            }

            val info = videoEnhancementRepository.probeVideo(uri.toUri())
            if (info == null) {
                _uiState.update {
                    it.copy(
                        enhancementError = getString(R.string.video_enhancement_error_unreadable),
                    )
                }
                return@launch
            }

            sourceInfo = info
            val isSourceTooLarge = !videoEnhancementRepository.isSourceSupported(info)
            val availableEffects = when {
                isSourceTooLarge -> emptySet()
                videoEnhancementRepository.isUpscaleSupported(info) ->
                    setOf(
                        VideoEnhancementEffect.TONEMAP,
                        VideoEnhancementEffect.DEBLUR_AND_DENOISE,
                        VideoEnhancementEffect.UPSCALE,
                    )

                else -> setOf(
                    VideoEnhancementEffect.TONEMAP,
                    VideoEnhancementEffect.DEBLUR_AND_DENOISE,
                )
            }

            _uiState.update {
                it.copy(
                    sourceWidth = info.width,
                    sourceHeight = info.height,
                    isSourceTooLarge = isSourceTooLarge,
                    availableEffects = availableEffects,
                    selectedEffects = it.selectedEffects.intersect(availableEffects),
                )
            }

            warmUpSession(debounce = false)
        }
    }

    /** Toggles an effect and schedules a debounced session recreate. */
    fun onEffectToggled(effect: VideoEnhancementEffect) {
        val state = _uiState.value
        if (effect !in state.availableEffects || state.isEnhancing) {
            return
        }

        val selectedEffects = if (effect in state.selectedEffects) {
            state.selectedEffects - effect
        } else {
            state.selectedEffects + effect
        }
        _uiState.update { it.copy(selectedEffects = selectedEffects) }
        warmUpSession(debounce = true)
    }

    fun enhanceVideo() {
        val state = _uiState.value
        val uri = state.sourceUri ?: return
        if (state.isSourceTooLarge || state.isEnhancing || !state.isModuleReady) {
            return
        }

        warmUpJob?.cancel()
        enhancementJob?.cancel()
        enhancementJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isEnhancing = true,
                    transcodeProgress = 0,
                    enhancedVideoUri = null,
                    enhancementError = null,
                )
            }

            deletePreviewFile()
            val outputFile = File(
                getApplication<Application>().cacheDir,
                "$PREVIEW_FILE_PREFIX${System.currentTimeMillis()}$MP4_EXTENSION",
            )
            previewFile = outputFile

            try {
                videoEnhancementRepository
                    .enhance(uri.toUri(), outputFile, selectedOptions())
                    .collect { progress -> onProgress(progress, startTime) }
            } catch (e: CancellationException) {
                deletePreviewFile()
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video enhancement failed", e)
                deletePreviewFile()
                _uiState.update {
                    it.copy(
                        enhancementError = getString(
                            R.string.video_enhancement_error_failed,
                            e.message ?: getString(R.string.unknown_error),
                        ),
                    )
                }
            } finally {
                _uiState.update { it.copy(isEnhancing = false) }
                // The session is bound to the encoder input surface of the run that just ended, so
                // it cannot survive that run and is always released. Rebuild it now, while the user
                // is looking at the result, rather than on the next tap: a repeat enhance (retry
                // after a cancel or an error, or re-running the same options) would otherwise pay
                // full session creation inside the user-visible latency.
                warmUpSession(debounce = false)
            }
        }
    }

    /** Stops an in-flight enhancement and discards the partial output. */
    fun cancelEnhancement() {
        videoEnhancementRepository.cancel()
        enhancementJob?.cancel()
        enhancementJob = null
        _uiState.update { it.copy(isEnhancing = false, transcodeProgress = 0) }
    }

    /**
     * Copies the enhanced preview into the app media directory and points [messageId] at it.
     *
     * The previous enhanced file of that message, if any, is deleted to bound storage usage.
     */
    fun saveEnhancedVideo(messageId: Long, oldUri: String, onComplete: () -> Unit) {
        val state = _uiState.value
        val enhancedVideoUri = state.enhancedVideoUri ?: return
        if (state.isSaving || state.isEnhancing) {
            return
        }
        _uiState.update { it.copy(isSaving = true, enhancementError = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val directory = File(context.filesDir, MEDIA_DIRECTORY)
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Prevent storage consumption by deleting only the previous enhanced file for this
                // message.
                val oldFileUri = oldUri.toUri()
                if (oldFileUri.scheme == "file") {
                    val oldFile = File(oldFileUri.path ?: "")
                    if (oldFile.exists() && oldFile.name.startsWith(ENHANCED_FILE_PREFIX)) {
                        oldFile.delete()
                    }
                }

                val sourcePath = enhancedVideoUri.toUri().path
                if (sourcePath == null) {
                    Log.e(TAG, "Enhanced video has no file path: $enhancedVideoUri")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            enhancementError = getString(R.string.unknown_error),
                        )
                    }
                    return@launch
                }

                val file = File(
                    directory,
                    "$ENHANCED_FILE_PREFIX${System.currentTimeMillis()}$MP4_EXTENSION",
                )
                File(sourcePath).copyTo(file, overwrite = true)
                deletePreviewFile()

                val newUri = file.toUri().toString()
                Log.d(TAG, "Saved enhanced video to $newUri, updating message $messageId")
                chatRepository.updateMessageMediaUri(messageId, newUri)

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save the enhanced video", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        enhancementError = getString(
                            R.string.video_enhancement_error_failed,
                            e.message ?: getString(R.string.unknown_error),
                        ),
                    )
                }
            }
        }
    }

    override fun onCleared() {
        warmUpJob?.cancel()
        enhancementJob?.cancel()
        videoEnhancementRepository.cancel()
        videoEnhancementRepository.releaseSession()
        deletePreviewFile()
        enhancementExecutor.shutdown()
        super.onCleared()
    }

    private fun onProgress(progress: VideoEnhancementProgress, startTimeMs: Long) {
        when (progress) {
            VideoEnhancementProgress.Preparing -> {
                _uiState.update { it.copy(transcodeProgress = 0) }
            }

            is VideoEnhancementProgress.Transcoding -> {
                _uiState.update { it.copy(transcodeProgress = progress.percent) }
            }

            is VideoEnhancementProgress.Completed -> {
                val wallClockMs = System.currentTimeMillis() - startTimeMs
                val timings = progress.timings
                Log.i(
                    TAG,
                    "Tap to playable: ${wallClockMs}ms | measured phases: $timings | " +
                        "unaccounted=${wallClockMs - timings.totalMs}ms",
                )
                _uiState.update {
                    it.copy(
                        transcodeProgress = 100,
                        enhancedVideoUri = Uri.fromFile(File(progress.outputPath)).toString(),
                        enhancementLatencyMs = wallClockMs,
                        enhancementTimings = timings,
                    )
                }
            }
        }
    }

    /**
     * Creates the session ahead of the first frame.
     *
     * Because the SDK binds the dimensions and the effect flags at creation time, this runs again
     * every time the selection changes, after [OPTIONS_DEBOUNCE_MS] so that a burst of taps only
     * creates one session.
     */
    private fun warmUpSession(debounce: Boolean) {
        val info = sourceInfo ?: return
        val state = _uiState.value
        if (state.isSourceTooLarge || state.isEnhancing) {
            return
        }

        warmUpJob?.cancel()
        warmUpJob = viewModelScope.launch {
            if (debounce) {
                delay(OPTIONS_DEBOUNCE_MS)
            }
            if (!_uiState.value.isModuleReady) {
                return@launch
            }

            _uiState.update { it.copy(isPreparingSession = true) }
            val startMs = SystemClock.elapsedRealtime()
            try {
                videoEnhancementRepository.prepareSession(info.width, info.height, selectedOptions())
                Log.i(
                    TAG,
                    "Session pre-warmed in ${SystemClock.elapsedRealtime() - startMs}ms",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed pre-warm is not fatal: the session is created again on demand.
                Log.w(TAG, "Failed to pre-warm the enhancement session", e)
            } finally {
                _uiState.update { it.copy(isPreparingSession = false) }
            }
        }
    }

    private fun selectedOptions(): VideoEnhancementOptions {
        val selectedEffects = _uiState.value.selectedEffects
        val isDeblurEnabled = VideoEnhancementEffect.DEBLUR_AND_DENOISE in selectedEffects
        return VideoEnhancementOptions(
            isTonemappingEnabled = VideoEnhancementEffect.TONEMAP in selectedEffects,
            isDeblurAndDenoiseVideoEnabled = isDeblurEnabled,
            isUpscaleVideoEnabled = VideoEnhancementEffect.UPSCALE in selectedEffects,
        )
    }

    private fun deletePreviewFile() {
        previewFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        previewFile = null
    }

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
        getApplication<Application>().getString(resId, *formatArgs)
}

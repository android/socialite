package com.google.android.samples.socialite.ui.mediaenhancement

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.media.effect.enhancement.Enhancement
import com.google.android.gms.media.effect.enhancement.EnhancementClient
import com.google.android.gms.media.effect.enhancement.EnhancementMode
import com.google.android.gms.media.effect.enhancement.EnhancementOptions
import com.google.android.gms.media.effect.enhancement.EnhancementSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.google.android.samples.socialite.repository.ChatRepository
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri
import kotlinx.coroutines.withContext

private const val TAG = "EnhancementViewModel"

// Data class to hold all information about an image
data class ImageInfo(
    val bitmap: Bitmap? = null, val latency: Long? = null, val qualityScore: Int? = null
)

// Defines the state of the UI
data class UiState(
    val originalImage: ImageInfo? = null,
    val enhancedImage: ImageInfo? = null,
    val isLoading: Boolean = false,
    val selectedOptions: Set<String> = setOf("Tonemap"),
    val enhancementMode: Int = EnhancementMode.BITMAP,
    val isModuleInstalling: Boolean = false,
    val moduleInstallProgress: Int = 0,
    val moduleInstallError: String? = null,
    val isModuleReady: Boolean = false,
    val moduleStatus: String = "Unknown",
    val isDeviceSupported: Boolean = true,
    val enhancementError: String? = null
)

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.R)
class EnhancementViewModel @Inject constructor(
    application: Application,
    private val chatRepository: ChatRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.R)
    private val enhancementClient: EnhancementClient = Enhancement.getClient(application)
    private val enhancementExecutor = Executors.newSingleThreadExecutor()
    private var enhancementSession: EnhancementSession? = null

    init {
        checkAndInstallModule()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkAndInstallModule() {
        viewModelScope.launch {
            _uiState.update { it.copy(moduleStatus = "Checking for device support...") }
            try {
                if (!enhancementClient.isDeviceSupportedAsync()) {
                    _uiState.update {
                        it.copy(
                            isDeviceSupported = false, moduleStatus = "Device not supported"
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
                            moduleStatus = "Not Installed"
                        )
                    }


                    _uiState.update { it.copy(moduleStatus = "Installing...") }
                    val installed = enhancementClient.installModuleAsync { progress ->
                        _uiState.update { it.copy(moduleInstallProgress = progress) }
                    }
                    if (!installed) {
                        _uiState.update {
                            it.copy(
                                isModuleInstalling = false,
                                moduleInstallError = "Module installation failed or cancelled.",
                                moduleStatus = "Install Failed"
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isModuleInstalling = false,
                            moduleInstallProgress = 100,
                            isModuleReady = true,
                            moduleStatus = "Installed"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isModuleInstalling = false,
                            moduleInstallProgress = 100,
                            isModuleReady = true,
                            moduleStatus = "Installed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isModuleInstalling = false,
                        moduleInstallError = "Failed to check or install module: ${e.message}",
                        moduleStatus = "Error"
                    )
                }
            }
        }
    }

    private suspend fun createSession(): EnhancementSession? {
        if (!_uiState.value.isDeviceSupported) return null

        val originalBitmap = _uiState.value.originalImage?.bitmap ?: return null

        return try {
            val options = getEnhancementOptionsFor(originalBitmap, _uiState.value.selectedOptions)
            enhancementClient.createSessionAsync(options, enhancementExecutor)
        } catch (e: Exception) {
            _uiState.update { it.copy(enhancementError = "Failed to create session: ${e.message}") }
            null
        }
    }

    override fun onCleared() {
        enhancementSession?.release()
        enhancementSession = null
        enhancementExecutor.shutdown()
        super.onCleared()
    }

    fun onImageSelected(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    originalImage = null,
                    enhancedImage = null,
                    enhancementError = null
                )
            }
            // Release any previous session, since we have a new image.
            enhancementSession?.release()
            enhancementSession = null

            val originalBitmap = decodeBitmapFromUri(uri, context)

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI.")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        enhancementError = "Failed to load image."
                    )
                }
                return@launch
            }

            val originalImageInfo = ImageInfo(bitmap = originalBitmap)
            // Show the original image and stop loading. Session will be created on demand.
            _uiState.update { it.copy(originalImage = originalImageInfo, isLoading = false) }
        }
    }

    fun onOptionSelected(option: String) {
        val currentOptions = _uiState.value.selectedOptions
        val newOptions = if (option in currentOptions) {
            currentOptions - option
        } else {
            currentOptions + option
        }
        _uiState.update { it.copy(selectedOptions = newOptions) }
    }

    fun setEnhancementMode(mode: Int) {
        _uiState.update { it.copy(enhancementMode = mode) }
    }

    fun enhanceImage() {
        val originalBitmap = _uiState.value.originalImage?.bitmap ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, enhancementError = null) }
            try {
                processImage(originalBitmap)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun processImage(bitmap: Bitmap) {
        try {
            // If the session doesn't exist, create it now.
            if (enhancementSession == null) {
                enhancementSession = createSession()
            }

            // If session creation failed or is not possible, just return.
            // The createSession function will have set the error message.
            val session = enhancementSession ?: return

            val options = getEnhancementOptionsFor(bitmap, _uiState.value.selectedOptions)
            val enhancementStartTime = System.currentTimeMillis()
            val enhancedBitmap = session.processBitmapAsync(bitmap, options)
            val enhancementLatency = System.currentTimeMillis() - enhancementStartTime

            _uiState.update {
                it.copy(
                    enhancedImage = ImageInfo(bitmap = enhancedBitmap, latency = enhancementLatency)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Enhancement failed: ${e.message}", e)
            _uiState.update {
                it.copy(
                    enhancedImage = null, enhancementError = "Enhancement failed: ${e.message}"
                )
            }
        }
    }

    private fun getEnhancementOptionsFor(
        bitmap: Bitmap,
        selectedOptions: Set<String>
    ): EnhancementOptions {
        return EnhancementOptions(
            bitmap.width,
            bitmap.height,
            _uiState.value.enhancementMode,
            "Tonemap" in selectedOptions,
            "Deblur & DeNoise" in selectedOptions,
            false,
            "Upscale" in selectedOptions,
            false
        )
    }

    private fun decodeBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from URI", e)
            null
        }
    }

    fun saveEnhancedImage(messageId: Long, onComplete: () -> Unit) {
        val enhancedBitmap = _uiState.value.enhancedImage?.bitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val directory = File(context.filesDir, "media")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Prevent storage bloat by deleting previous enhanced files
                directory.listFiles()?.forEach { file ->
                    if (file.name.startsWith("enhanced_")) {
                        file.delete()
                    }
                }

                val filename = "enhanced_${System.currentTimeMillis()}.jpg"
                val file = File(directory, filename)
                FileOutputStream(file).use { out ->
                    var success = enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    if (!success) {
                        Log.w(TAG, "Failed to compress hardware bitmap, trying software fallback.")
                        val swBitmap = enhancedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        if (swBitmap != null) {
                            success = swBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }
                    }
                    if (!success) {
                        Log.e(TAG, "Bitmap compression completely failed!")
                    }
                }

                val newUri = file.toUri().toString()
                Log.d(TAG, "Saved enhanced image to $newUri, updating message $messageId")
                chatRepository.updateMessageMediaUri(messageId, newUri)

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save image", e)
            }
        }
    }
}

package com.google.android.samples.socialite.ui.mediaenhancement

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.common.api.Status
import com.google.android.gms.media.effect.enhancement.EnhancementCallback
import com.google.android.gms.media.effect.enhancement.EnhancementClient
import com.google.android.gms.media.effect.enhancement.EnhancementOptions
import com.google.android.gms.media.effect.enhancement.EnhancementSession
import com.google.android.gms.media.effect.enhancement.EnhancementSessionCallback
import com.google.android.gms.media.effect.enhancement.Enhancement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log
import android.os.Build
import androidx.annotation.RequiresApi


class EnhancementFailedException(val errorCode: Int, message: String) : Exception(message)

/**
 * A modern coroutine wrapper for the enhancement process.
 *
 * This suspend function encapsulates the entire callback-based process of creating a session,
 * processing a bitmap, and handling success or failure.
 *
 * @param context The application context.
 * @param bitmap The input bitmap to enhance.
 * @param options The enhancement options to apply.
 * @param executor The executor on which to run the callbacks.
 * @return The enhanced [Bitmap] on success.
 * @throws [EnhancementFailedException] if any step of the process fails.
 */
/**
 * Extension to create an enhancement session asynchronously.
 */
@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementClient.createSessionAsync(
    options: EnhancementOptions,
    executor: Executor,
): EnhancementSession = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { continuation ->
        val callback = object : EnhancementSessionCallback {
            override fun onSessionCreated(session: EnhancementSession) {
                continuation.resume(session)
            }

            override fun onSessionCreationFailed(status: Status) {
                continuation.resumeWithException(
                    Exception("Session creation failed: ${status.statusMessage} (${status.statusCode})"),
                )
            }

            override fun onSessionDestroyed() {
                // Log or handle if needed
            }

            override fun onSessionDisconnected(status: Status) {
                // Log or handle if needed
            }
        }

        this@createSessionAsync.createSession(options, callback)
            .addOnFailureListener(executor) { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
    }
}

/**
 * Extension to process a bitmap using an existing session.
 */
@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementSession.processBitmapAsync(
    bitmap: Bitmap,
    options: EnhancementOptions,
): Bitmap = suspendCancellableCoroutine { continuation ->
    val callback = object : EnhancementCallback {
        override fun onBitmapProcessed(bitmap: Bitmap) {
            continuation.resume(bitmap)
        }

        override fun onError(statusCode: Int) {
            continuation.resumeWithException(
                Exception("Processing failed with status code: $statusCode"),
            )
        }

        override fun onSurfaceProcessed(timestamp: Long) { /* Not used in bitmap flow */
        }
    }

    this.process(bitmap, options, callback)
}

@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementClient.installModuleAsync(onProgress: (Int) -> Unit): Boolean =
    suspendCancellableCoroutine { continuation ->
        val callback = object : EnhancementClient.InstallStatusCallback {
            override fun onDownloadPending() {
                Log.d("EnhancementUtils", "onDownloadPending")
            }

            override fun onDownloadStart() {
                Log.d("EnhancementUtils", "onDownloadStart")
            }

            override fun onDownloadPaused() {
                Log.d("EnhancementUtils", "onDownloadPaused")
            }

            override fun onDownloadProgressUpdate(progress: Int) {
                Log.d("EnhancementUtils", "onDownloadProgressUpdate: $progress")
                onProgress(progress)
            }

            override fun onDownloadComplete() {
                Log.d("EnhancementUtils", "onDownloadComplete")
            }

            override fun onInstalled() {
                Log.d("EnhancementUtils", "onInstalled")
            }

            override fun onCancelled() {
                Log.d("EnhancementUtils", "onCancelled")
                if (continuation.isActive) continuation.resume(false)
            }

            override fun onError(description: String) {
                Log.e("EnhancementUtils", "onError: $description")
                if (continuation.isActive) continuation.resumeWithException(Exception(description))
            }
        }

        this.installModule(callback)
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resumeWithException(e)
            }
    }

@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementClient.isModuleInstalledAsync(): Boolean =
    suspendCancellableCoroutine { continuation ->
        this.isModuleInstalled()
            .addOnSuccessListener { result -> continuation.resume(result) }
            .addOnFailureListener { e -> continuation.resumeWithException(e) }
    }

@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementClient.isDeviceSupportedAsync(): Boolean =
    suspendCancellableCoroutine { continuation ->
        this.isDeviceSupported()
            .addOnSuccessListener { result -> continuation.resume(result) }
            .addOnFailureListener { e -> continuation.resumeWithException(e) }
    }

object EnhancementSupportManager {
    private var isSupported: Boolean? = null

    suspend fun checkSupport(context: Context): Boolean {
        if (isSupported != null) return isSupported!!

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val client = Enhancement.getClient(context.applicationContext)

                // First check if it's already installed
                val installed = client.isModuleInstalledAsync()
                if (!installed) {
                    // If not installed, attempt to install it before querying device support
                    try {
                        client.installModuleAsync { progress ->
                            // Silent background install progress
                        }
                    } catch (e: Exception) {
                        Log.e("EnhancementSupport", "Failed to silently install module", e)
                    }
                }

                isSupported = client.isDeviceSupportedAsync()
                return isSupported!!
            } catch (e: Exception) {
                Log.e("EnhancementSupport", "Error checking support", e)
                isSupported = false
                return false
            }
        } else {
            isSupported = false
            return false
        }
    }
}

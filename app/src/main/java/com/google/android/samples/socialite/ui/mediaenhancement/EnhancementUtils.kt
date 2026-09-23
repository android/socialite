package com.google.android.samples.socialite.ui.mediaenhancement

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import com.google.android.gms.common.api.Status
import com.google.android.gms.media.effect.enhancement.Enhancement
import com.google.android.gms.media.effect.enhancement.EnhancementCallback
import com.google.android.gms.media.effect.enhancement.EnhancementClient
import com.google.android.gms.media.effect.enhancement.EnhancementOptions
import com.google.android.gms.media.effect.enhancement.EnhancementSession
import com.google.android.gms.media.effect.enhancement.EnhancementSessionCallback
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class EnhancementFailedException(message: String) : Exception(message)

/**
 * Extension to create an [EnhancementSession] asynchronously.
 */
@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementClient.createSessionAsync(
    options: EnhancementOptions,
    executor: Executor,
): EnhancementSession = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { continuation ->
        val callback = object : EnhancementSessionCallback {
            override fun onSessionCreated(session: EnhancementSession) {
                // The SDK hands ownership of the session to this callback and keeps no reference
                // of its own. If the caller has already been cancelled, resuming would drop the
                // session somewhere unreachable, so the resume is paired with a release.
                continuation.resume(session) { _, value, _ -> value.releaseQuietly() }
            }

            override fun onSessionCreationFailed(status: Status) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        Exception(
                            "Session creation failed: ${status.statusMessage} (${status.statusCode})",
                        ),
                    )
                }
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

@RequiresApi(Build.VERSION_CODES.R)
private fun EnhancementSession.releaseQuietly() {
    try {
        release()
    } catch (e: Exception) {
        Log.w("EnhancementUtils", "Failed to release an orphaned enhancement session", e)
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
            if (continuation.isActive) {
                continuation.resume(bitmap)
            }
        }

        override fun onError(statusCode: Int) {
            if (continuation.isActive) {
                continuation.resumeWithException(
                    Exception("Processing failed with status code: $statusCode"),
                )
            }
        }

        override fun onCancelled(statusCode: Int) {
            if (continuation.isActive) {
                continuation.cancel(
                    Exception("Processing cancelled with status code: $statusCode"),
                )
            }
        }

        override fun onSurfaceProcessed(timestamp: Long) {
            /* Not used in bitmap flow */
        }
    }

    this.process(bitmap, options, callback)
}

/** Events reported by the SDK while it processes frames in surface mode. */
sealed interface SurfaceFrameEvent {
    /** A frame was rendered to the output surface. */
    data class Processed(val timestamp: Long) : SurfaceFrameEvent

    /** Processing failed and no further frames will be produced. */
    data class Failed(val statusCode: Int) : SurfaceFrameEvent

    /** Processing was cancelled and no further frames will be produced. */
    data class Cancelled(val statusCode: Int) : SurfaceFrameEvent
}

/**
 * Extension that attaches [surface] as the session output and bridges the frame callbacks into a
 * [Channel].
 *
 * Surface mode requires the producer (the video decoder) to wait until frame n has been processed
 * before feeding frame n + 1, otherwise frames are silently dropped. Receiving from the returned
 * channel is the suspending equivalent of that wait, and unlike a lock it cooperates with
 * coroutine cancellation.
 *
 * The caller owns the returned channel and should close it once the session is done.
 */
@RequiresApi(Build.VERSION_CODES.R)
fun EnhancementSession.setOutputSurfaceWithEvents(
    surface: Surface,
    options: EnhancementOptions,
): Channel<SurfaceFrameEvent> {
    val events = Channel<SurfaceFrameEvent>(Channel.CONFLATED)
    val callback = object : EnhancementCallback {
        override fun onSurfaceProcessed(timestamp: Long) {
            events.trySend(SurfaceFrameEvent.Processed(timestamp))
        }

        override fun onBitmapProcessed(bitmap: Bitmap) {
            /* Not used in surface flow */
        }

        override fun onError(statusCode: Int) {
            events.trySend(SurfaceFrameEvent.Failed(statusCode))
        }

        override fun onCancelled(statusCode: Int) {
            events.trySend(SurfaceFrameEvent.Cancelled(statusCode))
        }
    }

    this.setOutputSurface(surface, options, callback)
    return events
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
                if (continuation.isActive) continuation.resume(true)
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
                if (result && continuation.isActive) {
                    continuation.resume(true)
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resumeWithException(e)
            }
    }

@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementClient.isModuleInstalledAsync(): Boolean =
    suspendCancellableCoroutine { continuation ->
        this.isModuleInstalled()
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resumeWithException(e)
            }
    }

@RequiresApi(Build.VERSION_CODES.R)
suspend fun EnhancementClient.isDeviceSupportedAsync(): Boolean =
    suspendCancellableCoroutine { continuation ->
        this.isDeviceSupported()
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resumeWithException(e)
            }
    }

object EnhancementSupportManager {
    @Volatile
    private var isSupported: Boolean? = null

    suspend fun checkSupport(context: Context): Boolean {
        isSupported?.let { return it }

        val supported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val client = Enhancement.getClient(context.applicationContext)
                client.isDeviceSupportedAsync()
            } catch (e: Exception) {
                Log.e("EnhancementSupport", "Error checking support", e)
                false
            }
        } else {
            false
        }
        isSupported = supported
        return supported
    }
}

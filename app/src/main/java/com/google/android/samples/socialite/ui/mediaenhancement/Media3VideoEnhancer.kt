package com.google.android.samples.socialite.ui.mediaenhancement

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.DefaultGlFrameProcessor
import androidx.media3.effect.playservices.EnhancementSessionDecorator
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "Media3VideoEnhancer"
private const val PROGRESS_POLL_INTERVAL_MS = 100L

/**
 * Media3 alternative to [VideoEnhancementRepository].
 *
 * Instead of driving `MediaCodec` and the `EnhancementSession` by hand, this exports with
 * [Transformer] and lets [EnhancementSessionDecorator] own the session: it creates it on the first
 * frame, feeds every frame through it, and releases it when the export ends.
 *
 * Only one enhancement session can be active per app, so any session pre-warmed by
 * [VideoEnhancementRepository] must be released before calling [enhance].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(UnstableApi::class, ExperimentalApi::class)
class Media3VideoEnhancer(private val context: Context) {

    /**
     * Enhances [inputUri] into [outputFile] and emits the same progress events as
     * [VideoEnhancementRepository.enhance]. Cancelling the collector cancels the export.
     *
     * In [PhaseTimings], `configureMs` covers building the frame processor and the [Transformer],
     * and `transcodeMs` covers everything from [Transformer.start] to
     * [Transformer.Listener.onCompleted]: session creation, decode, enhance, encode, audio and
     * muxing all happen inside it and are not observable separately.
     */
    fun enhance(
        inputUri: Uri,
        outputFile: File,
        options: VideoEnhancementOptions,
    ): Flow<VideoEnhancementProgress> = callbackFlow {
        trySend(VideoEnhancementProgress.Preparing)

        val configureStart = SystemClock.elapsedRealtime()
        val frameProcessorFactory = EnhancementSessionDecorator.Builder(
            context,
            DefaultGlFrameProcessor.Factory.Builder(context).build(),
        )
            .setTonemappingEnabled(options.isTonemappingEnabled)
            .setDeblurAndDenoiseVideoEnabled(options.isDeblurAndDenoiseVideoEnabled)
            .setUpscaleVideoEnabled(options.isUpscaleVideoEnabled)
            .build()

        var exportStart = 0L
        var configureMs = 0L
        val transformer = Transformer.Builder(context)
            .setFrameProcessorFactory(frameProcessorFactory)
            // Match the custom pipeline's output codec so both paths are compared like for like.
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        val timings = PhaseTimings(
                            configureMs = configureMs,
                            transcodeMs = SystemClock.elapsedRealtime() - exportStart,
                            frameCount = exportResult.videoFrameCount,
                        )
                        Log.i(
                            TAG,
                            "Media3 export: configure=${timings.configureMs}ms " +
                                "export=${timings.transcodeMs}ms " +
                                "frames=${timings.frameCount} perFrame=${timings.perFrameMs}ms " +
                                "output=${exportResult.width}x${exportResult.height} " +
                                "encoder=${exportResult.videoEncoderName}",
                        )
                        trySend(VideoEnhancementProgress.Completed(outputFile.absolutePath, timings))
                        close()
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        Log.e(TAG, "Media3 export failed", exportException)
                        close(exportException)
                    }
                },
            )
            .build()
        configureMs = SystemClock.elapsedRealtime() - configureStart

        exportStart = SystemClock.elapsedRealtime()
        transformer.start(MediaItem.fromUri(inputUri), outputFile.absolutePath)

        // Transformer only exposes progress by polling.
        val progressJob = launch {
            val progressHolder = ProgressHolder()
            while (isActive) {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    trySend(VideoEnhancementProgress.Transcoding(progressHolder.progress))
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }

        awaitClose {
            progressJob.cancel()
            // A no-op once the export has completed.
            transformer.cancel()
        }
    }.flowOn(Dispatchers.Main)
}

package com.google.android.samples.socialite.ui.mediaenhancement

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import com.google.android.gms.media.effect.enhancement.EnhancementClient
import com.google.android.gms.media.effect.enhancement.EnhancementMode
import com.google.android.gms.media.effect.enhancement.EnhancementOptions
import com.google.android.gms.media.effect.enhancement.EnhancementSession
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "VideoEnhancementRepo"

/** Maximum output resolution this sample is willing to produce (2K). */
const val MAX_OUTPUT_WIDTH = 2560
const val MAX_OUTPUT_HEIGHT = 1440

/** The video upscaler is a fixed 2x, it is not configurable. */
const val UPSCALE_FACTOR = 2

private const val CODEC_TIMEOUT_US = 10_000L
private const val REF_PIXELS = 1920 * 1080
private const val REF_BITRATE = 8_000_000L
private const val REF_FPS = 30
private const val MIN_BITRATE = 2_000_000
private const val MAX_BITRATE = 40_000_000
private const val DEFAULT_FRAME_RATE = 30
private const val AUDIO_BUFFER_SIZE = 1024 * 1024
private const val VIDEO_MIME_PREFIX = "video/"
private const val AUDIO_MIME_PREFIX = "audio/"
private const val FALLBACK_VIDEO_MIME = "video/avc"
private const val OUTPUT_VIDEO_MIME = "video/avc"

/**
 * The three effects that apply to video. The two photo flags of [EnhancementOptions] must stay
 * disabled in [EnhancementMode.SURFACE] mode.
 */
data class VideoEnhancementOptions(
    val isTonemappingEnabled: Boolean = false,
    val isDeblurAndDenoiseVideoEnabled: Boolean = false,
    val isUpscaleVideoEnabled: Boolean = false,
)

/**
 * Rotation corrected description of a source video.
 *
 * [width] and [height] are the logical (displayed) dimensions: for a video recorded with a 90° or
 * 270° rotation the stored dimensions are swapped.
 */
data class VideoInfo(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val frameRate: Int,
    val durationUs: Long,
    val mimeType: String,
)

/**
 * Wall clock breakdown of a single enhancement run, in milliseconds.
 *
 * Only [transcodeMs] is actual frame processing. Everything else is fixed overhead that the
 * user still waits through, which is why the end to end figure is larger than the per frame cost
 * multiplied by [frameCount] would suggest.
 */
data class PhaseTimings(
    /** Reading the dimensions, rotation and duration of the source. */
    val probeMs: Long = 0,
    /** [EnhancementClient.createSession], zero when the pre-warmed session is reused. */
    val sessionMs: Long = 0,
    /** False when the pre-warm was missed and the session had to be created on the hot path. */
    val sessionReused: Boolean = false,
    /** Creating and starting the decoder, the encoder, the surfaces and the muxer. */
    val configureMs: Long = 0,
    /** The decode, enhance and encode loop. This is the only phase that scales with duration. */
    val transcodeMs: Long = 0,
    /** Copying the audio track across untouched. */
    val audioMs: Long = 0,
    /** Stopping the codecs and finalizing the container. The file is not playable before this. */
    val teardownMs: Long = 0,
    val frameCount: Int = 0,
) {
    /** Everything the user waits through, from the tap to a playable file. */
    val totalMs: Long
        get() = probeMs + sessionMs + configureMs + transcodeMs + audioMs + teardownMs

    /** Fixed cost that does not scale with the length of the video. */
    val overheadMs: Long
        get() = probeMs + sessionMs + configureMs + teardownMs

    val perFrameMs: Long
        get() = if (frameCount > 0) transcodeMs / frameCount else 0

    override fun toString(): String =
        "total=${totalMs}ms (probe=$probeMs session=$sessionMs" +
            "${if (sessionReused) " reused" else " created"} configure=$configureMs " +
            "transcode=$transcodeMs audio=$audioMs teardown=$teardownMs) " +
            "frames=$frameCount perFrame=${perFrameMs}ms overhead=${overheadMs}ms"
}

/** Progress reported while enhancing a video. */
sealed interface VideoEnhancementProgress {
    /** The session is being created and the codecs configured. */
    data object Preparing : VideoEnhancementProgress

    /** [percent] of the source timeline has been transcoded. */
    data class Transcoding(val percent: Int) : VideoEnhancementProgress

    /** The enhanced video is fully written to [outputPath]. */
    data class Completed(
        val outputPath: String,
        val timings: PhaseTimings,
    ) : VideoEnhancementProgress
}

/**
 * Transcode engine for on-device video enhancement.
 *
 * The pipeline is `MediaExtractor -> MediaCodec decoder -> EnhancementSession -> MediaCodec
 * encoder -> MediaMuxer`, where the decoder renders into the session input surface and the session
 * renders into the encoder input surface.
 */
@RequiresApi(Build.VERSION_CODES.R)
class VideoEnhancementRepository(
    private val context: Context,
    private val enhancementClient: EnhancementClient,
    private val enhancementExecutor: Executor,
) {

    private data class SessionKey(
        val width: Int,
        val height: Int,
        val options: VideoEnhancementOptions,
    )

    private val sessionMutex = Mutex()
    private val stateLock = Any()

    private var session: EnhancementSession? = null
    private var sessionKey: SessionKey? = null

    @Volatile
    private var cancelled = false

    /** True when the source is small enough to be enhanced at all. */
    fun isSourceSupported(info: VideoInfo): Boolean =
        longEdge(info) <= MAX_OUTPUT_WIDTH && shortEdge(info) <= MAX_OUTPUT_HEIGHT

    /** True when a 2x upscale still fits inside the 2K output cap. */
    fun isUpscaleSupported(info: VideoInfo): Boolean =
        longEdge(info) * UPSCALE_FACTOR <= MAX_OUTPUT_WIDTH &&
            shortEdge(info) * UPSCALE_FACTOR <= MAX_OUTPUT_HEIGHT

    /**
     * Reads the rotation corrected dimensions and timing of [uri].
     *
     * @return the [VideoInfo], or null when the video has no readable video track.
     */
    suspend fun probeVideo(uri: Uri): VideoInfo? = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            val trackIndex = extractor.findTrackIndex(VIDEO_MIME_PREFIX)
            if (trackIndex < 0) {
                Log.e(TAG, "No video track found in $uri")
                return@withContext null
            }

            val format = extractor.getTrackFormat(trackIndex)
            val storedWidth = format.getInteger(MediaFormat.KEY_WIDTH)
            val storedHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
            val rotation = if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                format.getInteger(MediaFormat.KEY_ROTATION)
            } else {
                retrieveRotation(uri)
            }
            val swapDimensions = rotation == 90 || rotation == 270

            VideoInfo(
                width = if (swapDimensions) storedHeight else storedWidth,
                height = if (swapDimensions) storedWidth else storedHeight,
                rotationDegrees = rotation,
                frameRate = format.getIntegerOrDefault(
                    MediaFormat.KEY_FRAME_RATE,
                    DEFAULT_FRAME_RATE,
                ),
                durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    format.getLong(MediaFormat.KEY_DURATION)
                } else {
                    0L
                },
                mimeType = format.getString(MediaFormat.KEY_MIME) ?: FALLBACK_VIDEO_MIME,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to probe video $uri", e)
            null
        } finally {
            extractor?.releaseQuietly()
        }
    }

    /**
     * Creates the session ahead of time so the first frame is not delayed by session creation.
     *
     * `createSession` binds both the dimensions and the effect flags, so changing any of them
     * invalidates the session and forces a full recreate.
     */
    suspend fun prepareSession(
        width: Int,
        height: Int,
        options: VideoEnhancementOptions,
    ) {
        obtainSession(width, height, options)
    }

    /**
     * Enhances [inputUri] into [outputFile].
     *
     * The returned flow runs the whole pipeline on [Dispatchers.IO] and emits progress as the
     * source timeline is consumed. Collecting is cancellable: cancelling the collector stops the
     * pipeline and deletes the partially written output.
     */
    fun enhance(
        inputUri: Uri,
        outputFile: File,
        options: VideoEnhancementOptions,
    ): Flow<VideoEnhancementProgress> = flow {
        cancelled = false
        emit(VideoEnhancementProgress.Preparing)

        val probeStart = SystemClock.elapsedRealtime()
        val info = probeVideo(inputUri)
            ?: throw IOException("Unable to read the source video")
        val probeMs = SystemClock.elapsedRealtime() - probeStart
        if (!isSourceSupported(info)) {
            throw IOException("Source video exceeds the ${MAX_OUTPUT_WIDTH}x$MAX_OUTPUT_HEIGHT cap")
        }

        // Guard against a stale request for an upscale that no longer fits the output cap.
        val effectiveOptions = options.copy(
            isUpscaleVideoEnabled = options.isUpscaleVideoEnabled && isUpscaleSupported(info),
        )
        val enhancementOptions = enhancementOptionsOf(info.width, info.height, effectiveOptions)
        val sessionStart = SystemClock.elapsedRealtime()
        val (activeSession, sessionReused) =
            obtainSession(info.width, info.height, effectiveOptions)
        val sessionMs = SystemClock.elapsedRealtime() - sessionStart

        val scale = if (effectiveOptions.isUpscaleVideoEnabled) UPSCALE_FACTOR else 1
        try {
            val timings = transcode(
                inputUri = inputUri,
                outputFile = outputFile,
                info = info,
                session = activeSession,
                enhancementOptions = enhancementOptions,
                outWidth = info.width * scale,
                outHeight = info.height * scale,
                onProgress = { percent -> emit(VideoEnhancementProgress.Transcoding(percent)) },
            ).copy(
                probeMs = probeMs,
                sessionMs = sessionMs,
                sessionReused = sessionReused,
            )
            Log.i(TAG, "Enhancement timings: $timings")
            emit(VideoEnhancementProgress.Completed(outputFile.absolutePath, timings))
        } finally {
            // The session cannot be reused once its output surface is gone.
            releaseSession()
        }
    }.flowOn(Dispatchers.IO)

    /** Requests cancellation of an in-flight enhancement. Safe to call from any thread. */
    fun cancel() {
        cancelled = true
        val activeSession = synchronized(stateLock) { session }
        try {
            activeSession?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel the enhancement session", e)
        }
    }

    /** Releases the current session, if any. */
    fun releaseSession() {
        val toRelease = synchronized(stateLock) {
            val current = session
            session = null
            sessionKey = null
            current
        }
        try {
            toRelease?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release the enhancement session", e)
        }
    }

    /**
     * Returns a session matching [width], [height] and [options], plus whether the cached
     * pre-warmed session was reused. A false reuse flag means the caller paid full session
     * creation cost inline.
     */
    private suspend fun obtainSession(
        width: Int,
        height: Int,
        options: VideoEnhancementOptions,
    ): Pair<EnhancementSession, Boolean> = sessionMutex.withLock {
        val key = SessionKey(width, height, options)
        synchronized(stateLock) {
            val existing = session
            if (existing != null && sessionKey == key) {
                return@withLock existing to true
            }
        }

        releaseSession()
        val created = enhancementClient.createSessionAsync(
            enhancementOptionsOf(width, height, options),
            enhancementExecutor,
        )
        synchronized(stateLock) {
            session = created
            sessionKey = key
        }
        created to false
    }

    /**
     * Builds the SDK options.
     *
     * Named arguments are deliberate. [EnhancementOptions] takes four consecutive booleans in the
     * order photo-deblur, video-deblur, photo-upscale, video-upscale, so positional construction
     * silently binds a video flag to its photo counterpart if the SDK ever reorders them. The two
     * photo flags must stay false in surface mode.
     */
    private fun enhancementOptionsOf(
        width: Int,
        height: Int,
        options: VideoEnhancementOptions,
    ): EnhancementOptions = EnhancementOptions(
        width = width,
        height = height,
        enhancementMode = EnhancementMode.SURFACE,
        isTonemappingEnabled = options.isTonemappingEnabled,
        isDeblurAndDenoisePhotoEnabled = false,
        isDeblurAndDenoiseVideoEnabled = options.isDeblurAndDenoiseVideoEnabled,
        isUpscalePhotoEnabled = false,
        isUpscaleVideoEnabled = options.isUpscaleVideoEnabled,
    )

    private suspend fun transcode(
        inputUri: Uri,
        outputFile: File,
        info: VideoInfo,
        session: EnhancementSession,
        enhancementOptions: EnhancementOptions,
        outWidth: Int,
        outHeight: Int,
        onProgress: suspend (Int) -> Unit,
    ): PhaseTimings {
        var extractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var encoderInputSurface: Surface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var frameEvents: Channel<SurfaceFrameEvent>? = null
        var completed = false

        val configureStart = SystemClock.elapsedRealtime()
        var configureMs = 0L
        var transcodeMs = 0L
        var audioMs = 0L
        var teardownMs = 0L
        var frameCount = 0

        try {
            val videoExtractor = MediaExtractor()
            extractor = videoExtractor
            videoExtractor.setDataSource(context, inputUri, null)
            val videoTrackIndex = videoExtractor.findTrackIndex(VIDEO_MIME_PREFIX)
            if (videoTrackIndex < 0) {
                throw IOException("No video track found in the source video")
            }
            videoExtractor.selectTrack(videoTrackIndex)

            val inputFormat = videoExtractor.getTrackFormat(videoTrackIndex)
            if (info.rotationDegrees != 0) {
                inputFormat.setInteger(MediaFormat.KEY_ROTATION, info.rotationDegrees)
            }

            val videoEncoder = createEncoder(outWidth, outHeight, info.frameRate)
            encoder = videoEncoder
            val inputSurface = videoEncoder.createInputSurface()
            encoderInputSurface = inputSurface
            videoEncoder.start()

            // The session renders into the encoder, the decoder renders into the session.
            val events = session.setOutputSurfaceWithEvents(inputSurface, enhancementOptions)
            frameEvents = events
            val videoDecoder = createDecoder(info.mimeType, inputFormat, session.getInputSurface())
            decoder = videoDecoder
            videoDecoder.start()

            val videoMuxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            )
            muxer = videoMuxer

            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()
            val decodedPts = mutableListOf<Long>()
            var encoderOutputFormat: MediaFormat? = null
            var extractorDone = false
            var decoderDone = false
            var encoderDone = false
            var decodedFrameCount = 0
            var encodedFrameCount = 0
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1

            configureMs = SystemClock.elapsedRealtime() - configureStart
            val loopStart = SystemClock.elapsedRealtime()

            while (!encoderDone && !cancelled) {
                currentCoroutineContext().ensureActive()

                // Feed the decoder.
                if (!extractorDone) {
                    val inputBufferIndex = videoDecoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = videoDecoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = videoExtractor.readSampleData(inputBuffer, 0)
                            if (sampleSize >= 0) {
                                videoDecoder.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    sampleSize,
                                    videoExtractor.sampleTime,
                                    videoExtractor.toCodecBufferFlags(),
                                )
                                videoExtractor.advance()
                            } else {
                                extractorDone = true
                                videoDecoder.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                            }
                        }
                    }
                }

                // Drain the decoder into the enhancement session.
                if (!decoderDone) {
                    val decoderOutputIndex =
                        videoDecoder.dequeueOutputBuffer(decoderBufferInfo, CODEC_TIMEOUT_US)
                    if (decoderOutputIndex >= 0) {
                        val isCodecConfig =
                            decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        if (isCodecConfig) {
                            videoDecoder.releaseOutputBuffer(decoderOutputIndex, false)
                        } else {
                            val render = decoderBufferInfo.size > 0
                            if (render) {
                                // The encoder output is re-stamped from this decode order list.
                                decodedPts.add(decoderBufferInfo.presentationTimeUs)
                                videoDecoder.releaseOutputBuffer(
                                    decoderOutputIndex,
                                    decoderBufferInfo.presentationTimeUs * 1000,
                                )
                            } else {
                                videoDecoder.releaseOutputBuffer(decoderOutputIndex, false)
                            }

                            // Frame n + 1 must not be fed before frame n has been processed,
                            // otherwise the session silently drops frames. This must also happen
                            // before end of stream is signalled: some decoders flag EOS on a real
                            // frame, and signalling first would race the encoder against the last
                            // frame still in flight through the session.
                            if (render) {
                                awaitFrameProcessed(events)
                                decodedFrameCount++
                                if (info.durationUs > 0) {
                                    val percent = (
                                        decoderBufferInfo.presentationTimeUs * 100 / info.durationUs
                                        ).toInt().coerceIn(0, 100)
                                    onProgress(percent)
                                }
                            }

                            val isEos =
                                decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            if (isEos) {
                                decoderDone = true
                                videoEncoder.signalEndOfInputStream()
                            }
                        }
                    }
                }

                // Drain the encoder into the muxer.
                val encoderOutputIndex =
                    videoEncoder.dequeueOutputBuffer(encoderBufferInfo, CODEC_TIMEOUT_US)
                if (encoderOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputFormat = videoEncoder.outputFormat
                } else if (encoderOutputIndex >= 0) {
                    val isCodecConfig =
                        encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (isCodecConfig) {
                        videoEncoder.releaseOutputBuffer(encoderOutputIndex, false)
                    } else {
                        val encodedData = videoEncoder.getOutputBuffer(encoderOutputIndex)
                        if (encodedData != null && encoderBufferInfo.size > 0 && muxerStarted) {
                            if (encodedFrameCount < decodedPts.size) {
                                encoderBufferInfo.presentationTimeUs = decodedPts[encodedFrameCount]
                            }
                            encodedData.position(encoderBufferInfo.offset)
                            encodedData.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                            videoMuxer.writeSampleData(
                                muxerVideoTrack,
                                encodedData,
                                encoderBufferInfo,
                            )
                            encodedFrameCount++
                        }
                        if (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoderDone = true
                        }
                        videoEncoder.releaseOutputBuffer(encoderOutputIndex, false)
                    }
                }

                // Tracks can only be added once the encoder has published its real output format.
                val outputFormat = encoderOutputFormat
                if (!muxerStarted && outputFormat != null) {
                    muxerVideoTrack = videoMuxer.addTrack(outputFormat)

                    val trackExtractor = MediaExtractor()
                    audioExtractor = trackExtractor
                    trackExtractor.setDataSource(context, inputUri, null)
                    val audioTrackIndex = trackExtractor.findTrackIndex(AUDIO_MIME_PREFIX)
                    if (audioTrackIndex >= 0) {
                        trackExtractor.selectTrack(audioTrackIndex)
                        muxerAudioTrack =
                            videoMuxer.addTrack(trackExtractor.getTrackFormat(audioTrackIndex))
                    } else {
                        trackExtractor.releaseQuietly()
                        audioExtractor = null
                    }

                    // The frames leaving the session are already rotation corrected.
                    videoMuxer.setOrientationHint(0)
                    videoMuxer.start()
                    muxerStarted = true
                }
            }

            transcodeMs = SystemClock.elapsedRealtime() - loopStart
            frameCount = decodedFrameCount

            if (cancelled) {
                throw CancellationException("Video enhancement was cancelled")
            }
            if (!muxerStarted || encodedFrameCount == 0) {
                throw IOException("Transcoding failed: no video frames were encoded")
            }

            val audioStart = SystemClock.elapsedRealtime()
            val trackExtractor = audioExtractor
            if (trackExtractor != null && muxerAudioTrack >= 0) {
                copyAudioTrack(trackExtractor, videoMuxer, muxerAudioTrack)
            }
            audioMs = SystemClock.elapsedRealtime() - audioStart

            onProgress(100)
            completed = true
            Log.i(TAG, "Transcoding completed, output=${outputFile.absolutePath}")
        } catch (e: CancellationException) {
            try {
                session.cancel()
            } catch (cancelError: Exception) {
                Log.w(TAG, "Failed to cancel the enhancement session", cancelError)
            }
            throw e
        } finally {
            // The container is only playable once the muxer has been stopped, so this teardown is
            // part of what the user waits for, not bookkeeping after the fact.
            val teardownStart = SystemClock.elapsedRealtime()
            frameEvents?.close()
            cleanupResources(
                decoder = decoder,
                encoder = encoder,
                encoderInputSurface = encoderInputSurface,
                muxer = muxer,
                muxerStarted = muxerStarted,
                extractor = extractor,
                audioExtractor = audioExtractor,
            )
            if (!completed) {
                outputFile.delete()
            }
            teardownMs = SystemClock.elapsedRealtime() - teardownStart
        }

        return PhaseTimings(
            configureMs = configureMs,
            transcodeMs = transcodeMs,
            audioMs = audioMs,
            teardownMs = teardownMs,
            frameCount = frameCount,
        )
    }

    /**
     * Suspends until the session finishes processing the frame just rendered into its input
     * surface, or throws if the session reports an error or cancellation.
     */
    private suspend fun awaitFrameProcessed(events: Channel<SurfaceFrameEvent>) {
        when (val event = events.receive()) {
            is SurfaceFrameEvent.Processed -> Unit
            is SurfaceFrameEvent.Failed ->
                throw IOException("Enhancement failed with status code ${event.statusCode}")

            is SurfaceFrameEvent.Cancelled ->
                throw CancellationException("Enhancement cancelled (${event.statusCode})")
        }
    }

    private suspend fun copyAudioTrack(
        audioExtractor: MediaExtractor,
        muxer: MediaMuxer,
        muxerAudioTrack: Int,
    ) {
        val buffer = ByteBuffer.allocate(AUDIO_BUFFER_SIZE)
        val bufferInfo = MediaCodec.BufferInfo()
        while (!cancelled) {
            currentCoroutineContext().ensureActive()
            val sampleSize = audioExtractor.readSampleData(buffer, 0)
            if (sampleSize < 0) {
                break
            }
            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = audioExtractor.sampleTime
            bufferInfo.flags = audioExtractor.toCodecBufferFlags()
            muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
            audioExtractor.advance()
        }
    }

    private fun createEncoder(width: Int, height: Int, frameRate: Int): MediaCodec {
        val format = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME, width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, calculateBitrate(width, height, frameRate))
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        format.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
        format.setInteger(MediaFormat.KEY_LATENCY, 0)
        format.setInteger(MediaFormat.KEY_PRIORITY, 0)

        // Standardize the encoder color settings for OpenGL surface rendering (BT.709 SDR limited
        // range) to prevent a red tint on 1080p and higher resolution videos.
        format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
        format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
        format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)

        val encoder = createCodec(OUTPUT_VIDEO_MIME, isEncoder = true)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return encoder
    }

    private fun createDecoder(
        mimeType: String,
        format: MediaFormat,
        surface: Surface,
    ): MediaCodec {
        val decoder = createCodec(mimeType, isEncoder = false)
        decoder.configure(format, surface, null, 0)
        return decoder
    }

    /** Prefers a hardware accelerated codec, falling back to the platform default. */
    private fun createCodec(mimeType: String, isEncoder: Boolean): MediaCodec {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (info.isEncoder != isEncoder || !info.isHardwareAccelerated) {
                continue
            }
            if (info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }) {
                Log.i(TAG, "Selected hardware codec ${info.name} for $mimeType")
                return MediaCodec.createByCodecName(info.name)
            }
        }

        Log.w(TAG, "No hardware codec for $mimeType, falling back to the default codec")
        return if (isEncoder) {
            MediaCodec.createEncoderByType(mimeType)
        } else {
            MediaCodec.createDecoderByType(mimeType)
        }
    }

    /**
     * Scales the reference bitrate (8 Mbps at 1920x1080@30) by pixel count and frame rate, clamped
     * to a sane range.
     */
    private fun calculateBitrate(width: Int, height: Int, frameRate: Int): Int {
        val pixels = width.toLong() * height.toLong()
        val bitrate = REF_BITRATE * pixels / REF_PIXELS * frameRate / REF_FPS
        return bitrate.coerceIn(MIN_BITRATE.toLong(), MAX_BITRATE.toLong()).toInt()
    }

    private fun retrieveRotation(uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve the video rotation", e)
            0
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release MediaMetadataRetriever", e)
            }
        }
    }

    private fun cleanupResources(
        decoder: MediaCodec?,
        encoder: MediaCodec?,
        encoderInputSurface: Surface?,
        muxer: MediaMuxer?,
        muxerStarted: Boolean,
        extractor: MediaExtractor?,
        audioExtractor: MediaExtractor?,
    ) {
        decoder?.releaseQuietly()
        encoder?.releaseQuietly()
        try {
            encoderInputSurface?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release the encoder input surface", e)
        }
        if (muxer != null) {
            if (muxerStarted) {
                try {
                    muxer.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stop the muxer", e)
                }
            }
            try {
                muxer.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release the muxer", e)
            }
        }
        extractor?.releaseQuietly()
        audioExtractor?.releaseQuietly()
    }

    private fun longEdge(info: VideoInfo): Int = maxOf(info.width, info.height)

    private fun shortEdge(info: VideoInfo): Int = minOf(info.width, info.height)
}

private fun MediaCodec.releaseQuietly() {
    try {
        stop()
    } catch (e: Exception) {
        Log.w(TAG, "Failed to stop a codec", e)
    }
    try {
        release()
    } catch (e: Exception) {
        Log.w(TAG, "Failed to release a codec", e)
    }
}

private fun MediaExtractor.releaseQuietly() {
    try {
        release()
    } catch (e: Exception) {
        Log.w(TAG, "Failed to release an extractor", e)
    }
}

private fun MediaExtractor.findTrackIndex(mimePrefix: String): Int {
    for (index in 0 until trackCount) {
        val mime = getTrackFormat(index).getString(MediaFormat.KEY_MIME)
        if (mime != null && mime.startsWith(mimePrefix)) {
            return index
        }
    }
    return -1
}

@RequiresApi(Build.VERSION_CODES.R)
private fun MediaExtractor.toCodecBufferFlags(): Int {
    val flags = sampleFlags
    var codecFlags = 0
    if (flags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
        codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
    }
    if (flags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
        codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
    }
    return codecFlags
}

private fun MediaFormat.getIntegerOrDefault(key: String, defaultValue: Int): Int = try {
    if (containsKey(key)) getInteger(key) else defaultValue
} catch (e: Exception) {
    // Some containers store these values with a different type.
    Log.w(TAG, "Failed to read $key from the media format", e)
    defaultValue
}

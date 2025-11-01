/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.samples.socialite.ui.metadata

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MediaExtractorCompat
import androidx.media3.exoplayer.MetadataRetriever
import java.nio.ByteBuffer
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "MetadataProcessor"

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
class MediaMetadataProcessor(private val context: Context, private val mediaPath: String) {

    fun populateMediaMetadata(): MediaMetadata {
        val mediaMetadata = MediaMetadata().apply {
            runMetadataRetriever(this)
            runMediaExtractor(this)
        }
        return mediaMetadata
    }

    /**
     * Retrieves container and track-level metadata using [MetadataRetriever].
     * This includes details like duration, mime types, bitrate, and resolution.
     */
    private fun runMetadataRetriever(
        mediaMetadata: MediaMetadata,
    ): MediaMetadata {
        val mediaItem = MediaItem.fromUri(mediaPath)
        val metadataRetriever = MetadataRetriever.Builder(context, mediaItem).build()
        try {
            mediaMetadata.containerAttributes.duration = retrieveDurationString(metadataRetriever)

            val trackGroupArray = metadataRetriever.retrieveTrackGroups().get()
            mediaMetadata.containerAttributes.numTracks = trackGroupArray.length

            for (i in 0 until trackGroupArray.length) {
                val trackGroup = trackGroupArray.get(i)
                for (j in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(j)
                    mediaMetadata.containerAttributes.containerMimeType =
                        format.containerMimeType.toString()

                    val trackMetadata = populateTrackAttributes(format).apply {
                        if (MimeTypes.isVideo(trackMimeType)) {
                            videoAttributes = populateVideoAttributes(format)
                        } else if (MimeTypes.isAudio(trackMimeType)) {
                            audioAttributes = populateAudioAttributes(format)
                        }
                    }
                    mediaMetadata.trackAttributesList.add(trackMetadata)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to retrieve metadata", e)
        } finally {
            // Ensure the retriever is always closed to free up resources.
            metadataRetriever.close()
        }
        return mediaMetadata
    }
    private fun retrieveDurationString(metadataRetriever: MetadataRetriever): String {
        val durationUs = metadataRetriever.retrieveDurationUs().get()
        val totalSeconds = (durationUs / 1_000_000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds)
        } else if (minutes > 0) {
            String.format(Locale.getDefault(), "%02dm %02ds", minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "00m %02ds", seconds)
        }
    }

    /**
     * Populates common track-level attributes from a [Format] object.
     */
    private fun populateTrackAttributes(format: Format): TrackAttributes {
        return TrackAttributes().apply {
            trackId = format.id
            codecs = format.codecs
            trackMimeType = format.sampleMimeType.toString()
            label = format.label
            language = format.language
            averageBitrateKbps = (format.averageBitrate / 1000.0).roundToInt()
            peakBitrateKbps = (format.peakBitrate / 1000.0).roundToInt()
        }
    }

    /**
     * Populates video-specific attributes from a [Format] object.
     */
    private fun populateVideoAttributes(format: Format): TrackAttributes.VideoAttributes {
        return TrackAttributes.VideoAttributes().apply {
            width = format.width
            height = format.height
            frameRate = format.frameRate.roundToInt()
        }
    }

    /**
     * Populates audio-specific attributes from a [Format] object.
     */
    private fun populateAudioAttributes(format: Format): TrackAttributes.AudioAttributes {
        return TrackAttributes.AudioAttributes().apply {
            sampleRateKHz = (format.sampleRate / 1000.0).roundToLong()
            channelCount = format.channelCount
            encoderDelay = format.encoderDelay
            encoderPadding = format.encoderPadding
        }
    }

    /**
     * Extracts encoded samples from the media file using [MediaExtractorCompat] to calculate
     * the total number of samples and their combined size.
     */
    private fun runMediaExtractor(
        mediaMetadata: MediaMetadata,
    ): MediaMetadata {
        var extractor: MediaExtractorCompat? = null
        try {
            extractor = MediaExtractorCompat(context)
            extractor.setDataSource(mediaPath)

            // Select all tracks to process samples from all of them.
            for (i in 0 until extractor.trackCount) {
                extractor.selectTrack(i)
            }

            // Allocate a large buffer to read sample data into. 1MB should be sufficient.
            val buffer = ByteBuffer.allocate(1 * 1024 * 1024)
            var totalSampleCount = 0L
            var totalSizeSamples = 0L

            // Loop through all the samples in the media file.
            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                // A negative track index indicates that there are no more samples.
                if (trackIndex < 0) {
                    break
                }

                val readBytes = extractor.readSampleData(buffer, 0)
                if (readBytes < 0) {
                    break
                }

                totalSampleCount++
                totalSizeSamples += readBytes

                // Advance to the next sample.
                extractor.advance()
            }

            mediaMetadata.containerAttributes.numSamples = totalSampleCount
            mediaMetadata.containerAttributes.totalSizeSamplesMb =
                (totalSizeSamples / (1024.0 * 1024.0)).roundToLong()
        } catch (e: Exception) {
            throw RuntimeException("Failed during media extraction", e)
        } finally {
            // Ensure the extractor is always released to free up resources.
            extractor?.release()
        }
        return mediaMetadata
    }

}

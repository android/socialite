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

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.android.samples.socialite.ui.metadata.components.MetadataItem

object AttributeUtil {

    fun getContainerAttributes(
        mediaPath: String,
        metadata: MediaMetadata,
    ): List<MetadataItem> = listOf(
        MetadataItem(
            "Source",
            mediaPath,
            "The absolute file path of the media on the device's storage.",
        ),
        MetadataItem(
            "Format",
            metadata.containerAttributes.containerMimeType.orDash(),
            "Container format (e.g., MP4, MKV) for media streams, defining data storage and organization.",
        ),
        MetadataItem(
            "Duration",
            metadata.containerAttributes.duration.orDash(),
            "Total playback length of the media content, displayed in a human-readable format (e.g., 10 min 5 sec).",
        ),
        MetadataItem(
            "No. of Tracks",
            metadata.containerAttributes.numTracks?.toString().orDash(),
            "Total count of audio, video, or subtitle tracks within the media container.",
        ),
        MetadataItem(
            "No. of Samples",
            metadata.containerAttributes.numSamples?.toString().orDash(),
            "Total number of audio, video, or subtitle samples within the media container.",
        ),
        MetadataItem(
            "Total Size of Samples",
            metadata.containerAttributes.totalSizeSamplesMb.formatWithUnit("MB"),
            "Total size of audio, video, or subtitle samples in megabytes (MB).",
        ),
    )

    @OptIn(UnstableApi::class)
    fun getTrackAttributes(trackMetadata: TrackAttributes): List<MetadataItem> {
        val commonTrackAttributes = listOf(
            MetadataItem(
                "MIME Type",
                trackMetadata.trackMimeType.orDash(),
                "Identifies the specific media type (e.g., video/avc, audio/aac) of this track, enabling appropriate handling and decoding.",
            ),
            MetadataItem(
                "Codecs",
                trackMetadata.codecs.orDash(),
                "Encoding algorithms (e.g., H.264, AAC) used for data compression and decompression in this track.",
            ),
            MetadataItem(
                "Label",
                trackMetadata.label.orDash(),
                "Human-readable identifier for the track, indicating its purpose or content (e.g., 'Main Audio').",
            ),
            MetadataItem(
                "Language",
                trackMetadata.language.orDash(),
                "Language of the content in this track (e.g., 'eng' for English audio/subtitles).",
            ),
            MetadataItem(
                "Average Bitrate",
                trackMetadata.averageBitrateKbps.formatWithUnit("kbps"),
                "Average data rate in kbps for this track, reflecting its overall data density.",
            ),
            MetadataItem(
                "Peak Bitrate",
                trackMetadata.peakBitrateKbps.formatWithUnit("kbps"),
                "Maximum data rate in kbps achieved by this track, representing its highest data demand.",
            ),
        )

        val specificTrackAttributes = trackMetadata.trackMimeType?.let { mimeType ->
            when {
                MimeTypes.isVideo(mimeType) -> getVideoAttributes(trackMetadata)
                MimeTypes.isAudio(mimeType) -> getAudioAttributes(trackMetadata)
                else -> emptyList() // Add other MimeType checks here if needed, e.g., for text/subtitles
            }
        }
        return commonTrackAttributes + (specificTrackAttributes ?: emptyList())
    }

    private fun getVideoAttributes(trackMetadata: TrackAttributes): List<MetadataItem> {
        val videoAttributes = trackMetadata.videoAttributes ?: return emptyList()
        return listOf(
            MetadataItem(
                "Width",
                videoAttributes.width.formatWithUnit("px"),
                "Horizontal resolution of the video frame in pixels, affecting dimensions and aspect ratio.",
            ),
            MetadataItem(
                "Height",
                videoAttributes.height.formatWithUnit("px"),
                "Vertical resolution of the video frame in pixels, affecting dimensions and aspect ratio.",
            ),
            MetadataItem(
                "Frame Rate",
                videoAttributes.frameRate.formatWithUnit("fps"),
                "Number of video frames displayed per second (fps), influencing motion smoothness.",
            ),
        )
    }

    private fun getAudioAttributes(trackMetadata: TrackAttributes): List<MetadataItem> {
        val audioAttributes = trackMetadata.audioAttributes ?: return emptyList()
        return listOf(
            MetadataItem(
                "Channel Count",
                audioAttributes.channelCount?.toString().orDash(),
                "Number of distinct audio channels (e.g., 2 for stereo, 6 for 5.1 surround).",
            ),
            MetadataItem(
                "Sample Rate",
                audioAttributes.sampleRateKHz.formatWithUnit("kHz"),
                "Number of audio samples per second (kHz), defining audio fidelity.",
            ),
            MetadataItem(
                "PCM Encoding",
                audioAttributes.pcmEncoding.orDash(),
                "Pulse Code Modulation encoding for raw audio data (e.g., 16-bit, 24-bit).",
            ),
            MetadataItem(
                "Encoder Delay",
                audioAttributes.encoderDelay.formatWithUnit("frames"),
                "Specifies the number of silent audio frames added by the encoder at the " +
                    "beginning of the track, impacting synchronization.",
            ),
            MetadataItem(
                "Encoder Padding",
                audioAttributes.encoderPadding.formatWithUnit("frames"),
                "Silent audio frames added by encoder at track end, ensuring complete playback.",
            ),
        )
    }

    private fun String?.orDash() = this ?: "-"

    private fun Number?.formatWithUnit(unit: String): String {
        return this?.let { "$it $unit" } ?: "-"
    }
}

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

import android.os.Build
import android.util.Log

private const val TAG = "MediaMetadata"

class MediaMetadata {
    val containerAttributes: ContainerAttributes = ContainerAttributes()
    val trackAttributesList: MutableList<TrackAttributes> = mutableListOf()

    fun print() {
        if (Build.TYPE == "debug") {
            Log.i(TAG, "MediaMetadata:")
            containerAttributes.print()
            trackAttributesList.forEach { it.print() }
        }
    }
}

class ContainerAttributes {
    var containerMimeType: String? = null
    var duration: String? = null
    var numTracks: Int? = null
    var numSamples: Long? = null
    var totalSizeSamplesMb: Long? = null

    fun print() {
        if (Build.TYPE == "debug") {
            Log.i(TAG, "  ContainerAttributes:")
            Log.d(TAG, "    mimeType: $containerMimeType")
            Log.d(TAG, "    numTracks: $numTracks")
            Log.d(TAG, "    duration: $duration")
            Log.d(TAG, "    numSamples: $numSamples")
            Log.d(TAG, "    totalSizeSamplesMb: $totalSizeSamplesMb MB")
        }
    }
}

class TrackAttributes {
    var trackId: String? = null
    var trackMimeType: String? = null
    var codecs: String? = null
    var label: String? = null
    var language: String? = null
    var averageBitrateKbps: Int? = null
    var peakBitrateKbps: Int? = null
    var videoAttributes: VideoAttributes? = null
    var audioAttributes: AudioAttributes? = null

    fun print() {
        if (Build.TYPE == "debug") {
            Log.i(TAG, " =========== TrackAttributes [$trackId] ===========")
            Log.d(TAG, "    mimeType: $trackMimeType")
            Log.d(TAG, "    codecs: $codecs")
            Log.d(TAG, "    label: $label")
            Log.d(TAG, "    language: $language")
            Log.d(TAG, "    averageBitrate: $averageBitrateKbps kbps")
            Log.d(TAG, "    peakBitrate: $peakBitrateKbps kbps")
            videoAttributes?.print()
            audioAttributes?.print()
        }
    }

    class VideoAttributes {
        var width: Int? = null
        var height: Int? = null
        var frameRate: Int? = null

        fun print() {
            if (Build.TYPE == "debug") {
                Log.i(TAG, "    VideoAttributes:")
                Log.d(TAG, "      width: $width px")
                Log.d(TAG, "      height: $height px")
                Log.d(TAG, "      frameRate: $frameRate fps")
            }
        }
    }

    class AudioAttributes {
        var sampleRateKHz: Long? = null
        var channelCount: Int? = null
        var pcmEncoding: String? = null
        var encoderDelay: Int? = null
        var encoderPadding: Int? = null

        fun print() {
            if (Build.TYPE == "debug") {
                Log.i(TAG, "    AudioAttributes:")
                Log.d(TAG, "      sampleRate: $sampleRateKHz KHz")
                Log.d(TAG, "      channelCount: $channelCount")
                Log.d(TAG, "      pcmEncoding: $pcmEncoding")
                Log.d(TAG, "      encoderDelay: $encoderDelay frames")
                Log.d(TAG, "      encoderPadding: $encoderPadding frames")
            }
        }
    }
}

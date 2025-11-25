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

class MediaMetadata {
    val containerAttributes: ContainerAttributes = ContainerAttributes()
    val trackAttributesList: MutableList<TrackAttributes> = mutableListOf()
}

class ContainerAttributes {
    var containerMimeType: String? = null
    var duration: String? = null
    var numTracks: Int? = null
    var numSamples: Long? = null
    var totalSizeSamplesMb: Long? = null
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

    class VideoAttributes {
        var width: Int? = null
        var height: Int? = null
        var frameRate: Int? = null
    }

    class AudioAttributes {
        var sampleRateKHz: Long? = null
        var channelCount: Int? = null
        var pcmEncoding: String? = null
        var encoderDelay: Int? = null
        var encoderPadding: Int? = null
    }
}

/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.samples.socialite.data.utils

/**
 *
 * Sample list of short form video urls, used for preloading multiple videos in background especially used for enabling preload manager of exoplayer.
 *
 */
class ShortsVideoList {
    companion object {
        val mediaUris =
            mutableListOf(
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_1.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_2.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_3.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_4.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_5.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_6.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_7.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_8.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_9.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_10.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shorts_android_developers/shorts_11.mp4",
            )
    }

    fun get(index: Int): String {
        return mediaUris[index.mod(mediaUris.size)]
    }
}

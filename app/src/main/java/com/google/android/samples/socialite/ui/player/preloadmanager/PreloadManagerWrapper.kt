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

package com.google.android.samples.socialite.ui.player.preloadmanager

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRendererCapabilitiesList
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager.Status.STAGE_LOADED_TO_POSITION_MS
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaType

/**
 * Created by Mayuri Khinvasara on 12/08/24.
 * Wrapper class to manage all functionalities of preload manager of exoplayer especially for short form content
 */
@androidx.media3.common.util.UnstableApi
class PreloadManagerWrapper
private constructor(
    private val defaultPreloadManager: DefaultPreloadManager,
) {
    private val preloadWindow: ArrayDeque<Pair<MediaItem, Int>> = ArrayDeque()

    // Default window size for preload manager
    private var preloadWindowSize = 5

    /** Builds a preload manager instance with default parameters. Preload manager should use the same looper and load control as the player */
    companion object {
        fun build(
            playbackLooper: Looper,
            loadControl: DefaultLoadControl,
            context: Context,
        ): PreloadManagerWrapper {
            val trackSelector = DefaultTrackSelector(context)
            trackSelector.init({}, DefaultBandwidthMeter.getSingletonInstance(context))
            val renderersFactory = DefaultRenderersFactory(context)
            val preloadManager = DefaultPreloadManager(
                PreloadStatusControl(),
                DefaultMediaSourceFactory(context),
                trackSelector,
                DefaultBandwidthMeter.getSingletonInstance(context),
                DefaultRendererCapabilitiesList.Factory(renderersFactory),
                loadControl.allocator,
                playbackLooper,
            )
            return PreloadManagerWrapper(preloadManager)
        }
    }

    /** Add initial list of videos to the preload manager. */
    fun init(mediaList: List<TimelineMediaItem>) {
        for ((index, item) in mediaList.withIndex()) {
            if (item.type == TimelineMediaType.VIDEO) {
                addMediaItem((MediaItem.fromUri(item.uri)), index)
            }
        }
    }

    /** Sets the index of the current playing media. */
    fun setCurrentPlayingIndex(currentPlayingIndex: Int) {
        defaultPreloadManager.setCurrentPlayingIndex(currentPlayingIndex)
    }

    /** Sets the size of the Preload Queue. */
    fun setPreloadWindowSize(preloadWindowSize1: Int) {
        preloadWindowSize = preloadWindowSize1
    }

    /** Add media item to the preload manager */
    fun addMediaItem(mediaItem: MediaItem, index: Int): Boolean {
        // item not found in list. This could happen if the list is auto purged or contents refreshed by APIs
        if (index < 0) {
            return false
        }
        // item already added , avoid duplicates
        if (preloadWindow.contains(Pair(mediaItem, index))) {
            return false
        }

        // Window full, purge old data added at the start of the queue
        if (preloadWindow.size >= preloadWindowSize) {
            defaultPreloadManager.remove(preloadWindow.first().first)
            preloadWindow.removeFirstOrNull()
        }
        // Add video to preload list
        preloadWindow.add(Pair(mediaItem, index))
        defaultPreloadManager.add(mediaItem, index)
        defaultPreloadManager.invalidate()
        return true
    }

    /** Releases the preload manager. This must be called on the main thread */
    @MainThread
    fun release() {
        defaultPreloadManager.release()
    }

    /** Customize time to preload, by default as per ranking data */
    @androidx.media3.common.util.UnstableApi
    class PreloadStatusControl : TargetPreloadStatusControl<Int> {
        override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.Status {
            // By default preload first 5 seconds of the video
            return DefaultPreloadManager.Status(STAGE_LOADED_TO_POSITION_MS, 5000L)
        }
    }
}

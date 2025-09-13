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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRendererCapabilitiesList
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
//import androidx.media3.exoplayer.source.preload.DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.google.android.samples.socialite.ui.home.timeline.TimelineMediaItem

/**
 * Wrapper class to manage all functionalities of preload manager of exoplayer especially for short form content
 */
@androidx.media3.common.util.UnstableApi
class PreloadManagerWrapper
private constructor(
    private val defaultPreloadManager: DefaultPreloadManager,
) {
    // Queue of media items to be preloaded. Can be ranked based on ranking data
    private val preloadWindow: ArrayDeque<Pair<MediaItem, Int>> = ArrayDeque()

    // Default window size for preload manager. This defines how many maximum items will be preloaded at a time.
    private var preloadWindowMaxSize = 5

    private var currentPlayingIndex = C.INDEX_UNSET

    // List of all items in our current list of media items to be rendered on the UI
    private var mediaItemsList = listOf<TimelineMediaItem>()

    // Defines when to start preloading next items w.r.t current playing item
    private val itemsRemainingToStartNextPreloading = 2

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
            val targetPreloadStatusControl = MyTargetPreloadStatusControl()
            val preloadManagerBuilder =
                DefaultPreloadManager.Builder(context, targetPreloadStatusControl)
            val preloadManager = preloadManagerBuilder.build()
            return PreloadManagerWrapper(preloadManager)
        }
    }

    /** Add initial list of videos to the preload manager. */
    fun init(mediaList: List<TimelineMediaItem>) {
        if (mediaList.isEmpty()) {
            return
        }
        setCurrentPlayingIndex(0)
        setMediaList(mediaList)
        preloadNextItems()
    }

    /** Sets the index of the current playing media. */
    fun setCurrentPlayingIndex(currentPlayingItemIndex: Int) {
        currentPlayingIndex = currentPlayingItemIndex
        defaultPreloadManager.setCurrentPlayingIndex(currentPlayingIndex)
        preloadNextItems()
    }

    /** Sets the list of media items to be played. Can be set as and when new data is loaded. */
    private fun setMediaList(mediaList: List<TimelineMediaItem>) {
        mediaItemsList = mediaList
    }

    /** Add the next set of items to preload, w.r.t to the current playing index. */
    private fun preloadNextItems() {
        var lastPreloadedIndex = 0
        if (!preloadWindow.isEmpty()) {
            lastPreloadedIndex = preloadWindow.last().second
        }

        if (lastPreloadedIndex - currentPlayingIndex <= itemsRemainingToStartNextPreloading) {
            for (i in 1 until (preloadWindowMaxSize - itemsRemainingToStartNextPreloading)) {
                addMediaItem(index = lastPreloadedIndex + i)
                removeMediaItem()
            }
        }
        // With invalidate, preload manager will internally sort the priorities of all the media items added to it, and trigger the preload from the most important one.
        defaultPreloadManager.invalidate()
    }

    /** Remove media item from the preload window. */
    private fun removeMediaItem() {
        if (preloadWindow.size <= preloadWindowMaxSize) {
            return
        }
        val itemAndIndex = preloadWindow.removeFirst()
        defaultPreloadManager.remove(itemAndIndex.first)
    }

    /** Add media item from the preload window. */
    private fun addMediaItem(index: Int) {
        if (index < 0 || index >= mediaItemsList.size) {
            return
        }

        val mediaItem = (MediaItem.fromUri(mediaItemsList[index].uri))
        defaultPreloadManager.add(mediaItem, index)
        preloadWindow.addLast(Pair(mediaItem, index))
    }

    /** Sets the size of the Preload Queue. */
    fun setPreloadWindowSize(size: Int) {
        preloadWindowMaxSize = size
    }

    /** Releases the preload manager. This must be called on the main thread */
    @MainThread
    fun release() {
        defaultPreloadManager.release()
        preloadWindow.clear()
        mediaItemsList.toMutableList().clear()
    }

    /** Retrieve the preloaded media source */
    fun getMediaSource(mediaItem: MediaItem): MediaSource? {
        return defaultPreloadManager.getMediaSource(mediaItem)
    }

    //    /** Customize time to preload, by default as per ranking data */
    class MyTargetPreloadStatusControl() :
        TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
        override fun getTargetPreloadStatus(index: Int): DefaultPreloadManager.PreloadStatus? {
            return null
        }
    }
}

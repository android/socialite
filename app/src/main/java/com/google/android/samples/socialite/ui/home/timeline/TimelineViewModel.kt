/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.google.android.samples.socialite.ui.home.timeline

import android.content.Context
import android.net.Uri
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.android.samples.socialite.ui.player.preloadmanager.PreloadManagerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@UnstableApi
@HiltViewModel
class TimelineViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val repository: ChatRepository,
) : ViewModel() {
    // List of videos and photos from chats
    var media by mutableStateOf<List<TimelineMediaItem>>(emptyList())

    // Single player instance - in the future, we can implement a pool of players to improve
    // latency and allow for concurrent playback
    var player by mutableStateOf<ExoPlayer?>(null)

    // Width/Height ratio of the current media item, used to properly size the Surface
    var videoRatio by mutableStateOf<Float?>(null)

    // Preload Manager for preloaded multiple videos
    private val enablePreloadManager: Boolean = true
    private lateinit var preloadManager: PreloadManagerWrapper

    // Playback thread; Internal playback / preload operations are running on the playback thread.
    private val playerThread: HandlerThread =
        HandlerThread("playback-thread", Process.THREAD_PRIORITY_AUDIO)

    var playbackStartTimeMs = C.TIME_UNSET

    private val videoSizeListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoRatio = if (videoSize.height > 0 && videoSize.width > 0) {
                videoSize.width.toFloat() / videoSize.height.toFloat()
            } else {
                null
            }
            super.onVideoSizeChanged(videoSize)
        }
    }

    private val firstFrameListener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            val timeToFirstFrameMs = System.currentTimeMillis() - playbackStartTimeMs
            Log.d("PreloadManager", "\t\tTime to first Frame = $timeToFirstFrameMs ")
            super.onRenderedFirstFrame()
        }
    }

    init {
        viewModelScope.launch {
            val allChats = repository.getChats().first()
            val newList = mutableListOf<TimelineMediaItem>()
            for (chatDetail in allChats) {
                val messages = repository.findMessages(chatDetail.chatWithLastMessage.id).first()
                for (message in messages) {
                    if (message.mediaUri != null) {
                        newList += TimelineMediaItem(
                            uri = message.mediaUri,
                            type = if (message.mediaMimeType?.contains("video") == true) {
                                TimelineMediaType.VIDEO
                            } else {
                                TimelineMediaType.PHOTO
                            },
                            timestamp = message.timestamp,
                            chatName = chatDetail.firstContact.name,
                            chatIconUri = chatDetail.firstContact.iconUri,
                        )
                    }
                }
            }
            newList.sortByDescending { it.timestamp }
            media = newList
        }
    }

    @OptIn(UnstableApi::class) // https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide#unstableapi
    fun initializePlayer() {
        if (player != null) return

        // Reduced buffer durations since the primary use-case is for short-form videos
        val loadControl =
            DefaultLoadControl.Builder().setBufferDurationsMs(5_000, 20_000, 5_00, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .setPrioritizeTimeOverSizeThresholds(true).build()

        playerThread.start()

        val newPlayer = ExoPlayer
            .Builder(application.applicationContext)
            .setLoadControl(loadControl)
            .setPlaybackLooper(playerThread.looper)
            .build()
            .also {
                it.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                it.playWhenReady = true
                it.addListener(videoSizeListener)
                it.addListener(firstFrameListener)
            }

        videoRatio = null
        player = newPlayer

        if (enablePreloadManager) {
            initPreloadManager(loadControl, playerThread)
        }
    }

    private fun initPreloadManager(
        loadControl: DefaultLoadControl,
        preloadAndPlaybackThread: HandlerThread,
    ) {
        preloadManager =
            PreloadManagerWrapper.build(
                preloadAndPlaybackThread.looper,
                loadControl,
                application.applicationContext,
            )
        preloadManager.setPreloadWindowSize(5)

        // Add videos to preload
        if (media.isNotEmpty()) {
            preloadManager.init(media)
        }
    }

    fun releasePlayer() {
        if (enablePreloadManager) {
            preloadManager.release()
        }
        player?.apply {
            removeListener(videoSizeListener)
            removeListener(firstFrameListener)
            release()
        }
        playerThread.quit()
        videoRatio = null
        player = null
    }

    fun changePlayerItem(uri: Uri?, currentPlayingIndex: Int) {
        if (player == null) return

        player?.apply {
            stop()
            videoRatio = null
            if (uri != null) {
                // Set the right source to play
                val mediaItem = MediaItem.fromUri(uri)

                if (enablePreloadManager) {
                    val mediaSource = preloadManager.getMediaSource(mediaItem)
                    Log.d("PreloadManager", "Mediasource $mediaSource ")

                    if (mediaSource == null) {
                        setMediaItem(mediaItem)
                    } else {
                        // Use the preloaded media source
                        setMediaSource(mediaSource)
                    }
                    preloadManager.setCurrentPlayingIndex(currentPlayingIndex)
                } else {
                    setMediaItem(mediaItem)
                }

                playbackStartTimeMs = System.currentTimeMillis()
                Log.d("PreloadManager", "Video Playing $uri ")
                prepare()
            } else {
                clearMediaItems()
            }
        }
    }
}

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
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.samples.socialite.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class TimelineViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val repository: ChatRepository,
) : ViewModel() {
    // List of videos and photos from chats
    var media by mutableStateOf<List<TimelineMediaItem>>(emptyList())

    // Single player instance - in the future, we can implement a pool of players to improve
    // latency and allow for concurrent playback
    var player by mutableStateOf<Player?>(null)

    // Width/Height ratio of the current media item, used to properly size the Surface
    var videoRatio by mutableStateOf<Float?>(null)

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
            DefaultLoadControl.Builder().setBufferDurationsMs(500, 1000, 0, 500).build()
        val newPlayer = ExoPlayer
            .Builder(application.applicationContext)
            .setLoadControl(loadControl)
            .build()
            .also {
                it.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                it.playWhenReady = true
                it.addListener(videoSizeListener)
            }

        videoRatio = null
        player = newPlayer
    }

    fun releasePlayer() {
        player?.apply {
            removeListener(videoSizeListener)
            release()
        }

        videoRatio = null
        player = null
    }

    fun changePlayerItem(uri: Uri?) {
        if (player == null) return

        player?.apply {
            stop()
            videoRatio = null
            if (uri != null) {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
            } else {
                clearMediaItems()
            }
        }
    }
}

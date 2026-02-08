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

import android.content.ContentResolver
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
import androidx.media3.cast.CastPlayer
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.samples.socialite.model.ChatDetail
import com.google.android.samples.socialite.repository.ChatRepository
import com.google.android.samples.socialite.ui.player.preloadmanager.PreloadManagerWrapper
import com.google.android.samples.socialite.utils.LocalMediaServer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "TimelineViewModel"

@UnstableApi
@HiltViewModel
class TimelineViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val repository: ChatRepository,
    private val localMediaServer: LocalMediaServer,
) : ViewModel() {
    // Single player instance - in the future, we can implement a pool of players to improve
    // latency and allow for concurrent playback
    var player by mutableStateOf<Player?>(null)

    // Cast player instance
    var castPlayer by mutableStateOf<CastPlayer?>(null)

    // Keeps track if the current playback location is remote or local
    var isRemote by mutableStateOf(false)


    // Width/Height ratio of the current media item, used to properly size the Surface
    var videoRatio by mutableStateOf<Float?>(null)

    // Preload Manager for preloaded multiple videos
    private val enablePreloadManager: Boolean = true
    private lateinit var preloadManager: PreloadManagerWrapper

    // Playback thread; Internal playback / preload operations are running on the playback thread.
    private var playerThread: HandlerThread? = null

    var playbackStartTimeMs = C.TIME_UNSET

    private var currentVideoUri: Uri? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                localMediaServer.start()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start local media server", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            localMediaServer.stop()
        }
        releasePlayer()
    }

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

    /**
     * Listens for MediaRoute connection events.
     * When a Cast device connects or disconnects, this listener seamlessly transfers
     * the currently playing video to the new playback destination.
     */
    private val castPlayerListener = object : Player.Listener {
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            isRemote = deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
            currentVideoUri?.let { uri ->
                val mediaItem = getMediaItemForUri(uri)
                val typeStr = if (isRemote) "REMOTE" else "LOCAL"
                Log.i(TAG, "Serving content on $typeStr: ${mediaItem.localConfiguration?.uri}")
                player?.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
            }
        }
    }

    /**
     * Determines the appropriate [MediaItem] source structure for a given URI.
     *
     * If the session is casting to a remote device AND the URI points to a local file
     * (content:// or file://), the Cast receiver cannot securely access that file path.
     * To solve this, we proxy the file stream over an HTTP URL backed by our own [LocalMediaServer].
     *
     * @param uri The original location of the file to play.
     * @return A [MediaItem] that ExoPlayer/CastPlayer can natively decode.
     */
    private fun getMediaItemForUri(uri: Uri): MediaItem {
        val isLocal = uri.scheme in listOf(
            ContentResolver.SCHEME_FILE,
            ContentResolver.SCHEME_CONTENT,
        ) || uri.path.orEmpty().startsWith("/storage/")
        if (!isRemote || !isLocal) return MediaItem.fromUri(uri)

        val ip = localMediaServer.getLocalIpAddress() ?: return MediaItem.fromUri(uri)
        val port = localMediaServer.listeningPort.takeIf { it > 0 } ?: return MediaItem.fromUri(uri)

        val proxyPath = if (uri.scheme == ContentResolver.SCHEME_CONTENT) "/$uri" else uri.path
        localMediaServer.currentSharedUri = proxyPath?.removePrefix("/")

        return MediaItem.fromUri("http://$ip:$port$proxyPath")
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val media = repository.getChats()
        .map { chats ->
            combine(
                chats.map { chat ->
                    createTimelineMediaItemList(chat)
                },
            ) { timelineMediaItems ->
                timelineMediaItems.toList().flatten()
            }
        }
        .flattenConcat()
        .onEach { list ->
            if (::preloadManager.isInitialized && list.isNotEmpty()) {
                preloadManager.init(list)
            }
        }

    private fun createTimelineMediaItemList(chatDetail: ChatDetail): Flow<List<TimelineMediaItem>> {
        return repository.findMessages(chatDetail.chatWithLastMessage.id).map { messages ->
            messages.filter {
                it.mediaUri != null
            }.map {
                TimelineMediaItem(
                    uri = it.mediaUri!!,
                    type = if (it.mediaMimeType?.contains("video") == true) {
                        TimelineMediaType.VIDEO
                    } else {
                        TimelineMediaType.PHOTO
                    },
                    timestamp = it.timestamp,
                    chatName = chatDetail.firstContact.name,
                    chatIconUri = chatDetail.firstContact.iconUri,
                )
            }
        }
    }

    @OptIn(UnstableApi::class) // https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide#unstableapi
    fun initializePlayer() {
        if (player != null) return

        // Reduced buffer durations since the primary use-case is for short-form videos
        val loadControl =
            DefaultLoadControl.Builder().setBufferDurationsMs(
                5_000,
                20_000,
                5_00,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
                .setPrioritizeTimeOverSizeThresholds(true).build()

        val thread = initPlayerThread()
        val newPlayer = ExoPlayer
            .Builder(application.applicationContext)
            .setLoadControl(loadControl)
            .setPlaybackLooper(thread.looper)
            .build()
            .also {
                it.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                it.playWhenReady = true
                it.addListener(videoSizeListener)
                it.addListener(firstFrameListener)
            }
        castPlayer =
            CastPlayer.Builder(application.applicationContext).setLocalPlayer(newPlayer).build()
        castPlayer?.addListener(castPlayerListener)

        videoRatio = null
        player = castPlayer

        if (enablePreloadManager) {
            initPreloadManager(loadControl, thread)
        }
    }

    private fun initPlayerThread(): HandlerThread {
        val thread = HandlerThread("PlayerThread", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
        }
        playerThread = thread
        return thread
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
    }

    fun releasePlayer() {
        if (enablePreloadManager) {
            preloadManager.release()
        }
        castPlayer?.removeListener(castPlayerListener)
        castPlayer?.release()
        player?.apply {
            removeListener(videoSizeListener)
            removeListener(firstFrameListener)
            release()
        }
        playerThread?.quitSafely()
        playerThread = null
        videoRatio = null
        player = null
    }

    fun changePlayerItem(uri: Uri?, currentPlayingIndex: Int) {
        if (player == null) return

        player?.apply {
            stop()
            videoRatio = null
            if (uri != null) {
                currentVideoUri = uri
                // Set the right source to play
                val mediaItem = getMediaItemForUri(uri)
                Log.d(TAG, "Media item changed: ${mediaItem.localConfiguration?.uri}")

                if (enablePreloadManager) {
                    val mediaSource = preloadManager.getMediaSource(mediaItem)
                    Log.d("PreloadManager", "Mediasource $mediaSource ")

                    if (!isRemote && mediaSource != null && this is ExoPlayer) {
                        setMediaSource(mediaSource)
                    } else {
                        setMediaItem(mediaItem)
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

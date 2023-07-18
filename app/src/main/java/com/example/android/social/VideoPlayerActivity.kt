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

package com.example.android.social

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

const val VIDEO_URI_EXTRA = "mediaUri"

class VideoPlayerActivity : ComponentActivity() {

    private lateinit var mediaUri: String
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        val extra = intent.extras
        if (extra != null) {
            val mediaUriFromBundle = extra.getString(VIDEO_URI_EXTRA)
            if (!mediaUriFromBundle.isNullOrEmpty()) {
                playerView = findViewById(R.id.player_view)!!
                mediaUri = mediaUriFromBundle
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer(mediaUri)
            playerView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer(mediaUri)
            playerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        // API 24 introduces multi-window mode, so on API >= 24 it's important to stop playback in
        // onStop and not onPause
        // More context: https://github.com/google/ExoPlayer/issues/4878#issuecomment-425427583
        if (Build.VERSION.SDK_INT <= 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    private fun initializePlayer(mediaUri: String) {
        // Instantiate the player.
        player = ExoPlayer.Builder(applicationContext).build().apply {
            // Attach player to the view.
            playerView.player = this
            // Set the media item to be played.
            setMediaItem(MediaItem.fromUri(mediaUri))
            // Prepare the player.
            prepare()
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        playerView.player = null
    }
}

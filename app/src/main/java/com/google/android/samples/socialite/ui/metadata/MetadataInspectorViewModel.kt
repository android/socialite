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

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MetadataInspectorViewModel"

@HiltViewModel
class MetadataInspectorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    var isLoaded by mutableStateOf(false)

    var mediaMetadata by mutableStateOf<MediaMetadata?>(null)

    @RequiresApi(Build.VERSION_CODES.O)
    fun processMedia(mediaPath: String) {
        if (isLoaded) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currMediaMetadata =
                    MediaMetadataProcessor(context, mediaPath).populateMediaMetadata()
                withContext(Dispatchers.Main) {
                    mediaMetadata = currMediaMetadata
                    isLoaded = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during media processing: $e", e)
            }
        }
    }
}

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

package com.google.android.samples.socialite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.samples.socialite.ui.LocalFoldingState
import com.google.android.samples.socialite.ui.Main
import com.google.android.samples.socialite.ui.ShortcutParams
import com.google.android.samples.socialite.ui.camera.FoldingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val foldingState = WindowInfoTracker.getOrCreate(this@MainActivity)
                .windowLayoutInfo(this@MainActivity)
                .map { layoutInfo ->
                    val displayFeatures = layoutInfo.displayFeatures
                    when {
                        displayFeatures.isEmpty() -> FoldingState.CLOSE
                        hasFlatFoldingFeature(displayFeatures) -> FoldingState.HALF_OPEN
                        else -> FoldingState.FLAT
                    }
                }
                .collectAsStateWithLifecycle(initialValue = FoldingState.FLAT)

            CompositionLocalProvider(
                LocalFoldingState provides foldingState.value,
            ) {
                Main(
                    shortcutParams = extractShortcutParams(intent),
                )
            }
        }
    }

    private fun extractShortcutParams(intent: Intent?): ShortcutParams? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        val shortcutId = intent.getStringExtra(
            ShortcutManagerCompat.EXTRA_SHORTCUT_ID,
        ) ?: return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return ShortcutParams(shortcutId, text)
    }

    private fun hasFlatFoldingFeature(displayFeatures: List<DisplayFeature>): Boolean =
        displayFeatures.any { feature ->
            feature is FoldingFeature &&
                feature.state == FoldingFeature.State.HALF_OPENED
        }
}

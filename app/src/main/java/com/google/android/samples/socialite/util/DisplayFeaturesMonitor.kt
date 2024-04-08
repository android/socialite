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

package com.google.android.samples.socialite.util

import android.content.Context
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.samples.socialite.domain.FoldingState
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface DisplayFeaturesMonitor {
    val foldingState: Flow<FoldingState>
}

@ActivityScoped
class DisplayFeaturesMonitorImpl @Inject constructor(
    @ActivityContext context: Context,
) : DisplayFeaturesMonitor {

    override val foldingState: Flow<FoldingState> =
        WindowInfoTracker.getOrCreate(context)
            .windowLayoutInfo(context)
            .map { layoutInfo ->
                val displayFeatures = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>()
                when {
                    displayFeatures.isEmpty() -> FoldingState.CLOSE
                    hasHalfOpenedFoldingFeature(displayFeatures) -> FoldingState.HALF_OPEN
                    else -> FoldingState.FLAT
                }
            }

    private fun hasHalfOpenedFoldingFeature(displayFeatures: List<FoldingFeature>): Boolean =
        displayFeatures.any { feature ->
            feature.state == FoldingFeature.State.HALF_OPENED
        }
}

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

package com.google.android.samples.socialite.ui.home.timeline.component

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

enum class TimelineFormat {
    Pager,
    Grid,
}

@Composable
fun rememberTimelineFormat(
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
): TimelineFormat {
    val isAtLeastMedium = windowAdaptiveInfo
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND)

    return if (isAtLeastMedium) {
        TimelineFormat.Grid
    } else {
        TimelineFormat.Pager
    }
}

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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.android.samples.socialite.ui.home.HomeAppBar
import com.google.android.samples.socialite.ui.home.HomeBackground
import com.google.android.samples.socialite.ui.navigation.TopLevelDestination

@Composable
internal fun TimelineScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {
        HomeAppBar(title = stringResource(TopLevelDestination.Timeline.label))
    },
    content: @Composable (contentPadding: PaddingValues) -> Unit = {},
) {
    Scaffold(
        topBar = topBar,
        modifier = modifier,
    ) { contentPadding ->
        HomeBackground(modifier = Modifier.Companion.fillMaxSize())
        content(contentPadding)
    }
}

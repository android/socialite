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

package com.google.android.samples.socialite.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun PlayArrowIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = Color.Companion.White,
) {
    val minSize = LocalMinimumInteractiveComponentSize.current
    Icon(
        Icons.Filled.PlayArrow,
        tint = tint,
        contentDescription = contentDescription,
        modifier = modifier
            .sizeIn(minWidth = minSize, minHeight = minSize)
            .border(3.dp, Color.Companion.White, shape = CircleShape),
    )
}

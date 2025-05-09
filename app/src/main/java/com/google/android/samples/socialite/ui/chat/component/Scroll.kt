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

package com.google.android.samples.socialite.ui.chat.component

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun Modifier.scrollWithKeyboards(
    scrollState: LazyListState,
    coroutineScope: CoroutineScope,
): Modifier {
    return onKeyEvent { event ->
        when {
            event.isKeyPressed(Key.DirectionDown, shouldShiftBePressed = true) -> {
                scrollState.pageDown(coroutineScope)
                true
            }

            event.isKeyPressed(Key.PageDown) -> {
                scrollState.pageDown(coroutineScope)
                true
            }

            event.isKeyPressed(Key.DirectionUp, shouldShiftBePressed = true) -> {
                scrollState.pageUp(coroutineScope)
                true
            }

            event.isKeyPressed(Key.PageUp) -> {
                scrollState.pageUp(coroutineScope)
                true
            }

            else -> false
        }
    }
}

private fun LazyListState.pageUp(
    coroutineScope: CoroutineScope,
    fraction: Float = 0.8f,
) {
    val amount = layoutInfo.viewportSize.height * fraction
    coroutineScope.launch {
        animateScrollBy(amount)
    }
}

private fun LazyListState.pageDown(
    coroutineScope: CoroutineScope,
    fraction: Float = 0.8f,
) {
    val amount = -layoutInfo.viewportSize.height * fraction
    coroutineScope.launch {
        animateScrollBy(amount)
    }
}

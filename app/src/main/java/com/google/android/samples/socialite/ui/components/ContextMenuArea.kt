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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.util.fastAll

@Composable
fun ContextMenuArea(
    items: List<ContextMenuItem>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var rightClickOffset by remember { mutableStateOf<DpOffset>(DpOffset.Zero) }
    var isMenuVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .rightClickDetector {
                isMenuVisible = true
                rightClickOffset = with(density) {
                    DpOffset(it.x.toDp(), it.y.toDp())
                }
            }
            .then(modifier),
    ) {
        content()
        AnimatedVisibility(isMenuVisible && items.isNotEmpty()) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { isMenuVisible = false },
                offset = rightClickOffset,
            ) {
                items.forEach {
                    DropdownMenuItem(
                        text = { Text(it.label) },
                        onClick = {
                            it.action()
                            isMenuVisible = false
                        },
                    )
                }
            }
        }
    }
}

private object ContextMenuKey

private fun Modifier.rightClickDetector(
    onRightClick: (Offset) -> Unit,
): Modifier =
    pointerInput(ContextMenuKey) {
        awaitEachGesture {
            val event = awaitPointerEvent()
            if (isRightClick(event) && event.changes.fastAll { it.changedToDown() }) {
                val press = event.changes.find {
                    it.pressed
                }
                if (press != null) {
                    onRightClick(press.position)
                }
                event.changes.forEach {
                    it.consume()
                }
                waitForUpOrCancellation()?.consume()
            }
        }
    }

private fun isRightClick(event: PointerEvent): Boolean {
    return event.isSecondaryPressed() || event.isPrimaryPressedWithCtrlKey()
}

private fun PointerEvent.isSecondaryPressed(): Boolean {
    return type == PointerEventType.Press &&
        !buttons.isPrimaryPressed &&
        buttons.isSecondaryPressed &&
        !buttons.isTertiaryPressed
}

private fun PointerEvent.isPrimaryPressedWithCtrlKey(): Boolean {
    return type == PointerEventType.Press &&
        buttons.isPrimaryPressed &&
        !buttons.isSecondaryPressed &&
        !buttons.isTertiaryPressed &&
        keyboardModifiers.isCtrlPressed
}

data class ContextMenuItem(
    val label: String,
    val action: () -> Unit = {},
)

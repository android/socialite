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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

fun KeyEvent.isKeyPressed(
    expectedKey: Key,
    expectedKeyEventType: KeyEventType = KeyEventType.KeyDown,
    shouldShiftBePressed: Boolean = false,
    shouldAltBePressed: Boolean = false,
    shouldCtrlBePressed: Boolean = false,
    shouldMetaBePressed: Boolean = false,
): Boolean {
    return key == expectedKey && type == expectedKeyEventType &&
        isModifierKeyPressed(
            shouldShiftBePressed = shouldShiftBePressed,
            shouldAltBePressed = shouldAltBePressed,
            shouldCtrlBePressed = shouldCtrlBePressed,
            shouldMetaBePressed = shouldMetaBePressed,
        )
}

private fun KeyEvent.isModifierKeyPressed(
    shouldShiftBePressed: Boolean = false,
    shouldAltBePressed: Boolean = false,
    shouldCtrlBePressed: Boolean = false,
    shouldMetaBePressed: Boolean = false,
): Boolean {
    return (isShiftPressed == shouldShiftBePressed) &&
        (isAltPressed == shouldAltBePressed) &&
        (isCtrlPressed == shouldCtrlBePressed) &&
        (isMetaPressed == shouldMetaBePressed)
}

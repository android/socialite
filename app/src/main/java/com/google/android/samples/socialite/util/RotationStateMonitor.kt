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
import androidx.camera.view.RotationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface RotationStateMonitor {
    val currentRotation: Flow<Int>
}

@Singleton
class RotationStateManager @Inject constructor(
    @ApplicationContext context: Context,
) : RotationStateMonitor {

    override val currentRotation: Flow<Int> = callbackFlow {
        val rotationProvider = RotationProvider(context)

        val rotationListener = RotationProvider.Listener { rotation: Int ->
            trySend(rotation)
        }

        rotationProvider.addListener(Dispatchers.Main.asExecutor(), rotationListener)

        awaitClose {
            rotationProvider.removeListener(rotationListener)
        }
    }
}

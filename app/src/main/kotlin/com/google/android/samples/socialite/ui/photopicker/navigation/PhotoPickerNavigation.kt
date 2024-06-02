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

package com.google.android.samples.socialite.ui.photopicker.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

fun NavController.navigateToPhotoPicker(chatId: Long, navOptions: NavOptions? = null) {
    this.navigate("chat/$chatId/photoPicker", navOptions)
}

fun NavGraphBuilder.photoPickerScreen(
    onPhotoPicked: () -> Unit,
) {
    composable(
        route = "chat/{chatId}/photoPicker",
        arguments = listOf(
            navArgument("chatId") { type = NavType.LongType },
        ),
    ) {
        PhotoPickerRoute(
            onPhotoPicked = onPhotoPicked,
        )
    }
}

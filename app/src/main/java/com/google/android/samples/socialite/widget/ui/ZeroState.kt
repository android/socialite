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

package com.google.android.samples.socialite.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import com.google.android.samples.socialite.model.Contact
import com.google.android.samples.socialite.widget.model.WidgetModelRepository

@Composable
fun ZeroState(repository: WidgetModelRepository, widgetId: Int, context: Context) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        LazyColumn {
            items(Contact.CONTACTS.size) { contactIndex ->
                val contact = Contact.CONTACTS[contactIndex]

                ContactRow(
                    contact = contact,
                    profileImageUri = contact.iconUri,
                    onClick = TODO(),
                )
            }
        }
    }
}

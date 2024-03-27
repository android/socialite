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

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.android.samples.socialite.model.Contact

@Composable
fun ContactRow(contact: Contact, profileImageUri: Uri, onClick: () -> Unit) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(onClick).wrapContentHeight().padding(4.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            modifier = GlanceModifier.size(48.dp).padding(end = 8.dp),
            contentScale = ContentScale.Fit,
            provider = ImageProvider(profileImageUri),
            contentDescription = "Avatar",
        )
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            Text(
                text = contact.name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GlanceTheme.colors.onBackground,
                ),
            )
            Text(
                text = "Click to select as favorite contact",
                style = TextStyle(
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = GlanceTheme.colors.onBackground,
                ),
            )
        }
    }
}

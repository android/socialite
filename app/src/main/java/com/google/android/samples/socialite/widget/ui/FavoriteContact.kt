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

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.android.samples.socialite.widget.model.WidgetModel

@Composable
fun FavoriteContact(modifier: GlanceModifier = GlanceModifier, model: WidgetModel, onClick: Action) {
    Column(
        modifier = modifier.fillMaxSize().clickable(onClick)
            .background(GlanceTheme.colors.widgetBackground).appWidgetBackground()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Vertical.Bottom,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Image(
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight().defaultWeight()
                .cornerRadius(16.dp),
            provider = ImageProvider(model.photo.toUri()),
            contentScale = ContentScale.Crop,
            contentDescription = model.displayName,
        )
        Column(
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight().padding(top = 4.dp),
            verticalAlignment = Alignment.Vertical.Bottom,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Text(
                text = model.displayName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = (GlanceTheme.colors.onSurface),
                ),
            )

            Text(
                text = if (model.unreadMessages) "New Message!" else "No messages",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = (GlanceTheme.colors.onSurface),
                ),
            )
        }
    }
}

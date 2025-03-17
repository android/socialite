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
import androidx.glance.appwidget.components.Scaffold
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
import androidx.glance.unit.Dimension
import com.google.android.samples.socialite.widget.model.WidgetModel
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.glance.LocalContext

@Composable
fun FavoriteContact(modifier: GlanceModifier = GlanceModifier, model: WidgetModel, onClick: Action) {
    Scaffold(modifier = modifier, horizontalPadding = 0.dp) {
        Column(
            modifier = modifier.fillMaxSize().clickable(onClick),
            verticalAlignment = Alignment.Vertical.Bottom,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Image(
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight().defaultWeight()
                                         .appWidgetInnerCornerRadius(16.dp),
                provider = ImageProvider(model.photo.toUri()),
                contentScale = ContentScale.Crop,
                contentDescription = model.displayName,
            )
            Column(
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight().padding(top = 4.dp, bottom = 4.dp),
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
}

/**
 * Applies corner radius for views that are visually positioned [widgetPadding]dp inside of the
 * widget background.
 */
@Composable
fun GlanceModifier.appWidgetInnerCornerRadius(widgetPadding: Dp): GlanceModifier {

    if (Build.VERSION.SDK_INT < 31) {
        return this
    }

    val resources = LocalContext.current.resources
    // get dimension in float (without rounding).
    val px = resources.getDimension(android.R.dimen.system_app_widget_background_radius)
    val widgetBackgroundRadiusDpValue = px / resources.displayMetrics.density
    if (widgetBackgroundRadiusDpValue < widgetPadding.value) {
        return this
    }
    return this.cornerRadius(Dp(widgetBackgroundRadiusDpValue - widgetPadding.value))
}

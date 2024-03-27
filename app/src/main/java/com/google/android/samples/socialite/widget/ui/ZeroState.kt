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

import android.appwidget.AppWidgetManager
import androidx.compose.runtime.Composable
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.google.android.samples.socialite.MainActivity
import com.google.android.samples.socialite.R
import com.google.android.samples.socialite.widget.SociaLiteAppWidgetConfigActivity

@Composable
fun ZeroState(widgetId: Int) {
    val widgetIdKey = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)
    Scaffold(
        titleBar = {
            TitleBar(
                modifier = GlanceModifier.clickable(actionStartActivity(MainActivity::class.java)),
                textColor = GlanceTheme.colors.onSurface,
                startIcon = ImageProvider(R.drawable.ic_launcher_monochrome),
                title = "SociaLite",
            )
        },
        backgroundColor = GlanceTheme.colors.widgetBackground,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                text = "Select Favorite Contact",
                onClick = actionStartActivity<SociaLiteAppWidgetConfigActivity>(
                    parameters = actionParametersOf(widgetIdKey to widgetId),
                ),
            )
        }
    }
}

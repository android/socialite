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

package com.google.android.samples.socialite.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import com.google.android.samples.socialite.widget.model.WidgetModel
import com.google.android.samples.socialite.widget.ui.FavoriteContact

class SociaLiteAppWidgetGeneratedPreview(val model: WidgetModel) : GlanceAppWidget() {

    companion object {
        val TAG = "SociaLiteWidget"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Content(model)
            }
        }
    }

    @Composable
    private fun Content(model: WidgetModel) {
        FavoriteContact(
            model = model,
            onClick = action {},
        )
    }

    suspend fun updateWidgetPreview(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val appwidgetManager = AppWidgetManager.getInstance(context)

                appwidgetManager.setWidgetPreview(
                    ComponentName(context, SociaLiteAppWidgetReceiver::class.java),
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    compose(context),
                )
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }
        }
    }
}

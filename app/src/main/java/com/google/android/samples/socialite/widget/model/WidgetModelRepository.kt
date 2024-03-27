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

package com.google.android.samples.socialite.widget.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import coil.Coil
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import com.google.android.samples.socialite.di.AppCoroutineScope
import com.google.android.samples.socialite.widget.SociaLiteAppWidget
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class WidgetModelRepository @Inject internal constructor(private val widgetModelDao: WidgetModelDao, @AppCoroutineScope private val coroutineScope: CoroutineScope, @ApplicationContext private val appContext: Context) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetModelRepositoryEntryoint {
        fun widgetModelRepository(): WidgetModelRepository
    }

    companion object {
        fun get(applicationContext: Context): WidgetModelRepository {
            var widgetModelRepositoryEntryoint: WidgetModelRepositoryEntryoint = EntryPoints.get(
                applicationContext,
                WidgetModelRepositoryEntryoint::class.java,
            )
            return widgetModelRepositoryEntryoint.widgetModelRepository()
        }
    }

    suspend fun create(model: WidgetModel): WidgetModel {
        widgetModelDao.insert(model)
        return widgetModelDao.loadWidgetModel(model.widgetId).filterNotNull().first()
    }

    fun loadModel(widgetId: Int): Flow<WidgetModel?> {
        return widgetModelDao.loadWidgetModel(widgetId).distinctUntilChanged()
    }

    fun cleanupWidgetModels(context: Context) {
        coroutineScope.launch {
            val widgetManager = GlanceAppWidgetManager(context)
            val widgetIds = widgetManager.getGlanceIds(SociaLiteAppWidget::class.java).map { glanceId ->
                widgetManager.getAppWidgetId(glanceId)
            }.toList()

            widgetModelDao.findOrphanModels(widgetIds).forEach { model ->
                widgetModelDao.delete(
                    model,
                )
            }
        }
    }

    fun updateUnreadMessagesForContact(contactId: Long, unread: Boolean) {
        coroutineScope.launch {
            widgetModelDao.modelsForContact(contactId).filterNotNull().forEach { model ->
                widgetModelDao.update(WidgetModel(model.widgetId, model.contactId, model.displayName, model.photo, unread))
                SociaLiteAppWidget().updateAll(appContext)
            }
        }
    }

    suspend fun getImage(url: Uri, force: Boolean = false, context: Context): Bitmap? {
        val request =
            ImageRequest.Builder(context).transformations(CircleCropTransformation()).data(url)
                .apply {
                    if (force) {
                        memoryCachePolicy(CachePolicy.DISABLED)
                        diskCachePolicy(CachePolicy.DISABLED)
                    }
                }.build()

        // Request the image to be loaded and throw error if it failed
        return when (val result = Coil.imageLoader(context).execute(request)) {
            is ErrorResult -> {
                Log.e("GLANCE", "Error " + result.throwable.message)
                throw result.throwable
            }

            is SuccessResult -> result.drawable.toBitmapOrNull()
        }
    }
}

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

package com.google.android.samples.socialite.di

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.video.MediaStoreOutputOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ImageContentValues

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class VideoContentValues

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @ImageContentValues
    fun providesImageContentValues(): ContentValues = ContentValues().apply {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SociaLite")
        }
    }

    @Provides
    @VideoContentValues
    fun providesVideoContentValues(): ContentValues = ContentValues().apply {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"

        put(MediaStore.Video.Media.DISPLAY_NAME, name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SociaLite")
        }
    }

    @Provides
    @Singleton
    fun providesImageCaptureOutputFileOptions(
        @ApplicationContext context: Context,
        @ImageContentValues contentValues: ContentValues,
    ): OutputFileOptions =
        OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        ).build()

    @Provides
    @Singleton
    fun providesMediaStoreOutputOptions(
        @ApplicationContext context: Context,
        @VideoContentValues contentValues: ContentValues,
    ): MediaStoreOutputOptions =
        MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
            .setContentValues(contentValues)
            .build()
}

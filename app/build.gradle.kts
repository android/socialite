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

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets)
    alias(libs.plugins.download)
    alias(libs.plugins.parcelize)
}

kotlin {
    jvmToolchain(17)
}

secrets {
    defaultPropertiesFileName = "secret.defaults.properties"
}

android {
    namespace = "com.google.android.samples.socialite"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.google.android.samples.socialite"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lintOptions {
        disable("RestrictedApi") // Disabled to use Media3's CompositionPlayer API
    }
}

project.ext.set("ASSET_DIR", "$projectDir/src/main/assets")
project.ext.set("TEST_ASSETS_DIR", "$projectDir/src/androidTest/assets")
// Download default models; if you wish to use your own models then
// place them in the "assets" directory and comment out this line.
apply {
    from("download_model.gradle")
}

dependencies {
    implementation(libs.adaptive)
    implementation(libs.adaptive.layout)
    implementation(libs.adaptive.navigation)

    implementation(libs.core.ktx)
    implementation(libs.camera.extensions)
    implementation(libs.profileinstaller)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material)
    implementation(libs.camera.media3.effect)
    implementation(libs.vision.common)
    implementation(libs.segmentation.selfie)
    implementation(libs.litert)
    implementation(libs.litert.gpu)
    implementation(libs.litert.support.api)

    testImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    baselineProfile(project(":baselineprofile"))

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.android.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text.google.fonts)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.compose.material.icons)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manisfest)

    implementation(libs.activity.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.accompanist.painter)
    implementation(libs.accompanist.permissions)

    implementation(libs.graphics.shapes)

    implementation(libs.lifecycle.ktx)
    implementation(libs.lifecycle.compose)

    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    androidTestImplementation(libs.room.testing)

    implementation(libs.splashscreen)
    implementation(libs.concurrent.kts)

    implementation(libs.core.performance)
    implementation(libs.core.performance.play.services)

    implementation(libs.camera.core)
    implementation(libs.camera.compose)
    implementation(libs.camera2)
    implementation(libs.camera.effects)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    implementation(libs.media3.common)
    implementation(libs.media3.effect)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.transformer)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.window)

    androidTestImplementation(libs.turbine)

    // For photopicker feature
    implementation(libs.activity)

    implementation(libs.coil)
    implementation(libs.coil.compose)

    implementation(libs.generativeai)
    implementation(libs.datastore)

    implementation(libs.adaptive.navigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
}

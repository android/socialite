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
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.example.android.social"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.android.social"
        minSdk = 21
        targetSdk = 34
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
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.camera.extensions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text.google.fonts)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manisfest)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    implementation(libs.accompanist.painter)
    implementation(libs.accompanist.permissions)

    implementation(libs.graphics.shapes)

    implementation(libs.lifecycle.ktx)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.runtime.compose)

    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    androidTestImplementation(libs.room.testing)

    implementation(libs.splashscreen)
    implementation(libs.concurrent.kts)

    implementation(libs.camera.core)
    implementation(libs.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    implementation (libs.media3.common)
    implementation (libs.media3.effect)
    implementation (libs.media3.exoplayer)
    implementation (libs.media3.transformer)
    implementation (libs.media3.ui)

    androidTestImplementation(libs.turbine)

    implementation(libs.coil)
    implementation(libs.coil.compose)
}

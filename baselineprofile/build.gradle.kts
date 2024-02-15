import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.google.android.samples.socialite.baselineprofile"
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    testOptions.managedDevices.devices {
        create<ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "google"
        }
    }
}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.ext.junit)
    implementation(libs.espresso.core)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}

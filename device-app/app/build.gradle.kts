plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.lanyard.payload"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "dev.lanyard.payload"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Pinned to the versions that are both present in the local Gradle cache
    // and accepted by AGP 8.13.2 / compileSdk 36, so :app:assembleDebug still
    // works with --offline.
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.compose.ui:ui:1.9.1")
    implementation("androidx.compose.ui:ui-graphics:1.9.1")
    implementation("androidx.compose.ui:ui-text:1.9.1")
    implementation("androidx.compose.foundation:foundation:1.9.1")
    implementation("androidx.compose.foundation:foundation-layout:1.9.1")
    implementation("androidx.compose.animation:animation:1.9.1")
    implementation("androidx.compose.material:material-ripple:1.9.1")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.2")

    testImplementation("junit:junit:4.13.2")
}

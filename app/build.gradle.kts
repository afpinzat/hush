plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)

}

android {
    namespace = "com.pinza.hush"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pinza.hush"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}

kapt {
    correctErrorTypes = true
}



dependencies {


// DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
// Coroutines (necesario para DataStore)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")    // Android Core

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Hilt
    implementation(libs.dagger.hilt)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.compose.remote.creation.core)
    kapt(libs.dagger.hilt.compiler)
    kapt(libs.androidx.room.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation(libs.androidx.recyclerview)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Unit Tests
    testImplementation(libs.junit)

    // Android Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Media3 / ExoPlayer — reproductor de audio/video de Google
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

// Media support para notificaciones (MediaStyle)
    implementation("androidx.media:media:1.7.0")

// LifecycleService (para MusicPlayerService)
    implementation("androidx.lifecycle:lifecycle-service:2.8.2")

    // Coil — para cargar imágenes (portadas)
    implementation("io.coil-kt:coil:2.6.0")

    implementation("androidx.media3:media3-exoplayer:1.1.1")
    implementation("androidx.media3:media3-ui:1.1.1")
    implementation("androidx.media3:media3-common:1.1.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    implementation("androidx.media:media:1.6.0")
}
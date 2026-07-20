
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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pinza.hush"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }
}

kapt {
    correctErrorTypes = true
}



dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.palette)

    // --- Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // --- Hilt ---
    implementation(libs.dagger.hilt)
    kapt(libs.dagger.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- Lifecycle (Usa versiones consistentes) ---
    val lifecycle_version = "2.8.4" // Usa una versión reciente y única
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycle_version")

    // --- Media3 (ExoPlayer) - MANTÉN SOLO ESTO ---
    val media3_version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-session:$media3_version")
    implementation("androidx.media3:media3-common:$media3_version")

    // --- Data, Firebase & Utils ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation("io.coil-kt:coil-compose:2.6.0") // Para Compose es mejor coil-compose
    implementation("io.coil-kt:coil:2.6.0") // Para Fragments (ViewBinding) usamos coil-core


    // --- Room ---
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Fragment KTX (proporciona 'by viewModels()')
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    // ... (Mantén tus bloques de test y debug iguales)
    val nav_version = "2.8.0" // O la versión más reciente disponible

    // Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")

    // Esta librería es la que contiene el fix para el error de InputManager
    androidTestImplementation("androidx.test:monitor:1.7.2")

    androidTestImplementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.android)

}

    configurations.all {
        resolutionStrategy {
            force("androidx.test:monitor:1.7.2")
        }
}
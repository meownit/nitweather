plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
//    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

android {
    namespace = "com.example.weather2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.weather2"
        minSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.tasks)
    implementation(libs.transport.api)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Ktor for networking
    implementation("io.ktor:ktor-client-core:2.3.10")
    implementation("io.ktor:ktor-client-cio:2.3.10") // CIO is the engine for Ktor
    implementation("io.ktor:ktor-client-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")

    // ViewModel for Jetpack Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")


    // Google Play Services for location
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // For Material Design icons like LocationOn and Add
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // ADD THIS LINE
    implementation("androidx.compose.material3:material3:1.3.0-alpha01")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")// Use the latest version
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")// For viewModelScope
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // For coroutines

    // ... other dependencies

}
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.marcusalemao.rokidlive"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.marcusalemao.rokidlive"
        minSdk = 27          // Android dos Rokid Glasses; sobe se necessário
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-vision"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Core + coroutines
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Camera: CameraX com Fallback pra desenvolvimento no celular.
    // Nos óculos, a captura real é via CXR SDK — ver camera/CxrFrameSource.kt
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")

    // HTTP — sem Retrofit, só o essencial
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Lifecycle (foreground service)
    implementation("androidx.lifecycle:lifecycle-service:2.8.3")

    // CXR SDK (Rokid): adicionar o AAR local aqui quando plugar a captura nativa.
    // Ex.: implementation(files("libs/cxr-sdk.aar"))
}

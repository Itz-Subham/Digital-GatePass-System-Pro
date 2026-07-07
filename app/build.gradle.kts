plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gatepass"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gatepass"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {
    // OCR & Barcode Brain
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // QR Generation
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Camera Engine
    val camerax_version = "1.4.0-rc01"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-firestore")

    // UI & Logic Essentials
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.browser:browser:1.8.0")
}

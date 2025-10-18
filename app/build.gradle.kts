plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Agregado: plugin de Google Services para procesar google-services.json
    id("com.google.gms.google-services")
    // Agregado: KAPT para Room (sintaxis kotlin DSL)
    kotlin("kapt")
}

android {
    namespace = "com.universidad.streamzone"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universidad.streamzone"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase (usar versiones explícitas para evitar problemas de resolución)
    // implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx:22.1.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.7.1")
    // Opcional: firebase-analytics fallaba en tu entorno (artifact no encontrado), comento temporalmente
    // implementation("com.google.firebase:firebase-analytics-ktx:21.6.0")
    implementation("com.google.firebase:firebase-storage-ktx:20.2.0")

    // Room (local database)
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")
}
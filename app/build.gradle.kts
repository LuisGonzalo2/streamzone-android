plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
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

    // --- CAMBIO IMPORTANTE AQUÍ ---
    // Actualizado a Java 17, que es el estándar para las herramientas de Android más recientes.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Alinear la versión de la JVM de Kotlin con la de Java.
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
         viewBinding = true
    }
}

dependencies {
    // Tus dependencias se ven bastante actualizadas y bien estructuradas
    // usando el BOM de Firebase y el catálogo de versiones (libs).
    // No se necesitan cambios urgentes aquí.

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Image loading (Coil) para cargar logos remotos/locales en los diálogos
    implementation("io.coil-kt:coil:2.7.0")

    // Firebase BOM - Plataforma (actualizada a 33.6.0)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    // Firebase productos (las versiones las maneja el BOM)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    //Libreria para el numero de telefno
    implementation("com.hbb20:ccp:2.7.3")

    // ExifInterface para corregir rotación de imágenes
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}

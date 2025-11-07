plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.evalua.classter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.evalua.classter"
        minSdk = 26
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
    buildFeatures {
        viewBinding = true
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
    implementation(platform(libs.firebase.bom))

    implementation("com.google.firebase:firebase-auth-ktx")

    // Firebase UI
    implementation(libs.firebase.ui.auth)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // ZXing para generar QR
    implementation(libs.core)

    // Para manejo de archivos
    implementation(libs.androidx.documentfile)

    //Tablas
    implementation(libs.tableView)

    // SwipeRefreshLayout
    implementation(libs.androidx.swiperefreshlayout)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Material Design - SOLO UNA VERSIÓN
    implementation(libs.material)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
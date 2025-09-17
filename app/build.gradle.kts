plugins {
    // Apply the plugins directly, no aliases
    id("com.android.application")        // ← changed from alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")  // ← changed from alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.theescapeplan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.theescapeplan"
        minSdk = 25
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.11.0")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // downgraded

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")   // downgraded
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2") // downgraded

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0") // downgraded
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")       // downgraded

    // Jetpack Compose graphics (if you need it)

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")  // updated to compatible version
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

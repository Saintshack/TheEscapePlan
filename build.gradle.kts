// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Explicit plugin versions instead of TOML catalog
    id("com.android.application") version "8.4.2" apply false   // ← changed from alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false  // ← changed from alias(libs.plugins.kotlin.android)
}
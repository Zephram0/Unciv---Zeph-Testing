pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("io.gitlab.arturbosch.detekt") apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

// Make versions available to all projects
ext {
    set("gdxVersion", "1.12.1")
    set("kotlinVersion", "1.9.21")  // Add explicit Kotlin version
}

// Define dependency versions retrieved from gradle.properties
val gdxVersion: String by project
val coroutinesVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val lwjglVersion: String by project
val appVersion: String by project
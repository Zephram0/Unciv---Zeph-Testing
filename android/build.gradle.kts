// android/build.gradle.kts

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "com.unciv.app.android" // Replace with your actual namespace
    compileSdk = 33

    defaultConfig {
        applicationId = "com.unciv.app.android"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = project.findProperty("appVersion") as String? ?: "1.0.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Proguard rules can be added here
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            // Debug-specific configurations if needed
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    // Add other Android-specific dependencies here
}

detekt {
    // Point to your detekt configuration file
    config = files("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    // Optional: Specify reports to be generated
    reports {
        html.enabled = true
        xml.enabled = false
        txt.enabled = false
    }
}
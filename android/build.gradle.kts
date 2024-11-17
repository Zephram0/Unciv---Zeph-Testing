plugins {
    kotlin("android")
    id("com.android.application")
}

val gdxVersion: String by project
val kotlinVersion: String by project
val coroutinesVersion: String by project
val ktorVersion: String by project

android {
    namespace = "com.unciv.app"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.unciv.app"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    packaging {
        resources {
            excludes += listOf(
                // Existing exclusions
                "META-INF/macos/**",
                "META-INF/windows/**",
                "META-INF/linux/**",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "META-INF/INDEX.LIST",
                // Add these new exclusions
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/versions/**"
            )
            pickFirsts += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
}
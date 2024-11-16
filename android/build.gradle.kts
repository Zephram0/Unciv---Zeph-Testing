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

    signingConfigs {
        create("release") {
            // You'll need to create a keystore file and provide these values
            storeFile = file("release-key.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: project.properties["KEYSTORE_PASSWORD"].toString()
            keyAlias = System.getenv("KEY_ALIAS") ?: project.properties["KEY_ALIAS"].toString()
            keyPassword = System.getenv("KEY_PASSWORD") ?: project.properties["KEY_PASSWORD"].toString()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
}
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
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
}
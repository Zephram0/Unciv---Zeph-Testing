plugins {
    kotlin("multiplatform") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0" apply false
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
}

// Define dependency versions retrieved from gradle.properties
val gdxVersion: String by project
val coroutinesVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val lwjglVersion: String by project
val appVersion: String by project
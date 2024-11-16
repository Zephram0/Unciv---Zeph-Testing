pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "1.9.21"
        kotlin("plugin.serialization") version "1.9.21"
        kotlin("android") version "1.9.21"
        id("com.android.application") version "8.5.0"
        id("com.android.library") version "8.5.0"
        id("io.gitlab.arturbosch.detekt") version "1.23.0"
    }
}

rootProject.name = "Unciv---Zeph-Testing"
include("core", "desktop", "android", "server")
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "1.9.21"
        kotlin("multiplatform") version "1.9.21"
        kotlin("plugin.serialization") version "1.9.21"
        kotlin("android") version "1.9.21"
        // Change these versions to be compatible
        id("com.android.application") version "8.2.2"
        id("com.android.library") version "8.2.2"
        id("io.gitlab.arturbosch.detekt") version "1.23.0"
    }
}

rootProject.name = "Unciv---Zeph-Testing"
include("core", "desktop", "android", "server", "tests")
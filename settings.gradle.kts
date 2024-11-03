pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "Unciv"

include("desktop", "core", "tests", "server")
if (System.getenv("ANDROID_HOME") != null) include("android")
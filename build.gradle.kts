plugins {
    // Base Kotlin plugins
    kotlin("jvm") version "1.9.21" apply false
    kotlin("android") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    
    // Android plugins
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    
    // Other plugins
    id("io.gitlab.arturbosch.detekt") version "1.23.0" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }

    configurations.all {
        resolutionStrategy {
            // Force Kotlin and related dependencies
            force(
                // Kotlin dependencies
                "org.jetbrains.kotlin:kotlin-stdlib:1.9.21",
                "org.jetbrains.kotlin:kotlin-stdlib-common:1.9.21",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.21",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.21",
                
                // Coroutines dependencies
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2",
                "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2",
                "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.2",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.2",
                "org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.7.2",
                "org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.7.2",
                
                // SLF4J
                "org.slf4j:slf4j-api:1.7.36",
                
                // JNA
                "net.java.dev.jna:jna:5.14.0",
                "net.java.dev.jna:jna-platform:5.14.0",
                
                // Other dependencies
                "org.jetbrains:annotations:23.0.0"
            )
            
            failOnVersionConflict()
            cacheDynamicVersionsFor(24, "hours")
            cacheChangingModulesFor(24, "hours")
        }
    }
}

tasks.register("jnaDependencyInsight") {
    doLast {
        configurations.forEach { config ->
            println("Configuration: ${config.name}")
            config.resolvedConfiguration.firstLevelModuleDependencies
                .filter { it.moduleGroup == "net.java.dev.jna" }
                .forEach { dep ->
                    println(" - ${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")
                }
        }
    }
}


// Make versions available to all projects
project.ext {
    set("gdxVersion", project.property("gdxVersion") as String)
    set("kotlinVersion", project.property("kotlinVersion") as String)
    set("coroutinesVersion", project.property("coroutinesVersion") as String)
    set("ktorVersion", project.property("ktorVersion") as String)
    set("lwjglVersion", project.property("lwjglVersion") as String)
}
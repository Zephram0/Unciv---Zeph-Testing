plugins {
    kotlin("multiplatform") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }

    configurations.all {
        resolutionStrategy {
            // Enforce specific JNA versions
            force("net.java.dev.jna:jna:5.14.0")
            force("net.java.dev.jna:jna-platform:5.14.0")
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
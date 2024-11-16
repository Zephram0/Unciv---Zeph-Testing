plugins {
    kotlin("jvm")
    application
}

val gdxVersion: String by project
val kotlinVersion: String by project

sourceSets {
    main {
        kotlin.srcDir("src")
        resources.srcDir("../android/assets")
    }
}

configurations {
    all {
        resolutionStrategy {
            force("net.java.dev.jna:jna:5.14.0")
            force("net.java.dev.jna:jna-platform:5.14.0")
            cacheDynamicVersionsFor(24, "hours")
            cacheChangingModulesFor(24, "hours")
        }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

// Add JVM args for Mac if needed
val jvmArgsForMac = listOf("-XstartOnFirstThread", "-Djava.awt.headless=true")
tasks.run {
    if ("mac" in System.getProperty("os.name").lowercase()) {
        jvmArgs = jvmArgsForMac
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("com.github.Vatuu:discord-rpc:1.6.2")
}

application {
    mainClass.set("com.unciv.app.desktop.DesktopLauncher")
}
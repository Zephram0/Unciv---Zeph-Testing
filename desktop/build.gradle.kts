plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

val gdxVersion: String by project
val kotlinVersion: String by project
val coroutinesVersion: String by project
val ktorVersion: String by project

sourceSets {
    main {
        kotlin.srcDir("src")
        resources.srcDir("src/main/resources")
        resources.srcDir("../android/assets")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("com.github.Vatuu:discord-rpc:1.6.2") {
        exclude(group = "net.java.dev.jna")
        exclude(group = "net.java.dev.jna", module = "jna-platform")
    }
}

application {
    mainClass.set("com.unciv.app.desktop.DesktopLauncher")
}

tasks.named<JavaExec>("run") {
    if ("mac" in System.getProperty("os.name").lowercase()) {
        jvmArgs = listOf("-XstartOnFirstThread", "-Djava.awt.headless=true")
    }
}
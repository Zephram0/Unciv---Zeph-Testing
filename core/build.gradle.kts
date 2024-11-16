plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

sourceSets {
    main {
        kotlin.srcDir("src")
        resources.srcDir("../android/assets")
    }
}

val gdxVersion: String by project
val coroutinesVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    api("net.java.dev.jna:jna:5.14.0")
    api("net.java.dev.jna:jna-platform:5.14.0")
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    api("io.ktor:ktor-client-auth:$ktorVersion")
    api("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.mockk:mockk:1.13.8")
}
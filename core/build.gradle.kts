plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val gdxVersion: String by project
val coroutinesVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")

    // Ensure JNA dependencies are exposed to other modules
    api("net.java.dev.jna:jna:5.14.0")
    api("net.java.dev.jna:jna-platform:5.14.0")

    // Kotlin Reflect
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    
    // Ktor Client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.mockk:mockk:1.13.8")
    
    // Other dependencies
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
}
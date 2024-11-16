plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val gdxVersion: String by project
val coroutinesVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project  // Add this line

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")

    // Add this near the top of the dependencies block
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")  // Use same version as Kotlin
    
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
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("../android/assets")
    }
}
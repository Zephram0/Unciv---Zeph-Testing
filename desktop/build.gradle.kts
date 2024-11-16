plugins {
    kotlin("jvm")
    application
}

sourceSets {
    main {
        java.srcDir("src")
    }
}

val gdxVersion: String by rootProject.ext

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.unciv.app.desktop.DesktopLauncher")
}
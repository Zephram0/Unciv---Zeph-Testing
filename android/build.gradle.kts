import com.unciv.build.AndroidImagePacker
import com.unciv.build.BuildConfig
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 34
    sourceSets {
        getByName("main").apply {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            aidl.srcDirs("src")
            renderscript.srcDirs("src")
            res.srcDirs("res")
            assets.srcDirs("assets")
            jniLibs.srcDirs("libs")
        }
    }
    packaging {
        resources.excludes += "META-INF/robovm/ios/robovm.xml"
        resources.excludes += "DebugProbesKt.bin"
    }
    defaultConfig {
        namespace = "com.zephram0.uncivmod" // Ensure the identifier matches your new package
        applicationId = "com.zephram0.uncivmod"
        minSdk = 21
        targetSdk = 34
        versionCode = 1 // Adjust this if necessary
        versionName = "1.0" // Modify based on your versioning

        base.archivesName.set("UncivMod") // Change the name if necessary
    }

    kotlinOptions {
        jvmTarget = "1.8" // Ensure JVM target is set to a valid value
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("android/debug.keystore")
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            isDebuggable = false
        }
    }

    lint {
        disable += "MissingTranslation"
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    androidResources {
        ignoreAssetsPattern = "!SaveFiles:!fonts:!maps:!music:!mods"
    }

    buildFeatures {
        renderScript = true
        aidl = true
    }
}

task("texturePacker") {
    doFirst {
        logger.info("Calling TexturePacker")
        AndroidImagePacker.packImages(projectDir.path)
    }
}

task("copyAndroidNatives") {
    val natives: Configuration by configurations

    doFirst {
        val rx = Regex(""".*natives-([^.]+)\.jar$""")
        natives.forEach { jar ->
            if (rx.matches(jar.name)) {
                val outputDir = file(rx.replace(jar.name) { "libs/" + it.groups[1]!!.value })
                outputDir.mkdirs()
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
    dependsOn("texturePacker")
}

tasks.whenTaskAdded {
    if ("package" in name || "assemble" in name || "bundleRelease" in name) {
        dependsOn("copyAndroidNatives")
    }
}

tasks.register<JavaExec>("run") {
    val localProperties = project.file("../local.properties")
    val path = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
    } else {
        System.getenv("ANDROID_HOME")
    }

    val adb = "$path/platform-tools/adb"

    doFirst {
        project.exec {
            commandLine(adb, "shell", "am", "start", "-n", "com.zephram0.uncivmod/AndroidLauncher")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}

import com.unciv.build.AndroidImagePacker
import com.unciv.build.BuildConfig
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("androidx.profileinstaller") version "1.2.0"
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
        resources.excludes += "DebugProbesKt.bin"  // part of kotlinx-coroutines-android, should not go into the apk
    }
    defaultConfig {
        namespace = BuildConfig.identifier
        applicationId = BuildConfig.identifier
        minSdk = 21
        targetSdk = 34  // See #5044
        versionCode = BuildConfig.appCodeNumber
        versionName = BuildConfig.appVersion

        base.archivesName.set("Unciv")
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("android/debug.keystore")
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
        }
        create("release") {
            storeFile = rootProject.file("my-release-key.jks")  // Path to your release keystore file
            keyAlias = "Unciv Zeph"  // Replace with your alias name used during keystore generation
            keyPassword = "1234qwer"  // Replace with the key password you used
            storePassword = "1234qwer"  // Replace with the store password you used
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")  // Add this line
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            isDebuggable = false
            matchingFallbacks += listOf("profile")  // Ensure the baseline profile is included in release build
        }
    }

    lint {
        disable += "MissingTranslation"   // see res/values/strings.xml
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    androidResources {
        ignoreAssetsPattern = "!SaveFiles:!fonts:!maps:!music:!mods"  // Don't add local save files and fonts to release, obviously
    }
    buildFeatures {
        renderScript = false  // Disable to improve build speed if not needed
        aidl = false  // Disable to improve build speed if not needed
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
            commandLine(adb, "shell", "am", "start", "-n", "com.unciv.app/AndroidLauncher")
        }
    }
}

tasks.register("generateBaselineProfile") {
    doFirst {
        println("Generating Baseline Profile for performance optimizations")
    }
    // Placeholder: You would run specific UI actions here if automating profile generation
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
    implementation("androidx.profileinstaller:profileinstaller:1.2.0")
}

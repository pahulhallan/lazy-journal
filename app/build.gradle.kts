import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun loadDotEnv(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()

    return file.readLines()
        .map { it.trim() }
        .filter { line ->
            line.isNotEmpty() && !line.startsWith("#") && line.contains("=")
        }
        .associate { line ->
            val key = line.substringBefore("=").trim()
            val rawValue = line.substringAfter("=").trim()
            val value = rawValue
                .removeSurrounding("\"")
                .removeSurrounding("'")
            key to value
        }
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val dotEnv = loadDotEnv(rootProject.file(".env"))

fun envValue(name: String, fallback: String = ""): String {
    return dotEnv[name]
        ?: System.getenv(name)
        ?: fallback
}

android {
    namespace = "com.lazyjournal.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lazyjournal.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            "String",
            "HUGGING_FACE_TOKEN",
            envValue("HUGGING_FACE_TOKEN").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "HUGGING_FACE_ENDPOINT",
            envValue("HUGGING_FACE_ENDPOINT", "https://huggingface.co").asBuildConfigString()
        )

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)

    ksp(libs.room.compiler)

    debugImplementation(libs.compose.ui.tooling)
}

val verifyNoInternetPermission = tasks.register("verifyNoInternetPermission") {
    group = "verification"
    description = "Fails the build if Lazy Journal declares android.permission.INTERNET."

    val manifestFiles = fileTree("src") {
        include("**/AndroidManifest.xml")
    }

    inputs.files(manifestFiles)

    doLast {
        val offenders = manifestFiles.files.filter { manifest ->
            manifest.readText().contains("android.permission.INTERNET")
        }

        check(offenders.isEmpty()) {
            "Lazy Journal must stay offline by default. Remove android.permission.INTERNET from: " +
                offenders.joinToString { it.relativeTo(projectDir).path }
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(verifyNoInternetPermission)
}

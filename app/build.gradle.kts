import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.footprint"
    compileSdk = 35
    layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("app_build_fresh"))

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

    defaultConfig {
        applicationId = "com.footprint"
        minSdk = 24
        targetSdk = 35
        versionCode = 57
        versionName = "3.8.1"

        manifestPlaceholders["AMAP_KEY"] = localProperties.getProperty("AMAP_KEY") ?: "YOUR_AMAP_API_KEY"

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            // 仅保留主流手机 64位 架构，移除 32位 和 x86 架构能显著减小体积
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
        // Note: xxhdpi filtering is now handled through splitting if needed, 
        // but for now, we only move the locale filters as suggested by the warning.
        resourceConfigurations += listOf("zh", "zh-rCN", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // 禁用混淆，保证 Release 版本功能与 Debug 完全一致
            isShrinkResources = false // 禁用资源缩减，防止误删图片处理相关资源
            // 关键：确保子项目（如 Flutter 模块）也使用 Release 模式
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += listOf("release")
            
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
        debug {
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xcontext-receivers")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/rust/**"
            excludes += "**/target/**"
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += "**/libVkLayer_khronos_validation.so"
            excludes += "**/libflutter.so.debug"
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir("src/main/jniLibs")
        }
    }

    configurations.all {
        resolutionStrategy {
            force("androidx.core:core-ktx:1.15.0")
            force("androidx.core:core:1.15.0")
            force("androidx.activity:activity:1.9.3")
            force("androidx.activity:activity-compose:1.9.3")
            force("androidx.activity:activity-ktx:1.9.3")
            force("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.6")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // AMap (高德地图) SDK
    implementation("com.amap.api:3dmap:latest.integration")

    // JSON Parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Haze (Glassmorphism)
    implementation("dev.chrisbanes.haze:haze-jetpack-compose:0.5.2")

    // Flutter Module
    "debugImplementation"(project(path = ":flutter", configuration = "debugRuntimeElements"))
    "releaseImplementation"(project(path = ":flutter", configuration = "releaseRuntimeElements"))
}

val buildRustTask = tasks.register<Exec>("buildRust") {
    workingDir = file("rust")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-o", "../src/main/jniLibs",
        "build", "--release"
    )
}

tasks.named("preBuild") {
    dependsOn(buildRustTask)
}

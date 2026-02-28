pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

rootProject.name = "Footprint"
include(":app")

// Kotlin DSL 引入 Flutter 模块的兼容写法
val flutterProjectRoot = file("flutter_ui")
apply(from = flutterProjectRoot.resolve(".android/include_flutter.groovy"))
project(":flutter").projectDir = flutterProjectRoot.resolve(".android/Flutter")

// 全局强制禁用 includeAndroidResources 修复 Windows 跨盘符路径 roots 错误
gradle.allprojects {
    afterEvaluate {
        extensions.findByName("android")?.let { android ->
            try {
                val testOptions = android.javaClass.getMethod("getTestOptions").invoke(android)
                val unitTests = testOptions.javaClass.getMethod("getUnitTests").invoke(testOptions)
                unitTests.javaClass.getMethod("setIncludeAndroidResources", Boolean::class.javaPrimitiveType).invoke(unitTests, false)
            } catch (e: Exception) {
                // Ignore if not a standard Android project
            }
        }
    }
}

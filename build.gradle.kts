import com.android.build.gradle.BaseExtension

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    // Inject fake flutter config if not exists to fix AGP 8.x+ error with some plugins
    // We use a Map so Groovy can access it via property syntax (flutter.compileSdkVersion)
    if (project.name == "flutter_plugin_android_lifecycle" || project.name.contains("city_picker")) {
        project.extensions.extraProperties.set("flutter", mapOf(
            "compileSdkVersion" to 35,
            "minSdkVersion" to 21,
            "targetSdkVersion" to 35
        ))
    }

    // Aggressive fix for "Namespace not specified" and low compileSdk in AGP 8.x+ for Flutter plugins
    project.afterEvaluate {
        if (project.hasProperty("android")) {
            val android = project.extensions.getByName("android")
            
            // 1. Namespace Fix
            try {
                val getNamespace = android.javaClass.getMethod("getNamespace")
                val namespace = getNamespace.invoke(android)
                if (namespace == null) {
                    val targetNamespace = when {
                        project.name == "amap_flutter_location" -> "com.amap.flutter.location"
                        project.name == "flutter_plugin_android_lifecycle" -> "io.flutter.plugins.android_lifecycle"
                        project.name.contains("_") -> "com.footprint." + project.name.replace("_", ".")
                        else -> "com.footprint." + project.name
                    }
                    val setNamespace = android.javaClass.getMethod("setNamespace", String::class.java)
                    setNamespace.invoke(android, targetNamespace)
                    println("Manually set namespace for ${project.name} to $targetNamespace")
                }
            } catch (e: Exception) {}

            // 2. CompileSdk Fix (Ensure at least 36)
            try {
                var currentSdkVersion: Int? = null
                
                // Try to get current SDK version
                try {
                    val getCompileSdk = android.javaClass.getMethod("getCompileSdk")
                    currentSdkVersion = getCompileSdk.invoke(android) as? Int
                } catch (e: Exception) {
                    try {
                        val getCompileSdkVersion = android.javaClass.getMethod("getCompileSdkVersion")
                        val sdkStr = getCompileSdkVersion.invoke(android) as? String
                        if (sdkStr != null && sdkStr.startsWith("android-")) {
                            currentSdkVersion = sdkStr.substringAfter("android-").toIntOrNull()
                        }
                    } catch (e2: Exception) {}
                }

                if (currentSdkVersion != null && currentSdkVersion < 36) {
                    // Try setCompileSdk(int) first
                    try {
                        val setCompileSdk = android.javaClass.getMethod("setCompileSdk", java.lang.Integer.TYPE)
                        setCompileSdk.invoke(android, 36)
                        println("Forced compileSdk to 36 for ${project.name} (was $currentSdkVersion)")
                    } catch (e: Exception) {
                        // Fallback to setCompileSdkVersion(String)
                        val setCompileSdkVersion = android.javaClass.getMethod("setCompileSdkVersion", String::class.java)
                        setCompileSdkVersion.invoke(android, "android-36")
                        println("Forced compileSdkVersion to android-36 for ${project.name} (was $currentSdkVersion)")
                    }
                }
            } catch (e: Exception) {}
        }
    }

    afterEvaluate {
        (extensions.findByName("android") as? BaseExtension)?.apply {
            testOptions.unitTests.isIncludeAndroidResources = false
        }
    }
}

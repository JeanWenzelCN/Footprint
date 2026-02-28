plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false
}

import com.android.build.gradle.BaseExtension

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    afterEvaluate {
        (extensions.findByName("android") as? BaseExtension)?.apply {
            testOptions.unitTests.isIncludeAndroidResources = false
        }
    }
}

plugins {
    kotlin("jvm") version "1.9.22" // Use any available kotlin version
}
repositories {
    google()
    mavenCentral()
}
dependencies {
    implementation("androidx.compose.foundation:foundation:1.7.0-beta02") // guessing version
}
tasks.register("downloadSources") {
    doLast {
        configurations.getByName("implementation").resolvedConfiguration.resolvedArtifacts.forEach {
            println("SRC: " + it.file)
        }
    }
}

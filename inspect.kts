
import java.net.URLClassLoader
import java.io.File

fun main() {
    val dir = File("C:/Users/xhq/.gradle/caches")
    val jars = dir.walkTopDown().filter { it.extension == "jar" && it.name.contains("foundation-android-1.9") }.toList()
    val urls = jars.map { it.toURI().toURL() }.toTypedArray()
    val cl = URLClassLoader(urls, ClassLoader.getSystemClassLoader())
    try {
        val clazz = cl.loadClass("androidx.compose.foundation.gestures.AnchoredDraggableState")
        println("Constructors:")
        clazz.constructors.forEach { println(" - " + it) }
        println("Methods:")
        clazz.methods.filter { it.name.contains("animateTo") || it.name.contains("requireOffset") }.forEach { println(" - " + it) }
    } catch (e: Exception) {
        println("Class not found or error: $e")
    }
}

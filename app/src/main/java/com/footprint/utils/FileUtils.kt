package com.footprint.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    fun zipDirectory(sourceDir: File, outputStream: OutputStream) {
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = sourceDir.toPath().relativize(file.toPath()).toString().replace("\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    fun unzip(inputStream: InputStream, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    fun copyFile(source: File, dest: File) {
        source.inputStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}

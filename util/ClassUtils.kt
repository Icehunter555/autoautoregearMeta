package dev.wizard.meta.util

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.nio.file.Paths
import java.util.Enumeration
import java.util.function.Predicate
import java.util.jar.JarInputStream

object ClassUtils {
    fun findClasses(packageName: String, predicate: Predicate<String> = Predicate { true }): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = packageName.replace('.', '/')
        
        val root: URL = if (packageName.startsWith("dev.wizard.meta")) {
            val thisFileName = this::class.java.name.replace('.', '/') + ".class"
            val thisURL = classLoader.getResource(thisFileName)!!
            val file = thisURL.file.substringBeforeLast(thisFileName.substringAfter("dev/wizard/meta"))
            URL(thisURL.protocol, thisURL.host, file)
        } else {
            classLoader.getResource(packagePath)!!
        }

        val isJar = root.toString().startsWith("jar")
        if (isJar) {
            val path = Paths.get(URL(root.path.substringBeforeLast('!')).toURI())
            findClassesInJar(classLoader, path.toFile(), packagePath, predicate, classes)
        } else {
            val resources: Enumeration<URL>? = classLoader.getResources(packagePath)
            resources?.asSequence()?.forEach { url ->
                findClasses(classLoader, File(url.file), packageName, predicate, classes)
            }
        }
        return classes
    }

    private fun findClasses(classLoader: ClassLoader, directory: File, packageName: String, predicate: Predicate<String>, list: MutableList<Class<*>>) {
        if (!directory.exists()) return
        val packagePath = packageName.replace('.', File.separatorChar)
        directory.walk()
            .filter { it.isFile && it.extension == "class" }
            .map { it.path.substringAfter(packagePath) }
            .map { it.replace(File.separatorChar, '.') }
            .map { it.substring(0, it.length - 6) }
            .map { packageName + it }
            .filter { predicate.test(it) }
            .map { Class.forName(it, false, classLoader) }
            .toCollection(list)
    }

    private fun findClassesInJar(classLoader: ClassLoader, jarFile: File, packageName: String, predicate: Predicate<String>, list: MutableList<Class<*>>) {
        JarInputStream(BufferedInputStream(FileInputStream(jarFile), 0x100000)).use { jar ->
            var entry = jar.nextJarEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name
                    if (name.startsWith(packageName) && name.endsWith(".class")) {
                        val className = name.replace('/', '.').substring(0, name.length - 6)
                        if (predicate.test(className)) {
                            val clazz = Class.forName(className, false, classLoader)
                            list.add(clazz)
                        }
                    }
                }
                entry = jar.nextJarEntry
            }
        }
    }

    fun <T> Class<out T>.getInstance(): T {
        return this.getDeclaredField("INSTANCE").get(null) as T
    }
}

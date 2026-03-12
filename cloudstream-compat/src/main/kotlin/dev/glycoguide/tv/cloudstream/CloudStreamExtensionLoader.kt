package dev.glycoguide.tv.cloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorRegistry
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.*

/**
 * Loads CloudStream3 extension JARs/DEX files and extracts MainAPI providers.
 *
 * CloudStream extensions are normally Android APKs containing DEX code.
 * For desktop compatibility, this loader supports:
 * 1. Repackaged JARs (CS extensions recompiled to JVM bytecode)
 * 2. JARs built from CS extension source code against our compat shim
 *
 * The loader scans for classes annotated with @CloudstreamPlugin,
 * instantiates the Plugin, calls load(), then collects registered APIs.
 */
class CloudStreamExtensionLoader {

    data class LoadResult(
        val apis: List<MainAPI>,
        val errors: List<String>,
    )

    /**
     * Load a single CloudStream extension JAR.
     */
    fun loadExtension(jarPath: Path): LoadResult {
        val errors = mutableListOf<String>()
        val apis = mutableListOf<MainAPI>()

        if (!jarPath.exists() || !jarPath.name.endsWith(".jar")) {
            errors.add("Datei nicht gefunden oder kein JAR: $jarPath")
            return LoadResult(apis, errors)
        }

        try {
            val classLoader = URLClassLoader(
                arrayOf(jarPath.toUri().toURL()),
                this::class.java.classLoader,
            )

            // Clear previous registrations
            CloudStreamRegistrar.clearRegistered()

            // Strategy 1: Look for @CloudstreamPlugin annotated classes
            val pluginClasses = findPluginClasses(jarPath, classLoader)

            if (pluginClasses.isNotEmpty()) {
                for (pluginClass in pluginClasses) {
                    try {
                        val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin
                        plugin.load(null)
                    } catch (e: Exception) {
                        errors.add("Plugin ${pluginClass.name} konnte nicht geladen werden: ${e.message}")
                    }
                }
                apis.addAll(CloudStreamRegistrar.takeRegisteredApis())
            }

            // Strategy 2: Look for MainAPI subclasses directly via ServiceLoader-style manifest
            if (apis.isEmpty()) {
                val mainApiClasses = findMainApiClasses(jarPath, classLoader)
                for (apiClass in mainApiClasses) {
                    try {
                        val api = apiClass.getDeclaredConstructor().newInstance() as MainAPI
                        apis.add(api)
                    } catch (e: Exception) {
                        errors.add("MainAPI ${apiClass.name} konnte nicht instanziiert werden: ${e.message}")
                    }
                }
            }

            // Strategy 3: Scan all classes for MainAPI subclasses
            if (apis.isEmpty()) {
                val allMainApis = scanForMainApis(jarPath, classLoader)
                for (apiClass in allMainApis) {
                    try {
                        val api = apiClass.getDeclaredConstructor().newInstance() as MainAPI
                        apis.add(api)
                    } catch (_: Exception) {
                        // Skip classes that can't be instantiated
                    }
                }
            }

            if (apis.isEmpty()) {
                errors.add("Keine CloudStream-Provider in ${jarPath.fileName} gefunden.")
            }
        } catch (e: Exception) {
            errors.add("Fehler beim Laden von ${jarPath.fileName}: ${e.message}")
        }

        return LoadResult(apis, errors)
    }

    /**
     * Load all CS extension JARs from a directory.
     */
    fun loadFromDirectory(dirPath: Path): LoadResult {
        if (!dirPath.exists()) {
            dirPath.createDirectories()
            return LoadResult(emptyList(), emptyList())
        }

        val allApis = mutableListOf<MainAPI>()
        val allErrors = mutableListOf<String>()

        // Look for .cs.jar files (CloudStream repackaged JARs) and .jar files
        val jars = dirPath.listDirectoryEntries("*.jar") +
            dirPath.listDirectoryEntries("*.cs3.jar")

        for (jar in jars.distinct()) {
            val result = loadExtension(jar)
            allApis.addAll(result.apis)
            allErrors.addAll(result.errors)
        }

        return LoadResult(allApis, allErrors)
    }

    /**
     * Find classes with @CloudstreamPlugin annotation.
     */
    private fun findPluginClasses(jarPath: Path, classLoader: ClassLoader): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        try {
            JarFile(jarPath.toFile()).use { jar ->
                for (entry in jar.entries()) {
                    if (entry.name.endsWith(".class") && !entry.name.contains('$')) {
                        val className = entry.name
                            .removeSuffix(".class")
                            .replace('/', '.')
                        try {
                            val clazz = classLoader.loadClass(className)
                            if (clazz.isAnnotationPresent(CloudstreamPlugin::class.java)) {
                                classes.add(clazz)
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Exception) {}
        return classes
    }

    /**
     * Look for MainAPI implementations listed in META-INF/services or manifest.
     */
    private fun findMainApiClasses(jarPath: Path, classLoader: ClassLoader): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        try {
            JarFile(jarPath.toFile()).use { jar ->
                // Check META-INF/services/com.lagradost.cloudstream3.MainAPI
                val serviceEntry = jar.getEntry("META-INF/services/com.lagradost.cloudstream3.MainAPI")
                if (serviceEntry != null) {
                    val classNames = jar.getInputStream(serviceEntry).bufferedReader().readLines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                    for (name in classNames) {
                        try {
                            classes.add(classLoader.loadClass(name))
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Exception) {}
        return classes
    }

    /**
     * Scan all classes in JAR for MainAPI subclasses.
     */
    private fun scanForMainApis(jarPath: Path, classLoader: ClassLoader): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        try {
            JarFile(jarPath.toFile()).use { jar ->
                for (entry in jar.entries()) {
                    if (entry.name.endsWith(".class") && !entry.name.contains('$')) {
                        val className = entry.name
                            .removeSuffix(".class")
                            .replace('/', '.')
                        try {
                            val clazz = classLoader.loadClass(className)
                            if (MainAPI::class.java.isAssignableFrom(clazz)
                                && clazz != MainAPI::class.java
                                && !java.lang.reflect.Modifier.isAbstract(clazz.modifiers)
                            ) {
                                classes.add(clazz)
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Exception) {}
        return classes
    }
}

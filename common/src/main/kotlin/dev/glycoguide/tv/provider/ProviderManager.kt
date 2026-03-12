package dev.glycoguide.tv.provider

import dev.glycoguide.tv.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.ServiceLoader
import kotlin.io.path.*

class ProviderManager {
    private val _providers = MutableStateFlow<List<ContentProvider>>(emptyList())
    val providers: StateFlow<List<ContentProvider>> = _providers

    private val _loadErrors = MutableStateFlow<List<String>>(emptyList())
    val loadErrors: StateFlow<List<String>> = _loadErrors

    /** Callback for loading CloudStream extensions — set by the app module */
    var cloudStreamLoader: ((Path) -> Pair<List<ContentProvider>, List<String>>)? = null

    init {
        // Register built-in demo provider
        registerProvider(DemoProvider())
    }

    fun registerProvider(provider: ContentProvider) {
        if (_providers.value.none { it.id == provider.id }) {
            _providers.value = _providers.value + provider
        }
    }

    fun removeProvider(id: String) {
        _providers.value = _providers.value.filter { it.id != id }
    }

    fun getProvider(id: String): ContentProvider? {
        return _providers.value.find { it.id == id }
    }

    fun loadExtensionsFromDirectory(dirPath: Path) {
        if (!dirPath.exists()) {
            dirPath.createDirectories()
            return
        }

        dirPath.listDirectoryEntries("*.jar").forEach { jarPath ->
            loadExtension(jarPath)
        }

        // Also try loading CloudStream extensions
        cloudStreamLoader?.let { loader ->
            dirPath.listDirectoryEntries("*.cs3.jar").forEach { jarPath ->
                val (providers, errors) = loader(jarPath)
                providers.forEach { registerProvider(it) }
                if (errors.isNotEmpty()) {
                    _loadErrors.value = _loadErrors.value + errors
                }
            }
        }
    }

    fun loadExtension(jarPath: Path) {
        // First try native CineStream extension (ServiceLoader)
        try {
            val classLoader = URLClassLoader(
                arrayOf(jarPath.toUri().toURL()),
                this::class.java.classLoader
            )

            val loader = ServiceLoader.load(ContentProvider::class.java, classLoader)
            var loaded = false
            loader.forEach { provider ->
                if (_providers.value.none { it.id == provider.id }) {
                    _providers.value = _providers.value + provider
                    loaded = true
                }
            }

            if (loaded) return
        } catch (_: Exception) {}

        // Then try CloudStream extension
        cloudStreamLoader?.let { loader ->
            val (providers, errors) = loader(jarPath)
            if (providers.isNotEmpty()) {
                providers.forEach { registerProvider(it) }
                return
            }
            if (errors.isNotEmpty()) {
                _loadErrors.value = _loadErrors.value + errors
                return
            }
        }

        _loadErrors.value = _loadErrors.value +
            "No providers found in ${jarPath.fileName}. Weder CineStream- noch CloudStream-Provider gefunden."
    }

    suspend fun searchAll(query: String, page: Int = 1): List<Pair<ContentProvider, SearchResult>> {
        return _providers.value.mapNotNull { provider ->
            try {
                provider to provider.search(query, page)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getHomeAll(): List<Pair<String, List<Category>>> {
        return _providers.value.mapNotNull { provider ->
            try {
                provider.name to provider.getHome()
            } catch (e: Exception) {
                null
            }
        }
    }
}

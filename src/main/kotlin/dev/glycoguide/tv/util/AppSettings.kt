package dev.glycoguide.tv.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.nio.file.Path
import kotlin.io.path.*

@Serializable
data class SettingsData(
    val downloadDirectory: String = defaultDownloadDir(),
    val extensionsDirectory: String = defaultExtensionsDir(),
    val playerCommand: String = "mpv",
    val showVpnWarning: Boolean = true,
    val maxDownloadSpeedKBps: Int = 0,
    val maxUploadSpeedKBps: Int = 0,
    val maxConnections: Int = 200,
    val seedAfterDownload: Boolean = false,
    val bufferSizeMB: Int = 50,
) {
    companion object {
        fun defaultDownloadDir(): String =
            Path.of(System.getProperty("user.home"), "Downloads", "CineStream").toString()

        fun defaultExtensionsDir(): String =
            Path.of(System.getProperty("user.home"), ".cinestream", "extensions").toString()
    }
}

class AppSettings {
    private val configDir = Path.of(System.getProperty("user.home"), ".cinestream")
    private val configFile = configDir.resolve("settings.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var data by mutableStateOf(SettingsData())
        private set

    fun load(): AppSettings {
        try {
            if (configFile.exists()) {
                val text = configFile.readText()
                data = json.decodeFromString<SettingsData>(text)
            }
        } catch (e: Exception) {
            System.err.println("Failed to load settings: ${e.message}")
        }
        ensureDirectories()
        return this
    }

    fun save() {
        try {
            configDir.createDirectories()
            configFile.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            System.err.println("Failed to save settings: ${e.message}")
        }
    }

    fun update(transform: SettingsData.() -> SettingsData) {
        data = data.transform()
        save()
    }

    private fun ensureDirectories() {
        try {
            Path.of(data.downloadDirectory).createDirectories()
            Path.of(data.extensionsDirectory).createDirectories()
        } catch (e: Exception) {
            System.err.println("Failed to create directories: ${e.message}")
        }
    }

    companion object {
        fun load(): AppSettings = AppSettings().also { it.load() }
    }
}

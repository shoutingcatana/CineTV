package dev.glycoguide.tv.android

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dev.glycoguide.tv.model.TorrentSettings
import kotlinx.serialization.Serializable

/** Detect if running on Android TV */
fun isAndroidTv(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

/** Settings storage for Android using SharedPreferences */
@Serializable
data class AndroidSettingsData(
    val playerPackage: String = "", // empty = system default
    val showVpnWarning: Boolean = true,
    val maxDownloadSpeedKBps: Int = 0,
    val maxUploadSpeedKBps: Int = 0,
    val maxConnections: Int = 200,
    val seedAfterDownload: Boolean = false,
    val bufferSizeMB: Int = 50,
)

class AndroidSettings(private val context: Context) {
    private val prefs = context.getSharedPreferences("cinestream", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var data: AndroidSettingsData = load()
        private set

    private fun load(): AndroidSettingsData {
        val str = prefs.getString("settings", null) ?: return AndroidSettingsData()
        return try {
            json.decodeFromString(str)
        } catch (_: Exception) {
            AndroidSettingsData()
        }
    }

    fun save() {
        prefs.edit().putString("settings", json.encodeToString(data)).apply()
    }

    fun update(transform: AndroidSettingsData.() -> AndroidSettingsData) {
        data = data.transform()
        save()
    }

    fun toTorrentSettings(): TorrentSettings {
        val downloadDir = context.getExternalFilesDir("downloads")?.absolutePath
            ?: context.filesDir.resolve("downloads").absolutePath
        return TorrentSettings(
            downloadDirectory = downloadDir,
            bufferSizeMB = data.bufferSizeMB,
            maxDownloadSpeedKBps = data.maxDownloadSpeedKBps,
            maxUploadSpeedKBps = data.maxUploadSpeedKBps,
            maxConnections = data.maxConnections,
            seedAfterDownload = data.seedAfterDownload,
        )
    }
}

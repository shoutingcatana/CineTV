package dev.glycoguide.tv

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.glycoguide.tv.model.TorrentSettings
import dev.glycoguide.tv.player.PlayerManager
import dev.glycoguide.tv.provider.ProviderManager
import dev.glycoguide.tv.cloudstream.CloudStreamExtensionLoader
import dev.glycoguide.tv.cloudstream.CloudStreamProviderAdapter
import dev.glycoguide.tv.torrent.TorrentEngine
import dev.glycoguide.tv.ui.theme.AppTheme
import dev.glycoguide.tv.util.AppSettings
import java.nio.file.Path

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

    val settings = remember { AppSettings.load() }
    val providerManager = remember {
        val csLoader = CloudStreamExtensionLoader()
        ProviderManager().apply {
            cloudStreamLoader = { jarPath ->
                val result = csLoader.loadExtension(jarPath)
                val providers = result.apis.map { CloudStreamProviderAdapter(it) }
                providers to result.errors
            }
        }
    }
    val torrentEngine = remember {
        TorrentEngine {
            val d = settings.data
            TorrentSettings(
                downloadDirectory = d.downloadDirectory,
                bufferSizeMB = d.bufferSizeMB,
                maxDownloadSpeedKBps = d.maxDownloadSpeedKBps,
                maxUploadSpeedKBps = d.maxUploadSpeedKBps,
                maxConnections = d.maxConnections,
                seedAfterDownload = d.seedAfterDownload,
            )
        }
    }
    val playerManager = remember { PlayerManager(settings) }

    // Initialize
    DisposableEffect(Unit) {
        torrentEngine.start()
        providerManager.loadExtensionsFromDirectory(Path.of(settings.data.extensionsDirectory))

        onDispose {
            torrentEngine.stop()
            settings.save()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "CineStream",
        state = windowState,
    ) {
        AppTheme {
            App(
                providerManager = providerManager,
                torrentEngine = torrentEngine,
                playerManager = playerManager,
                settings = settings,
            )
        }
    }
}

package dev.glycoguide.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.glycoguide.tv.model.*
import dev.glycoguide.tv.player.PlayerManager
import dev.glycoguide.tv.provider.ProviderManager
import dev.glycoguide.tv.torrent.TorrentEngine
import dev.glycoguide.tv.ui.components.SideNavigation
import dev.glycoguide.tv.ui.components.VPNWarningDialog
import dev.glycoguide.tv.ui.screens.*
import dev.glycoguide.tv.util.AppSettings

@Composable
fun App(
    providerManager: ProviderManager,
    torrentEngine: TorrentEngine,
    playerManager: PlayerManager,
    settings: AppSettings,
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val screenHistory = remember { mutableListOf<Screen>() }

    // VPN warning state
    var showVpnWarning by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<StreamSource?>(null) }
    var pendingMediaItem by remember { mutableStateOf<MediaItem?>(null) }

    fun navigateTo(screen: Screen) {
        screenHistory.add(currentScreen)
        currentScreen = screen
    }

    fun goBack() {
        if (screenHistory.isNotEmpty()) {
            currentScreen = screenHistory.removeLast()
        }
    }

    fun handlePlaySource(source: StreamSource, mediaItem: MediaItem) {
        val isTorrent = source.type == StreamSourceType.TORRENT || source.type == StreamSourceType.MAGNET
        if (isTorrent && settings.data.showVpnWarning) {
            pendingSource = source
            pendingMediaItem = mediaItem
            showVpnWarning = true
        } else {
            navigateTo(Screen.Player(source, mediaItem))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SideNavigation(
            currentScreen = currentScreen,
            onScreenSelected = { screen ->
                screenHistory.clear()
                currentScreen = screen
            },
        )

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (val screen = currentScreen) {
                is Screen.Home -> HomeScreen(
                    providerManager = providerManager,
                    onItemClick = { item -> navigateTo(Screen.Detail(item)) },
                )

                is Screen.Search -> SearchScreen(
                    providerManager = providerManager,
                    onItemClick = { item -> navigateTo(Screen.Detail(item)) },
                )

                is Screen.Detail -> DetailScreen(
                    mediaItem = screen.item,
                    providerManager = providerManager,
                    onBack = { goBack() },
                    onPlaySource = { source -> handlePlaySource(source, screen.item) },
                )

                is Screen.Player -> PlayerScreen(
                    mediaItem = screen.item,
                    source = screen.source,
                    torrentEngine = torrentEngine,
                    playerManager = playerManager,
                    settings = settings,
                    onBack = { goBack() },
                )

                is Screen.Extensions -> ExtensionsScreen(
                    providerManager = providerManager,
                    settings = settings,
                )

                is Screen.Settings -> SettingsScreen(
                    settings = settings,
                )
            }
        }
    }

    // VPN Warning Dialog
    if (showVpnWarning) {
        VPNWarningDialog(
            onConfirm = {
                showVpnWarning = false
                val source = pendingSource
                val item = pendingMediaItem
                if (source != null && item != null) {
                    navigateTo(Screen.Player(source, item))
                }
                pendingSource = null
                pendingMediaItem = null
            },
            onCancel = {
                showVpnWarning = false
                pendingSource = null
                pendingMediaItem = null
            },
        )
    }
}

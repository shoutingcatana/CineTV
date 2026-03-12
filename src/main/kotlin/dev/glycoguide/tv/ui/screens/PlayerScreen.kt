package dev.glycoguide.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.glycoguide.tv.model.MediaItem
import dev.glycoguide.tv.model.StreamSource
import dev.glycoguide.tv.model.StreamSourceType
import dev.glycoguide.tv.player.PlayerManager
import dev.glycoguide.tv.torrent.TorrentEngine
import dev.glycoguide.tv.torrent.TorrentState
import dev.glycoguide.tv.torrent.TorrentStatus
import dev.glycoguide.tv.torrent.TorrentStream
import dev.glycoguide.tv.util.AppSettings
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    mediaItem: MediaItem,
    source: StreamSource,
    torrentEngine: TorrentEngine,
    playerManager: PlayerManager,
    settings: AppSettings,
    onBack: () -> Unit,
) {
    var torrentStream by remember { mutableStateOf<TorrentStream?>(null) }
    var status by remember { mutableStateOf(TorrentStatus()) }
    var playerLaunched by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Collect torrent status
    LaunchedEffect(torrentStream) {
        torrentStream?.status?.collect {
            status = it
        }
    }

    // Start streaming
    LaunchedEffect(source) {
        when (source.type) {
            StreamSourceType.DIRECT -> {
                try {
                    playerManager.playUrl(source.url)
                    playerLaunched = true
                } catch (e: Exception) {
                    playerError = e.message
                }
            }
            StreamSourceType.MAGNET, StreamSourceType.TORRENT -> {
                torrentStream = torrentEngine.streamMagnet(source.url)
            }
        }
    }

    // Auto-launch player when buffer is ready
    LaunchedEffect(status.state) {
        if (status.state == TorrentState.READY && !playerLaunched) {
            status.videoFilePath?.let { path ->
                try {
                    playerManager.play(path)
                    playerLaunched = true
                } catch (e: Exception) {
                    playerError = e.message
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        // Back button
        TextButton(
            onClick = {
                torrentStream?.stop()
                onBack()
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Zurück")
        }

        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            mediaItem.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Text(
            source.name + (source.quality?.let { " • $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // Error display
        playerError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Fehler",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        when (source.type) {
            StreamSourceType.DIRECT -> {
                DirectStreamStatus(playerLaunched, source, playerManager, playerError)
            }
            StreamSourceType.MAGNET, StreamSourceType.TORRENT -> {
                TorrentStreamStatus(
                    status = status,
                    playerLaunched = playerLaunched,
                    onLaunchPlayer = {
                        status.videoFilePath?.let { path ->
                            scope.launch {
                                try {
                                    playerManager.play(path)
                                    playerLaunched = true
                                } catch (e: Exception) {
                                    playerError = e.message
                                }
                            }
                        }
                    },
                    onStop = {
                        torrentStream?.stop()
                    },
                )
            }
        }
    }
}

@Composable
private fun DirectStreamStatus(
    launched: Boolean,
    source: StreamSource,
    playerManager: PlayerManager,
    error: String?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                if (launched) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                contentDescription = null,
                tint = if (launched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (launched) "Stream gestartet" else "Starte Stream...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (launched) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Der Video-Player wurde geöffnet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TorrentStreamStatus(
    status: TorrentStatus,
    playerLaunched: Boolean,
    onLaunchPlayer: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            // State icon and message
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (status.state) {
                    TorrentState.INITIALIZING, TorrentState.METADATA -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            if (status.state == TorrentState.METADATA) "Lade Torrent-Metadaten..."
                            else "Initialisiere...",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    TorrentState.BUFFERING -> {
                        CircularProgressIndicator(
                            progress = { status.bufferProgress },
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Buffering... ${(status.bufferProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Warte auf genügend Daten zum Abspielen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TorrentState.READY, TorrentState.DOWNLOADING, TorrentState.SEEDING -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            if (playerLaunched) "Video wird abgespielt"
                            else "Bereit zum Abspielen",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    TorrentState.ERROR -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Fehler",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            status.errorMessage?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            // Progress bar
            if (status.state == TorrentState.BUFFERING || status.state == TorrentState.DOWNLOADING ||
                status.state == TorrentState.READY || status.state == TorrentState.SEEDING
            ) {
                Spacer(Modifier.height(20.dp))

                LinearProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                )

                Spacer(Modifier.height(12.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatLabel("Fortschritt", "${(status.progress * 100).toInt()}%")
                    StatLabel("↓ Download", formatSpeed(status.downloadSpeed))
                    StatLabel("↑ Upload", formatSpeed(status.uploadSpeed))
                    StatLabel("Seeds", status.seeds.toString())
                    StatLabel("Peers", status.peers.toString())
                }
            }

            // Action buttons
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if ((status.state == TorrentState.READY || status.state == TorrentState.DOWNLOADING ||
                    status.state == TorrentState.SEEDING) && status.videoFilePath != null
                ) {
                    Button(onClick = onLaunchPlayer) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (playerLaunched) "Erneut öffnen" else "Im Player öffnen")
                    }
                }

                OutlinedButton(
                    onClick = onStop,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stoppen")
                }
            }

            // File path
            status.videoFilePath?.let { path ->
                Spacer(Modifier.height(12.dp))
                Text(
                    "Datei: $path",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1_048_576 -> "%.1f MB/s".format(bytesPerSecond / 1_048_576.0)
        bytesPerSecond >= 1024 -> "%.0f KB/s".format(bytesPerSecond / 1024.0)
        else -> "$bytesPerSecond B/s"
    }
}

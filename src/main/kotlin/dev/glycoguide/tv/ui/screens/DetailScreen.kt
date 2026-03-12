package dev.glycoguide.tv.ui.screens

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.glycoguide.tv.model.*
import dev.glycoguide.tv.provider.ProviderManager
import dev.glycoguide.tv.util.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    mediaItem: MediaItem,
    providerManager: ProviderManager,
    onBack: () -> Unit,
    onPlaySource: (StreamSource) -> Unit,
) {
    val provider = providerManager.getProvider(mediaItem.providerId)
    var details by remember { mutableStateOf(mediaItem) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var sources by remember { mutableStateOf<List<StreamSource>>(emptyList()) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    var isLoadingSources by remember { mutableStateOf(false) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaItem) {
        isLoadingDetails = true
        provider?.let {
            try {
                details = it.getDetails(mediaItem)
                if (mediaItem.type == MediaType.TV_SHOW || mediaItem.type == MediaType.ANIME) {
                    episodes = it.getEpisodes(mediaItem)
                }
            } catch (_: Exception) { }
        }
        isLoadingDetails = false
    }

    fun loadSources(episode: Episode? = null) {
        scope.launch {
            isLoadingSources = true
            provider?.let {
                try {
                    sources = it.getSources(details, episode)
                } catch (_: Exception) {
                    sources = emptyList()
                }
            }
            isLoadingSources = false
        }
    }

    // Auto-load sources for movies
    LaunchedEffect(details) {
        if (details.type == MediaType.MOVIE || details.type == MediaType.DOCUMENTARY) {
            loadSources()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        // Back button
        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Zurück")
        }

        Spacer(Modifier.height(16.dp))

        // Media info section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    url = details.posterUrl,
                    contentDescription = details.title,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Details
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    details.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(8.dp))

                // Metadata row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    details.year?.let {
                        MetadataChip(it.toString())
                    }
                    details.rating?.let {
                        MetadataChip("⭐ %.1f".format(it))
                    }
                    MetadataChip(
                        when (details.type) {
                            MediaType.MOVIE -> "Film"
                            MediaType.TV_SHOW -> "Serie"
                            MediaType.ANIME -> "Anime"
                            MediaType.DOCUMENTARY -> "Doku"
                        }
                    )
                }

                // Genres
                if (details.genres.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        details.genres.forEach { genre ->
                            MetadataChip(genre, containerColor = MaterialTheme.colorScheme.primaryContainer)
                        }
                    }
                }

                // Description
                details.description?.let { desc ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Provider info
                Spacer(Modifier.height(12.dp))
                Text(
                    "Provider: ${provider?.name ?: "Unbekannt"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        // Episodes (for TV shows)
        if (episodes.isNotEmpty()) {
            Text(
                "Episoden",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))

            episodes.forEach { episode ->
                val isSelected = selectedEpisode == episode
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable {
                            selectedEpisode = episode
                            loadSources(episode)
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "S${episode.seasonNumber}E${episode.episodeNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            episode.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
        }

        // Stream sources
        Text(
            "Quellen",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))

        if (isLoadingSources) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (sources.isEmpty()) {
            Text(
                if (episodes.isNotEmpty() && selectedEpisode == null)
                    "Wähle eine Episode aus um Quellen zu laden."
                else
                    "Keine Quellen verfügbar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            sources.forEach { source ->
                SourceCard(source = source, onClick = { onPlaySource(source) })
            }
        }
    }
}

@Composable
private fun MetadataChip(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SourceCard(
    source: StreamSource,
    onClick: () -> Unit,
) {
    val isTorrent = source.type == StreamSourceType.TORRENT || source.type == StreamSourceType.MAGNET

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Type icon
            Icon(
                when (source.type) {
                    StreamSourceType.DIRECT -> Icons.Default.PlayCircle
                    StreamSourceType.TORRENT, StreamSourceType.MAGNET -> Icons.Default.CloudDownload
                },
                contentDescription = null,
                tint = if (isTorrent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    source.quality?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (isTorrent) {
                        Text(
                            "Torrent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        source.seeders?.let {
                            Text(
                                "↑$it Seeds",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        source.leechers?.let {
                            Text(
                                "↓$it Peers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    source.size?.let {
                        Text(
                            formatFileSize(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Abspielen",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}

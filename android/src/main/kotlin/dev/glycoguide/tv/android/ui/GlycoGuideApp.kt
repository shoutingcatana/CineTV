package dev.glycoguide.tv.android.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.glycoguide.tv.android.AndroidSettings
import dev.glycoguide.tv.android.AndroidSettingsData
import dev.glycoguide.tv.android.isAndroidTv
import dev.glycoguide.tv.android.player.AndroidPlayerManager
import dev.glycoguide.tv.android.provider.AndroidVidSrcProvider
import dev.glycoguide.tv.model.*
import dev.glycoguide.tv.provider.ContentProvider
import dev.glycoguide.tv.provider.DemoProvider
import dev.glycoguide.tv.provider.ProviderManager
import dev.glycoguide.tv.torrent.TorrentEngine
import dev.glycoguide.tv.torrent.TorrentState
import dev.glycoguide.tv.torrent.TorrentStatus
import dev.glycoguide.tv.torrent.TorrentStream
import kotlinx.coroutines.launch

// ──────────────────────────── Theme ────────────────────────────

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1F7A),
    secondary = Color(0xFF3B82F6),
    tertiary = Color(0xFF10B981),
    background = Color(0xFF0B0B14),
    surface = Color(0xFF12121E),
    surfaceVariant = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE8E8F0),
    onSurface = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFF9898B0),
    error = Color(0xFFEF4444),
    outline = Color(0xFF2A2A40),
)

// ──────────────────────────── App Entry ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlycoGuideApp(context: Context) {
    val isTv = remember { isAndroidTv(context) }
    val settings = remember { AndroidSettings(context) }
    val providerManager = remember {
        ProviderManager().apply {
            registerProvider(AndroidVidSrcProvider())
        }
    }
    val torrentEngine = remember { TorrentEngine { settings.toTorrentSettings() } }
    val playerManager = remember { AndroidPlayerManager(context) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val screenHistory = remember { mutableListOf<Screen>() }

    fun navigateTo(screen: Screen) {
        screenHistory.add(currentScreen)
        currentScreen = screen
    }

    fun goBack() {
        if (screenHistory.isNotEmpty()) {
            currentScreen = screenHistory.removeLast()
        }
    }

    // VPN warning dialog
    var showVpnWarning by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<Pair<StreamSource, MediaItem>?>(null) }

    DisposableEffect(Unit) {
        torrentEngine.start()
        onDispose { torrentEngine.stop() }
    }

    MaterialTheme(colorScheme = DarkColors) {
        // VPN Warning Dialog
        if (showVpnWarning) {
            AlertDialog(
                onDismissRequest = { showVpnWarning = false; pendingSource = null },
                icon = { Icon(Icons.Default.Shield, null, tint = Color(0xFFFBBF24)) },
                title = { Text("VPN-Warnung", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Du bist dabei, einen Torrent zu streamen. Dabei wird deine IP-Adresse " +
                        "für andere Teilnehmer sichtbar. In Deutschland kann dies zu kostenpflichtigen " +
                        "Abmahnungen führen.\n\nNutze einen VPN-Dienst für Anonymität.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showVpnWarning = false
                        pendingSource?.let { (source, item) ->
                            navigateTo(Screen.Player(source, item))
                        }
                        pendingSource = null
                    }) { Text("Trotzdem fortfahren") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showVpnWarning = false; pendingSource = null }) {
                        Text("Abbrechen")
                    }
                },
            )
        }

        Scaffold(
            bottomBar = {
                if (currentScreen !is Screen.Player) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, "Home") },
                            label = { Text("Home") },
                            selected = currentScreen is Screen.Home,
                            onClick = { currentScreen = Screen.Home; screenHistory.clear() },
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, "Suche") },
                            label = { Text("Suche") },
                            selected = currentScreen is Screen.Search,
                            onClick = { navigateTo(Screen.Search) },
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Extension, "Extensions") },
                            label = { Text("Extensions") },
                            selected = currentScreen is Screen.Extensions,
                            onClick = { navigateTo(Screen.Extensions) },
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, "Settings") },
                            label = { Text("Einstellungen") },
                            selected = currentScreen is Screen.Settings,
                            onClick = { navigateTo(Screen.Settings) },
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (val screen = currentScreen) {
                    is Screen.Home -> HomeScreen(providerManager) { navigateTo(Screen.Detail(it)) }
                    is Screen.Search -> SearchScreen(providerManager) { navigateTo(Screen.Detail(it)) }
                    is Screen.Detail -> DetailScreen(
                        mediaItem = screen.item,
                        providerManager = providerManager,
                        onBack = { goBack() },
                        onPlaySource = { source ->
                            val isTorrent = source.type == StreamSourceType.MAGNET || source.type == StreamSourceType.TORRENT
                            if (isTorrent && settings.data.showVpnWarning) {
                                pendingSource = source to screen.item
                                showVpnWarning = true
                            } else {
                                navigateTo(Screen.Player(source, screen.item))
                            }
                        },
                    )
                    is Screen.Player -> PlayerScreen(
                        mediaItem = screen.item,
                        source = screen.source,
                        torrentEngine = torrentEngine,
                        playerManager = playerManager,
                        onBack = { goBack() },
                    )
                    is Screen.Extensions -> ExtensionsScreen(providerManager)
                    is Screen.Settings -> SettingsScreen(settings) { goBack() }
                }
            }
        }
    }
}

// ──────────────────────────── Home Screen ────────────────────────────

@Composable
fun HomeScreen(
    providerManager: ProviderManager,
    onItemClick: (MediaItem) -> Unit,
) {
    val providers by providerManager.providers.collectAsState()
    var homeContent by remember { mutableStateOf<List<Pair<String, List<Category>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(providers) {
        isLoading = true
        try { homeContent = providerManager.getHomeAll() } catch (_: Exception) {}
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("CineStream", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            homeContent.forEach { (providerName, categories) ->
                if (categories.isNotEmpty()) {
                    Text(providerName, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    categories.forEach { category ->
                        Text(category.name, style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(category.items) { item ->
                                MobileMediaCard(item) { onItemClick(item) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────── Search Screen ────────────────────────────

@Composable
fun SearchScreen(
    providerManager: ProviderManager,
    onItemClick: (MediaItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Film oder Serie suchen...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; results = emptyList() }) {
                        Icon(Icons.Default.Clear, "Löschen")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (query.isNotBlank()) scope.launch {
                    isSearching = true
                    results = providerManager.searchAll(query).flatMap { (provider, sr) ->
                        sr.items.map { it.copy(providerId = provider.id) }
                    }
                    isSearching = false
                }
            },
            enabled = query.isNotBlank() && !isSearching,
            modifier = Modifier.align(Alignment.End),
        ) {
            if (isSearching) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text("Suchen")
        }

        Spacer(Modifier.height(12.dp))

        if (isSearching) {
            Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (results.isNotEmpty()) {
            Text("${results.size} Ergebnis${if (results.size != 1) "se" else ""}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { "${it.providerId}_${it.id}" }) { item ->
                    MobileMediaCard(item) { onItemClick(item) }
                }
            }
        }
    }
}

// ──────────────────────────── Detail Screen ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaItem) {
        provider?.let {
            try {
                details = it.getDetails(mediaItem)
                if (mediaItem.type == MediaType.TV_SHOW || mediaItem.type == MediaType.ANIME)
                    episodes = it.getEpisodes(mediaItem)
            } catch (_: Exception) {}
        }
    }

    fun loadSources(ep: Episode? = null) {
        scope.launch {
            isLoadingSources = true
            try { sources = provider?.getSources(details, ep) ?: emptyList() } catch (_: Exception) { sources = emptyList() }
            isLoadingSources = false
        }
    }

    LaunchedEffect(details) {
        if (details.type == MediaType.MOVIE || details.type == MediaType.DOCUMENTARY) loadSources()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Poster + Info
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = details.posterUrl,
                contentDescription = details.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(130.dp).height(195.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(details.title, style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.year?.let { Chip(it.toString()) }
                    details.rating?.let { Chip("⭐ %.1f".format(it)) }
                    Chip(when (details.type) {
                        MediaType.MOVIE -> "Film"; MediaType.TV_SHOW -> "Serie"
                        MediaType.ANIME -> "Anime"; MediaType.DOCUMENTARY -> "Doku"
                    })
                }
                details.description?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 5,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))

        // Episodes
        if (episodes.isNotEmpty()) {
            Text("Episoden", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            episodes.forEach { ep ->
                val selected = selectedEpisode == ep
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        .clickable { selectedEpisode = ep; loadSources(ep) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("S${ep.seasonNumber}E${ep.episodeNumber}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text(ep.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))
        }

        // Sources
        Text("Quellen", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (isLoadingSources) {
            CircularProgressIndicator(Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
        } else if (sources.isEmpty()) {
            Text(
                if (episodes.isNotEmpty() && selectedEpisode == null) "Wähle eine Episode aus."
                else "Keine Quellen verfügbar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            sources.forEach { source ->
                val isTorrent = source.type == StreamSourceType.MAGNET || source.type == StreamSourceType.TORRENT
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onPlaySource(source) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isTorrent) Icons.Default.CloudDownload else Icons.Default.PlayCircle,
                            null, tint = if (isTorrent) Color(0xFFFBBF24) else MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(source.name, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                source.quality?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary) }
                                if (isTorrent) {
                                    source.seeders?.let { Text("↑$it", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary) }
                                    source.leechers?.let { Text("↓$it", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────── Player Screen ────────────────────────────

@Composable
fun PlayerScreen(
    mediaItem: MediaItem,
    source: StreamSource,
    torrentEngine: TorrentEngine,
    playerManager: AndroidPlayerManager,
    onBack: () -> Unit,
) {
    var torrentStream by remember { mutableStateOf<TorrentStream?>(null) }
    var status by remember { mutableStateOf(TorrentStatus()) }
    var playerLaunched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(torrentStream) {
        torrentStream?.status?.collect { status = it }
    }

    LaunchedEffect(source) {
        when (source.type) {
            StreamSourceType.DIRECT -> {
                try { playerManager.playUrl(source.url, mediaItem.title); playerLaunched = true }
                catch (e: Exception) { error = e.message }
            }
            StreamSourceType.MAGNET, StreamSourceType.TORRENT -> {
                torrentStream = torrentEngine.streamMagnet(source.url)
            }
        }
    }

    LaunchedEffect(status.state) {
        if (status.state == TorrentState.READY && !playerLaunched) {
            status.videoFilePath?.let {
                try { playerManager.playFile(it, mediaItem.title); playerLaunched = true }
                catch (e: Exception) { error = e.message }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { torrentStream?.stop(); onBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
        }

        Text(mediaItem.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Text(source.name + (source.quality?.let { " • $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))

        error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()) {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(Modifier.height(16.dp))
        }

        when (source.type) {
            StreamSourceType.DIRECT -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (playerLaunched) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                            null, modifier = Modifier.size(48.dp),
                            tint = if (playerLaunched) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(if (playerLaunched) "Stream gestartet" else "Starte Stream...",
                            style = MaterialTheme.typography.titleMedium)
                        if (playerLaunched) {
                            Text("Externer Player wurde geöffnet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            else -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        when (status.state) {
                            TorrentState.INITIALIZING, TorrentState.METADATA -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                                    Spacer(Modifier.width(16.dp))
                                    Text(if (status.state == TorrentState.METADATA) "Lade Metadaten..."
                                        else "Initialisiere...", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            TorrentState.BUFFERING -> {
                                Text("Buffering ${(status.bufferProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium)
                                LinearProgressIndicator(
                                    progress = { status.bufferProgress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            TorrentState.READY, TorrentState.DOWNLOADING, TorrentState.SEEDING -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.width(16.dp))
                                    Text(if (playerLaunched) "Wird abgespielt" else "Bereit",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.tertiary)
                                }
                                if (!playerLaunched) {
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = {
                                        status.videoFilePath?.let {
                                            playerManager.playFile(it, mediaItem.title)
                                            playerLaunched = true
                                        }
                                    }) { Text("Abspielen") }
                                }
                            }
                            TorrentState.ERROR -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, null, Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Fehler", color = MaterialTheme.colorScheme.error)
                                        status.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                            }
                            else -> {}
                        }

                        // Stats
                        if (status.downloadSpeed > 0 || status.seeds > 0) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("↓ ${formatSpeed(status.downloadSpeed)}", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary)
                                Text("↑ ${formatSpeed(status.uploadSpeed)}", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary)
                                Text("Seeds: ${status.seeds}", style = MaterialTheme.typography.labelMedium)
                                Text("Peers: ${status.peers}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { torrentStream?.stop(); onBack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Stream beenden")
                }
            }
        }
    }
}

// ──────────────────────────── Settings Screen ────────────────────────────

@Composable
fun SettingsScreen(settings: dev.glycoguide.tv.android.AndroidSettings, onBack: () -> Unit) {
    var data by remember { mutableStateOf(settings.data) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
        }

        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // === Torrent Settings ===
        Text("Torrent", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))

        // Buffer size
        OutlinedTextField(
            value = data.bufferSizeMB.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let {
                    settings.update { copy(bufferSizeMB = it) }
                    data = settings.data
                }
            },
            label = { Text("Buffer-Größe (MB)") },
            supportingText = { Text("Wie viel gepuffert wird bevor der Player startet") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Max download speed
        OutlinedTextField(
            value = data.maxDownloadSpeedKBps.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let {
                    settings.update { copy(maxDownloadSpeedKBps = it) }
                    data = settings.data
                }
            },
            label = { Text("Max Download-Speed (KB/s, 0 = unbegrenzt)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Max upload speed
        OutlinedTextField(
            value = data.maxUploadSpeedKBps.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let {
                    settings.update { copy(maxUploadSpeedKBps = it) }
                    data = settings.data
                }
            },
            label = { Text("Max Upload-Speed (KB/s, 0 = unbegrenzt)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Max connections
        OutlinedTextField(
            value = data.maxConnections.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let {
                    settings.update { copy(maxConnections = it) }
                    data = settings.data
                }
            },
            label = { Text("Max Verbindungen") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Seed after download switch
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nach Download weiter seeden", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = data.seedAfterDownload, onCheckedChange = {
                settings.update { copy(seedAfterDownload = it) }
                data = settings.data
            })
        }

        Spacer(Modifier.height(16.dp))

        // === Player Settings ===
        Text("Video Player", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = data.playerPackage,
            onValueChange = {
                settings.update { copy(playerPackage = it) }
                data = settings.data
            },
            label = { Text("Player-App Package") },
            supportingText = { Text("Leer = System-Standard (z.B. org.videolan.vlc)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Spacer(Modifier.height(16.dp))

        // === Privacy Settings ===
        Text("Datenschutz", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))

        // VPN Warning switch
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("VPN-Warnung", style = MaterialTheme.typography.bodyMedium)
                Text("Warnung vor Torrent-Streams anzeigen", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = data.showVpnWarning, onCheckedChange = {
                settings.update { copy(showVpnWarning = it) }
                data = settings.data
            })
        }

        Spacer(Modifier.height(24.dp))

        // Reset button
        OutlinedButton(
            onClick = {
                settings.update { AndroidSettingsData() }
                data = settings.data
            },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Auf Standard zurücksetzen")
        }

        Spacer(Modifier.height(16.dp))

        // About
        Text("Über", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("CineStream v1.0.0", fontWeight = FontWeight.SemiBold)
                Text("Desktop & Mobile Streaming Client", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ──────────────────────────── Extensions Screen ────────────────────────────

@Composable
fun ExtensionsScreen(providerManager: ProviderManager) {
    val providers by providerManager.providers.collectAsState()
    val loadErrors by providerManager.loadErrors.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("Extensions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Verwalte Content-Provider für Film- und Serienquellen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))

        // Info card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Provider werden beim App-Start automatisch geladen. " +
                    "Auf der Desktop-Version können zusätzliche JAR-Extensions installiert werden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Load errors
        if (loadErrors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            loadErrors.forEach { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Text(error, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))

        Text("Installierte Provider (${providers.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))

        if (providers.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Extension, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Keine Provider installiert", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            providers.forEach { provider ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(provider.name, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(provider.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)) {
                                Text(provider.language.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                provider.supportedTypes.forEach { type ->
                                    Text(type.name, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (provider.id == "demo" || provider.id == "vidsrc") {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text("Built-in", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = { providerManager.removeProvider(provider.id) }) {
                                Icon(Icons.Default.Delete, "Entfernen",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────── Components ────────────────────────────

@Composable
fun MobileMediaCard(item: MediaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(120.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )

            // Gradient overlay
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))),
            )

            // Rating badge
            item.rating?.let { rating ->
                Box(
                    modifier = Modifier.padding(4.dp).align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                rating >= 7.0 -> Color(0xFF10B981)
                                rating >= 5.0 -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                        ).padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text("%.1f".format(rating), style = MaterialTheme.typography.labelSmall,
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Title at bottom
            Text(
                item.title,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun Chip(text: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec > 1_048_576 -> "%.1f MB/s".format(bytesPerSec / 1_048_576.0)
        bytesPerSec > 1_024 -> "%.0f KB/s".format(bytesPerSec / 1_024.0)
        else -> "$bytesPerSec B/s"
    }
}

# GlycoGuide TV

Desktop-Streaming-Client mit BitTorrent-Support – inspiriert von [CloudStream](https://github.com/recloudstream/cloudstream), aber für Linux/Windows/macOS Desktop.

## Features

- **Dark Theme UI** – Modernes, dunkles Design mit Compose for Desktop (Material 3)
- **Provider/Extension-System** – Erweiterbar über JAR-Plugins (ähnlich CloudStream-Extensions)
- **Torrent-Streaming** – Filme direkt über Magnet-Links streamen via libtorrent4j
- **Externer Video-Player** – Unterstützt mpv, VLC und andere Player
- **VPN-Warnung** – Warnt automatisch vor Torrent-Streams ohne VPN (besonders wichtig in DE!)
- **Einstellungen** – Download-Verzeichnis, Buffer-Größe, Speed-Limits, etc.
- **Suche** – Gleichzeitige Suche über alle installierten Provider

## Voraussetzungen

- **Java 17+** mit GUI-Support (nicht headless!):
  ```bash
  sudo apt install openjdk-17-jdk openjdk-17-jre
  ```
- **Video-Player** – mpv (empfohlen) oder VLC:
  ```bash
  sudo apt install mpv
  ```

## Bauen & Starten

```bash
# Einfach (Start-Script mit Auto-Checks):
./start.sh

# Alternativ direkt:
./gradlew run

# Distribution erstellen (DEB/RPM/AppImage)
./gradlew packageDeb
./gradlew packageRpm
./gradlew packageAppImage
```

## Architektur

```
src/main/kotlin/dev/glycoguide/tv/
├── Main.kt                    # Entry Point
├── App.kt                     # Hauptnavigation
├── model/Models.kt            # Datenmodelle
├── provider/
│   ├── ContentProvider.kt     # Provider-Interface
│   ├── DemoProvider.kt        # Demo-Provider (built-in)
│   └── ProviderManager.kt    # Provider-Verwaltung & Extension-Loading
├── torrent/TorrentEngine.kt  # Torrent-Streaming via libtorrent4j
├── player/PlayerManager.kt   # Video-Player Integration
├── util/
│   ├── AppSettings.kt        # Persistente Einstellungen
│   └── ImageLoader.kt        # Async Image Loading
└── ui/
    ├── theme/Theme.kt         # Dark Theme (Material 3)
    ├── components/
    │   ├── SideNavigation.kt  # Seitenleisten-Navigation
    │   ├── MediaCard.kt       # Film/Serien-Karte
    │   └── VPNWarningDialog.kt # VPN-Warnung
    └── screens/
        ├── HomeScreen.kt      # Startseite mit Provider-Inhalten
        ├── SearchScreen.kt    # Suche
        ├── DetailScreen.kt    # Film/Serien-Details + Quellen
        ├── PlayerScreen.kt    # Torrent-Download-Status + Player
        ├── ExtensionsScreen.kt # Extension-Verwaltung
        └── SettingsScreen.kt  # Einstellungen
```

## Extension / Provider erstellen

Provider werden als JAR-Dateien geladen (Java ServiceLoader Pattern):

### 1. ContentProvider implementieren

```kotlin
class MyProvider : ContentProvider {
    override val id = "my-provider"
    override val name = "Mein Provider"
    override val language = "de"
    override val description = "Beschreibung"
    override val supportedTypes = setOf(MediaType.MOVIE, MediaType.TV_SHOW)
    override val iconUrl: String? = null

    override suspend fun getHome(): List<Category> { /* ... */ }
    override suspend fun search(query: String, page: Int): SearchResult { /* ... */ }
    override suspend fun getDetails(mediaItem: MediaItem): MediaItem { /* ... */ }
    override suspend fun getEpisodes(mediaItem: MediaItem): List<Episode> { /* ... */ }
    override suspend fun getSources(mediaItem: MediaItem, episode: Episode?): List<StreamSource> { /* ... */ }
}
```

### 2. ServiceLoader registrieren

Erstelle `META-INF/services/dev.glycoguide.tv.provider.ContentProvider` mit dem Inhalt:
```
com.example.MyProvider
```

### 3. Als JAR bauen und im Extensions-Ordner ablegen

Standard: `~/.glycoguide-tv/extensions/`

## Tech Stack

- **Kotlin 2.0** + **Compose Multiplatform 1.6** (Desktop)
- **Material 3** Dark Theme
- **libtorrent4j** für BitTorrent-Streaming
- **Ktor** HTTP Client
- **kotlinx-serialization** für JSON
- **Jsoup** für HTML Parsing

## Wichtiger Hinweis

⚠️ **Nutze immer ein VPN beim Torrent-Streaming!** In Deutschland können Abmahnungen für das Streamen urheberrechtlich geschützter Inhalte über BitTorrent drohen. Die App warnt dich automatisch vor Torrent-Streams.

## Lizenz

Privates Projekt – GlycoGuide

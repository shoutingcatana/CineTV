package dev.glycoguide.tv.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    MOVIE, TV_SHOW, ANIME, DOCUMENTARY
}

@Serializable
enum class StreamSourceType {
    DIRECT, TORRENT, MAGNET
}

@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val description: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val type: MediaType = MediaType.MOVIE,
    val providerId: String = "",
    val url: String? = null,
)

@Serializable
data class Episode(
    val id: String,
    val title: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val description: String? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
data class StreamSource(
    val name: String,
    val url: String,
    val type: StreamSourceType,
    val quality: String? = null,
    val size: Long? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val headers: Map<String, String> = emptyMap(),
)

data class Category(
    val name: String,
    val items: List<MediaItem>,
)

data class SearchResult(
    val items: List<MediaItem>,
    val hasMore: Boolean = false,
    val page: Int = 1,
)

sealed class Screen {
    data object Home : Screen()
    data object Search : Screen()
    data object Extensions : Screen()
    data object Settings : Screen()
    data class Detail(val item: MediaItem) : Screen()
    data class Player(val source: StreamSource, val item: MediaItem) : Screen()
}

@Serializable
data class TorrentSettings(
    val downloadDirectory: String,
    val bufferSizeMB: Int = 50,
    val maxDownloadSpeedKBps: Int = 0,
    val maxUploadSpeedKBps: Int = 0,
    val maxConnections: Int = 200,
    val seedAfterDownload: Boolean = false,
)

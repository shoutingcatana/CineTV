package dev.glycoguide.tv.cloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.Episode as CSEpisode
import dev.glycoguide.tv.model.*
import dev.glycoguide.tv.model.Episode as CineEpisode
import dev.glycoguide.tv.provider.ContentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adapter that wraps a CloudStream3 MainAPI into a CineStream ContentProvider.
 * This bridges the CS extension API to our native provider interface.
 */
class CloudStreamProviderAdapter(
    private val csApi: MainAPI,
) : ContentProvider {

    override val id: String = "cs3_${csApi.name.lowercase().replace(" ", "_")}"
    override val name: String = csApi.name
    override val language: String = csApi.lang
    override val description: String = "CloudStream: ${csApi.name} (${csApi.mainUrl})"
    override val supportedTypes: Set<MediaType> = csApi.supportedTypes.mapNotNull { it.toCineMediaType() }.toSet()
    override val iconUrl: String? = null

    override suspend fun getHome(): List<Category> = withContext(Dispatchers.IO) {
        if (!csApi.hasMainPage || csApi.mainPage.isEmpty()) return@withContext emptyList()

        val categories = mutableListOf<Category>()
        for (pageData in csApi.mainPage) {
            try {
                val request = MainPageRequest(
                    name = pageData.name,
                    data = pageData.data,
                    horizontalImages = pageData.horizontalImages,
                )
                val response = csApi.getMainPage(page = 1, request = request)
                for (homeList in response.items) {
                    val items = homeList.list.mapNotNull { it.toCineMediaItem(id) }
                    if (items.isNotEmpty()) {
                        categories.add(Category(homeList.name, items))
                    }
                }
            } catch (_: Exception) {}
        }
        categories
    }

    override suspend fun search(query: String, page: Int): SearchResult = withContext(Dispatchers.IO) {
        val results = csApi.search(query)
        val items = results?.mapNotNull { it.toCineMediaItem(id) } ?: emptyList()
        SearchResult(items = items, hasMore = false, page = page)
    }

    override suspend fun getDetails(mediaItem: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        val url = mediaItem.url ?: return@withContext mediaItem
        val loadResponse = csApi.load(url) ?: return@withContext mediaItem

        mediaItem.copy(
            title = loadResponse.name,
            description = loadResponse.plot,
            posterUrl = loadResponse.posterUrl ?: mediaItem.posterUrl,
            backdropUrl = loadResponse.backgroundPosterUrl ?: mediaItem.backdropUrl,
            year = loadResponse.year ?: mediaItem.year,
            rating = loadResponse.rating?.let { it.toDouble() / 1000.0 },
            genres = loadResponse.tags ?: mediaItem.genres,
        )
    }

    override suspend fun getEpisodes(mediaItem: MediaItem): List<CineEpisode> = withContext(Dispatchers.IO) {
        val url = mediaItem.url ?: return@withContext emptyList()
        val loadResponse = csApi.load(url) ?: return@withContext emptyList()

        when (loadResponse) {
            is TvSeriesLoadResponse -> {
                loadResponse.episodes.mapIndexed { index, ep ->
                    CineEpisode(
                        id = ep.data.ifEmpty { "${mediaItem.id}_ep$index" },
                        title = ep.name ?: "Episode ${ep.episode ?: (index + 1)}",
                        episodeNumber = ep.episode ?: (index + 1),
                        seasonNumber = ep.season ?: 1,
                        description = ep.description,
                        thumbnailUrl = ep.posterUrl,
                    )
                }
            }
            is AnimeLoadResponse -> {
                // Flatten episodes from all dub statuses, preferring subbed
                val episodes = loadResponse.episodes[DubStatus.Subbed]
                    ?: loadResponse.episodes[DubStatus.Dubbed]
                    ?: loadResponse.episodes.values.firstOrNull()
                    ?: return@withContext emptyList()

                episodes.mapIndexed { index, ep ->
                    CineEpisode(
                        id = ep.data.ifEmpty { "${mediaItem.id}_ep$index" },
                        title = ep.name ?: "Episode ${ep.episode ?: (index + 1)}",
                        episodeNumber = ep.episode ?: (index + 1),
                        seasonNumber = ep.season ?: 1,
                        description = ep.description,
                        thumbnailUrl = ep.posterUrl,
                    )
                }
            }
            else -> emptyList()
        }
    }

    override suspend fun getSources(mediaItem: MediaItem, episode: CineEpisode?): List<StreamSource> =
        withContext(Dispatchers.IO) {
            // Determine the data string to pass to loadLinks
            val data = when {
                episode != null -> episode.id  // Episode ID carries the loadLinks data
                else -> {
                    // For movies, we need to load() first to get the dataUrl
                    val url = mediaItem.url ?: return@withContext emptyList()
                    val loadResponse = csApi.load(url)
                    when (loadResponse) {
                        is MovieLoadResponse -> loadResponse.dataUrl
                        else -> url
                    }
                }
            }

            val sources = mutableListOf<StreamSource>()
            val subtitles = mutableListOf<SubtitleFile>()

            try {
                csApi.loadLinks(
                    data = data,
                    isCasting = false,
                    subtitleCallback = { sub -> subtitles.add(sub) },
                    callback = { link -> sources.add(link.toCineStreamSource()) },
                )
            } catch (_: Exception) {}

            sources
        }
}

// ──────────────────────── Type Mapping Helpers ────────────────────────

private fun TvType.toCineMediaType(): MediaType? = when (this) {
    TvType.Movie, TvType.AnimeMovie -> MediaType.MOVIE
    TvType.TvSeries, TvType.AsianDrama, TvType.Cartoon, TvType.Live -> MediaType.TV_SHOW
    TvType.Anime, TvType.OVA -> MediaType.ANIME
    TvType.Documentary -> MediaType.DOCUMENTARY
    else -> MediaType.MOVIE
}

private fun SearchResponse.toCineMediaItem(providerId: String): MediaItem? {
    return MediaItem(
        id = url,  // CS uses URL as ID
        title = name,
        posterUrl = posterUrl,
        year = year,
        type = type?.toCineMediaType() ?: MediaType.MOVIE,
        providerId = providerId,
        url = url,
    )
}

private fun ExtractorLink.toCineStreamSource(): StreamSource {
    val sourceType = when (type) {
        ExtractorLinkType.MAGNET -> StreamSourceType.MAGNET
        ExtractorLinkType.TORRENT -> StreamSourceType.TORRENT
        else -> StreamSourceType.DIRECT
    }

    return StreamSource(
        name = "$source - $name",
        url = url,
        type = sourceType,
        quality = if (quality > 0) quality.toQualityString() else null,
        headers = headers,
    )
}

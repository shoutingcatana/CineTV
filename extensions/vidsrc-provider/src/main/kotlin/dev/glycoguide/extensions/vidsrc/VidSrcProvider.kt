package dev.glycoguide.extensions.vidsrc

import dev.glycoguide.tv.model.*
import dev.glycoguide.tv.provider.ContentProvider
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

/**
 * VidSrc Provider — fetches movie/TV metadata from The Movie Database (TMDB)
 * free API and resolves streams from VidSrc-compatible embed sources.
 *
 * Uses TMDB for browse/search (freely available metadata) and
 * resolves actual video streams from multiple embed providers.
 */
class VidSrcProvider : ContentProvider {
    override val id = "vidsrc"
    override val name = "VidSrc"
    override val language = "en"
    override val description = "Movies & TV Shows via VidSrc embeds + TMDB metadata"
    override val supportedTypes = setOf(MediaType.MOVIE, MediaType.TV_SHOW)
    override val iconUrl: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@VidSrcProvider.json)
        }
        engine {
            requestTimeout = 15_000
        }
    }

    // TMDB free API (v3) — using publicly documented API
    // Users should set their own API key in production
    private val tmdbBaseUrl = "https://api.themoviedb.org/3"
    private val tmdbApiKey = "d56e51fb77b081a9cb5192571b7c679d" // Public demo key
    private val tmdbImageBase = "https://image.tmdb.org/t/p"

    private fun posterUrl(path: String?) = path?.let { "$tmdbImageBase/w342$it" }
    private fun backdropUrl(path: String?) = path?.let { "$tmdbImageBase/w780$it" }

    override suspend fun getHome(): List<Category> {
        val trending = fetchTmdb("/trending/all/week")
        val popularMovies = fetchTmdb("/movie/popular")
        val popularTv = fetchTmdb("/tv/popular")
        val topMovies = fetchTmdb("/movie/top_rated")

        return listOfNotNull(
            trending?.let { Category("Trending", parseResults(it, null)) },
            popularMovies?.let { Category("Beliebte Filme", parseResults(it, MediaType.MOVIE)) },
            popularTv?.let { Category("Beliebte Serien", parseResults(it, MediaType.TV_SHOW)) },
            topMovies?.let { Category("Top Rated", parseResults(it, MediaType.MOVIE)) },
        )
    }

    override suspend fun search(query: String, page: Int): SearchResult {
        val data = fetchTmdb("/search/multi", mapOf("query" to query, "page" to page.toString()))
            ?: return SearchResult(emptyList())

        val results = parseResults(data, null)
        val totalPages = data.jsonObject["total_pages"]?.jsonPrimitive?.intOrNull ?: 1

        return SearchResult(
            items = results,
            hasMore = page < totalPages,
            page = page,
        )
    }

    override suspend fun getDetails(mediaItem: MediaItem): MediaItem {
        val type = if (mediaItem.type == MediaType.TV_SHOW) "tv" else "movie"
        val tmdbId = mediaItem.id.removePrefix("tmdb_movie_").removePrefix("tmdb_tv_")
        val data = fetchTmdb("/$type/$tmdbId") ?: return mediaItem

        val obj = data.jsonObject
        return mediaItem.copy(
            title = obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
                ?: mediaItem.title,
            description = obj["overview"]?.jsonPrimitive?.contentOrNull ?: mediaItem.description,
            posterUrl = posterUrl(obj["poster_path"]?.jsonPrimitive?.contentOrNull) ?: mediaItem.posterUrl,
            backdropUrl = backdropUrl(obj["backdrop_path"]?.jsonPrimitive?.contentOrNull) ?: mediaItem.backdropUrl,
            rating = obj["vote_average"]?.jsonPrimitive?.doubleOrNull ?: mediaItem.rating,
            year = extractYear(obj) ?: mediaItem.year,
            genres = obj["genres"]?.jsonArray?.mapNotNull {
                it.jsonObject["name"]?.jsonPrimitive?.contentOrNull
            } ?: mediaItem.genres,
        )
    }

    override suspend fun getEpisodes(mediaItem: MediaItem): List<Episode> {
        if (mediaItem.type != MediaType.TV_SHOW) return emptyList()
        val tmdbId = mediaItem.id.removePrefix("tmdb_tv_")

        // First get the show details to know how many seasons
        val showData = fetchTmdb("/tv/$tmdbId") ?: return emptyList()
        val seasons = showData.jsonObject["seasons"]?.jsonArray ?: return emptyList()

        val episodes = mutableListOf<Episode>()

        for (season in seasons) {
            val seasonNum = season.jsonObject["season_number"]?.jsonPrimitive?.intOrNull ?: continue
            if (seasonNum == 0) continue // Skip specials

            val seasonData = fetchTmdb("/tv/$tmdbId/season/$seasonNum") ?: continue
            val eps = seasonData.jsonObject["episodes"]?.jsonArray ?: continue

            for (ep in eps) {
                val epObj = ep.jsonObject
                val epNum = epObj["episode_number"]?.jsonPrimitive?.intOrNull ?: continue
                episodes.add(
                    Episode(
                        id = "${tmdbId}_s${seasonNum}e${epNum}",
                        title = epObj["name"]?.jsonPrimitive?.contentOrNull ?: "Episode $epNum",
                        episodeNumber = epNum,
                        seasonNumber = seasonNum,
                        description = epObj["overview"]?.jsonPrimitive?.contentOrNull,
                        thumbnailUrl = epObj["still_path"]?.jsonPrimitive?.contentOrNull?.let {
                            "$tmdbImageBase/w300$it"
                        },
                    )
                )
            }
        }

        return episodes
    }

    override suspend fun getSources(mediaItem: MediaItem, episode: Episode?): List<StreamSource> {
        val tmdbId = mediaItem.id.removePrefix("tmdb_movie_").removePrefix("tmdb_tv_")
        val sources = mutableListOf<StreamSource>()

        if (mediaItem.type == MediaType.TV_SHOW && episode != null) {
            // TV show episode — multiple embed sources for yt-dlp / mpv resolution
            sources.add(
                StreamSource(
                    name = "VidSrc - S${episode.seasonNumber}E${episode.episodeNumber}",
                    url = "https://vidsrc.icu/embed/tv/$tmdbId/${episode.seasonNumber}/${episode.episodeNumber}",
                    type = StreamSourceType.DIRECT,
                    quality = "Auto",
                )
            )
            sources.add(
                StreamSource(
                    name = "VidSrc PRO",
                    url = "https://vidsrc.pro/embed/tv/$tmdbId/${episode.seasonNumber}/${episode.episodeNumber}",
                    type = StreamSourceType.DIRECT,
                    quality = "HD",
                )
            )
            sources.add(
                StreamSource(
                    name = "2Embed",
                    url = "https://www.2embed.cc/embedtv/$tmdbId&s=${episode.seasonNumber}&e=${episode.episodeNumber}",
                    type = StreamSourceType.DIRECT,
                    quality = "HD",
                )
            )
            sources.add(
                StreamSource(
                    name = "SuperEmbed",
                    url = "https://multiembed.mov/?video_id=$tmdbId&tmdb=1&s=${episode.seasonNumber}&e=${episode.episodeNumber}",
                    type = StreamSourceType.DIRECT,
                    quality = "HD",
                )
            )
        } else {
            // Movie
            sources.add(
                StreamSource(
                    name = "VidSrc",
                    url = "https://vidsrc.icu/embed/movie/$tmdbId",
                    type = StreamSourceType.DIRECT,
                    quality = "Auto",
                )
            )
            sources.add(
                StreamSource(
                    name = "VidSrc PRO",
                    url = "https://vidsrc.pro/embed/movie/$tmdbId",
                    type = StreamSourceType.DIRECT,
                    quality = "HD",
                )
            )
            sources.add(
                StreamSource(
                    name = "2Embed",
                    url = "https://www.2embed.cc/embed/$tmdbId",
                    type = StreamSourceType.DIRECT,
                    quality = "HD",
                )
            )
            sources.add(
                StreamSource(
                    name = "SuperEmbed",
                    url = "https://multiembed.mov/?video_id=$tmdbId&tmdb=1",
                    type = StreamSourceType.DIRECT,
                    quality = "HD",
                )
            )
        }

        // Also try to find torrent sources via public torrent search
        try {
            val torrentSources = findTorrentSources(mediaItem, episode)
            sources.addAll(torrentSources)
        } catch (_: Exception) { }

        return sources
    }

    private suspend fun findTorrentSources(mediaItem: MediaItem, episode: Episode?): List<StreamSource> {
        val sources = mutableListOf<StreamSource>()
        val searchQuery = buildString {
            append(mediaItem.title)
            mediaItem.year?.let { append(" $it") }
            if (episode != null) {
                append(" S%02dE%02d".format(episode.seasonNumber, episode.episodeNumber))
            }
        }

        // Search via 1337x (scrape search results)
        try {
            val encoded = java.net.URLEncoder.encode(searchQuery, "UTF-8")
            val response = client.get("https://1337x.to/search/$encoded/1/") {
                header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
            }
            val html = response.bodyAsText()
            val doc = org.jsoup.Jsoup.parse(html)

            val rows = doc.select("table.table-list tbody tr")
            for (row in rows.take(5)) {
                val titleCell = row.selectFirst("td.name a:nth-child(2)") ?: continue
                val title = titleCell.text()
                val href = titleCell.attr("href")
                val seedsText = row.selectFirst("td.seeds")?.text() ?: "0"
                val leechText = row.selectFirst("td.leeches")?.text() ?: "0"
                val sizeCell = row.selectFirst("td.size")?.ownText() ?: ""

                // Fetch the detail page to get magnet link
                try {
                    val detailResp = client.get("https://1337x.to$href") {
                        header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                    }
                    val detailHtml = detailResp.bodyAsText()
                    val detailDoc = org.jsoup.Jsoup.parse(detailHtml)
                    val magnetLink = detailDoc.selectFirst("a[href^=magnet:]")?.attr("href")

                    if (magnetLink != null) {
                        sources.add(
                            StreamSource(
                                name = title.take(80),
                                url = magnetLink,
                                type = StreamSourceType.MAGNET,
                                quality = extractQuality(title),
                                seeders = seedsText.toIntOrNull(),
                                leechers = leechText.toIntOrNull(),
                            )
                        )
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }

        return sources
    }

    private fun extractQuality(title: String): String {
        val t = title.lowercase()
        return when {
            "2160p" in t || "4k" in t -> "4K"
            "1080p" in t -> "1080p"
            "720p" in t -> "720p"
            "480p" in t -> "480p"
            "hdcam" in t || "cam" in t -> "CAM"
            "web-dl" in t || "webdl" in t -> "WEB-DL"
            "webrip" in t -> "WEBRip"
            "bluray" in t || "bdrip" in t -> "BluRay"
            else -> "Unknown"
        }
    }

    // ---- TMDB helpers ----

    private suspend fun fetchTmdb(path: String, params: Map<String, String> = emptyMap()): JsonElement? {
        return try {
            val response = client.get("$tmdbBaseUrl$path") {
                url {
                    parameters.append("api_key", tmdbApiKey)
                    parameters.append("language", "de-DE")
                    params.forEach { (k, v) -> parameters.append(k, v) }
                }
                header("User-Agent", "CineStream/1.0")
            }
            json.parseToJsonElement(response.bodyAsText())
        } catch (e: Exception) {
            System.err.println("TMDB fetch failed: $path - ${e.message}")
            null
        }
    }

    private fun parseResults(data: JsonElement, forceType: MediaType?): List<MediaItem> {
        val results = data.jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { parseMediaItem(it.jsonObject, forceType) }
    }

    private fun parseMediaItem(obj: JsonObject, forceType: MediaType?): MediaItem? {
        val mediaType = forceType ?: when (obj["media_type"]?.jsonPrimitive?.contentOrNull) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV_SHOW
            else -> return null // skip person results etc.
        }

        val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return null
        val prefix = if (mediaType == MediaType.TV_SHOW) "tmdb_tv_" else "tmdb_movie_"

        return MediaItem(
            id = "$prefix$id",
            title = obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
                ?: return null,
            posterUrl = posterUrl(obj["poster_path"]?.jsonPrimitive?.contentOrNull),
            backdropUrl = backdropUrl(obj["backdrop_path"]?.jsonPrimitive?.contentOrNull),
            description = obj["overview"]?.jsonPrimitive?.contentOrNull,
            year = extractYear(obj),
            rating = obj["vote_average"]?.jsonPrimitive?.doubleOrNull,
            type = mediaType,
            providerId = this.id,
            genres = obj["genre_ids"]?.jsonArray?.mapNotNull {
                genreMap[it.jsonPrimitive.intOrNull]
            } ?: emptyList(),
        )
    }

    private fun extractYear(obj: JsonObject): Int? {
        val date = obj["release_date"]?.jsonPrimitive?.contentOrNull
            ?: obj["first_air_date"]?.jsonPrimitive?.contentOrNull
            ?: return null
        return date.take(4).toIntOrNull()
    }

    companion object {
        // TMDB genre ID -> Name mapping (DE/EN mixed)
        private val genreMap = mapOf(
            28 to "Action", 12 to "Abenteuer", 16 to "Animation",
            35 to "Komödie", 80 to "Krimi", 99 to "Dokumentation",
            18 to "Drama", 10751 to "Familie", 14 to "Fantasy",
            36 to "Historie", 27 to "Horror", 10402 to "Musik",
            9648 to "Mystery", 10749 to "Romantik", 878 to "Sci-Fi",
            10770 to "TV-Film", 53 to "Thriller", 10752 to "Krieg",
            37 to "Western",
            // TV genres
            10759 to "Action & Abenteuer", 10762 to "Kinder",
            10763 to "News", 10764 to "Reality", 10765 to "Sci-Fi & Fantasy",
            10766 to "Soap", 10767 to "Talk", 10768 to "War & Politik",
        )
    }
}

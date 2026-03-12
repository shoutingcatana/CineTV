package dev.glycoguide.tv.provider

import dev.glycoguide.tv.model.*

/**
 * Built-in demo provider for testing the UI.
 * Shows sample data so the app has content even without extensions installed.
 */
class DemoProvider : ContentProvider {
    override val id = "demo"
    override val name = "Demo"
    override val language = "en"
    override val description = "Demo provider with sample content for testing the UI"
    override val supportedTypes = setOf(MediaType.MOVIE, MediaType.TV_SHOW)
    override val iconUrl: String? = null

    private val sampleMovies = listOf(
        MediaItem("demo_1", "Big Buck Bunny", description = "A large and lovable rabbit deals with three tiny bullies.", year = 2008, rating = 7.5, type = MediaType.MOVIE, providerId = id, genres = listOf("Animation", "Comedy")),
        MediaItem("demo_2", "Sintel", description = "A lonely young woman searches for her best friend, a dragon.", year = 2010, rating = 7.2, type = MediaType.MOVIE, providerId = id, genres = listOf("Animation", "Fantasy")),
        MediaItem("demo_3", "Tears of Steel", description = "In an apocalyptic future, a group of soldiers and scientists takes on robots.", year = 2012, rating = 6.8, type = MediaType.MOVIE, providerId = id, genres = listOf("Sci-Fi", "Action")),
        MediaItem("demo_4", "Cosmos Laundromat", description = "On a desolate island, a suicidal sheep called Franck meets his fate.", year = 2015, rating = 7.0, type = MediaType.MOVIE, providerId = id, genres = listOf("Animation", "Drama")),
        MediaItem("demo_5", "Elephant's Dream", description = "Two people explore a strange mechanical world.", year = 2006, rating = 6.5, type = MediaType.MOVIE, providerId = id, genres = listOf("Animation", "Sci-Fi")),
        MediaItem("demo_6", "Spring", description = "The story of a shepherd girl and her dog.", year = 2019, rating = 7.8, type = MediaType.MOVIE, providerId = id, genres = listOf("Animation")),
        MediaItem("demo_7", "Agent 327", description = "Agent 327 investigates a mysterious barbershop.", year = 2017, rating = 7.4, type = MediaType.MOVIE, providerId = id, genres = listOf("Animation", "Action")),
        MediaItem("demo_8", "Coffee Run", description = "A caffeinated adventure through the city.", year = 2020, rating = 6.9, type = MediaType.MOVIE, providerId = id, genres = listOf("Animation", "Comedy")),
    )

    private val sampleShows = listOf(
        MediaItem("demo_s1", "Demo Series Alpha", description = "A thrilling demo series.", year = 2023, rating = 8.1, type = MediaType.TV_SHOW, providerId = id, genres = listOf("Drama", "Thriller")),
        MediaItem("demo_s2", "Demo Series Beta", description = "A comedy demo series.", year = 2024, rating = 7.6, type = MediaType.TV_SHOW, providerId = id, genres = listOf("Comedy")),
    )

    override suspend fun getHome(): List<Category> {
        return listOf(
            Category("Trending Movies", sampleMovies.take(5)),
            Category("Top Rated", sampleMovies.sortedByDescending { it.rating }.take(5)),
            Category("TV Shows", sampleShows),
        )
    }

    override suspend fun search(query: String, page: Int): SearchResult {
        val q = query.lowercase()
        val results = (sampleMovies + sampleShows).filter {
            it.title.lowercase().contains(q) ||
            it.description?.lowercase()?.contains(q) == true ||
            it.genres.any { g -> g.lowercase().contains(q) }
        }
        return SearchResult(results, hasMore = false, page = page)
    }

    override suspend fun getDetails(mediaItem: MediaItem): MediaItem = mediaItem

    override suspend fun getEpisodes(mediaItem: MediaItem): List<Episode> {
        if (mediaItem.type != MediaType.TV_SHOW) return emptyList()
        return (1..10).map { ep ->
            Episode(
                id = "${mediaItem.id}_s1e$ep",
                title = "Episode $ep",
                episodeNumber = ep,
                seasonNumber = 1,
                description = "Episode $ep of ${mediaItem.title}",
            )
        }
    }

    override suspend fun getSources(mediaItem: MediaItem, episode: Episode?): List<StreamSource> {
        // These are Blender Foundation open movie URLs (free/legal content)
        return listOf(
            StreamSource(
                name = "Direct Stream (1080p)",
                url = "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4",
                type = StreamSourceType.DIRECT,
                quality = "1080p",
            ),
            StreamSource(
                name = "Torrent (720p)",
                url = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny",
                type = StreamSourceType.MAGNET,
                quality = "720p",
                seeders = 42,
                leechers = 5,
            ),
        )
    }
}

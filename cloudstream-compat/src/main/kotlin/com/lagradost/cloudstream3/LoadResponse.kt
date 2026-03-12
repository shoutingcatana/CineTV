@file:Suppress("unused")

package com.lagradost.cloudstream3

/**
 * CloudStream3 LoadResponse — replicated for compatibility.
 * Returned by MainAPI.load() with full media details.
 */
open class LoadResponse {
    var name: String = ""
    var url: String = ""
    var apiName: String = ""
    var type: TvType = TvType.Movie
    var posterUrl: String? = null
    var year: Int? = null
    var plot: String? = null
    var rating: Int? = null  // 0-10000 (CS uses x1000 scale)
    var tags: List<String>? = null
    var duration: Int? = null
    var recommendations: List<SearchResponse>? = null
    var actors: List<ActorData>? = null
    var comingSoon: Boolean = false
    var backgroundPosterUrl: String? = null
    var contentRating: String? = null
}

class MovieLoadResponse(
    name: String = "",
    url: String = "",
    apiName: String = "",
    type: TvType = TvType.Movie,
    var dataUrl: String = "",  // URL/data passed to loadLinks
) : LoadResponse() {
    init {
        this.name = name
        this.url = url
        this.apiName = apiName
        this.type = type
    }
}

class TvSeriesLoadResponse(
    name: String = "",
    url: String = "",
    apiName: String = "",
    type: TvType = TvType.TvSeries,
    var episodes: List<TvSeriesEpisode> = emptyList(),
    var showStatus: ShowStatus? = null,
) : LoadResponse() {
    init {
        this.name = name
        this.url = url
        this.apiName = apiName
        this.type = type
    }
}

class AnimeLoadResponse(
    name: String = "",
    url: String = "",
    apiName: String = "",
    type: TvType = TvType.Anime,
    var episodes: MutableMap<DubStatus, List<Episode>> = mutableMapOf(),
    var showStatus: ShowStatus? = null,
    var japName: String? = null,
    var engName: String? = null,
) : LoadResponse() {
    init {
        this.name = name
        this.url = url
        this.apiName = apiName
        this.type = type
    }
}

/** Episode data classes */
data class TvSeriesEpisode(
    val name: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val data: String = "",  // passed to loadLinks
    val posterUrl: String? = null,
    val rating: Int? = null,
    val description: String? = null,
    val date: Long? = null,
)

data class Episode(
    var data: String = "",
    var name: String? = null,
    var season: Int? = null,
    var episode: Int? = null,
    var posterUrl: String? = null,
    var rating: Int? = null,
    var description: String? = null,
    var date: Long? = null,
    var runTime: Int? = null,
)

/** Convenience constructors matching CS3 API */
fun newMovieLoadResponse(
    name: String,
    url: String,
    type: TvType,
    dataUrl: String,
): MovieLoadResponse {
    return MovieLoadResponse(name = name, url = url, type = type, dataUrl = dataUrl)
}

fun MainAPI.newMovieLoadResponse(
    name: String,
    url: String,
    type: TvType = TvType.Movie,
    data: String? = null,
    initializer: MovieLoadResponse.() -> Unit = {},
): MovieLoadResponse {
    return MovieLoadResponse(
        name = name, url = url, apiName = this.name, type = type,
        dataUrl = data ?: url,
    ).apply(initializer)
}

fun newTvSeriesLoadResponse(
    name: String,
    url: String,
    type: TvType,
    episodes: List<TvSeriesEpisode>,
): TvSeriesLoadResponse {
    return TvSeriesLoadResponse(name = name, url = url, type = type, episodes = episodes)
}

fun MainAPI.newTvSeriesLoadResponse(
    name: String,
    url: String,
    type: TvType = TvType.TvSeries,
    episodes: List<TvSeriesEpisode> = emptyList(),
    initializer: TvSeriesLoadResponse.() -> Unit = {},
): TvSeriesLoadResponse {
    return TvSeriesLoadResponse(
        name = name, url = url, apiName = this.name, type = type, episodes = episodes,
    ).apply(initializer)
}

fun MainAPI.newAnimeLoadResponse(
    name: String,
    url: String,
    type: TvType = TvType.Anime,
    initializer: AnimeLoadResponse.() -> Unit = {},
): AnimeLoadResponse {
    return AnimeLoadResponse(
        name = name, url = url, apiName = this.name, type = type,
    ).apply(initializer)
}

fun newEpisode(
    data: String,
    initializer: Episode.() -> Unit = {},
): Episode {
    return Episode(data = data).apply(initializer)
}

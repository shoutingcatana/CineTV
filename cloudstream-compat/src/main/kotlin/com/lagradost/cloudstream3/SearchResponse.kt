@file:Suppress("unused")

package com.lagradost.cloudstream3

/**
 * CloudStream3 SearchResponse — replicated for compatibility.
 * Base class for search results returned by providers.
 */
open class SearchResponse(
    var name: String,
    var url: String,
    var apiName: String = "",
    var type: TvType? = null,
    var posterUrl: String? = null,
    var year: Int? = null,
    var id: Int? = null,
    var quality: Int? = null,
    var posterHeaders: Map<String, String>? = null,
)

class MovieSearchResponse(
    name: String,
    url: String,
    apiName: String = "",
    type: TvType = TvType.Movie,
    posterUrl: String? = null,
    year: Int? = null,
    id: Int? = null,
    quality: Int? = null,
) : SearchResponse(name, url, apiName, type, posterUrl, year, id, quality)

class TvSeriesSearchResponse(
    name: String,
    url: String,
    apiName: String = "",
    type: TvType = TvType.TvSeries,
    posterUrl: String? = null,
    year: Int? = null,
    id: Int? = null,
    quality: Int? = null,
    val episodes: Int? = null,
) : SearchResponse(name, url, apiName, type, posterUrl, year, id, quality)

class AnimeSearchResponse(
    name: String,
    url: String,
    apiName: String = "",
    type: TvType = TvType.Anime,
    posterUrl: String? = null,
    year: Int? = null,
    id: Int? = null,
    quality: Int? = null,
    var dubStatus: Set<DubStatus>? = null,
    var subEpisodes: Int? = null,
    var dubEpisodes: Int? = null,
) : SearchResponse(name, url, apiName, type, posterUrl, year, id, quality)

/** Convenience constructors matching CS3 API */
fun newMovieSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Movie,
    fix: Boolean = true,
): MovieSearchResponse {
    return MovieSearchResponse(name = name, url = url, type = type)
}

fun newTvSeriesSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.TvSeries,
    fix: Boolean = true,
): TvSeriesSearchResponse {
    return TvSeriesSearchResponse(name = name, url = url, type = type)
}

fun newAnimeSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Anime,
    fix: Boolean = true,
): AnimeSearchResponse {
    return AnimeSearchResponse(name = name, url = url, type = type)
}

fun MainAPI.newMovieSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Movie,
    fix: Boolean = true,
    initializer: MovieSearchResponse.() -> Unit = {},
): MovieSearchResponse {
    val realUrl = if (fix) fixUrl(url, mainUrl) else url
    return MovieSearchResponse(name = name, url = realUrl, apiName = this.name, type = type).apply(initializer)
}

fun MainAPI.newTvSeriesSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.TvSeries,
    fix: Boolean = true,
    initializer: TvSeriesSearchResponse.() -> Unit = {},
): TvSeriesSearchResponse {
    val realUrl = if (fix) fixUrl(url, mainUrl) else url
    return TvSeriesSearchResponse(name = name, url = realUrl, apiName = this.name, type = type).apply(initializer)
}

fun MainAPI.newAnimeSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Anime,
    fix: Boolean = true,
    initializer: AnimeSearchResponse.() -> Unit = {},
): AnimeSearchResponse {
    val realUrl = if (fix) fixUrl(url, mainUrl) else url
    return AnimeSearchResponse(name = name, url = realUrl, apiName = this.name, type = type).apply(initializer)
}

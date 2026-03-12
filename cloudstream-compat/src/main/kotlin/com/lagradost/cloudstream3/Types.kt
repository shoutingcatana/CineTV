@file:Suppress("unused")

package com.lagradost.cloudstream3

/**
 * CloudStream3 TvType — replicated for compatibility.
 * Maps to CineStream MediaType.
 */
enum class TvType {
    Movie,
    TvSeries,
    Anime,
    AnimeMovie,
    OVA,
    Cartoon,
    Documentary,
    AsianDrama,
    Live,
    NSFW,
    Others,
    Torrent;

    companion object {
        // Helper used by some extensions
        val all = entries.toSet()
    }
}

enum class ShowStatus {
    Completed,
    Ongoing,
    Unknown
}

enum class DubStatus {
    Dubbed,
    Subbed
}

enum class ActorRole {
    Main,
    Supporting,
    Background
}

data class ActorData(
    val actor: Actor,
    val role: ActorRole = ActorRole.Main,
    val roleString: String? = null,
    val voiceActor: Actor? = null,
)

data class Actor(
    val name: String,
    val image: String? = null,
)

/** Quality profiles used by ExtractorLink */
object Qualities {
    const val Unknown = 0
    const val P360 = 360
    const val P480 = 480
    const val P720 = 720
    const val P1080 = 1080
    const val P1440 = 1440
    const val P2160 = 2160
}

/** Helper to get quality label */
fun getQualityFromName(name: String?): Int {
    if (name == null) return Qualities.Unknown
    return when {
        name.contains("2160") || name.contains("4k", ignoreCase = true) -> Qualities.P2160
        name.contains("1440") -> Qualities.P1440
        name.contains("1080") -> Qualities.P1080
        name.contains("720") -> Qualities.P720
        name.contains("480") -> Qualities.P480
        name.contains("360") -> Qualities.P360
        else -> Qualities.Unknown
    }
}

fun Int.toQualityString(): String = when (this) {
    Qualities.P2160 -> "4K"
    Qualities.P1440 -> "1440p"
    Qualities.P1080 -> "1080p"
    Qualities.P720 -> "720p"
    Qualities.P480 -> "480p"
    Qualities.P360 -> "360p"
    else -> "Unknown"
}

/** URL fixed helper used by many extensions */
fun fixUrl(url: String, baseUrl: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    if (url.startsWith("//")) return "https:$url"
    if (url.startsWith("/")) return baseUrl.trimEnd('/') + url
    return "$baseUrl/$url"
}

fun fixUrlNull(url: String?, baseUrl: String): String? {
    if (url.isNullOrBlank()) return null
    return fixUrl(url, baseUrl)
}

@file:Suppress("unused")

package com.lagradost.cloudstream3

/**
 * CloudStream3 ExtractorLink & SubtitleFile — replicated for compatibility.
 * These are the types passed to loadLinks callbacks.
 */
data class ExtractorLink(
    val source: String,
    val name: String,
    val url: String,
    val referer: String = "",
    val quality: Int = Qualities.Unknown,
    val type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    val headers: Map<String, String> = emptyMap(),
    val extractorData: String? = null,
    val isM3u8: Boolean = false,
    val isDash: Boolean = false,
)

enum class ExtractorLinkType {
    VIDEO,
    M3U8,
    DASH,
    MAGNET,
    TORRENT,
}

data class SubtitleFile(
    val lang: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

/** Home page data classes */
data class HomePageResponse(
    val items: List<HomePageList>,
    val hasNext: Boolean = false,
)

data class HomePageList(
    val name: String,
    val list: List<SearchResponse>,
    val isHorizontalImages: Boolean = false,
)

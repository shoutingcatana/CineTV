@file:Suppress("unused")

package com.lagradost.cloudstream3

/**
 * CloudStream3 MainAPI — the base class all CS extensions extend.
 * This is the compatibility shim that replicates the CS3 API surface.
 */
abstract class MainAPI {
    /** Unique name used as ID */
    open var name: String = "Unknown"

    /** Main URL of the provider site */
    open var mainUrl: String = ""

    /** Language tag (e.g., "en", "de") */
    open var lang: String = "en"

    /** Supported content types */
    open val supportedTypes: Set<TvType> = setOf(TvType.Movie)

    /** Whether this provider has a main page / home */
    open val hasMainPage: Boolean = false

    /** Whether search has quick-search */
    open val hasQuickSearch: Boolean = false

    /** Whether downloads are supported */
    open val hasDownloadSupport: Boolean = true

    /** Main page requests (pairs of name to URL) */
    open val mainPage: List<MainPageData> = emptyList()

    /**
     * Get the home/main page content.
     * Default implementation uses mainPage URLs.
     */
    open suspend fun getMainPage(page: Int = 1, request: MainPageRequest): HomePageResponse {
        throw NotImplementedError()
    }

    /** Search for content */
    open suspend fun search(query: String): List<SearchResponse>? {
        return null
    }

    /** Load full details for a media item */
    open suspend fun load(url: String): LoadResponse? {
        return null
    }

    /**
     * Load stream links for watching.
     * Implementations call callback() and subtitleCallback() to add links.
     */
    open suspend fun loadLinks(
        data: String,
        isCasting: Boolean = false,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        return false
    }
}

/** Data class for main page requests */
data class MainPageData(
    val name: String,
    val data: String,
    val horizontalImages: Boolean = false,
)

data class MainPageRequest(
    val name: String,
    val data: String,
    val horizontalImages: Boolean = false,
)

fun mainPageOf(vararg pairs: Pair<String, String>): List<MainPageData> {
    return pairs.map { (name, data) -> MainPageData(name, data) }
}

/** Convenience to create a newHomePageResponse */
fun newHomePageResponse(
    name: String,
    list: List<SearchResponse>,
    hasNext: Boolean = false,
): HomePageResponse {
    return HomePageResponse(listOf(HomePageList(name, list, isHorizontalImages = false)), hasNext)
}

fun newHomePageResponse(
    list: HomePageList,
    hasNext: Boolean = false,
): HomePageResponse {
    return HomePageResponse(listOf(list), hasNext)
}

fun newHomePageResponse(
    list: List<HomePageList>,
    hasNext: Boolean = false,
): HomePageResponse {
    return HomePageResponse(list, hasNext)
}

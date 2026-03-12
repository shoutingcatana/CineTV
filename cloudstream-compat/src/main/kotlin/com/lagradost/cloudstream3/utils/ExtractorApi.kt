@file:Suppress("unused")

package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.*

/**
 * CloudStream3 Extractor base class — replicated for compatibility.
 * Many CS extensions define custom extractors for embed sites.
 */
abstract class ExtractorApi {
    abstract val name: String
    abstract val mainUrl: String
    abstract val requiresReferer: Boolean

    open suspend fun getUrl(
        url: String,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit = {},
        callback: (ExtractorLink) -> Unit,
    ) {
        // Default: no-op. Subclasses override to extract links.
    }
}

/**
 * Registry of known extractors — CS extensions register extractors here.
 */
object ExtractorRegistry {
    private val extractors = mutableListOf<ExtractorApi>()

    fun register(extractor: ExtractorApi) {
        if (extractors.none { it.name == extractor.name }) {
            extractors.add(extractor)
        }
    }

    fun getExtractor(url: String): ExtractorApi? {
        return extractors.find { url.contains(it.mainUrl) }
    }

    fun getAll(): List<ExtractorApi> = extractors.toList()

    suspend fun loadExtractorLinks(
        url: String,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit = {},
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val extractor = getExtractor(url) ?: return false
        extractor.getUrl(url, referer, subtitleCallback, callback)
        return true
    }
}

/** Helper function used by extensions: loadExtractor */
suspend fun loadExtractor(
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit = {},
    callback: (ExtractorLink) -> Unit,
): Boolean {
    return ExtractorRegistry.loadExtractorLinks(url, referer, subtitleCallback, callback)
}

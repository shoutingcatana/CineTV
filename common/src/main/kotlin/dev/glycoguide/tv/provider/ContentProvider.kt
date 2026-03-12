package dev.glycoguide.tv.provider

import dev.glycoguide.tv.model.*

/**
 * Interface for content providers (extensions).
 * Each provider supplies media content from a specific source.
 * Providers are loaded as JAR extensions via Java ServiceLoader.
 *
 * To create an extension:
 * 1. Implement this interface
 * 2. Register it in META-INF/services/dev.glycoguide.tv.provider.ContentProvider
 * 3. Package as a JAR and place in the extensions directory
 */
interface ContentProvider {
    val id: String
    val name: String
    val language: String
    val description: String
    val supportedTypes: Set<MediaType>
    val iconUrl: String?

    /** Get homepage categories (trending, popular, etc.) */
    suspend fun getHome(): List<Category>

    /** Search for media items */
    suspend fun search(query: String, page: Int = 1): SearchResult

    /** Get full details for a media item */
    suspend fun getDetails(mediaItem: MediaItem): MediaItem

    /** Get episodes for a TV show / anime */
    suspend fun getEpisodes(mediaItem: MediaItem): List<Episode>

    /** Get available stream sources for playback */
    suspend fun getSources(mediaItem: MediaItem, episode: Episode? = null): List<StreamSource>
}

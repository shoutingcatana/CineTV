@file:Suppress("unused")

package com.lagradost.cloudstream3

/**
 * CloudStream3 Plugin — the entry point for CS extensions.
 * Extensions typically implement a class extending Plugin and
 * annotate with @CloudstreamPlugin.
 */
abstract class Plugin {
    /** Called when the plugin is loaded */
    open fun load(context: Any?) {}
}

/**
 * CloudStream plugin annotation — used to identify plugin classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CloudstreamPlugin

/**
 * Registry that CloudStream plugins use to register their providers.
 * CS extensions call registerMainAPI() in their Plugin.load() method.
 */
object CloudStreamRegistrar {
    private val registeredApis = mutableListOf<MainAPI>()
    private val registeredExtractors = mutableListOf<com.lagradost.cloudstream3.utils.ExtractorApi>()

    fun registerMainAPI(api: MainAPI) {
        if (registeredApis.none { it.name == api.name }) {
            registeredApis.add(api)
        }
    }

    fun registerExtractorAPI(extractor: com.lagradost.cloudstream3.utils.ExtractorApi) {
        com.lagradost.cloudstream3.utils.ExtractorRegistry.register(extractor)
        registeredExtractors.add(extractor)
    }

    fun getRegisteredApis(): List<MainAPI> = registeredApis.toList()

    fun clearRegistered() {
        registeredApis.clear()
        registeredExtractors.clear()
    }

    /** Take all newly registered APIs and clear the list */
    fun takeRegisteredApis(): List<MainAPI> {
        val apis = registeredApis.toList()
        registeredApis.clear()
        return apis
    }
}

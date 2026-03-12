@file:Suppress("unused")

package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.*
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * CloudStream3 app.get / app.post — replicated for compatibility.
 * Many CS extensions use `app.get(url)` for HTTP requests.
 */
object AppUtils {
    val mapper = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    inline fun <reified T> parseJson(json: String): T {
        return mapper.decodeFromString(json)
    }

    inline fun <reified T> tryParseJson(json: String): T? {
        return try {
            mapper.decodeFromString(json)
        } catch (_: Exception) {
            null
        }
    }
}

/** Simple HTTP response wrapper */
data class NapiResponse(
    val text: String,
    val url: String,
    val code: Int,
    val headers: Map<String, List<String>>,
) {
    val isSuccessful get() = code in 200..299

    /** Parse the body via Jsoup */
    val document get() = org.jsoup.Jsoup.parse(text)

    /** Parse JSON via kotlinx serialization */
    inline fun <reified T> parsed(): T = AppUtils.parseJson(text)
}

/**
 * Network access object used by CS extensions via `app.get()` / `app.post()`.
 */
class CloudStreamNetwork {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        referer: String? = null,
        params: Map<String, String> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        timeout: Long = 15_000,
    ): NapiResponse {
        val urlBuilder = url.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("Invalid URL: $url")
        params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

        val requestBuilder = Request.Builder().url(urlBuilder.build()).get()
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        referer?.let { requestBuilder.addHeader("Referer", it) }
        if (cookies.isNotEmpty()) {
            requestBuilder.addHeader("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
        }
        requestBuilder.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        val response = client.newCall(requestBuilder.build()).execute()
        return responseToNapi(response)
    }

    fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        referer: String? = null,
        data: Map<String, String> = emptyMap(),
        json: Any? = null,
        requestBody: okhttp3.RequestBody? = null,
        cookies: Map<String, String> = emptyMap(),
        timeout: Long = 15_000,
    ): NapiResponse {
        val body = requestBody ?: when {
            json != null -> {
                val jsonStr = kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.json.JsonElement.serializer(),
                    kotlinx.serialization.json.JsonPrimitive(json.toString())
                )
                jsonStr.toRequestBody("application/json".toMediaTypeOrNull())
            }
            data.isNotEmpty() -> {
                val formBuilder = FormBody.Builder()
                data.forEach { (k, v) -> formBuilder.add(k, v) }
                formBuilder.build()
            }
            else -> ByteArray(0).toRequestBody(null)
        }

        val requestBuilder = Request.Builder().url(url).post(body)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        referer?.let { requestBuilder.addHeader("Referer", it) }
        if (cookies.isNotEmpty()) {
            requestBuilder.addHeader("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
        }
        requestBuilder.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        val response = client.newCall(requestBuilder.build()).execute()
        return responseToNapi(response)
    }

    private fun responseToNapi(response: Response): NapiResponse {
        val body = response.body?.string() ?: ""
        val headersMap = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { name ->
            headersMap[name] = response.headers.values(name)
        }
        return NapiResponse(
            text = body,
            url = response.request.url.toString(),
            code = response.code,
            headers = headersMap,
        )
    }
}

/** Global network instance — this is what CS extensions access as `app` */
val app = CloudStreamNetwork()

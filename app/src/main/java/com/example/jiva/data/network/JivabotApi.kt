package com.example.jiva.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Minimal Jivabot WhatsApp REST API client using OkHttp.
 * All endpoints are POST with query parameters.
 */
object JivabotApi {
    private const val HOST = "jivabot.com"
    private const val BASE_PATH = "api"

    private val client: OkHttpClient by lazy { OkHttpClient() }
    private val emptyBody = ByteArray(0).toRequestBody("application/x-www-form-urlencoded".toMediaType())

    private fun buildUrl(pathSegment: String, params: Map<String, String?>): HttpUrl {
        // jivabot requires HTTP (not HTTPS) and accepts application/x-www-form-urlencoded style encoding
        val builder = HttpUrl.Builder()
            .scheme("http")
            .host(HOST)
            .addPathSegments("$BASE_PATH/$pathSegment")
        params.forEach { (k, v) ->
            if (!v.isNullOrBlank()) {
                // Preserve Postman-like encoding for spaces as '+' by pre-encoding message
                val value = if (k == "message") {
                    URLEncoder.encode(v, StandardCharsets.UTF_8.name())
                } else v
                builder.addEncodedQueryParameter(k, value)
            }
        }
        return builder.build()
    }

    private suspend fun execute(url: HttpUrl): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "JIVA-Android/1.0")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(emptyBody)
            .build()
        client.newCall(request).execute().use { resp ->
            val ok = resp.isSuccessful
            val body = resp.body?.string()
            if (!ok) {
                Timber.w("Jivabot API error ${'$'}{resp.code}: ${'$'}{resp.message}. Body: ${'$'}body")
            } else {
                Timber.d("Jivabot API success ${'$'}{resp.code}")
            }
            ok to body
        }
    }

    // Messaging
    suspend fun send(
        number: String,
        type: String = "text",
        message: String,
        mediaUrl: String? = null,
        filename: String? = null,
        instanceId: String,
        accessToken: String
    ): Pair<Boolean, String?> {
        val url = buildUrl(
            pathSegment = "send",
            params = mapOf(
                "number" to number,
                "type" to type,
                "message" to message,
                "media_url" to mediaUrl,
                "filename" to filename,
                "instance_id" to instanceId,
                "access_token" to accessToken
            )
        )
        return execute(url)
    }

    suspend fun sendGroup(
        groupId: String,
        type: String = "text",
        message: String,
        mediaUrl: String? = null,
        filename: String? = null,
        instanceId: String,
        accessToken: String
    ): Pair<Boolean, String?> {
        val url = buildUrl(
            pathSegment = "send_group",
            params = mapOf(
                "group_id" to groupId,
                "type" to type,
                "message" to message,
                "media_url" to mediaUrl,
                "filename" to filename,
                "instance_id" to instanceId,
                "access_token" to accessToken
            )
        )
        return execute(url)
    }

    // Connection management
    suspend fun reconnect(instanceId: String, accessToken: String): Pair<Boolean, String?> {
        val url = buildUrl("reconnect", mapOf("instance_id" to instanceId, "access_token" to accessToken))
        return execute(url)
    }

    suspend fun reboot(instanceId: String, accessToken: String): Pair<Boolean, String?> {
        val url = buildUrl("reboot", mapOf("instance_id" to instanceId, "access_token" to accessToken))
        return execute(url)
    }

    suspend fun resetInstance(instanceId: String, accessToken: String): Pair<Boolean, String?> {
        val url = buildUrl("reset_instance", mapOf("instance_id" to instanceId, "access_token" to accessToken))
        return execute(url)
    }

    suspend fun setWebhook(webhookUrl: String, instanceId: String, accessToken: String): Pair<Boolean, String?> {
        val url = buildUrl(
            "set_webhook",
            mapOf(
                "webhook_url" to webhookUrl,
                "instance_id" to instanceId,
                "access_token" to accessToken
            )
        )
        return execute(url)
    }
}
package com.tanmaybaid.am.publisher

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class PushoverPublisher(private val http: HttpClient) : Publisher {
    override suspend fun publish(request: String, message: String) {
        val requestParts = request.split('|')
        val user = requestParts[0]
        val requiredParams = mapOf(
            "token" to TOKEN,
            "user" to user,
            "message" to message
        )
        val optionalParams = requestParts.getOrNull(1)?.toMap("&", "=").orEmpty()

        http.post {
            url("https://api.pushover.net/1/messages.json")
            contentType(ContentType.Application.Json)
            setBody(requiredParams + optionalParams)
        }
    }

    private fun String.toMap(entryDelimiter: String, keyValueDelimiter: String) =
        this.split(entryDelimiter).associate {
            val (left, right) = it.split(keyValueDelimiter)
            left to right
        }

    companion object {
        private const val TOKEN: String = "ad8f99m1o2eck1zhzhvk5gcjxevaa7"
    }
}

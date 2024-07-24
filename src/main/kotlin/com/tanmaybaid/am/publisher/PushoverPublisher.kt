package com.tanmaybaid.am.publisher

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PushoverPublisher(private val http: HttpClient) : Publisher {
    override suspend fun publish(request: String, message: String) {
        val (user, params) = request.split('|')
        val requiredParams = mapOf(
            "token" to TOKEN,
            "user" to user!!,
            "message" to message
        )
        val optionalParams = params?.toMap("&", "=").orEmpty()

        http.post {
            url("https://api.pushover.net/1/messages.json")
            contentType(ContentType.Application.Json)
            setBody(requiredParams + optionalParams)
        }
    }

    private fun String.toMap(
        entryDelimiter: String,
        keyValueDelimiter: String
    ) = this.split(entryDelimiter).associate {
        val (left, right) = it.split(keyValueDelimiter)
        left to right
    }

    companion object {
        private const val TOKEN: String = "ad8f99m1o2eck1zhzhvk5gcjxevaa7"
    }
}

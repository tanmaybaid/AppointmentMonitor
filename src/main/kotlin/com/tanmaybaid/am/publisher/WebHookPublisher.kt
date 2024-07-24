package com.tanmaybaid.am.publisher

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

class WebHookPublisher(private val http: HttpClient) : Publisher {
    override suspend fun publish(request: String, message: String) {
        val (url, param) = request.split('|')
        val key = param ?: "Content"

        http.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(mapOf(key to message))
        }
    }
}

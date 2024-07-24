package com.tanmaybaid.am.publisher

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class WebHookPublisher(private val http: HttpClient) : Publisher {
    override suspend fun publish(request: String, message: String) {
        val (url, param) = request.split('|')
        val key = param ?: "Content"
        val body = mapOf(key to message)

        val response = http.post {
            url(url!!)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (response.status != HttpStatusCode.OK) {
            logger.warn("Failed to publish message with status ${response.status}")
        }
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(WebHookPublisher::class.java.name)
    }
}

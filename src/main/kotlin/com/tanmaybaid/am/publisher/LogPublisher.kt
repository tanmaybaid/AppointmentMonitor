package com.tanmaybaid.am.publisher

import org.apache.logging.log4j.kotlin.logger

class LogPublisher : Publisher {
    override suspend fun publish(request: String, message: String) {
        LOGGER.info { message }
    }

    companion object {
        private val LOGGER = logger()
    }
}

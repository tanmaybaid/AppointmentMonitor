package com.tanmaybaid.am.publisher

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class LogPublisher : Publisher {
    override suspend fun publish(request: String, message: String) {
        logger.info(message)
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(LogPublisher::class.java.name)
    }
}

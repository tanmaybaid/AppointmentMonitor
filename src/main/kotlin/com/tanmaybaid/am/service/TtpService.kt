package com.tanmaybaid.am.service

import com.tanmaybaid.am.model.Location
import com.tanmaybaid.am.model.SlotAvailability
import com.tanmaybaid.am.publisher.LogPublisher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class TtpService(private val http: HttpClient) {
    suspend fun getLocations(): List<Location> =
        call("locations")

    suspend fun getSlotAvailability(location: Location): SlotAvailability =
        call("slot-availability", "locationId" to location.id)

    private suspend inline fun <reified T> call(api: String, vararg params: Pair<String, Any?>): T {
        val url = "$ENDPOINT/$api/"

        val failed = "Request to $url with params: ${toQueryParams(*params)} failed"
        val response = try {
            http.get {
                url(url)
                params.forEach { parameter(it.first, it.second) }
            }
        } catch (ex: Exception) {
            logger.error("$failed due to ${ex.message}.", ex)
            throw ex
        }

        try {
            return response.body<T>()
        } catch (ex: Exception) {
            logger.error("$failed with response: ${response.bodyAsText()} due to ${ex.message}.", ex)
            throw ex
        }
    }

    private fun toQueryParams(vararg params: Pair<String, Any?>) =
        params.joinToString("&") { "${it.first}=${it.second}" }

    companion object {
        private const val ENDPOINT = "https://ttp.cbp.dhs.gov/schedulerapi"
        private val logger: Logger = LogManager.getLogger(LogPublisher::class.java.name)
    }
}

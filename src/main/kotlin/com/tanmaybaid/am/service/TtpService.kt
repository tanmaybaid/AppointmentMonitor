package com.tanmaybaid.am.service

import com.tanmaybaid.am.model.Location
import com.tanmaybaid.am.model.SlotAvailability
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url

class TtpService(private val http: HttpClient) {
    suspend fun getLocations() = runCatching {
        call("locations")
    }.mapCatching {
        it.body<List<Location>>()
    }

    suspend fun getSlotAvailability(location: Location) = runCatching {
        call("slot-availability", "locationId" to location.id)
    }.mapCatching {
        it.body<SlotAvailability>()
    }

    private suspend fun call(api: String, vararg params: Pair<String, Any?>) = http.get {
        url("$ENDPOINT/$api/")
        params.forEach { parameter(it.first, it.second) }
    }

    companion object {
        private const val ENDPOINT = "https://ttp.cbp.dhs.gov/schedulerapi"
    }
}

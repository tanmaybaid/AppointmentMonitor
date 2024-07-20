package com.tanmaybaid.am.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import java.time.LocalDate

class TtpService(private val http: HttpClient) {
    suspend fun getLocations(): List<Location> =
        call("locations").body()

    suspend fun getSlotAvailability(location: Location): SlotAvailability =
        call("slot-availability", "locationId" to location.id).body()

    private suspend fun call(api: String, vararg params: Pair<String, Any?>) = http.get {
        url("$ENDPOINT/$api/")
        params.forEach { parameter(it.first, it.second) }
    }

    data class LocationService(
        val id: Int,
        val name: String,
    )

    data class Location(
        val id: Int,
        val name: String,
        val shortName: String,
        val locationType: String,
        val locationCode: String,
        val address: String,
        val addressAdditional: String,
        val city: String,
        val state: String,
        val postalCode: String,
        val countryCode: String,
        val tzData: String,
        val temporary: Boolean,
        val inviteOnly: Boolean,
        val operational: Boolean,
        val services: List<LocationService>,
    )

    data class SlotAvailability(
        val availableSlots: List<LocalDate>,
        val lastPublishedDate: LocalDate?
    )

    companion object {
        private const val ENDPOINT = "https://ttp.cbp.dhs.gov/schedulerapi"
    }
}

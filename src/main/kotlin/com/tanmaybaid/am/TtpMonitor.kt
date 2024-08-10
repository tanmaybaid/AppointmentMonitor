package com.tanmaybaid.am

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.tanmaybaid.am.model.Location
import com.tanmaybaid.am.model.SlotAvailability
import com.tanmaybaid.am.publisher.LogPublisher
import com.tanmaybaid.am.publisher.Publisher
import com.tanmaybaid.am.publisher.PushoverPublisher
import com.tanmaybaid.am.publisher.WebHookPublisher
import com.tanmaybaid.am.service.TtpService
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger

class TtpMonitor : CliktCommand() {
    private val client: HttpClient by lazy {
        HttpClient(Apache5) {
            install(HttpRequestRetry) {
                retryOnException(maxRetries = 3, retryOnTimeout = true)
                exponentialDelay()
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 6000
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 5000
            }

            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true)
                    registerModules(JavaTimeModule())
                }
            }
        }
    }

    private val locationIds: List<Int> by option()
        .help("Comma seperated list of location ids.")
        .int().split(",").required()
    private val pollPeriod: Duration by option()
        .help("[Optional] [Default: 15] Seconds to wait between polling for available slots.")
        .int().restrictTo(10..3600).convert { it.seconds }.default(15.seconds)
    private val backoffPeriod: Duration? by option()
        .help("[Optional] [Defaults to poll period] Seconds to wait after identifying available slots.")
        .int().restrictTo(10..36000).convert { it.seconds }
    private val before: LocalDateTime by option()
        .help("[Optional] [Publisher called for any available slots] Date in the ISO_LOCAL_DATE_TIME format" +
                " (e.g. 2024-05-01T08:25:30). Publisher will only be invoked if available slot is before this date.")
        .convert { LocalDateTime.parse(it) }.default(LocalDateTime.MAX)
    private val publishTo: List<String> by option()
        .help("[Optional] [Default: Log] Comma separated list of publishers, e.g. $PUBLISH_TO_EXAMPLES")
        .split(",").default(listOf("Log"))

    override fun run() = runBlocking {
        val ttp = TtpService(client)
        val publishers = listOf(
            LogPublisher(),
            PushoverPublisher(client),
            WebHookPublisher(client),
        ).associateBy(Publisher::name).mapKeys { it.key.lowercase() }

        run(ttp, publishers).join()
    }

    private fun CoroutineScope.run(ttp: TtpService, publishers: Map<String, Publisher>) = launch {
        val availableLocations: List<Location> = ttp.getLocations().getOrThrow()
        val inputLocations: Set<Location> = normalize(locationIds.toSet(), availableLocations)

        while (!Thread.interrupted()) {
            val hasAvailableSlots = inputLocations.associateWith { location ->
                try {
                    checkSlotAvailability(ttp, location, publishers)
                } catch (ex: Exception) {
                    LOGGER.warn(ex) { "Failed to check slot availability for ${location.id} due to ${ex.message}" }
                    CompletableDeferred(false)
                }
            }.mapValues { (_, deferred) ->
                deferred.await()
            }

            val locationsWithNoAvailableSlots = hasAvailableSlots.filter { !it.value }.keys.map { it.simpleName }
            if (locationsWithNoAvailableSlots.size > 1) {
                LOGGER.info { "No slots found for ${locationsWithNoAvailableSlots.joinToString()}." }
            }

            val slotsFound = hasAvailableSlots.values.reduce { a, b -> a || b }
            val delay = if (slotsFound) backoffPeriod ?: pollPeriod else pollPeriod
            LOGGER.info { "Sleeping for $delay before checking again." }
            delay(delay)
        }

        LOGGER.info { "Exiting!" }
    }

    private fun CoroutineScope.checkSlotAvailability(
        ttp: TtpService,
        location: Location,
        publishers: Map<String, Publisher>,
    ) = async {
        val slotAvailability: SlotAvailability = ttp.getSlotAvailability(location).getOrThrow()
        LOGGER.debug { "SlotAvailability retrieved for ${location.simpleName}: $slotAvailability" }

        val hasAvailableSlots = slotAvailability.availableSlots.isNotEmpty()
        if (hasAvailableSlots) {
            val inactiveSlots = mutableListOf<LocalDateTime>()
            val ineligibleSlots = mutableListOf<LocalDateTime>()
            val eligibleSlots = mutableListOf<LocalDateTime>()

            slotAvailability.availableSlots.forEach { availableSlot ->
                val slot = availableSlot.startTimestamp
                if (availableSlot.active) {
                    if (slot.isBefore(before)) {
                        eligibleSlots.add(slot)
                    } else {
                        ineligibleSlots.add(slot)
                    }
                } else {
                    inactiveSlots.add(slot)
                }
            }

            if (eligibleSlots.isNotEmpty()) {
                val message = "Found ${countSlots(eligibleSlots.size)}: ${eligibleSlots.joinToStringWithLast()}" +
                        " at ${location.simpleName}"

                publishTo.forEach { publish(publishers, it, message) }
            }

            if (ineligibleSlots.isNotEmpty()) {
                LOGGER.debug { "Available slot after requested date of $before: $ineligibleSlots." }
            }

            if (inactiveSlots.isNotEmpty()) {
                LOGGER.debug { "Slot available, but inactive: $inactiveSlots." }
            }
        }

        hasAvailableSlots
    }

    private fun CoroutineScope.publish(
        publishers: Map<String, Publisher>,
        publishTo: String,
        message: String
    ) = launch {
        val (publisherName, publisherRequest) = publishTo.split('=')
        val publisher = publisherName?.lowercase()?.let { publishers[it] }

        publisher?.publish(publisherRequest.orEmpty(), message)
    }

    private fun normalize(
        inputLocationIds: Set<Int>,
        availableLocations: List<Location>
    ): Set<Location> {
        val inputLocationByIds = availableLocations.filter { inputLocationIds.contains(it.id) }.associateBy { it.id }

        if (inputLocationByIds.size != inputLocationIds.size) {
            val invalidIds = inputLocationIds.filterNot(inputLocationByIds::containsKey)
            throw IllegalArgumentException("Following locationIds are not valid: $invalidIds")
        }

        return inputLocationByIds.values.toSet()
    }

    companion object {
        private const val PUBLISH_TO_EXAMPLES = """
            - For slack or chime webhooks:
              - To use default message field "Content: "Webhook=webhook_url"
                - "Webhook=https://hooks.slack.com/workflows/T012V3P4FHQ/A01MJ2ECBJ3/123456789012345678/somemorestring"
              - To provide a custom message field: "Webhook=webhook_url|custom_field_name"
                - "Webhook=https://hooks.slack.com/workflows/T012V3P4FHQ/A01MJ2ECBJ3/123456789012345678/somemorestring|msg"
            - For Pushover:
              - To use default settings: "Pushover=pushover_user_token"
                - "Pushover=it1is2not3a4valid5user6token"
              - To override settings: "Pushover=pushover_user_token|setting-name=setting-value&another-name=another-value"
                - "Pushover=it1is2not3a4valid5user6token|device=myDeviceName&title=CustomTitle"
            - For Logs on STDOUT:
              - "Log"
        """

        private val LOGGER = logger()
    }

    private fun <T> Collection<T>.joinToStringWithLast(limit: Int = 3) =
        joinToString(limit = limit) + if (size > limit) " ${last()}" else ""

    private fun countSlots(count: Int): String {
        val suffix = if (count > 1) "s" else ""
        return "$count slot$suffix"
    }

    operator fun <T> List<T>.component1(): T? = getOrNull(0)
    operator fun <T> List<T>.component2(): T? = getOrNull(1)
}

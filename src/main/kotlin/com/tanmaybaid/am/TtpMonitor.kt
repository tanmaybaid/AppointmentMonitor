package com.tanmaybaid.am

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.StreamReadFeature
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
import com.tanmaybaid.am.publisher.LogPublisher
import com.tanmaybaid.am.publisher.Publisher
import com.tanmaybaid.am.publisher.PushoverPublisher
import com.tanmaybaid.am.publisher.WebHookPublisher
import com.tanmaybaid.am.service.TtpService
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class TtpMonitor : CliktCommand() {
    private val client: HttpClient by lazy {
        HttpClient(Apache5) {
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
        .help("[Optional] [Default: 30] Seconds to wait between polling for available slots.")
        .int().restrictTo(10..3600).convert { it.seconds }.default(30.seconds)
    private val backoffPeriod: Duration by option()
        .help("[Optional] [Default: 60] Minutes to wait after identifying available slots.")
        .int().restrictTo(60).convert { it.seconds }.default(1.hours)
    private val before: LocalDateTime by option()
        .help("[Optional] [Publisher called for any available slots] Date in the ISO_LOCAL_DATE_TIME format" +
                " (e.g. 2024-05-01T08:25:30). Publisher will only be invoked if available slot is before this date.")
        .convert { LocalDateTime.parse(it) }.default(LocalDateTime.MAX)
    private val publishTo: List<String> by option()
        .help("[Optional] [Default: Log] Comma separated list of publishers, e.g. $PUBLISH_TO_EXAMPLES")
        .split(",").default(listOf("Log"))

    override fun run() {
        val ttp = TtpService(client)
        val publishers = listOf(
            LogPublisher(),
            PushoverPublisher(client),
            WebHookPublisher(client),
        ).associateBy(Publisher::name)

        runBlocking {
            run(ttp, publishers)
        }
    }

    private suspend fun run(ttp: TtpService, publishers: Map<String, Publisher>) {
        val availableLocations: Map<Int, TtpService.Location> = ttp.getLocations().associateBy { it.id }
        val inputLocationByIds: Map<Int, TtpService.Location> = normalize(locationIds, availableLocations)

        while (!Thread.interrupted()) {
            inputLocationByIds.forEach { (id, location) ->
                val slotAvailability: TtpService.SlotAvailability = ttp.getSlotAvailability(location)
                logger.debug("SlotAvailability retrieved for $id (${location.shortName}): $slotAvailability")

                if (slotAvailability.availableSlots.isEmpty()) {
                    logger.info("No slots found for $id")
                } else {
                    slotAvailability.availableSlots.forEach { availableSlot ->
                        if (availableSlot.active && availableSlot.startTimestamp.isBefore(before)) {
                            val message = "Found a slot at ${location.shortName} ($id) starting at" +
                                    " ${availableSlot.startTimestamp} for ${availableSlot.duration} minutes."
                            publishTo.forEach { publish(publishers, it, message) }

                            delay(backoffPeriod)
                        } else {
                            logger.info("Slot available, but after requested date of $before: $availableSlot.")
                        }
                    }
                }
            }

            logger.info("Sleeping for $pollPeriod before checking again...")
            delay(pollPeriod)
        }

        logger.info("Exiting!")
    }

    private suspend fun publish(publishers: Map<String, Publisher>, publishTo: String, message: String) {
        val request = publishTo.split('=')
        val publisher = publishers[request[0]]
        val publisherRequest = request.getOrElse(1) { "" }

        publisher?.publish(publisherRequest, message)
    }

    private fun normalize(
        inputLocationIds: Collection<Int>,
        availableLocations: Map<Int, TtpService.Location>
    ) : Map<Int, TtpService.Location> {
        val inputLocationByIds: Map<Int, TtpService.Location> =
            availableLocations.filter { inputLocationIds.contains(it.key) }

        if (inputLocationByIds.size != inputLocationIds.size) {
            val invalidIds = inputLocationIds.toSet().filterNot(inputLocationByIds::containsKey)
            throw IllegalArgumentException("Following locationIds are not valid: $invalidIds")
        }

        return inputLocationByIds
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(LogPublisher::class.java.name)
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
    }
}

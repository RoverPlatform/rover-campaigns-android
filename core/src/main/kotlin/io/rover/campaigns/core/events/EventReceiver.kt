package io.rover.campaigns.core.events

import io.rover.campaigns.core.data.domain.Attributes
import io.rover.campaigns.core.events.domain.Event
import io.rover.campaigns.core.logging.log
import io.rover.campaigns.core.streams.subscribe
import io.rover.sdk.data.domain.Block
import io.rover.sdk.data.domain.Experience
import io.rover.sdk.data.domain.Screen
import io.rover.sdk.data.events.RoverEvent
import io.rover.sdk.services.EventEmitter

/**
 * Receive events emitted by the Rover SDK.
 */
open class EventReceiver(
    private val eventEmitter: EventEmitter?,
    private val eventQueueService: EventQueueServiceInterface
) {
    open fun startListening() {
        eventEmitter?.let {
            eventEmitter.trackedEvents.subscribe { event ->
                eventQueueService.trackEvent(transformEvent(event), "rover")
            }
        }
            ?: log.w("A Rover SDK event emitter wasn't available; Rover events will not be tracked.  Make sure you call Rover.initialize() before initializing the Campaigns SDK.")
    }

    private fun transformEvent(event: RoverEvent): Event {
        return when (event) {
            is RoverEvent.BlockTapped -> event.transformToEvent()
            is RoverEvent.ExperienceDismissed -> event.transformToEvent()
            is RoverEvent.ScreenDismissed -> event.transformToEvent()
            is RoverEvent.ExperiencePresented -> event.transformToEvent()
            is RoverEvent.ExperienceViewed -> event.transformToEvent()
            is RoverEvent.ScreenViewed -> event.transformToEvent()
            is RoverEvent.ScreenPresented -> event.transformToEvent()
        }
    }
}

private fun experienceAttributes(experience: Experience, campaignId: String?) = mapOf(
    "id" to experience.id,
    "name" to experience.name,
    "keys" to experience.keys,
    "tags" to experience.tags
) + if(campaignId != null) hashMapOf("campaignID" to campaignId) else hashMapOf()

private fun screenAttributes(screen: Screen) = mapOf(
    "id" to screen.id,
    "name" to screen.name,
    "keys" to screen.keys,
    "tags" to screen.tags
)

private fun blockAttributes(block: Block) = mapOf(
    "id" to block.id,
    "name" to block.name,
    "keys" to block.keys,
    "tags" to block.tags
)

private fun RoverEvent.BlockTapped.transformToEvent(): Event {
    val attributes: Attributes = mapOf(
        "experience" to experienceAttributes(experience, campaignId),
        "screen" to screenAttributes(screen),
        "block" to blockAttributes(block)
    )

    return Event("Block Tapped", attributes)
}

private fun RoverEvent.ExperienceDismissed.transformToEvent(): Event {
    return Event("Experience Dismissed", mapOf("experience" to experienceAttributes(experience, campaignId)))
}

private fun RoverEvent.ScreenDismissed.transformToEvent(): Event {
    val attributes: Attributes = mapOf(
        "experience" to experienceAttributes(experience, campaignId),
        "screen" to screenAttributes(screen)
    )

    return Event("Screen Dismissed", attributes)
}

private fun RoverEvent.ExperiencePresented.transformToEvent(): Event {
    return Event("Experience Presented", mapOf("experience" to experienceAttributes(experience, campaignId)))
}

private fun RoverEvent.ExperienceViewed.transformToEvent(): Event {
    val attributes: Attributes = mapOf(
        "experience" to experienceAttributes(experience, campaignId),
        "duration" to duration
    )

    return Event("Experience Viewed", attributes)
}

private fun RoverEvent.ScreenViewed.transformToEvent(): Event {
    val attributes: Attributes = mapOf(
        "experience" to experienceAttributes(experience, campaignId),
        "screen" to screenAttributes(screen),
        "duration" to duration
    )

    return Event("Screen Viewed", attributes)
}

private fun RoverEvent.ScreenPresented.transformToEvent(): Event {
    val attributes: Attributes = mapOf(
        "experience" to experienceAttributes(experience, campaignId),
        "screen" to screenAttributes(screen)
    )

    return Event("Screen Presented", attributes)
}

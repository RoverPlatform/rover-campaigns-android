package io.rover.campaigns.core.events.domain

import io.rover.campaigns.core.data.domain.Attributes
import java.util.Date
import java.util.UUID

data class Event(
    val name: String,
    val attributes: Attributes,
    val timestamp: Date,
    val id: UUID
) {
    constructor(
        name: String,
        attributes: Attributes
    ): this(name, attributes, Date(), UUID.randomUUID())

    companion object {
        fun screenViewed(screenName: String, contentID: String? = null, contentName: String? = null): Event {
            val attributes = mutableMapOf<String, Any>("screenName" to screenName)

            contentName?.let { attributes.put("contentName", it) }
            contentID?.let { attributes.put("contentID", it) }

            return Event("Screen Viewed", attributes)
        }
    }
}

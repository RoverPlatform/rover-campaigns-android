package io.rover.campaigns.ticketmaster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.core.events.EventQueueServiceInterface
import io.rover.campaigns.core.events.domain.Event

private const val MY_TICKET_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.MYTICKETSCREENSHOWED"
private const val MANAGE_TICKET_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.MANAGETICKETSCREENSHOWED"
private const val ADD_PAYMENT_INFO_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.ADDPAYMENTINFOSCREENSHOWED"
private const val MY_TICKET_BARCODE_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.MYTICKETBARCODESCREENSHOWED"
private const val TICKET_DETAIL_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.TICKETDETAILSSCREENSHOWED"

private const val TICKET_MASTER_NAMESPACE = "ticketmaster"

private const val EVENT_ID = "event_id"
private const val EVENT_NAME = "event_name"
private const val EVENT_DATE = "event_date"
private const val EVENT_IMAGE_URL = "event_image_url"
private const val VENUE_NAME = "venue_name"
private const val VENUE_ID = "venu_id"
private const val VENUE_ID_CORRECT_SPELLING = "venue_id"
private const val CURRENT_TICKET_COUNT = "current_ticket_count"
private const val EVENT_ARTIST_NAME = "artist_name"
private const val EVENT_ARTIST_ID = "artist_id"

private const val MY_TICKET_SCREEN_ROVER_NAME = "My Tickets"
private const val MANAGE_TICKET_SCREEN_ROVER_NAME = "Manage Ticket"
private const val ADD_PAYMENT_INFO_SCREEN_ROVER_NAME = "Add Payment Info"
private const val MY_TICKET_BARCODE_SCREEN_ROVER_NAME = "Ticket Barcode"
private const val TICKET_DETAIL_SCREEN_ROVER_NAME = "Ticket Details"

private const val SCREEN_NAME_KEY = "screenName"

class TicketMasterAnalyticsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.retrieveCorrectTMEventScreenName()?.let { screenName ->
            trackTicketMasterScreenViewedEvent(intent, screenName)
        }
    }

    private fun trackTicketMasterScreenViewedEvent(intent: Intent, screenName: String) {
        val eventQueue = RoverCampaigns.shared?.resolveSingletonOrFail(EventQueueServiceInterface::class.java)

        val attributes = mutableMapOf<String, Any>(SCREEN_NAME_KEY to screenName)

        val eventAttributes = mutableMapOf<String, Any>()
        val venueAttributes = mutableMapOf<String, Any>()
        val artistAttributes = mutableMapOf<String, Any>()

        with(intent) {
            getStringExtra(EVENT_ID)?.let { eventAttributes.put("id", it) }
            getStringExtra(EVENT_NAME)?.let { eventAttributes.put("name", it) }
            getStringExtra(EVENT_DATE)?.let { eventAttributes.put("date", it) }
            getStringExtra(EVENT_IMAGE_URL)?.let { eventAttributes.put("imageURL", it) }


            (getStringExtra(VENUE_ID_CORRECT_SPELLING) ?: getStringExtra(VENUE_ID))?.let {
                venueAttributes.put("id", it)
            }

            getStringExtra(VENUE_NAME)?.let { venueAttributes.put("name", it) }

            getStringExtra(CURRENT_TICKET_COUNT)?.let { attributes.put("currentTicketCount", it) }

            getStringExtra(EVENT_ARTIST_ID)?.let { artistAttributes.put("id", it) }
            getStringExtra(EVENT_ARTIST_NAME)?.let { artistAttributes.put("name", it) }
        }

        if (eventAttributes.isNotEmpty()) attributes.put("event", eventAttributes)
        if (venueAttributes.isNotEmpty()) attributes.put("venue", venueAttributes)
        if (artistAttributes.isNotEmpty()) attributes.put("artist", artistAttributes)

        val event = Event("Screen Viewed", attributes)
        eventQueue?.trackEvent(event, TICKET_MASTER_NAMESPACE)
    }
}

private fun Intent.retrieveCorrectTMEventScreenName(): String? {
    return when (action) {
        MY_TICKET_SCREEN_SHOWED -> MY_TICKET_SCREEN_ROVER_NAME
        MANAGE_TICKET_SCREEN_SHOWED -> MANAGE_TICKET_SCREEN_ROVER_NAME
        ADD_PAYMENT_INFO_SCREEN_SHOWED -> ADD_PAYMENT_INFO_SCREEN_ROVER_NAME
        MY_TICKET_BARCODE_SCREEN_SHOWED -> MY_TICKET_BARCODE_SCREEN_ROVER_NAME
        TICKET_DETAIL_SCREEN_SHOWED -> TICKET_DETAIL_SCREEN_ROVER_NAME
        else -> null
    }
}
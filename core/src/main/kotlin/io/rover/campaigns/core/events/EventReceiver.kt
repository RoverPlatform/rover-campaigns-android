package io.rover.campaigns.core.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import io.rover.campaigns.core.data.domain.AttributeValue
import io.rover.campaigns.core.data.graphql.operations.data.toAttributesHash
import io.rover.campaigns.core.events.domain.Event
import io.rover.campaigns.core.logging.log
import io.rover.campaigns.core.platform.whenNotNull
import org.json.JSONException
import org.json.JSONObject
import java.lang.RuntimeException

/**
 * Receive events emitted as local broadcasts by the Rover SDK.
 */
private const val EXPERIENCE = "experience"
private const val CAMPAIGN_ID = "campaignID"

open class EventReceiver(
    private val broadcastManager: LocalBroadcastManager,
    private val eventQueueService: EventQueueServiceInterface
) {
    open fun processReceivedIntent(intent: Intent) {
        val camelcaseEventName = intent.action.removePrefix("io.rover.")
        val attributesJson: String? = intent.getStringExtra("attributes")

        val attributesJsonbObject = attributesJson.whenNotNull { json ->
            try {
                JSONObject(json).toAttributesHash()
            } catch (jsonException: JSONException) {
                log.w("Received invalid JSON in broadcast intent: $jsonException")
                null
            } catch (runtimeException: RuntimeException) {
                // TODO: remove this handler once the old Attributes system is removed.
                log.w("Received invalid Attributes in JSON in broadcast intent: $runtimeException")
                null
            }
        }

        val attributesObject = attributesJsonbObject
            ?.addCampaignIDToExperienceIfBothPresent()
            ?.removeTopLevelCampaignIDIfPresent() ?: emptyMap()

        val event = Event(camelcaseEventName.humanize(), attributesObject)

        eventQueueService.trackEvent(event, "rover")
    }

    private fun Map<String, AttributeValue>.removeTopLevelCampaignIDIfPresent(): Map<String, AttributeValue> {
        return if (containsKey(CAMPAIGN_ID)) this.toMutableMap().apply { remove(CAMPAIGN_ID) } else this
    }

    private fun Map<String, AttributeValue>.addCampaignIDToExperienceIfBothPresent(): Map<String, AttributeValue> {
        return if (containsKey(EXPERIENCE) && containsKey(CAMPAIGN_ID)) {
            val experienceHash = ((this[EXPERIENCE] as AttributeValue.Object).hash).toMutableMap()

            experienceHash[CAMPAIGN_ID] = getValue(CAMPAIGN_ID)
            toMutableMap().apply { this[EXPERIENCE] = AttributeValue.Object(experienceHash) }
        } else this
    }

    open fun startListening() {
        val receiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action.startsWith("io.rover.")) {
                    processReceivedIntent(intent)
                }
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction("io.rover.ExperiencePresented")
            addAction("io.rover.ExperienceDismissed")
            addAction("io.rover.ExperienceViewed")
            addAction("io.rover.BlockTapped")
            addAction("io.rover.ScreenViewed")
            addAction("io.rover.ScreenDismissed")
            addAction("io.rover.ScreenPresented")
        }

        broadcastManager.registerReceiver(receiver, intentFilter)
        log.v("Now listening for Rover Campaigns events encapsulated in local broadcast intents.")
    }


}

fun String.humanize(): String {
    /// Convert "CamelCase" to "Camel Case".
    return this.fold("") { x, y ->
        if (y.isUpperCase()) {
            "$x $y"
        } else {
            "$x$y"
        }
    }.trim()
}
package io.rover.campaigns.core.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
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

        val event = Event(
            camelcaseEventName.humanize(),
            attributesJsonbObject ?: emptyMap()
        )

        eventQueueService.trackEvent(event, "rover")
    }

    open fun startListening() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if(intent.action.startsWith("io.rover.")) {
                    processReceivedIntent(intent)
                }
            }
        }
        broadcastManager.registerReceiver(receiver, IntentFilter())
        log.v("Now listening for RoverCampaigns events encapsulated in local broadcast intents.")
    }


}

fun String.humanize(): String {
    /// Convert "CamelCase" to "Camel Case".
    return this.fold("") { x, y ->
        if(y.isUpperCase()) {
            "$x $y"
        } else {
            "$x$y"
        }
    }.trim()
}
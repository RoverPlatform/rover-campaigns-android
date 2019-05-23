package io.rover.campaigns.core.routing.routes

import android.content.Context
import android.content.Intent
import io.rover.campaigns.core.logging.log
import io.rover.campaigns.core.platform.asAndroidUri
import io.rover.campaigns.core.platform.parseAsQueryParameters
import io.rover.campaigns.core.routing.Route
import io.rover.sdk.ui.containers.RoverActivity
import java.net.URI
import kotlin.math.exp

class PresentExperienceRoute(
    private val urlSchemes: List<String>,
    private val associatedDomains: List<String>,
    private val presentExperienceIntents: PresentExperienceIntents
) : Route {
    override fun resolveUri(uri: URI?): Intent? {
        // Experiences can be opened either by a deep link or a universal link.
        return when {
            (uri?.scheme == "https" || uri?.scheme == "http") && associatedDomains.contains(uri.host) -> {
                // universal link!
                presentExperienceIntents.displayExperienceIntentFromCampaignLink(uri)
            }
            urlSchemes.contains(uri?.scheme) && uri?.authority == "presentExperience" -> {
                val queryParameters = uri.query.parseAsQueryParameters()
                val possibleCampaignId = queryParameters["campaignID"]

                val experienceId = queryParameters["experienceID"] ?: queryParameters["id"]

                if(experienceId == null) {
                    log.w("A presentExperience deep link lacked either a `campaignID` or `id` parameter.")
                    return null
                }

                presentExperienceIntents.displayExperienceIntentByExperienceId(experienceId, possibleCampaignId)
            }
            else -> null // no match.
        }
    }
}

/**
 * Override this class to configure Rover to open Experiences with a different Activity other than
 * the bundled [RoverActivity].
 */
open class PresentExperienceIntents(
    private val applicationContext: Context
) {
    fun displayExperienceIntentByExperienceId(experienceId: String, possibleCampaignId: String?): Intent {
        return RoverActivity.makeIntent(applicationContext, experienceId = experienceId, campaignId = possibleCampaignId)
    }

    fun displayExperienceIntentFromCampaignLink(universalLink: URI): Intent {
        return RoverActivity.makeIntent(applicationContext, experienceUrl = universalLink.asAndroidUri())
    }
}

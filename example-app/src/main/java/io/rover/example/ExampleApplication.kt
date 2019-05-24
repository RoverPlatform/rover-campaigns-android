package io.rover.example

import android.app.Application
import android.content.Intent
import android.graphics.Color
import com.google.firebase.iid.FirebaseInstanceId
import io.rover.campaigns.core.CoreAssembler
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.debug.DebugAssembler
import io.rover.campaigns.experiences.ExperiencesAssembler
import io.rover.campaigns.location.LocationAssembler
import io.rover.campaigns.notifications.NotificationsAssembler
import io.rover.campaigns.ticketmaster.TicketmasterAssembler
import io.rover.sdk.Rover
import timber.log.Timber

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        RoverCampaigns.installSaneGlobalHttpCache(this)

        // Initialize the Campaigns sdk
        RoverCampaigns.initialize(
            CoreAssembler(
                accountToken = getString(R.string.rover_api_token),
                application = this,
                urlSchemes = listOf(getString(R.string.rover_campaigns_uri_scheme)),
                associatedDomains = listOf(getString(R.string.rover_campaigns_associated_domain))
            ),
            NotificationsAssembler(
                applicationContext = this,
                smallIconResId = R.mipmap.rover_notification_icon,
                notificationCenterIntent = Intent(applicationContext, ExampleMainActivity::class.java)
            ) { tokenCallback ->
                FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                    tokenCallback(task.result?.token)
                }
            },
            LocationAssembler(),
            DebugAssembler(),
            TicketmasterAssembler(),
            ExperiencesAssembler()
        )
    }
}

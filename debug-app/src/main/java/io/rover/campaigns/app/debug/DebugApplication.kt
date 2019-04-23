package io.rover.campaigns.app.debug

import android.app.Application
import android.content.Intent
import android.graphics.Color
import com.google.firebase.iid.FirebaseInstanceId
import io.rover.campaigns.core.CoreAssembler
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.debug.DebugAssembler
import io.rover.campaigns.location.LocationAssembler
import io.rover.campaigns.notifications.NotificationsAssembler
import io.rover.campaigns.ticketmaster.TicketmasterAssembler
import io.rover.sdk.Rover
import timber.log.Timber


class DebugApplication : Application() {
    private val roverBaseUrl by lazy { resources.getString(R.string.rover_endpoint) }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Rover.initialize(
            this,
            getString(R.string.rover_api_token),
            Color.BLUE
        )

        RoverCampaigns.installSaneGlobalHttpCache(this)

        RoverCampaigns.initialize(
            CoreAssembler(
                accountToken = getString(R.string.rover_api_token),
                application = this,
                urlSchemes = listOf("rv-rover-labs-inc"),
                endpoint = "$roverBaseUrl/graphql"
            ),
            NotificationsAssembler(
                applicationContext = this,
                smallIconResId = R.mipmap.rover_notification_icon,
                notificationCenterIntent = Intent(applicationContext, DebugMainActivity::class.java)
            ) { tokenCallback ->
                FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                    tokenCallback(task.result?.token)
                }
            },
            LocationAssembler(),
            DebugAssembler(),
            TicketmasterAssembler()
        )
    }


}

package io.rover.campaigns.app.debug.fcm

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.notifications.PushReceiverInterface

class FirebaseInstanceIdReceiver : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        RoverCampaigns.shared?.resolve(PushReceiverInterface::class.java)?.onTokenRefresh(
            FirebaseInstanceId.getInstance().token
        )
    }
}

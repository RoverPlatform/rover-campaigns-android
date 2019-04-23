package io.rover.campaigns.campaigns.app.debug.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.notifications.PushReceiverInterface

class FirebaseMessageReceiver : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        RoverCampaigns.shared?.resolve(PushReceiverInterface::class.java)?.onMessageReceivedData(
            remoteMessage.data
        )
    }
}

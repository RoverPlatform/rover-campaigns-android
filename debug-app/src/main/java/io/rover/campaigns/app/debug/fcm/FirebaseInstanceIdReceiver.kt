package io.rover.campaigns.campaigns.app.debug.fcm

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import io.rover.campaigns.core.Rover
import io.rover.campaigns.notifications.PushReceiverInterface

class FirebaseInstanceIdReceiver : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        Rover.shared?.resolve(PushReceiverInterface::class.java)?.onTokenRefresh(
            FirebaseInstanceId.getInstance().token
        )
    }
}

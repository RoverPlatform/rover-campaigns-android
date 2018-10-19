package io.rover.core.events.contextproviders

import android.os.Handler
import io.rover.core.data.domain.DeviceContext
import io.rover.core.events.ContextProvider
import io.rover.core.events.PushTokenTransmissionChannel
import io.rover.core.logging.log
import io.rover.core.platform.LocalStorage
import io.rover.core.platform.whenNotNull
import java.util.Date
import java.util.concurrent.Executors

/**
 * Captures and adds the Firebase push token to [DeviceContext].  As a [PushTokenTransmissionChannel], it
 * expects to be informed of any changes to the push token.
 */
class FirebasePushTokenContextProvider(
    localStorage: LocalStorage,
    private val resetPushToken: () -> Unit
) : ContextProvider, PushTokenTransmissionChannel {
    override fun captureContext(deviceContext: DeviceContext): DeviceContext {
        return deviceContext.copy(pushToken = token.whenNotNull {
            DeviceContext.PushToken(
                it,
                timestampAsNeeded()
            )
        })
    }

    override fun setPushToken(token: String?) {
        if (this.token != token) {
            this.token = token
            this.timestamp = null
            timestampAsNeeded()
            val elapsed = (Date().time - launchTime.time) / 1000
            log.v("Push token set after $elapsed seconds.")
        }
    }

    private val launchTime = Date()
    private val keyValueStorage = localStorage.getKeyValueStorageFor(Companion.STORAGE_CONTEXT_IDENTIFIER)

    private var token: String?
        get() = keyValueStorage[TOKEN_KEY]
        set(token) { keyValueStorage[TOKEN_KEY] = token }

    private var timestamp: String?
        get() = keyValueStorage[TIMESTAMP_KEY]
        set(token) { keyValueStorage[TIMESTAMP_KEY] = token }

    private fun timestampAsNeeded(): Date {
        // retrieves the current timestamp value, setting it to now if it's missing (say, if running
        // on an early 2.0 beta install where timestamp was not set).

        if(timestamp == null) {
            timestamp = (System.currentTimeMillis() / 1000L).toString()
        }

        return Date(timestamp!!.toLong() * 1000)
    }


    init {
        if (token == null) {
            log.e("No push token is set yet.")
            Handler().postDelayed({
                if (token == null) {
                    // token still null? then attempt a reset. This case can happen if the FCM token
                    // was already set and received before the Rover SDK 2.x was integrated, meaning
                    // that FCM believes that the app knows what the push token is, but at least the
                    // Rover SDK itself does not.

                    log.w("Push token is still not set. Perhaps token was received before Rover SDK was integrated. Forcing reset.")
                    Executors.newSingleThreadExecutor().execute {
                        resetPushToken()
                    }
                }
            }, TOKEN_RESET_TIMEOUT)
        }
    }

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "io.rover.rover.fcm-push-context-provider"
        private const val TOKEN_KEY = "push-token"
        private const val TIMESTAMP_KEY = "timestamp"
        private const val TOKEN_RESET_TIMEOUT = 4 * 1000L
    }
}
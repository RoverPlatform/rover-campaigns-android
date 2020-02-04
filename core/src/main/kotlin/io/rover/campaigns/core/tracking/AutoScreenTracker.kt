package io.rover.campaigns.core.tracking

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.core.events.EventQueueServiceInterface
import io.rover.campaigns.core.logging.log
import java.lang.Exception

private const val TM_PACKAGE_PREFIX = "com.ticketmaster"
private const val ROVER_PACKAGE_PREFIX = "io.rover"

private const val TRACKING_LABEL_KEY = "rvAutoTrackingLabelKey"
private const val ACTIVITY_EXCLUDE_FROM_TRACKING = "rvAutoTrackingExcludeActivity"

internal class AutoScreenTracker(private val activityAutoTrackingEnabled: Boolean) : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity?) {}

    override fun onActivityResumed(activity: Activity?) {
        activity?.let { activity ->
            try {
                val activityInfo = activity.packageManager?.getActivityInfo(activity.componentName, PackageManager.GET_META_DATA)
                val activityMetaData = activityInfo?.metaData

                val activityNotExcludedByUserFromTracking = activityMetaData?.getBoolean(ACTIVITY_EXCLUDE_FROM_TRACKING) != true
                val activityNotExcludedByRoverFromTracking = activity.packageName?.startsWith(TM_PACKAGE_PREFIX) != true && (activity.packageName?.startsWith(ROVER_PACKAGE_PREFIX) != true)

                if (activityAutoTrackingEnabled && activityNotExcludedByUserFromTracking && activityNotExcludedByRoverFromTracking) {
                    val label = activityMetaData?.getString(TRACKING_LABEL_KEY) ?: activityInfo?.loadLabel(activity.packageManager).toString()

                    val trackableContentScreen = (activity as? TrackableContentScreen)

                    RoverCampaigns.shared?.resolveSingletonOrFail(EventQueueServiceInterface::class.java)?.trackScreenViewed(label, trackableContentScreen?.contentID, trackableContentScreen?.contentName)
                }
            } catch (e: Exception) {
                log.w("Failed to track screen. ${e.message}")
            }
        }
    }

    override fun onActivityStarted(activity: Activity?) {}
    override fun onActivityDestroyed(activity: Activity?) {}
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
    override fun onActivityStopped(activity: Activity?) {}
    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
}

interface TrackableContentScreen {
    var contentName: String?
    var contentID: String?
}

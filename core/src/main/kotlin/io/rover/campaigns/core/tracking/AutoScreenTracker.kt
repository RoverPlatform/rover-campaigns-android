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

const val TRACKING_LABEL_KEY = "rvAutoTrackingLabelKey"
const val AUTO_TRACKING_ENABLED_KEY = "rvAutoTrackingEnabled"
const val ACTIVITY_EXCLUDE_FROM_TRACKING = "rvAutoTrackingExcludeActivity"

internal class AutoScreenTracker : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity?) {}

    override fun onActivityResumed(activity: Activity?) {
        activity?.let {
            try {
                val activityInfo = it.packageManager?.getActivityInfo(it.componentName, PackageManager.GET_META_DATA)
                val activityMetaData = activityInfo?.metaData

                val applicationInfo = it.packageManager?.getApplicationInfo(it.packageName, PackageManager.GET_META_DATA)
                val applicationMetadata = applicationInfo?.metaData

                val autoTrackingEnabled = applicationMetadata?.getBoolean(AUTO_TRACKING_ENABLED_KEY) == true
                val activityNotExcludedByUserFromTracking = activityMetaData?.getBoolean(ACTIVITY_EXCLUDE_FROM_TRACKING) != true
                val activityNotExcludedByRoverFromTracking = it.packageName?.startsWith(TM_PACKAGE_PREFIX) != true && (it.packageName?.startsWith(ROVER_PACKAGE_PREFIX) != true)

                if (autoTrackingEnabled && activityNotExcludedByUserFromTracking && activityNotExcludedByRoverFromTracking) {
                    val label = activityMetaData?.getString(TRACKING_LABEL_KEY) ?: activityInfo?.loadLabel(it.packageManager).toString()
                    RoverCampaigns.shared?.resolveSingletonOrFail(EventQueueServiceInterface::class.java)?.trackScreenViewed(label)
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

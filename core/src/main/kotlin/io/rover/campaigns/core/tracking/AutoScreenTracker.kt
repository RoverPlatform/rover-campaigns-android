package io.rover.campaigns.core.tracking

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.core.events.EventQueueServiceInterface
import io.rover.campaigns.core.events.domain.Event
import java.lang.Exception

private const val TM_PACKAGE_PREFIX = "com.ticketmaster"
private const val ROVER_PACKAGE_PREFIX = "io.rover"
const val TRACKING_LABEL_KEY = "rvAutoTrackingLabelKey"
const val TRACKING_KEY = "rvAutoTrackingEnabled"
const val ACTIVITY_EXCLUDE_KEY = "rvAutoTrackingExcludeActivity"

internal class AutoScreenTracker : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity?) {}

    override fun onActivityResumed(activity: Activity?) {
        if (activity?.packageName?.startsWith(TM_PACKAGE_PREFIX) != true
            && (activity?.packageName?.startsWith(ROVER_PACKAGE_PREFIX) != true)
            && activity != null) {
            try {
                val activityInfo = activity.packageManager?.getActivityInfo(activity.componentName, PackageManager.GET_META_DATA)
                val activityMetaData = activityInfo?.metaData

                val applicationInfo = activity.application.packageManager?.getApplicationInfo(activity.application.packageName, PackageManager.GET_META_DATA)
                val applicationMetadata = applicationInfo?.metaData

                if (applicationMetadata?.getBoolean(TRACKING_KEY) == true && activityMetaData?.getBoolean(ACTIVITY_EXCLUDE_KEY) != true) {
                    val label = activityMetaData?.getString(TRACKING_LABEL_KEY) ?: activityInfo?.loadLabel(activity.packageManager).toString()
                    RoverCampaigns.shared?.resolveSingletonOrFail(EventQueueServiceInterface::class.java)?.trackEvent(
                        Event.screenViewed(label))
                }
            } catch (e: PackageManager.NameNotFoundException) {

            } catch (e: Exception) {

            }
        }
    }

    override fun onActivityStarted(activity: Activity?) {}
    override fun onActivityDestroyed(activity: Activity?) {}
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
    override fun onActivityStopped(activity: Activity?) {}
    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
}

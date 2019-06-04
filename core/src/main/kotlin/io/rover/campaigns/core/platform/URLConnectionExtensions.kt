package io.rover.campaigns.core.platform

import android.content.pm.PackageInfo
import io.rover.campaigns.core.BuildConfig
import java.net.URLConnection

internal fun URLConnection.setRoverUserAgent(packageInfo: PackageInfo) {
    // get the version number of the app we're embedded into (thus can't use BuildConfig for that)
    val appDescriptor = "${packageInfo.packageName}/${packageInfo.versionName}"
    val roverDescriptor = "RoverCampaignsSDK/${BuildConfig.VERSION_NAME}"
    setRequestProperty("User-Agent", "${ System.getProperty("http.agent")} $appDescriptor $roverDescriptor")
}

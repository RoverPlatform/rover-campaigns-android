package io.rover.campaigns.app.sample

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.core.permissions.PermissionsNotifierInterface
import io.rover.sdk.ui.containers.RoverActivity
import kotlinx.android.synthetic.main.activity_debug_main.navigation
import kotlinx.android.synthetic.main.activity_debug_main.notification_center
import kotlinx.android.synthetic.main.activity_debug_main.settings_fragment

class SampleMainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        selectTab(item.itemId)
        return@OnNavigationItemSelectedListener true
    }

    private fun selectTab(itemId: Int) {
        if(itemId == R.id.navigation_settings) {
            // show
            supportFragmentManager.beginTransaction().show(this.settings_fragment).commit()
        } else {
            // hide
            supportFragmentManager.beginTransaction().hide(this.settings_fragment).commit()
        }
        this.notification_center.visibility = if(itemId == R.id.navigation_notifications) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_debug_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        notification_center.activity = this

        selectTab(R.id.navigation_notifications)

        val uri : Uri? = intent.data
        // Tries to retrieve experienceId from last path segment
        val possibleExperienceId = uri?.getQueryParameter("experienceID")
        val possibleCampaignId = uri?.getQueryParameter("campaignID")


        // A simple routing example follows:
        // Your app can handle the intent data as it prefers - here, we're handling a simple deep
        // link scheme and a universal link domain as defined in the manifest.
        if (uri?.scheme == getString(R.string.uri_scheme) && uri?.host == "presentExperience" && possibleExperienceId != null) {
            startActivity(RoverActivity.makeIntent(packageContext = this, experienceId = possibleExperienceId, campaignId = possibleCampaignId))
        } else if(uri?.scheme in listOf("http", "https") && uri != null && uri.host == getString(R.string.associated_domain)) {
            startActivity(RoverActivity.makeIntent(packageContext = this, experienceUrl = uri, campaignId = possibleCampaignId))
        } else {
            // no matching deep or universal link, just do default "main screen" behaviour.
            makePermissionsAttempt()
        }
    }

    private fun makePermissionsAttempt() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                AlertDialog.Builder(this)
                    .setMessage("Debug App would like to use your location to discover Geofences and Beacons.")
                    .setNeutralButton("Got it") { _, _ ->
                        makePermissionsAttempt()
                    }
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    0
                )
            }
        } else {
            // Permission has already been granted
            RoverCampaigns.shared?.resolveSingletonOrFail(PermissionsNotifierInterface::class.java)?.permissionGranted(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val perms = permissions.zip(grantResults.toList()).associate { it }

        if(perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED) {
            RoverCampaigns.shared?.resolveSingletonOrFail(PermissionsNotifierInterface::class.java)?.permissionGranted(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
}

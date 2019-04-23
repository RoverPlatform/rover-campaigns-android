package io.rover.campaigns.app.debug

import android.Manifest
import android.content.Intent
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
import io.rover.campaigns.core.routing.LinkOpenInterface
import io.rover.campaigns.core.routing.Router
import io.rover.campaigns.core.routing.RouterService
import io.rover.campaigns.core.ui.LinkOpen
import io.rover.sdk.ui.containers.RoverActivity
import kotlinx.android.synthetic.main.activity_debug_main.navigation
import kotlinx.android.synthetic.main.activity_debug_main.notification_center
import kotlinx.android.synthetic.main.activity_debug_main.settings_fragment

class DebugMainActivity : AppCompatActivity() {

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

//        uri?.let { it.host == "presentExperience" && it.scheme == "rv-rover-labs-inc" }?.

        //

        // for Rover experience deep & universal links:
        val experienceId = intent?.data?.let { intentUri ->
            if(intentUri.host == "presentExperience" && intentUri.scheme == "rv-rover-labs-inc") {
                intentUri.getQueryParameter("id")
            } else RoverCampaigns.shared!!.resolveSingletonOrFail(Router::class.java).route(intentUri)
        }

        // ANDREW & SAM: start here and restore TransientLinkLaunchActivity.  Find it in `master` branch of `rover-android`. And then integrate it into debug-app just like it was done historically with SDK 2.x, so carry on with integration in: https://developer.rover.io/v2/android/deep-universal-links/

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

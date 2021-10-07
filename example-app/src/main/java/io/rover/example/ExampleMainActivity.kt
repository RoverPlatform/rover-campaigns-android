package io.rover.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.fragment.app.FragmentContainerView
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.core.permissions.PermissionsNotifierInterface
import io.rover.example.databinding.ActivityExampleMainBinding

class ExampleMainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        selectTab(item.itemId)
        return@OnNavigationItemSelectedListener true
    }

    private fun selectTab(itemId: Int) {
        val fragment = this.findViewById<FragmentContainerView>(R.id.settings_fragment)
        if(itemId == R.id.navigation_settings) {
            // show

            supportFragmentManager.beginTransaction().show(fragment.).commit()
        } else {
            // hide
            supportFragmentManager.beginTransaction().hide(fragment).commit()
        }
        binding.notificationCenter.visibility = if(itemId == R.id.navigation_notifications) View.VISIBLE else View.GONE
    }

    private lateinit var binding: ActivityExampleMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example_main)

        binding = ActivityExampleMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        binding.notificationCenter.activity = this

        selectTab(R.id.navigation_notifications)

        makePermissionsAttempt()
    }

    // Request fine location permission for Location module functionality
    private fun makePermissionsAttempt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Does Android want us to show an explanation of why the permission is needed first?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                showLocationExplanationDialog()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        } else {
            // Permission has already been granted
            RoverCampaigns.shared?.resolveSingletonOrFail(PermissionsNotifierInterface::class.java)?.permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
            makeBackgroundLocationPermissionAttempt()
        }
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), 0)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }
    }

    private fun makeBackgroundLocationPermissionAttempt() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val perms = permissions.zip(grantResults.toList()).associate { it }

        if(perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED) {
            RoverCampaigns.shared?.resolveSingletonOrFail(PermissionsNotifierInterface::class.java)?.permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) makeBackgroundLocationPermissionAttempt()
        }

        if(perms[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == PackageManager.PERMISSION_GRANTED) {
            RoverCampaigns.shared?.resolveSingletonOrFail(PermissionsNotifierInterface::class.java)?.permissionGranted(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    private fun showLocationExplanationDialog() {
        AlertDialog.Builder(this)
            .setMessage("Debug App would like to use your location to discover Geofences and Beacons.")
            .setNeutralButton("Got it") { _, _ ->
                makePermissionsAttempt()
            }
    }
    private fun showBackgroundLocationExplanationDialog() {
        AlertDialog.Builder(this)
            .setMessage("Debug App would like to use your location in the background to discover Geofences and Beacons.")
            .setNeutralButton("Got it") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 0)
            }
    }
}

package io.rover.Example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.rover.Example.ui.theme.RoverCampaignsAndroidExampleTheme
import io.rover.campaigns.debug.RoverDebugActivity
import io.rover.campaigns.notifications.ui.containers.NotificationCenterActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoverCampaignsAndroidExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Dp(8.0f)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TopAppBar(title = { Text(text = "Rover Campaigns Example App") })

                        Button(onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    RoverDebugActivity::class.java
                                )
                            )
                        }) {
                            Text(text = "Open Rover Settings")
                        }
                        Button(onClick = {
                            startActivity(
                                NotificationCenterActivity.makeIntent(this@MainActivity)
                            )
                        }) {
                            Text(text = "Open Rover Notification Center")
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RoverCampaignsAndroidExampleTheme {
        Greeting("Android")
    }
}
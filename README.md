# Rover Campaigns Android SDK

The standlone Rover Campaigns SDK has been deprecated and merged with the Rover Experiences SDK.  In order to upgrade, please use the Rover SDK available on [GitHub](https://github.com/RoverPlatform/rover-android).

---

## Transition to the Rover SDK

In your application-level 'build.gradle' file, remove the existing dependencies on the previous version of Rover Campaigns.  Then, replace it with new the Rover SDK, adding the dependencies as follows:

```groovy
dependencies {
    // ...
    implementation "io.rover.sdk:core:4.0.0"
    implementation "io.rover.sdk:notifications:4.0.0"
    implementation "io.rover.sdk:location:4.0.0"
    implementation "io.rover.sdk:debug:4.0.0"
    implementation "io.rover.sdk:experiences:4.0.0"
}
```

The new version of the Rover SDK requries the following changes to your codebase:

* All packages have been changed from io.rover.campaigns.* to io.rover.sdk.*
* The Rover Campaigns singleton has been renamed to Rover from RoverCampaigns
* The Rover experiences singleton renamed to RoverExperiences from Rover

Please continue onwards from [GitHub](https://github.com/RoverPlatform/rover-android).


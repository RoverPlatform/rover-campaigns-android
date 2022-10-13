plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

val roverCampaignsVersion: String by rootProject.extra
val spekVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        buildConfigField("String", "ROVER_CAMPAIGNS_VERSION", "\"$roverCampaignsVersion\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }

    sourceSets.getByName("main") {
        java.srcDir("src/main/java")
        java.srcDir("src/main/kotlin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    namespace = "io.rover.campaigns.core"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.2.0")
    // Until java.util.concurrent.Flow appears in Android SDK, import:
    api("org.reactivestreams:reactive-streams:1.0.2")

    implementation("androidx.work:work-runtime-ktx:2.2.0")

    implementation("androidx.browser:browser:1.2.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    testImplementation("junit:junit:4.12")

    // spek
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testImplementation("org.spekframework.spek2:spek-runner-junit5:$spekVersion")

    testImplementation("com.natpryce:hamkrest:1.6.0.0")
    testImplementation("org.amshove.kluent:kluent:1.30")
    testImplementation("org.amshove.kluent:kluent-android:1.30")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")

    // JUnit 5 support.  Needed for Spek.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "io.rover.campaigns"
                artifactId = "core"
                version = roverCampaignsVersion

                pom {
                    name.set("Rover Campaigns SDK Core Module")
                    description.set("From the Rover Campaigns Android SDK")
                    url.set("https://github.com/roverplatform/rover-campaigns-android")
                    licenses {
                        license {
                            name.set("Apache 2.0 License")
                            url.set("https://github.com/RoverPlatform/rover-campaigns-android/blob/master/LICENSE")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                url = uri(
                        System.getenv("DEPLOY_MAVEN_PATH")
                                ?: layout.projectDirectory.dir("maven")
                )
            }
        }
    }
}






//
//apply plugin: "com.android.library"
//apply plugin: "kotlin-android"
//apply plugin: "maven-publish"
//apply plugin: "de.mannodermaus.android-junit5"
//
//android {
//    compileSdkVersion 33
//
//    defaultConfig {
//        minSdkVersion 21
//        targetSdkVersion 33
//
//        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
//
//        // While this used to be default, now explicitly add VERSION_NAME to BuildConfig.
//        buildConfigField("String","VERSION_NAME","\"${rootProject.ext.rover_campaigns_sdk_version}\"")
//
//    }
//    buildTypes {
//        release {
//            minifyEnabled false
//            proguardFiles getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"
//        }
//        // debug {
//        //     minifyEnabled true
//        // }
//    }
//
//    // Add Kotlin source directory to all source sets
//    sourceSets.each {
//        it.java.srcDirs += "src/$it.name/kotlin"
//    }
//
//    testOptions {
//        junitPlatform {
//            filters {
//                engines {
//                    include "spek2"
//                }
//            }
//        }
//    }
//
//    kotlinOptions { jvmTarget = "1.8" }
//    namespace "io.rover.campaigns.core"
//}
//
//dependencies {
//    implementation fileTree(dir: "libs", include: ["*.jar"])
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0", {
//        exclude group: "com.android.support", module: "support-annotations"
//    })
//
//    implementation "androidx.appcompat:appcompat:1.2.0"
//    // Until java.util.concurrent.Flow appears in Android SDK, import:
//    api "org.reactivestreams:reactive-streams:1.0.2"
//
//    implementation "androidx.work:work-runtime-ktx:2.2.0"
//
//    implementation "androidx.browser:browser:1.2.0"
//    implementation "androidx.lifecycle:lifecycle-extensions:2.2.0"
//
//    testImplementation "junit:junit:4.12"
//
//    // spek
//    testImplementation "org.spekframework.spek2:spek-dsl-jvm:$spek_version"
//    testImplementation "org.spekframework.spek2:spek-runner-junit5:$spek_version"
//
//    testImplementation "com.natpryce:hamkrest:1.6.0.0"
//    testImplementation "org.amshove.kluent:kluent:1.30"
//    testImplementation "org.amshove.kluent:kluent-android:1.30"
//    testImplementation "org.skyscreamer:jsonassert:1.5.0"
//
//    // JUnit 5 support.  Needed for Spek.
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.2")
//
//    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
//    implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
//}
//
//ext {
//    groupId = "io.rover.campaigns" // package name of the project
//    artifactId = "core" // module name of the library
//    libVersion = rover_campaigns_sdk_version
//}
//
//afterEvaluate {
//    publishing {
//        repositories {
//            maven {
//                url System.getenv("DEPLOY_MAVEN_PATH")
//            }
//        }
//
//        publications {
//            production(MavenPublication) {
//                from components.release
//
//                groupId "io.rover.campaigns"
//
//                version rover_campaigns_sdk_version
//            }
//        }
//    }
//}
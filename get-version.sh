#!/bin/bash

# Interrogate the SDK for its version number.

# usage: ./get-version.sh

# Run the gradle target that prints out the version number and grep the value out.
./gradlew printVersionNumber | grep ROVER_CAMPAIGNS_VERSION | cut -d '=' -f 2

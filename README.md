# AppointmentMonitor
Script to monitor appointment slot availabilities for Trusted Traveler Programs like NEXUS

# Following are the options to execute the script
## Prerequisites
Make sure to build the package using `./gradlew build` (or clean build using `./gradlew clean build`)
## Option 1: 
`build/install/AppointmentMonitor/bin/AppointmentMonitor ttp-monitor --location-ids 5500`

## Option 2:
`./gradlew run --args="ttp-monitor --location-ids 5500"`

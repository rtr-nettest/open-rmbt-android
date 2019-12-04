#!/usr/bin/env bash
set -e
set -x
typeset err_code
adb -e emu kill
chmod a+x ./set_network_parameters.exp
chmod a+x ./restartEmulator.sh
emulator -avd $1 -no-audio -no-boot-anim -no-snapshot-load -no-snapshot-save &
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed | tr -d '\r') ]]; do sleep 10; done; echo EmulatorBooted'
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
./set_network_parameters.exp
adb shell svc data disable
adb shell  "su 0 svc wifi disable"
./gradlew --no-daemon installLocalDevDebug
./gradlew --no-daemon installLocalDevDebugAndroidTest
adb shell am instrument -w -e package at.rtr.rmbt.android.noConnection com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
adb shell svc data enable
adb shell settings put secure location_providers_allowed -gps
./restartEmulator.sh $1
adb shell am instrument -w -e package at.rtr.rmbt.android.locationDisabled com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
./restartEmulator.sh $1
adb shell am instrument -w -e package at.rtr.rmbt.android.mobileData com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
adb shell svc data disable
adb shell "su 0 svc wifi enable"
adb shell settings put secure location_providers_allowed +gps
./restartEmulator.sh $1
adb shell am instrument -w -e package at.rtr.rmbt.android.locationEnabled com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
./restartEmulator.sh $1
adb shell am instrument -w -e package at.rtr.rmbt.android.wifi com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
err_code=$?
adb -e emu kill
exit ${err_code}
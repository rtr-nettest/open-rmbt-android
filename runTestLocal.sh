
emulator -avd myEmulator -no-audio -no-boot-anim -skin 480x800 -no-snapshot-load -no-snapshot-save &
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed | tr -d '\r') ]]; do sleep 10; done; echo EmulatorBooted'
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell svc data disable
adb shell svc wifi disable
adb shell settings put secure location_providers_allowed -gps
adb shell settings put secure location_providers_allowed -network
#(echo -e "auth 2rgr8PUXTKcOMr/U";echo -e "gsm signal-profile 2"; echo -e "exit" ) | telnet localhost 5554
./gradlew.bat installLocalDevDebug
./gradlew.bat installLocalDevDebugAndroidTest
adb shell am instrument -w -e package at.rtr.rmbt.android.noConnection com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e package at.rtr.rmbt.android.locationDisabled com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
adb shell settings put secure location_providers_allowed +gps
adb shell settings put secure location_providers_allowed +network
adb shell svc data enable
sleep 2
adb shell am instrument -w -e package at.rtr.rmbt.android.mobileData com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
adb shell svc data disable
adb shell svc wifi enable
sleep 2
adb shell am instrument -w -e package at.rtr.rmbt.android.locationEnabled com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e package at.rtr.rmbt.android.wifi com.specure.nettest.local.dev.test/androidx.test.runner.AndroidJUnitRunner
#adb -e emu kill

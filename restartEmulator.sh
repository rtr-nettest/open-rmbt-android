adb -e emu kill
sleep 3
emulator -avd $1 -no-audio -no-boot-anim -no-snapshot-load -no-snapshot-save &
sleep 3
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed | tr -d '\r') ]]; do sleep 10; done; echo EmulatorBooted'
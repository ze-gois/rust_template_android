adb logcat -c
adb shell am force-stop com.zegois.demo
adb shell am start -n com.zegois.demo/.MainActivity
adb logcat -d | grep ZEGOIS

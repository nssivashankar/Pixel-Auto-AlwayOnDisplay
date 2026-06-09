# Pixel Auto Always on Display!!!

Pixel Auto Always On Display automatically manages Always-On Display on Google Pixel devices. Enable AOD while charging, react to important notifications, and automate display behavior without manually changing system settings.

## Permissions

The app requires the ```WRITE_SECURE_SETTINGS``` permission to be able to toggle AoD on and off.\
You can grant it with [Shizuku](https://shizuku.rikka.app/) or adb:

```bash
adb shell pm grant org.alberto97.aodtoggle android.permission.WRITE_SECURE_SETTINGS
```
or in case of multiple users (`<userId>` is listed as first number in output of `adb shell pm list users`):
```bash
adb shell pm grant --user <userId> org.alberto97.aodtoggle android.permission.WRITE_SECURE_SETTINGS
```

Please note that this app has only been tested on Google Pixels.\
Different manufacturers may have implemented AoD their own way and therefore the app might not work.

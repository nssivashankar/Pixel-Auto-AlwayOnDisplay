# Walkthrough - Decoupled Charging Info from AOD Trigger

I have modified the AOD logic to ensure that the charging notification does not force the display on. This is a local build for testing and has not been pushed to Git.

## Changes Made

### AOD Automation Service
#### [NotificationAodService.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/NotificationAodService.kt)
- **Decoupled Notification Trigger:** I removed the `charging_info_notif` setting from the AOD trigger condition.
- Now, turning on "Charging Info Notification" will show the stats in your notification shade, but it will **not** automatically turn on the AOD when you plug in the phone.
- The AOD will now strictly follow the **Charging Mode** toggle for its power-on behavior.

## Verification Results

### Automated Tests
- [x] Successfully compiled the project.
- [x] Verified that `:app:assembleDebug` produced a new APK.

### Manual Verification Steps
1. **Disable Charging Mode:** Go to app settings and turn OFF "Charging Mode".
2. **Enable Charging Info:** Turn ON "Charging Info Notification".
3. **Plug in:** Connect your charger.
4. **Result:** Verify that the notification appears, but the AOD **stays off**.

> [!TIP]
> You can find the new debug APK here:
> `app/build/outputs/apk/debug/app-debug.apk`

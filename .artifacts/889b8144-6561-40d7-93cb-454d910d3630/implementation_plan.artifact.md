# Implementation Plan - Decouple Charging AOD from Notification

I will modify the AOD logic to ensure that "Charging Mode" is the sole trigger for turning on the Always-On Display when plugged in. This allows users to keep the "Charging Info Notification" visible in their status bar/lockscreen without forcing the AOD to stay on automatically.

## User Review Required

> [!NOTE]
> **Behavioral Change:** If you turn OFF "Charging Mode" but keep "Charging Info Notification" ON, the AOD will NOT turn on automatically when you plug in. However, the charging stats will still be visible if the AOD turns on for other reasons (like a new notification).

## Proposed Changes

### AOD Automation Service

#### [MODIFY] [NotificationAodService.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/NotificationAodService.kt)
- Update `updateAodState()`: Remove `chargingInfoMode` from the `chargingTrigger` calculation. This ensures that the notification's existence doesn't force the AOD state.
- The `updateChargingNotification()` method will remain unchanged, as it correctly manages the visibility of the notification based on the `charging_info_notif` setting.

## Verification Plan

### Automated Tests
- Build the app with `:app:assembleDebug`.

### Manual Verification
1. **Notification Only:** Turn OFF "Charging Mode" and turn ON "Charging Info Notification". Plug in the phone. Verify the notification appears in the status bar, but the AOD stays OFF.
2. **AOD Trigger:** Turn ON "Charging Mode". Plug in. Verify the AOD turns ON.
3. **Combined:** Verify that if AOD is ON due to "Charging Mode", the charging info is visible on it.

---

**I will focus on local debugging and building a debug APK for you to test before pushing any changes to Git.**

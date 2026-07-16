# Implementation Plan - Dynamic Battery Completion Notification

We will fix the bug where the battery completion notification hardcodes "80%" even when a custom limit (e.g., 85%) is set.

## User Review Required

> [!NOTE]
> I will make the notification title and text dynamic based on the current custom limit.
> I will also update the onboarding description to be more accurate regarding custom limits.

## Proposed Changes

### Notification Logic

#### [MODIFY] [NotificationAodService.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/NotificationAodService.kt)
- Update `checkBatteryCompletion` signature to accept `customLimitEnabled` and `customTarget`.
- Replace hardcoded `"80%"` strings with dynamic values using the active target (80 or custom).
- Update the trigger condition to correctly detect when the custom target is reached.
- Pass the necessary parameters from `onReceive`'s `ACTION_BATTERY_CHANGED` block.

### Onboarding UI

#### [MODIFY] [SetupScreen.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/ui/screens/SetupScreen.kt)
- Update the `PostNotificationsPage` description to say "custom limit (e.g., 80%)" instead of just "80%".

## Verification Plan

### Manual Verification
1. Set a custom limit of 85% in the app.
2. Charge the device.
3. Verify that when 85% is reached, the notification says "**85% Charging Complete**" and "**Battery has reached 85% limit**".
4. Verify that clicking "Full Charge" correctly resumes charging to 100%.

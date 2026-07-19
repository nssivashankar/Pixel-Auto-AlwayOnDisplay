# Walkthrough - Smart Wattage Guard and Optimization Fix

I have implemented the "Wattage Guard" logic to turn off the AOD during prolonged charging pauses (0W) and decoupled the "Custom Limit" from the system's "Adaptive Charging" to prevent conflicts.

## Changes Made

### AOD Automation Service
#### [NotificationAodService.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/NotificationAodService.kt)
- Added a **Wattage Guard** that monitors active power draw.
- If the phone is plugged in but the wattage stays below **0.5W for more than 10 minutes**, the AOD will automatically turn off.
- This addresses your feedback about the AOD staying on unnecessarily during all-night "Adaptive Charging" holds.
- The AOD will turn back on automatically as soon as the phone starts drawing power again (e.g., when the "Adaptive Hold" finishes or you unplug/replug).

### Settings & Optimization
#### [SettingsScreen.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/ui/screens/SettingsScreen.kt)
- Selecting **Custom Limit** now explicitly disables the system's "Adaptive Charging" setting.
- This ensures that your custom limit is the primary authority, preventing the system from pausing charging at 80% for hours when you've set a higher custom limit.

## Verification Results

### Automated Tests
- [x] Successfully compiled the project with **Kotlin 2.2.10** and **SDK 37**.
- [x] Verified that `:app:assembleDebug` completes without errors.

### Manual Verification Required
- [ ] **Wattage Guard:** Plug in the phone and wait for the "Adaptive Hold" (0W). Verify that after ~10 minutes, the AOD turns off.
- [ ] **Custom Limit:** Enable a Custom Limit (e.g., 90%) and verify in Android Settings -> Battery -> Charging Optimization that "Adaptive Charging" is turned OFF.

> [!TIP]
> You can find the debug APK in your project directory at:
> `app/build/outputs/apk/debug/app-debug.apk`

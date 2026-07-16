# Walkthrough - Temperature Unit Preference

I have added a setting to allow users to choose between Celsius and Fahrenheit for the battery temperature displayed in the charging notification.

## Changes Made

### 1. Temperature Unit Selection UI
- **`SettingsScreen.kt`**:
    - Added a `tempUnit` state to the `SettingsState` class to track the user's preference (Celsius by default).
    - Added a `Temperature Unit` dialog that allows users to pick between `Celsius (°C)` and `Fahrenheit (°F)`.
    - Added a settings (gear) icon to the "Charging Details Notification" preference. Clicking this icon opens the temperature unit dialog.

### 2. Live Notification Update
- **`NotificationAodService.kt`**:
    - Updated the `updateChargingNotification` logic to read the user's preferred temperature unit from `SharedPreferences`.
    - If Fahrenheit is selected, the app now automatically performs the conversion `(C * 9/5) + 32` before updating the notification text.
    - The notification now displays either `°C` or `°F` based on the user's selection.

## Verification Results

### Automated Tests
- The project successfully built using `gradle assembleDebug`.

### Manual Verification Recommended
1. **Open Settings**: Verify that a gear icon now appears next to the "Charging Details Notification" switch.
2. **Change Unit**: Tap the gear icon and select "Fahrenheit (°F)".
3. **Check Notification**: Plug in your charger and verify that the charging notification shows the temperature in Fahrenheit (e.g., `95.0°F`).
4. **Switch Back**: Change the unit back to "Celsius (°C)" and verify the notification immediately updates to show Celsius (e.g., `35.0°C`).

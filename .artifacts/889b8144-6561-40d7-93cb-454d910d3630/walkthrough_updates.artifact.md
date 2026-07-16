# Walkthrough - Background Update Notifications & Logic Fix

I have implemented background update checks and fixed the version comparison logic to ensure you and your users always stay on the latest version of Pixel AOD.

## Changes Made

### 1. Robust Version Comparison
- **`UpdateChecker.kt`**: Fixed a bug where version strings like `v1.1.2` were incorrectly compared against `1.1.4`.
- **Sanitization**: The app now automatically strips the `v` prefix and any suffixes (like `-debug`) before comparing.
- **Accurate Math**: It now correctly identifies that `1.1.2` is older than `1.1.4` by comparing each numeric part individually.

### 2. Background Update Checks (WorkManager)
- **`UpdateWorker.kt`**: Created a new background worker that checks for updates silently.
- **Scheduling**: The app now schedules this check to run once every **24 hours**.
- **Efficiency**: It only runs when the device has an active internet connection. Because it's a simple text-based API call, the battery impact is virtually zero.

### 3. System Notifications
- **`UpdateChecker.kt`**: Added a new function to show a system notification when an update is found while the app is in the background.
- **Immediate Action**: The notification includes a "Tap to update" action that takes the user directly to the GitHub release page.

### 4. App Startup Integration
- **`AodApplication.kt`**: Integrated the `WorkManager` initialization into the main Application class, ensuring background checks are always active.

## Verification Results

### Automated Tests
- The project successfully built using `gradle assembleDebug`.
- `WorkManager` dependency was correctly added and synced.

### Manual Verification Recommended
1. **Force Update Check**: You can manually change your `versionName` in `build.gradle` to something lower (e.g., `1.1.0`), build the app, and open it. It should immediately detect `1.1.4` as a newer version.
2. **Background Test**: To verify the background notification, you can use the following ADB command to force the worker to run:
   ```bash
   adb shell am broadcast -a com.google.android.gms.gcm.ACTION_CHECK_QUEUE
   ```
   *(Note: WorkManager behavior varies by device, but the startup scheduling is the standard Android implementation.)*

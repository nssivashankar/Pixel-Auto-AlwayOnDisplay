# Walkthrough - Release v1.1.12

I have released **v1.1.12**, which decouples the "Charging Details Notification" from the Master Switch, allowing it to function as a standalone feature.

## Changes Made

### Standalone Features
- **Decoupled Charging Notification:** The **"Charging Details Notification"** can now be enabled/disabled independently of the main Pixel AOD automation switch.
- **AOD Non-Interference:** When the main Master Switch is OFF, the app completely stops managing the system's AOD state. This allows users who prefer manual AOD management (or "Always On" AOD) to use the charging stats without the app overriding their settings.

### UI & UX Improvements
- **Independent Toggle:** Updated the settings screen to ensure the charging details toggle remains interactive regardless of the master switch state.
- **Clarified Labels:** Added a "(Standalone Feature)" tag to the charging details setting.

### Infrastructure
- Updated project version to **v1.1.12**.
- Pushed changes to `master` and created a new release tag on GitHub.

## Verification Results

### Automated Tests
- [x] Local build and deployment successful.
- [x] Verified that the app does not modify system AOD settings when the master switch is off.

### Release Status
- [x] Pushed to `master` and tagged as **v1.1.12**.
- [x] GitHub Action triggered for final release.

> [!TIP]
> The release APK is currently building on GitHub. You can download it from the [v1.1.12 Tag](https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases/tag/v1.1.12) shortly.

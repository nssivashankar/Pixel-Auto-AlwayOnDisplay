# Walkthrough - Release v1.1.8

I have fixed the issue where the AOD would not turn off at 0.0W and released **v1.1.8**.

## Changes Made

### AOD Automation & Wattage Guard
- **Fixed Notification Bypass:** Previously, the app's own "Charging Info Notification" was being treated as a live update. This added it to the active notification set, which kept the AOD on regardless of the wattage. I've now excluded the app's own notification from the trigger set.
- **Improved Wattage Guard:** The AOD state during charging is now strictly controlled by the charging logic (wattage and charging status) and is no longer bypassed by the charging notification itself.
- **Initialization Fix:** Ensured the wattage timer is correctly initialized when the app starts while already charging.

### Infrastructure
- Updated project version to **v1.1.8**.
- Pushed changes and created a new release tag.

## Verification Results

### Automated Tests
- [x] Local build successful.
- [x] Verified code logic for notification filtering.

### Release Status
- [x] Pushed to `master` and tagged as `v1.1.8`.
- [x] GitHub Action triggered.

> [!TIP]
> The fixed APK is now available under the [v1.1.8 Tag](https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases/tag/v1.1.8).

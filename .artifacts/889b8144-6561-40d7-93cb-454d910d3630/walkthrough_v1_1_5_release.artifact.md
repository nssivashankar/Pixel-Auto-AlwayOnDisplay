# Walkthrough - v1.1.5 Release

I have successfully released version **v1.1.5** of Pixel Auto AOD, incorporating several critical fixes, performance optimizations, and new features.

## Major Changes in this Release

### 1. Power Efficiency (Adaptive Charging AOD Sleep)
- **Feature**: The app now intelligently detects when Adaptive Charging is in its "hold" phase at 80% (low wattage).
- **Result**: The AOD automatically turns OFF during this period to save battery and reduce screen wear, resuming only when the final charge to 100% starts.

### 2. User Experience (Background Update Notifications)
- **Feature**: Integrated Android **WorkManager** to check for updates once every 24 hours.
- **Result**: Users will now receive a system notification when a new version is available, even if the app hasn't been opened.

### 3. Reliability (Notification Buttons & Sync)
- **Reliable Actions**: Fixed an issue on Android 14/15 where notification buttons would sometimes not respond by using `RECEIVER_EXPORTED`.
- **Real-time UI**: The settings screen now updates instantly when modes are changed via notification buttons or system settings.

### 4. Performance (App List Optimization)
- **Fluid Scrolling**: Implemented `LruCache` for icons and optimized bitmap resolutions (72x72).
- **Stability**: Added `@Stable` annotations to data models to ensure butter-smooth scrolling in the per-app notification settings.

### 5. Localization & PR Integration
- **Support**: Added official string resources and translations for **Chinese (Taiwan)**.
- **Standardization**: Migrated temperature unit settings to a standard `unit_system` key while preserving user preferences from v1.1.4.

## Deployment Status
- **Build Version**: `1.1.5`
- **Git Status**: All changes pushed to `master`.
- **Release Trigger**: Tag `v1.1.5` pushed to GitHub.
- **CI/CD**: GitHub Actions has started the automated build and release workflow.

You can verify the release progress on your GitHub repository's Actions tab. Once complete, the signed APK will be available for download.

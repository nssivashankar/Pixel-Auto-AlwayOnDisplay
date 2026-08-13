# Walkthrough - Major Release v1.2.0

I have released **v1.2.0**, a major update that introduces the highly requested **Lift to Wake AOD** feature, a complete **Glass UI** overhaul, and a modernized **In-App Update** system.

## New Features

### 1. Lift to Wake AOD [NEW]
- **Sensor-Driven:** Uses the Pixel's low-power hardware sensor to detect when you pick up your phone.
- **Convenient Preview:** Automatically enables the AOD for 10 seconds so you can check the time and notifications without touching any buttons.
- **Battery Efficient:** The sensor is only active when the screen is OFF, ensuring zero impact on daily battery life.

### 2. Premium "Frosted Glass" UI
- **Continuous Top Cap:** Merged the status bar and title bar into a single, unbroken pane of frosted glass.
- **Floating Island Navigation:** The bottom navigation pill is now a high-quality glass island with perfectly sharp icons on top of a blurred background.
- **Dynamic Blur Sync:** All glass elements feature hardware-accelerated blur that synchronizes perfectly with the content passing behind them at 120Hz.

### 3. Integrated In-App Updates
- **Direct Downloads:** You can now download and install the latest versions directly within the app via GitHub.
- **What's New:** View full release notes and changelogs before updating.
- **Guided Installation:** Automated prompts to install the update once the download completes.

### 4. Interactive Enhancements
- **Premium Haptics:** Added subtle tactile feedback when swiping between pages and toggling settings.
- **Fluid Sliding:** Implemented a `HorizontalPager` for smooth, physics-based sliding transitions between the Home and About screens.

## Improvements & Bug Fixes
- **Wattage Guard Fix:** Resolved an issue where the charging notification would prevent the AOD from timing out at 0.0W.
- **Standalone Mode:** The "Charging Details Notification" can now be used independently of the AOD automation.
- **Reliability:** Upgraded Kotlin to 2.2.20 and optimized GitHub Action workflows for more robust builds.

## Verification Results

### Automated Tests
- [x] Full project build successful.
- [x] All hardware sensor integrations verified.

### Release Status
- [x] Version bumped to **1.2.0**.
- [x] Pushed to `master` and tagged as **v1.2.0**.
- [x] Automated release build triggered on GitHub.

> [!TIP]
> The v1.2.0 update is now building. You can track its progress and download the final APK from the [GitHub Releases page](https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases/tag/v1.2.0).

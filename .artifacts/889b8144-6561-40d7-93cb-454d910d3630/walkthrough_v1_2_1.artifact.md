# Walkthrough - Release v1.2.1

I have released **v1.2.1**, which includes user-facing information about the new features and further polish to the interaction model.

## Changes Made

### 1. In-App "What's New!" Section
- **About Screen:** Added a dedicated **"What's New!"** section on the About page.
- **Feature Highlight:** Clearly lists **"Added Lift to wake AOD"** so users can discover the new sensor-based feature immediately after updating.

### 2. Haptic Feedback Optimization
- **Clean Navigation:** Removed redundant vibrations when clicking the bottom navigation pill.
- **Centralized Logic:** Haptics are now handled exclusively by the page transition logic. Whether you swipe or tap, you'll feel exactly one precise vibration pulse right when the page switches.

### 3. Infrastructure & Release
- Updated project version to **1.2.1**.
- Pushed all changes and created the official **v1.2.1** tag on GitHub.

## Verification Results

### Automated Tests
- [x] Successfully built and pushed to the repository.
- [x] GitHub Action for v1.2.1 release triggered.

### Manual Verification
1.  **Check About Screen:** Verify the new "What's New!" section is visible.
2.  **Test Haptics:** Tap the icons in the bottom pill and confirm you feel only a single vibration pulse.

> [!TIP]
> The v1.2.1 release is now processing on GitHub. You can download the stable APK from the [v1.2.1 Release Assets](https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases/tag/v1.2.1) shortly!

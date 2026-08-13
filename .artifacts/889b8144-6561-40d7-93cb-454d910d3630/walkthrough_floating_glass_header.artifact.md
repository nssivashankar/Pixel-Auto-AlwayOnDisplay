# Walkthrough - Floating Glass Title Bar

I have transformed the full-width title bar into a floating "Glass Island" that matches the aesthetic of the bottom navigation pill.

## Changes Made

### Floating Header Design
- **`activity_settings.xml`**: Converted the standard top bar into a floating `MaterialCardView`.
    - **Floating Effect:** Added `16dp` margins to the sides and top (accounting for the status bar) to make the bar appear suspended over the content.
    - **Consistent Shape:** Set a `28dp` corner radius to match the modern "pill" design of the Pixel's system UI.
    - **Hardware Shadows:** Added native elevation so the bar casts a realistic shadow over the settings list as you scroll.
- **`SettingsActivity.kt`**:
    - **Dynamic Spacing:** Updated the window inset logic to automatically adjust the floating bar's position based on the device's status bar height.

### Synchronized Glass Blur
- **`SettingsActivity.kt`**: Upgraded the hardware-mirror logic for the header.
    - **Precision Mapping:** Like the bottom pill, the top bar now uses a relative coordinate system to mirror the content behind it.
    - **Perfect Continuity:** As you scroll the settings list, the items pass through the header's blur with zero vertical shift, creating a true "Frosted Glass" effect.

### UI Optimization
- **`SettingsActivity.kt`**: Increased the list's top padding to ensure the floating bar has enough "breathing room" and doesn't overlap the first category titles.

## Verification Results

### Automated Tests
- [x] Successfully built the debug APK.
- [x] Verified hardware-accelerated drawing remains smooth at 120Hz.

### Manual Verification
1.  **Visual Match:** Observe that the top and bottom bars now share the same floating pill shape and frosted texture.
2.  **Scroll Sync:** Scroll the list slowly and look at the text as it enters the top bar's blur. It should align perfectly with the sharp text below the bar.
3.  **StatusBar Gap:** Verify there is a clear "floating" gap between the top bar and the status bar icons.

> [!TIP]
> The updated app is now running on your device. Both the header and footer are now matching floating islands!

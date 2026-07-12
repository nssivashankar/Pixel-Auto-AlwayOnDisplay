# Walkthrough - Pixel Auto AOD v1.1.0 Modernization

This release marks a significant transformation of "Pixel Auto AOD," moving from a simple utility to a premium, feature-rich system assistant for Pixel users.

## 🌟 Major Enhancements

### 1. Dynamic Theme Engine 🌓
The app now fully embraces Material 3 Dynamic Color and theme switching.
- **Adaptive Frost**: The glass header intelligently adjusts its opacity and tint based on your theme (85% charcoal in Dark Mode, 95% milky white in Light Mode) for maximum readability.
- **Color Sync**: Icons and accent colors across the app and notifications now use system-defined Material You tones.

### 2. Bold System Identity 💎
- **Headline 30sp**: The "Pixel AOD" title is now a massive, bold anchor for the UI.
- **Backdrop Blur**: A stable, hardware-accelerated "Zero-Glow" mirroring engine provides real-time blur as you scroll.
- **Modern Categories**: Sections are clearly separated with bold, uppercase, and spaced headers for a professional feel.

### 3. Integrated Battery Health (Pixel Specific) 🔋
- **Direct Control**: You can now toggle **Charging Optimization (80% Limit)** and **Adaptive Charging** directly from the settings list.
- **Live Sync**: Any changes made via system settings or notifications are instantly reflected in the app UI.

### 4. Smart Alerts & Notifications 🔔
- **Completion Alarms**: The app now plays the default system sound and vibrates when your battery hits **80%** (if limited) or **100%**.
- **Lockscreen Quick Actions**:
    - The 80% completion alert includes a **"Full Charge"** button that lets you disable the limit and continue to 100% without unlocking your phone.
- **Accurate ETA**: Refined the time-to-full calculation using conservative battery models and rate-limiting to ensure estimates are stable and realistic.

## 🛠 Bug Fixes & Stability
- **Lockscreen Visibility**: Fixed a critical bug where charging info was missing from the AOD/Lockscreen on Android 15.
- **Toggle Persistence**: Resolved issues with setting switches not saving or responding to touch.
- **UI Crash Fix**: Removed incompatible custom adapters to ensure 100% runtime stability.
- **Broadcast Reliability**: Fixed the "Full Charge" button by explicitly registering background listeners.

## 📖 Documentation
- Updated `README.md` with **feature-aligned screenshots**.
- Clarified that the `WRITE_SECURE_SETTINGS` permission is a **one-time setup**, and Developer Options can be disabled immediately after.

---
*Developed with ❤️ for the Pixel community.*

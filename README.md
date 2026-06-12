# ✨ Pixel Auto AOD

**The missing automation for your Pixel's Always-On Display.**

Pixel Auto AOD is a lightweight utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD), ensuring it's only active when you actually need it—saving battery and reducing screen wear while keeping you informed.

---

## 🚀 Features

-   **⚡ Smart Charging Mode**: Automatically enable AOD when you plug in. Once removed or fully charged (100%), it turns off instantly.
-   **🔔 Per-App Notifications**: Select specific apps (WhatsApp, Mail, etc.) to trigger AOD. When a notification arrives, AOD wakes up; when you clear it, AOD goes back to sleep.
-   **📍 Live Updates Support**: Keep AOD on during "Live" events like Google Maps navigation, Uber/Lyft rides, or food delivery tracking (Swiggy/Zomato).
-   **🌙 Respect System DND**: Optionally silence all AOD triggers when your phone is in Do Not Disturb mode.
-   **⏰ Scheduled Quiet Hours**: Set a custom in-app schedule (e.g., 11 PM to 7 AM) to ensure your room stays dark while you sleep.
-   **🛠 Quick Settings Tile**: A master toggle in your notification shade for instant control.

---

## 🔐 Permissions & Setup

To toggle system settings without Root, this app requires the `WRITE_SECURE_SETTINGS` permission. You can grant this easily via **[Shizuku](https://shizuku.rikka.app/)** (recommended) or ADB.

### Grant via ADB
Connect your phone to a PC and run:
```bash
adb shell pm grant com.nssivashankar.pixelaod android.permission.WRITE_SECURE_SETTINGS
```

---

## 📱 Screenshots
*(Screenshots coming soon!)*

---

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Tested on Pixel series.
-   **Android 15/16/17 Support**: Optimized for the latest Android versions, including specialized fixes for AOD refresh issues on newer betas.

---
*Developed with ❤️ for the Pixel community.*

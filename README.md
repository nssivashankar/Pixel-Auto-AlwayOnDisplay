# ✨ Pixel Auto AOD

**The missing automation for your Pixel's Always-On Display.**

Pixel Auto AOD is a lightweight utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD), ensuring it's only active when you actually need it—saving battery and reducing screen wear while keeping you informed.

---

## 🚀 Features

-   **⚡ Smart Charging Mode**: Automatically enable AOD when you plug in. Once removed or fully charged (100%), it turns off instantly to protect your battery and screen overnight.
-   **📊 Enhanced Charging Info**: A rich notification showing real-time charging wattage, battery temperature, and precise ETA.
    -   **Smart ETA**: Automatically switches between System-provided time and a custom internal calculation if the device is thermal throttling (heat protection).
    -   **80% Health Target**: Intelligently calculates the time to reach 80% for users who want to maximize battery longevity.
    -   **Trickle Status**: Identifies and labels the final "Trickle Charging" phase for better clarity.
-   **🔔 Per-App Notifications**: Select specific apps (WhatsApp, Mail, etc.) to trigger AOD. When a notification arrives, AOD wakes up; when you clear it, AOD goes back to sleep.
-   **📍 Live Updates Support**: Keep AOD on during "Live" events like Google Maps navigation, Uber/Rapido rides, or food delivery tracking (Swiggy/Zomato).
-   **🌙 Respect System DND**: Optionally silence all AOD triggers when your phone is in Do Not Disturb mode.
-   **⏰ Scheduled Quiet Hours**: Set a custom in-app schedule (e.g., 11 PM to 7 AM) to ensure your room stays dark while you sleep.
-   **🛠 Quick Settings Tile**: A master toggle in your notification shade for instant control.

---

## 🔋 Battery Impact
Pixel Auto AOD is designed for zero-drain performance:
- **Event-Driven**: It only runs when the system triggers an update (notification, battery change, or time tick).
- **No WakeLocks**: The app never forces the CPU to stay awake.
- **Low Footprint**: Minimal memory usage (~15MB), making it significantly lighter than most system utilities.

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
APP UI:
<img width="1080" height="2400" alt="Screenshot_20260620_025327" src="https://github.com/user-attachments/assets/10219f8a-379b-451e-988a-ac798108e225" />
<img width="1080" height="2400" alt="Screenshot_20260620_025341" src="https://github.com/user-attachments/assets/9a3afe7d-6ee4-4bde-9d69-18ef8d47200b" />
APP Functions:
 Live Battery Charging Details: <img width="1080" height="2400" alt="Screenshot_20260620_025623" src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" />

 Maps: <img width="1080" height="2400" alt="Screenshot_20260620_030745" src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" />

 Others: <img width="1080" height="2400" alt="Screenshot_20260620_030806" src="https://github.com/user-attachments/assets/74346844-9de1-428d-ad50-358755dd8caf" />







---

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Tested on Pixel series.
-   **Android 15/16/17 Support**: Optimized for the latest Android versions, including specialized fixes for AOD refresh issues on newer betas.

---
*Developed with ❤️ for the Pixel community.*

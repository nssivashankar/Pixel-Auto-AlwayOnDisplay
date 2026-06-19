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

<div align="center">
  <a href="https://github.com/user-attachments/assets/fce10889-84d4-4d4e-9789-8cdcfb8bd0b4"><img src="https://github.com/user-attachments/assets/fce10889-84d4-4d4e-9789-8cdcfb8bd0b4" width="160" alt="Settings Screen" /></a>
  <a href="https://github.com/user-attachments/assets/6e19fbf0-60e1-47d2-b6a4-eab1dce7deee"><img src="https://github.com/user-attachments/assets/6e19fbf0-60e1-47d2-b6a4-eab1dce7deee" width="160" alt="Advanced Config" /></a>
  <a href="https://github.com/user-attachments/assets/b9f4f33f-8d48-4092-8caf-1ea99fa16e7a"><img src="https://github.com/user-attachments/assets/b9f4f33f-8d48-4092-8caf-1ea99fa16e7a" width="160" alt="AOD Charging" /></a>
  <a href="https://github.com/user-attachments/assets/e9b9555b-1cdf-4496-a505-8496fc51cd40"><img src="https://github.com/user-attachments/assets/e9b9555b-1cdf-4496-a505-8496fc51cd40" width="160" alt="AOD Notification" /></a>
</div>

---

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Tested on Pixel series.
-   **Android 15/16/17 Support**: Optimized for the latest Android versions, including specialized fixes for AOD refresh issues on newer betas.

---
## 📜 Credits & License
- **Original Author**: [Alberto Pedron](https://github.com/albertopedron) (Original concept and core logic).
- **Maintenance & Modernization**: Shankar (Modern Android 15+ support, GitHub Actions, and UI enhancements).
- **License**: This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Developed with ❤️ for the Pixel community.*

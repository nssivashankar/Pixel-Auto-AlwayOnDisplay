# ✨ Pixel Auto AOD v1.1.0

**The missing automation for your Pixel's Always-On Display.**

Pixel Auto AOD is a high-performance utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD) while providing integrated Pixel Battery Health management and smart notification tracking.

---

## 🚀 Key Features (v1.1.0)

### 🔋 Integrated Battery Health (Pixel Only)
-   **Direct Optimization Control**: Toggle between **Charging Optimization (80% Limit)** and **Adaptive Charging** directly from the app.
-   **Unified Sync**: Real-time synchronization between system settings, the app UI, and the notification quick actions.
-   **Battery Longevity**: Automated AOD management to reduce screen wear while maintaining battery-safe charging profiles.

### ⚡ Smart Charging Intelligence
-   **Automated Activation**: Automatically enables AOD when the charger is connected and disables it upon disconnection or full charge.
-   **Enhanced Charging Notification**: A live update showing real-time **Charging Wattage**, **Battery Temperature**, and **Precise ETA**.
-   **Smart ETA Engine**: Automatically switches between System-provided time and custom internal calculations if the device is thermal throttling.
-   **80% Health Target**: Calculates exactly when the device will reach the 80% limit for longevity-focused users.

### 🔔 Smart Notification Management
-   **Per-App Triggers**: Choose specific apps (Messaging, Mail, etc.) to trigger the AOD.
-   **Live Notification Mode**: Intelligent detection for ongoing events like **Google Maps navigation**, **Uber/Ride-sharing**, and **Food Delivery tracking**.
-   **Automatic Cleanup**: AOD is automatically disabled once the relevant notifications are cleared.

### 🌙 Adaptive Restrictions
-   **DND Integration**: Optionally silence all AOD triggers when the phone is in **Do Not Disturb** mode.
-   **Scheduled Quiet Hours**: Set custom in-app schedules (e.g., 11 PM to 7 AM) to ensure the display remains off during sleep.

---

## 📱 Screenshots

| | | | |
| :---: | :---: | :---: | :---: |
| <a href="https://github.com/user-attachments/assets/10219f8a-379b-451e-988a-ac798108e225"><img src="https://github.com/user-attachments/assets/10219f8a-379b-451e-988a-ac798108e225" height="400" alt="App UI 1" /></a> | <a href="https://github.com/user-attachments/assets/9a3afe7d-6ee4-4bde-9d69-18ef8d47200b"><img src="https://github.com/user-attachments/assets/9a3afe7d-6ee4-4bde-9d69-18ef8d47200b" height="400" alt="App UI 2" /></a> | <a href="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d"><img src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" height="400" alt="Charging Info" /></a> | <a href="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706"><img src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" height="400" alt="Maps Navigation" /></a> |

---

## 🔋 Battery Impact
- **Event-Driven Architecture**: Runs only when the system triggers an update (notification, battery change).
- **No WakeLocks**: Never forces the CPU to stay awake.
- **Low Footprint**: Minimal memory usage (~15MB).

---

## 🔐 Setup

To toggle system settings without Root, this app requires the `WRITE_SECURE_SETTINGS` permission. You can grant this via **[Shizuku](https://shizuku.rikka.app/)** or ADB.

### Grant via ADB
```bash
adb shell pm grant com.nssivashankar.pixelaod android.permission.WRITE_SECURE_SETTINGS
```

---

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Optimized for Pixel series.
-   **Android 15/16/17 Support**: Includes specialized fixes for AOD refresh states on the latest Android versions.

---
## 📜 Credits & License
- **Original Author**: [Alberto Pedron](https://github.com/albertopedron)
- **Maintenance & Features**: Shankar (Android 15+ support, Battery Health integration, Live Tracking)
- **License**: MIT License.

---
*Developed with ❤️ for the Pixel community.*

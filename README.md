# ✨ Pixel Auto AOD v1.2.0

**The missing automation and premium UI for your Pixel's Always-On Display.**

Pixel Auto AOD is a high-performance utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD) while providing a modern, professional interface and integrated Pixel Battery Health management.

---

## 💎 v1.2.0 Modernization

The latest update transforms Pixel Auto AOD into a premium system utility with a focus on aesthetics and battery longevity.

### 🧊 Glassmorphism UI
- **True Backdrop Blur**: Features a "Dolby-style" frosted glass header that blurs content scrolling beneath it in real-time.
- **Zero-Glow Engine**: Custom rendering engine with dual-edge glow suppression for artifact-free visuals.
- **Ultra-Bold Typography**: High-impact **30sp Black Weight** headlines for a strong system identity.
- **Dynamic Theming**: Intelligent Light and Dark mode support with adaptive "milky" frost density and high-contrast Material 3 colors.

### 🔋 Pixel Battery Health (Pixel Only)
- **Direct Integration**: Toggle between **Charging Optimization (80% Limit)** and **Adaptive Charging** directly from the app or the charging notification.
- **Real-Time Sync**: Settings stay perfectly synchronized between the system, the app UI, and notification quick actions.

---

## 🚀 Core Features

-   **⚡ Smart Charging Mode**: Automatically enable AOD when you plug in. Once removed or fully charged, it turns off instantly.
-   **📊 Enhanced Charging Info**: A rich notification showing real-time charging wattage, battery temperature, and precise ETA.
    -   **Smart ETA**: Automatically switches between System time and custom internal calculations.
    -   **80% Health Target**: Precisely calculates the time to reach the 80% limit for longevity.
-   **🔔 Per-App Notifications**: Select specific apps to trigger AOD. Wakes on arrival, sleeps on clear.
-   **📍 Live Updates Support**: Keep AOD on during "Live" events like Google Maps navigation, Uber/Rapido rides, or Zomato/Swiggy tracking.
-   **🌙 Smart Restrictions**: Optionally silence AOD triggers via **System DND** or **Scheduled Quiet Hours**.
-   **🛠 Quick Settings Tile**: A master toggle in your notification shade for instant control.

---

## 🔋 Battery Impact
Pixel Auto AOD is designed for zero-drain performance:
- **Event-Driven**: It only runs when the system triggers an update (notification, battery change, etc.).
- **No WakeLocks**: The app never forces the CPU to stay awake.
- **Low Footprint**: Minimal memory usage (~15MB), significantly lighter than standard system utilities.

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

| | | | |
| :---: | :---: | :---: | :---: |
| <a href="https://github.com/user-attachments/assets/10219f8a-379b-451e-988a-ac798108e225"><img src="https://github.com/user-attachments/assets/10219f8a-379b-451e-988a-ac798108e225" height="400" alt="App UI 1" /></a> | <a href="https://github.com/user-attachments/assets/9a3afe7d-6ee4-4bde-9d69-18ef8d47200b"><img src="https://github.com/user-attachments/assets/9a3afe7d-6ee4-4bde-9d69-18ef8d47200b" height="400" alt="App UI 2" /></a> | <a href="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d"><img src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" height="400" alt="Charging Info" /></a> | <a href="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706"><img src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" height="400" alt="Maps Navigation" /></a> |

---

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Tested on Pixel series.
-   **Android 15/16/17 Support**: Optimized for the latest Android versions, including specialized fixes for AOD refresh issues on newer betas.

---
## 📜 Credits & License
- **Original Author**: [Alberto Pedron](https://github.com/albertopedron) (Original concept and core logic).
- **Maintenance & Modernization**: Shankar (Modern Android 15+ support, Glassmorphism UI, and Battery Health features).
- **License**: MIT License.

---
*Developed with ❤️ for the Pixel community.*

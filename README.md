# ✨ Pixel Auto AOD

**The missing automation for your Pixel's Always-On Display.**

Pixel Auto AOD is a high-performance utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD) while providing integrated Pixel Battery Health management and smart notification tracking.

---

## 🚀 Key Features

### 🔋 Integrated Battery Health (Pixel Only)
| Feature Details | Preview |
| :--- | :---: |
| - **Direct Optimization Control**: Toggle between **Charging Optimization (80% Limit)** and **Adaptive Charging** directly from the app.<br><br>- **⭐ Control charging modes in lockscreen itself**: A specialized live notification that lets you switch battery modes instantly from your lockscreen.<br><br>- **Unified Sync**: Real-time synchronization between system settings, the app UI, and the notification quick actions. | <img src="https://github.com/user-attachments/assets/01842e2f-60e8-44cb-a8b0-260719ac7974" width="200" alt="Integrated Health" /> |

### ⚡ Smart Charging Intelligence
| Feature Details | Preview |
| :--- | :---: |
| - **Automated Activation**: Automatically enables AOD when the charger is connected and disables it upon disconnection or full charge.<br><br>- **Charging info in AOD (exact time and temp)**: A live update showing real-time **Charging Wattage**, **Battery Temperature**, and **Precise ETA**.<br><br>- **Smart ETA Engine**: Automatically switches between System time and custom internal calculations if the device is thermal throttling. | <img src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" width="200" alt="AOD Info" /> |

### 🔔 Smart Notification Management
| Feature Details | Preview |
| :--- | :---: |
| - **Settings - Per-App Customization**: Choose specific apps (Messaging, Mail, etc.) to trigger the AOD.<br><br>- **Live notifications for apps that support live notifications**: Intelligent detection for ongoing events like **Google Maps navigation**, **Uber/Ride-sharing**, and **Food Delivery tracking**.<br><br>- **Automatic Cleanup**: AOD is automatically disabled once the relevant notifications are cleared. | <img src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" width="200" alt="Live Tracking" /> |

---

## 🔐 Setup & Permission

To toggle system settings without Root, this app requires the `WRITE_SECURE_SETTINGS` permission. 

> [!IMPORTANT]
> **One-Time Setup**: You only need to grant this permission **ONCE**. After granting it via Shizuku or ADB, you can **immediately turn off Developer Options** and USB Debugging. The app will continue to function perfectly.

### Grant via ADB
```bash
adb shell pm grant com.nssivashankar.pixelaod android.permission.WRITE_SECURE_SETTINGS
```

---

## 🔋 Battery Impact
- **Event-Driven Architecture**: Runs only when the system triggers an update.
- **No WakeLocks**: Never forces the CPU to stay awake.
- **Low Footprint**: Minimal memory usage (~15MB).

---

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Optimized for Pixel series.
-   **Android 15/16/17 Support**: Includes specialized fixes for AOD refresh states.

---
## 📜 Credits & License
- **Original Author**: [Alberto Pedron](https://github.com/albertopedron)
- **Maintenance & Features**: Shankar (Android 15+ support, Battery Health integration, Live Tracking)
- **License**: MIT License.

---
*Developed with ❤️ for the Pixel community.*

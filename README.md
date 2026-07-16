# ✨ Pixel Auto AOD

**The missing automation for your Pixel's Always-On Display.**

Pixel Auto AOD is a high-performance utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD) while providing integrated Pixel Battery Health management and smart notification tracking.

---

Video sample of app features : 

https://github.com/user-attachments/assets/8eb3a271-120a-4d18-9030-ed0ce7ee6406






## 🚀 Key Features

### 🔋 Ease of Charging mode control
| Feature Details | Preview |
| :--- | :---: |
| - **Direct Optimization Control**: Toggle between **Charging Optimization (80% Limit)** and **Adaptive Charging** directly from the app/lockscreen notification.<br><br> | <img src="https://github.com/user-attachments/assets/01842e2f-60e8-44cb-a8b0-260719ac7974" width="200" alt="Integrated Health" /> |

### 🔋 Custom Charging Limits (First App in market to include this feature)
| Feature Details | Preview |
| :--- | :---: |
| - **⭐ Custom Charging Limits**: Set your own charging Limit (80%–100%) once the limit is reached it will stop the charging.<br><br> | <img src="https://github.com/user-attachments/assets/1463e0dc-89a0-450f-88b3-00b0c79e4f91" width="200" alt="Integrated Health" /> |

### ⚡ Smart Charging Intelligence
| Feature Details | Preview |
| :--- | :---: |
| - **Automated Activation**: Automatically enables AOD when the charger is connected and disables it upon disconnection or full charge.<br><br>- **Charging info in AOD (exact time and temp)**: A live update showing real-time **Charging Wattage**, **Battery Temperature**, and **Precise ETA**.<br><br>- **Smart ETA Engine**: Automatically switches between System time and custom internal calculations if the device is thermal throttling. | <img src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" width="200" alt="AOD Info" /> |

### 🔔 Smart Notification Management
| Feature Details | Preview |
| :--- | :---: |
| - **Settings - Per-App Customization**: Choose specific apps (Messaging, Mail, etc.) to trigger the AOD.<br><br>- **Live notifications for apps that support live notifications**: Intelligent detection for ongoing events like **Google Maps navigation**, **Uber/Ride-sharing**, and **Food Delivery tracking**.<br><br>- **Block List**: Easily exclude specific apps from triggering Live Notification mode. | <img src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" width="200" alt="Live Tracking" /> |

---

## 🎨 Premium Experience
- **Material 3 Expressive UI**: Built using the latest organic design guidelines with fluid shape-morphing animations and modern spacing.
- **120Hz Fluidity**: Optimized for high-refresh-rate displays with a throttled "Glassmorphism" rendering engine to ensure zero scrolling jank.
- **Premium Haptics**: Differentiated tactile feedback for system actions and precise snaps for slider interactions.
- **Seamless Setup**: A guided onboarding flow that explains the "why" behind every permission, ensuring a foolproof initial configuration.

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

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Optimized for Pixel series.
-   **Android 15/16/17 Support**: Includes specialized fixes for AOD refresh states and Live Update standards.

---
## 📜 Credits & License
- **Original Author**: [Alberto Pedron](https://github.com/albertopedron)
- **Maintenance & Features**: Shankar (Android 15+ support, Material 3 UI, Custom Battery Limits, Live Tracking)
- **License**: MIT License.

---
*Developed with ❤️ for the Pixel community.*

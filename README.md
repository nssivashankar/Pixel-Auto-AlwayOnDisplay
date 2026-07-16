# ✨ Pixel Auto AOD

**The missing automation for your Pixel's Always-On Display.**

Pixel Auto AOD is a high-performance utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD) while providing integrated Pixel Battery Health management and smart notification tracking.

---

### 📺 Feature Showcase

<div align="center">
  <video src="https://github.com/user-attachments/assets/8eb3a271-120a-4d18-9030-ed0ce7ee6406" width="400">
  </video>
</div>

---

## 🚀 Key Features

### 🔋 Ease of Charging Mode Control
| Feature Details | Preview |
| :--- | :---: |
| - **Direct Optimization Control**: Toggle between **Charging Optimization (80% Limit)** and **Adaptive Charging** directly from the app or the lockscreen notification. | <img src="https://github.com/user-attachments/assets/01842e2f-60e8-44cb-a8b0-260719ac7974" width="200" alt="Integrated Health" /> |

### 🔋 Custom Charging Limits (First to Market)
| Feature Details | Preview |
| :--- | :---: |
| - **⭐ User-Defined Thresholds**: Set your own charging limit (80%–100%). The app intelligently stops hardware charging the moment your target is reached, preserving your long-term battery health. | <img src="https://github.com/user-attachments/assets/1463e0dc-89a0-450f-88b3-00b0c79e4f91" width="200" alt="Custom Limits" /> |

### ⚡ Smart Charging Intelligence
| Feature Details | Preview |
| :--- | :---: |
| - **Automated Activation**: Automatically enables AOD when the charger is connected and disables it upon disconnection or target reach.<br><br>- **Real-time AOD Stats**: View **Charging Wattage**, **Battery Temperature**, and **Precise ETA** directly on your Always-On Display. | <img src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" width="200" alt="AOD Info" /> |

### 🔔 Smart Notification Management
| Feature Details | Preview |
| :--- | :---: |
| - **Per-App Customization**: Choose exactly which apps trigger the AOD when a notification arrives.<br><br>- **Live Tracking**: Keeps the display awake for active events like **Google Maps Navigation**, **Uber/Ride-sharing**, and **Food Delivery**. | <img src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" width="200" alt="Live Tracking" /> |

---

## 🎨 Premium Experience
- **Material 3 Expressive UI**: Built using the latest organic design guidelines with fluid shape-morphing animations.
- **120Hz Fluidity**: Optimized for high-refresh-rate displays with a frame-synchronized rendering engine for zero scrolling jank.
- **Guided Setup**: A foolproof, multi-step start page that explains "why" each permission is needed and helps you configure everything in seconds.

---

## 🔐 Setup & Permission

To manage system features without Root, this app requires the `WRITE_SECURE_SETTINGS` permission. 

> [!TIP]
> **New in v1.1.2**: The app now includes a **Guided Onboarding Flow** to help you grant this via Shizuku or ADB easily. You only need to do this **ONCE**.

### Grant via ADB
```bash
adb shell pm grant com.nssivashankar.pixelaod android.permission.WRITE_SECURE_SETTINGS
```

---

## ⚠️ Compatibility
-   **Exclusively for Google Pixel**: Optimized for the Pixel series.
-   **Android 13 to 17 Support**: Includes specialized fixes for AOD refresh states and the latest notification standards.

---
## 📜 Credits & License
- **Original Author**: [Alberto Pedron](https://github.com/albertopedron)
- **Maintenance & Features**: [Shankar](https://github.com/nssivashankar) (Android 15+ support, Material 3 UI, Custom Battery Limits, Live Tracking)
- **License**: MIT License.

---
*Developed with ❤️ for the Pixel community.*

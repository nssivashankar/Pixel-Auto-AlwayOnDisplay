# ✨ Pixel Auto AOD

**The missing automation for your Pixel's Always-On Display.**

Pixel Auto AOD is a high-performance utility designed specifically for Google Pixel devices. It intelligently manages your Always-On Display (AOD) while providing integrated Pixel Battery Health management and smart notification tracking.

---

## 🚀 Version 1.1.2 Highlights

### 🏁 Seamless Onboarding
- **Guided Setup Flow**: Introduced a professional, multi-step start page that explains "why" each permission is needed, making the initial configuration foolproof.
- **Permission Reliability**: Fixed critical issues with Battery Optimization and Notification Access requests to ensure the app works out-of-the-box.

### 🛠️ Stability & Synchronization
- **Unified State Engine**: Bridged the gap between the Android View-based Master Switch and the Compose settings list. Toggles now react instantly and accurately.
- **Double-Toggle Fix**: Resolved a UX bug where clicking settings could trigger redundant state changes, resulting in unreliable activation.
- **Haptic Precision**: Integrated fine-tuned tactile feedback directly into components for a more tactile, premium feel.

## 🚀 Version 1.1.1 Highlights

### 🎨 Material 3 Expressive UI
- **Official Morphing Loader**: Integrated the official Material 3 Expressive `LoadingIndicator` with its signature 7-shape organic morphing animation.
- **OCD-Level Alignment**: Refined the entire settings list with a "perfect horizontal baseline." Every label and switch now aligns precisely to a consistent vertical axis for a premium, structured look.
- **Enhanced Iconography**: Added missing icons for DND, Quiet Hours, and Service Status items to ensure visual balance and completeness across the UI.
- **Glassmorphism Header**: A hardware-accelerated "Frosted Glass" mirror engine that diffuses the underlying settings list in real-time.

### ⚡ 120Hz Performance & Fluidity
- **Throttled Blur Engine**: Optimized the heavy blur effect to redraw using a frame-synchronized throttler, eliminating scrolling jank and preserving battery on high-refresh-rate Pixel displays.
- **Zero-Wait App List**: Implemented `AppRepository` caching. The app list dialog now opens instantly with zero loading delay.
- **Parallel Icon Loading**: App icons are now lazy-loaded in the background without blocking the UI, ensuring buttery-smooth scrolling even in massive lists of installed apps.

### 🔋 Custom Battery Limit
- **User-Defined Thresholds**: Go beyond Google's standard 80% limit. Set custom charging targets (80%, 85%, 90%, 95%) with a precise 5% step slider.
- **Intelligent System Trick**: Uses a clever automation logic that "tricks" the system into stopping hardware charging the moment your custom percentage is reached.
- **Dynamic Notifications**: The charging notification automatically adapts to show your custom target and precise ETA for that threshold.

### 📳 Premium Haptics & Feel
- **Differentiated Feedback**: Crisp, high-frequency haptics for turning features ON and solid, deep feedback for turning them OFF.
- **Tactile Sliders**: Every 5% snap on the battery slider provides a satisfying tactile click.
- **Animated Footer**: A beautiful, heart-beat animated "Made with Love" signature with synchronized haptic pulses.

---

## 🚀 Key Features

### 🔋 Integrated Battery Health (Pixel Only)
| Feature Details | Preview |
| :--- | :---: |
| - **Direct Optimization Control**: Toggle between **Charging Optimization (80% Limit)** and **Adaptive Charging** directly from the app.<br><br>- **⭐ Custom Limits**: Set your own charging thresholds (80%–95%) with a smooth Material 3 slider.<br><br>- **Unified Sync**: Real-time synchronization between system settings, the app UI, and the notification quick actions. | <img src="https://github.com/user-attachments/assets/01842e2f-60e8-44cb-a8b0-260719ac7974" width="200" alt="Integrated Health" /> |

### ⚡ Smart Charging Intelligence
| Feature Details | Preview |
| :--- | :---: |
| - **Automated Activation**: Automatically enables AOD when the charger is connected and disables it upon disconnection or full charge.<br><br>- **Charging info in AOD (exact time and temp)**: A live update showing real-time **Charging Wattage**, **Battery Temperature**, and **Precise ETA**.<br><br>- **Smart ETA Engine**: Automatically switches between System time and custom internal calculations if the device is thermal throttling. | <img src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" width="200" alt="AOD Info" /> |

### 🔔 Smart Notification Management
| Feature Details | Preview |
| :--- | :---: |
| - **Settings - Per-App Customization**: Choose specific apps (Messaging, Mail, etc.) to trigger the AOD.<br><br>- **Live notifications for apps that support live notifications**: Intelligent detection for ongoing events like **Google Maps navigation**, **Uber/Ride-sharing**, and **Food Delivery tracking**.<br><br>- **Block List**: Easily exclude specific apps from triggering Live Notification mode. | <img src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" width="200" alt="Live Tracking" /> |

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

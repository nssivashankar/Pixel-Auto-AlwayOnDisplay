<div align="center">

# ✨ Pixel Auto AOD

**Smart Always-On Display & Battery Health Automation for Google Pixel.**

[![Latest Release](https://img.shields.io/github/v/release/nssivashankar/Pixel-Auto-AlwayOnDisplay?style=flat-square&color=34a853)](https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%2013%20--%2016-4285f4?style=flat-square&logo=android)](https://developer.android.com)
[![Device](https://img.shields.io/badge/Device-Google%20Pixel%20Exclusive-ea4335?style=flat-square&logo=googlepixel)](https://store.google.com)
[![License](https://img.shields.io/github/license/nssivashankar/Pixel-Auto-AlwayOnDisplay?style=flat-square&color=fbbc05)](LICENSE)

<br/>

<video src="https://github.com/user-attachments/assets/8eb3a271-120a-4d18-9030-ed0ce7ee6406" width="320" muted autoplay loop></video>

</div>

---

### ✨ What is Pixel Auto AOD?

Pixel Auto AOD seamlessly automates your Pixel's Always-On Display (AOD). It turns AOD on when charging or when selected app notifications arrive, and turns it off when idle—protecting battery life while providing real-time charging metrics and custom battery health limits.

---

### ⚡ Key Features

* **🔋 Custom Charging Limits (80%–100%)** — Set your custom threshold. The app halts hardware charging at your target level to preserve long-term battery health.
* **⚡ Live AOD Metrics** — Displays real-time charging wattage, battery temperature, and calculated clock time to target directly on your Always-On Display.
* **⚡ Quick Optimization Controls** — Toggle between **Limit to 80%**, **Adaptive Charging**, or **Custom Limit** directly from the lockscreen notification or QS tile.
* **🔔 Per-App Notification AOD** — Choose specifically which apps trigger the display upon notification arrival.
* **🚗 Live Activity Tracking** — Keeps the display active for ongoing tasks like **Google Maps Navigation**, **Uber**, or food delivery tracking.
* **🎨 Material 3 Expressive UI** — Designed with modern fluid shape-morphing animations and smooth 120Hz refresh rate support.

---

### 📸 Showcase

<div align="center">
  <img src="https://github.com/user-attachments/assets/01842e2f-60e8-44cb-a8b0-260719ac7974" width="22%" alt="Health Controls" />
  &nbsp;
  <img src="https://github.com/user-attachments/assets/1463e0dc-89a0-450f-88b3-00b0c79e4f91" width="22%" alt="Custom Limits" />
  &nbsp;
  <img src="https://github.com/user-attachments/assets/9d673555-feae-4293-92f2-051842babe1d" width="22%" alt="AOD Info" />
  &nbsp;
  <img src="https://github.com/user-attachments/assets/b2435a10-c60e-491b-80ac-722eeaea6706" width="22%" alt="Live Tracking" />
</div>

---

### 🔐 Setup & Permission

Pixel Auto AOD uses `WRITE_SECURE_SETTINGS` to manage AOD and charging modes without root.

> [!TIP]
> The app includes a guided setup screen that can grant this permission automatically via **Shizuku**.

If granting manually via ADB:
```bash
adb shell pm grant com.nssivashankar.pixelaod android.permission.WRITE_SECURE_SETTINGS
```

---

### 📜 Credits & License

* **Lead Developer & Creator**: **[Shankar (nssivashankar)](https://github.com/nssivashankar)** — *App architecture, Material 3 Expressive UI, custom charging limits, live activity tracking, and ongoing development.*
* **Original Code Logic**: **[Alberto Pedron](https://github.com/albertopedron)** — *Early base code logic.*
* **License**: [MIT License](LICENSE)

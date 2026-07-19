# Walkthrough - Release v1.1.7

I have implemented the "Wattage Guard" logic, refined the charging optimization behavior, and improved CI/CD reliability for the new **v1.1.7** release.

## Changes Made

### AOD Automation & Wattage Guard
- **Wattage Guard Implementation:** The AOD will now automatically turn off if the charging wattage remains near **0W for more than 10 minutes**. This prevents the screen from staying on unnecessarily during prolonged "Adaptive Charging" pauses or when the battery is full but still connected.
- **Smart Resume:** The AOD will turn back on as soon as the phone starts drawing significant power (>0.5W) again.

### Charging Optimization
- **Mode Decoupling:** Selecting the **Custom Limit** optimization in the app now explicitly disables the system's legacy "Adaptive Charging" toggle. This ensures your custom limit is the sole authority and prevents conflicts that could cause unnecessary charging delays.

### CI/CD & Infrastructure
- **Kotlin Update:** Upgraded Kotlin to **2.2.20** to meet the requirements of modern AndroidX libraries (`androidx.activity:1.13.0`).
- **CI Robustness:**
    - Updated Build Tools to **37.0.0** to match SDK 37.
    - Improved **Keystore Decoding** in GitHub Actions to handle whitespace and PEM headers more reliably.
    - Updated fallback versioning to **v1.1.7**.

## Verification Results

### Automated Tests
- [x] Local build `:app:assembleDebug` successful.
- [x] Kotlin 2.2.20 compatibility verified.

### Release Status
- [x] Pushed to `master` and tagged as `v1.1.7`.
- [x] GitHub Action triggered for automated APK generation and release.

> [!TIP]
> You can monitor the progress of the release build on your [GitHub Actions page](https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/actions). Once finished, the APK will be available in the **v1.1.7** release assets.

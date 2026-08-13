# Walkthrough - Lift to Wake AOD

I have implemented the **Lift to Wake AOD** feature, allowing you to check your time and notifications just by picking up your phone.

## Changes Made

### AOD Automation Service
- **Hardware Sensor Integration:** Integrated the device's **Pick Up Gesture** sensor (low-power hardware sensor).
- **Intelligent Monitoring:**
    - The sensor is **only active when the screen is off** to ensure maximum battery efficiency.
    - When you pick up the phone, the AOD is triggered for **10 seconds**.
- **State Management:** Added a new `isLiftToWakeActive` flag to the AOD evaluation logic.

### UI & Settings
- **New Toggle:** Added the **"Lift to Wake AOD"** switch under the **Display Automation** category.
- **Independent Control:** This feature works alongside "Lock Screen AOD" but can be toggled independently.

## Verification Results

### Automated Tests
- [x] Successfully built the debug APK with the new sensor logic.
- [x] Verified that the sensor registration and unregistration follow the screen lifecycle (OFF/ON).

### Manual Verification Required
1.  Open the app and enable **"Lift to Wake AOD"**.
2.  Turn off your screen and place the phone on a table.
3.  Wait a few seconds, then **pick up the phone**.
4.  Verify the AOD clock appears for **10 seconds** and then fades out.

> [!TIP]
> The debug app has been deployed to your device. Please try the "Lift to Wake" gesture!

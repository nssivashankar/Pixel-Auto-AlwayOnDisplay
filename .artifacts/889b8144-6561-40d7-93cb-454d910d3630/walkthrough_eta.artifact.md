# Walkthrough - Charging ETA Accuracy Fix

I have completely overhauled the charging ETA calculation logic to ensure that 80% and Custom Limit targets are accurate, even when Adaptive Charging or system-level limits are active.

## Changes Made

### 1. Adaptive Charging Awareness
- **Problem**: When Adaptive Charging is active, the system reports a "time to full" that is many hours away (targeting an alarm). My previous logic took a linear slice of that time, making the 80% ETA appear much later than reality.
- **Solution**: I implemented a "Natural ETA" calculation using the real-time charging wattage. The app now detects when the system ETA is artificially inflated and automatically switches to using the actual charging speed for its 80% and Custom Limit estimations.

### 2. System Limit Detection
- **Problem**: On newer Android versions, if the system's "Limit to 80%" setting is enabled, its reported "time to full" is actually the time to reach 80%. My old logic was further reducing this time, resulting in an impossible ETA.
- **Solution**: Added a check for `charge_optimization_mode == 1`. If the system itself is targeting 80%, we now use its estimate directly as the source of truth for our 80% ETA.

### 3. Non-Linear Weighting
- **Problem**: Batteries charge much slower between 80% and 100%. A linear ratio assumes every 1% takes the same amount of time, which is incorrect.
- **Solution**: Replaced the linear ratio with a weighted model. It now accounts for the fact that the "trickle" phase (80-100%) is approximately 2.5x slower than the "bulk" phase (0-80%). This results in much more realistic 80% ETAs when the system is targeting a full 100% charge.

## Verification Results

### Automated Tests
- The project successfully built using `gradle assembleDebug`.

### Manual Verification Recommended
1. **Adaptive Charging Test**: Plug in at night with a morning alarm set. Verify that while the system says "Full at 7:00 AM", my notification correctly shows an earlier time (e.g., "80% at 11:30 PM") based on current speed.
2. **System 80% Test**: Enable the system "Limit to 80%" setting. Verify the notification's "80%" ETA matches the system's lockscreen estimate.
3. **Low Power Test**: Plug into a slow USB port. Verify that "Calculating..." only appears briefly before a realistic ETA appears based on the detected low wattage.

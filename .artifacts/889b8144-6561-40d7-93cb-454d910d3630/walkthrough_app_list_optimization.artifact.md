# Walkthrough - App List Performance Optimization

I have implemented a series of optimizations to the App List (Per-App Notifications) to ensure smooth scrolling and efficient memory usage.

## Changes Made

### 1. Smart Icon Caching with LruCache
- **`AppRepository.kt`**: Replaced the previous `ConcurrentHashMap` (which grew indefinitely) with an `android.util.LruCache`.
- **Memory Management**: The cache is now limited to 100 icons. This keeps the most frequently used icons in memory while automatically clearing older ones to prevent RAM bloat and Garbage Collection "stutters."

### 2. Optimized Bitmap Resolution
- **`AppRepository.kt`**: Reduced the icon capture resolution from 100x100 to **72x72**.
- **Efficiency**: Since list icons are displayed at 42dp, 72x72 is the perfect "Goldilocks" resolution for high-density Pixel displays. This change reduces the memory footprint of each icon by approximately **50%**, allowing for faster decoding and rendering.

### 3. Composition Stability
- **`AppRepository.kt`**: Added the `@Stable` annotation to the `CachedAppInfo` data class. This signals to Jetpack Compose that the app data hasn't changed, allowing it to skip unnecessary recompositions during fast scrolling.

### 4. High-Performance List Layout
- **`AppListDialog.kt`**: Overhauled the `AppListItem` to use the official Material 3 `ListItem` component.
- **Scrolling Smoothness**: `ListItem` is internally optimized for performance in large lists. I also simplified the layout hierarchy and ensured that toggling an app uses a stable lambda, making the scrolling experience much more fluid.

## Verification Results

### Automated Tests
- The project successfully built using `gradle assembleDebug`.

### Manual Verification Recommended
1. **Fluid Scrolling**: Open the "Per-App Notifications" settings and scroll rapidly. Verify the experience is "butter-smooth" without frame drops.
2. **Memory Efficiency**: Verify the app remains responsive even after scrolling through a very long list of installed applications.
3. **Search Responsiveness**: Test the search bar to ensure filtering remains fast and icons update correctly.

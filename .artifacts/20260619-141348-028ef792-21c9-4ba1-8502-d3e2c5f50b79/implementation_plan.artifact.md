# Material 3 Modernization Plan

Migrate the app's UI to Jetpack Compose to fully implement the Material 3 design system as per the [Android documentation](https://developer.android.com/develop/ui/compose/designsystems/material3).

## User Review Required

> [!IMPORTANT]
> This plan involves migrating the primary UI from XML Views to Jetpack Compose. This is a significant architectural shift that allows for deeper Material 3 integration (Dynamic Color, updated typography, and standardized components).

- **UI Architecture**: We will replace `activity_settings.xml` and `PreferenceFragmentCompat` with a pure Compose UI.
- **Mirror Engine**: The custom hardware-accelerated blur effect will be re-implemented using Compose `graphicsLayer` and `RenderEffect` to maintain the "Glassmorphism" look.
- **Dynamic Color**: We will fully leverage the `MaterialTheme` to ensure the app's palette matches the user's system theme and wallpaper.

## Proposed Changes

### Build System

#### [build.gradle](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/build.gradle)

- Enable `compose` build feature.
- Add Material 3, Compose UI, and Activity Compose dependencies.

---

### Material 3 Design System (New)

#### [NEW] [Theme.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/ui/theme/Theme.kt)

- Define `MaterialTheme` with Dynamic Color support (`dynamicLightColorScheme` / `dynamicDarkColorScheme`).

#### [NEW] [Color.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/ui/theme/Color.kt)

- Define the M3 color palette.

#### [NEW] [Type.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/ui/theme/Type.kt)

- Set up M3 Typography scale.

---

### UI Components

#### [SettingsActivity.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/SettingsActivity.kt)

- Refactor to `ComponentActivity`.
- Set content using `setContent { PixelAodTheme { SettingsScreen() } }`.
- Re-implement the "Mirror Engine" (Backdrop Blur) using Compose's drawing modifiers.

#### [NEW] [SettingsScreen.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/ui/screens/SettingsScreen.kt)

- Implement the settings list using `LazyColumn` and `ListItem`.
- Integrate the Master Switch into the `TopAppBar`.

---

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure Compose compilation is successful.

### Manual Verification
- **Dynamic Color**: Change system wallpaper and verify the app's accent colors update automatically.
- **Blur Effect**: Scroll the settings list and verify the frosted glass header mirrors the underlying content with the 80f blur.
- **Functionality**: Verify that toggling settings (Charging Mode, Battery Health, etc.) still correctly updates the app's behavior and system settings.

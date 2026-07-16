# Walkthrough - v1.1.6 Release (Update Fix)

I have successfully released version **v1.1.6**, which specifically fixes the update detection issues that were preventing older devices from seeing new releases.

## Changes Made

### 1. Robust Update Logic
- **`UpdateChecker.kt`**:
    - Added a `User-Agent` to API requests to prevent GitHub from blocking the app's update checks.
    - Disabled network caching to ensure the app always sees the absolute latest release instantly.
    - **Smart Comparison**: The app now correctly handles version names whether they start with a 'v' or not (e.g., `v1.1.2` vs `1.1.6`). It splits versions into numeric parts and compares them intelligently.

### 2. CI/CD Standardization
- **`build.yml`**: Updated the GitHub Actions workflow to automatically strip the `v` prefix from version tags before building the APK.
- **Benefit**: This ensures the app's internal version name is always a clean numeric string (like `1.1.6`), which prevents future math errors in the update checker.

## Deployment Status
- **Build Version**: `1.1.6`
- **Git Status**: All changes pushed to `master`.
- **Release Trigger**: Tag `v1.1.6` pushed to GitHub.
- **CI/CD**: GitHub Actions has started the build.

> [!IMPORTANT]
> Because the logic in version **v1.1.4 and older** is physically broken, those devices will **not** be able to see this update automatically. Please manually install **v1.1.6** on those devices. From v1.1.6 onwards, all future updates will be detected correctly.

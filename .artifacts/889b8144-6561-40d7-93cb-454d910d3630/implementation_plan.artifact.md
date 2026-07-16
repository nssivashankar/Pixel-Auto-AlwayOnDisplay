# Fix Update Detection for Older Versions and CI/CD Cleanup

The update checker in version v1.1.4 and older is broken when the version name starts with a 'v'. This plan fixes the CI/CD pipeline to ensure future versions are stored correctly and improves the app's update logic to handle inconsistent versioning better.

## User Review Required

> [!IMPORTANT]
> Because version v1.1.4 has a "broken" update checker, it will **not** automatically see the v1.1.5 update. You will need to manually install v1.1.5 (or newer) on those devices once. From v1.1.5 onwards, the logic is fixed and future updates will work correctly.

## Proposed Changes

### CI/CD Workflow

#### [MODIFY] [build.yml](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/.github/workflows/build.yml)
- Update the `VERSION_NAME` logic to automatically strip the 'v' prefix before passing it to the Android build.
- This ensures that the app's internal version name is always a clean numeric string (e.g., `1.1.6` instead of `v1.1.6`), which is the standard practice.

### Automation & Logic

#### [MODIFY] [UpdateChecker.kt](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/src/main/java/com/nssivashankar/pixelaod/utils/UpdateChecker.kt)
- **User-Agent Requirement**: Add a custom `User-Agent` to the HTTP request. GitHub API sometimes returns 403 or 503 errors to requests without a User-Agent.
- **Cache Busting**: Disable HTTP caching to ensure the app always sees the absolute latest release.
- **Enhanced Comparison**: Further improve the numeric comparison to handle edge cases like empty strings or unexpected characters.

## Verification Plan

### Automated Tests
- Build verification via `gradle assembleDebug`.

### Manual Verification
1. **CI/CD Test**: Trigger a manual build from GitHub Actions and verify (via the build logs) that the `versionName` passed to Gradle does not contain a 'v'.
2. **Logic Test**: Manually set `currentVersion` to `v1.1.4` in the code and verify that it correctly identifies `1.1.5` as a newer version with the new logic.

# Implementation Plan - Release v1.1.1

We will prepare and tag the project for the `v1.1.1` release, including all recent performance and UI improvements.

## User Review Required

> [!IMPORTANT]
> This will commit all currently modified files and tag the repository.
> The version code will automatically increment if `GITHUB_RUN_NUMBER` is set, otherwise, I will manually increment it to `11` for this release.

## Proposed Changes

### Versioning

#### [MODIFY] [build.gradle](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/build.gradle)
- Update `versionName` fallback to `"1.1.1"`.
- Manually increment `versionCode` default to `11` (or next logical step).

### Release Operations

- **Commit**: Commit all current changes (M3 Loader, Performance Fixes, UI Polish).
- **Tag**: Create a git tag `v1.1.1` on the release commit.
- **Build**: Run `./gradlew assembleRelease` to generate the production package.

## Verification Plan

### Release Package
- Verify that the `outputs/apk/release` directory contains the signed APK (if signing is configured) or the unsigned release APK.
- Verify that `git tag` shows `v1.1.1`.

# Implementation Plan - Release v1.1.2

We will finalize the current state of the app and release version **v1.1.2**, featuring the new onboarding flow and critical synchronization fixes.

## User Review Required

> [!IMPORTANT]
> **Release Signing**: I will use the same signing credentials provided previously (`nssivashankar` / `shankarc`) to build the signed stable APK.
> **GitHub Release**: This will create a new release entry `v1.1.2` and move the `latest` tag to this version.

## Proposed Changes

### Versioning

#### [MODIFY] [build.gradle](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/build.gradle)
- Update `versionName` to `"1.1.2"`.
- Increment `versionCode` to `12`.

### Documentation

#### [MODIFY] [README.md](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/README.md)
- Update release highlights with the new Onboarding Flow and reliability fixes.

### Release Operations
- **Commit**: Stash all current improvements (Setup Screen, State Sync, Permission Fixes).
- **Tag**: Create git tag `v1.1.2`.
- **Build**: Generate signed stable APK.
- **Publish**: Push to GitHub and create a formal release via `gh cli`.

## Verification Plan
- Verify `app-release.apk` is generated successfully.
- Verify GitHub release `v1.1.2` is accessible with the correct notes and asset.

# Push and Release v1.1.5

This plan covers updating the app version to v1.1.5 and releasing all recent features and fixes.

## User Review Required

> [!IMPORTANT]
> This will push all recent changes to GitHub and trigger the automated release build for version 1.1.5.

## Proposed Changes

### Configuration

#### [MODIFY] [app/build.gradle](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/build.gradle)
- Update `versionName` from `1.1.4` to `1.1.5`.

### Version Control

1. **Commit Changes**:
   - Message: "Release v1.1.5: Background update notifications, Adaptive Charging AOD sleep, and Chinese localization."
2. **Push to master**:
   - `git push origin master`
3. **Create Tag**:
   - `git tag v1.1.5`
4. **Push Tag**:
   - `git push origin v1.1.5`

## Verification Plan

### Automated Verification
- GitHub Actions will automatically start the "Build and Release APK" workflow for the `v1.1.5` tag.

### Manual Verification
- Verify the new release appears on the GitHub repository's Release page.

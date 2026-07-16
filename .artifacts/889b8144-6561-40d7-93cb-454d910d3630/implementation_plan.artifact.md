# Push and Release v1.1.4

This plan covers updating the app version, committing all recent improvements, and pushing a new tag to GitHub to trigger the automated release workflow.

## User Review Required

> [!IMPORTANT]
> This action will push all local changes to the `master` branch and create a new public release on GitHub.

## Proposed Changes

### Configuration

#### [MODIFY] [app/build.gradle](file:///C:/Users/Shankar/StudioProjects/Pixel-Auto-AlwayOnDisplay/app/build.gradle)
- Update `versionName` from `1.1.3` to `1.1.4`.

### Version Control

1. **Commit Changes**:
   - Message: "Release v1.1.4: Reliable notification actions, real-time settings sync, ETA accuracy improvements, and app list optimizations."
2. **Push to master**:
   - `git push origin master`
3. **Create Tag**:
   - `git tag v1.1.4`
4. **Push Tag**:
   - `git push origin v1.1.4`

## Verification Plan

### Automated Verification
- GitHub Actions will automatically start the "Build and Release APK" workflow. I will provide a link to the actions tab for monitoring.

### Manual Verification
- Verify the new release appears on the GitHub repository's Release page with the signed APK attached.

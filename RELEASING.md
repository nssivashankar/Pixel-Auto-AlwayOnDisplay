# 🚀 Release Workflow & Guidelines

> [!IMPORTANT]
> **CRITICAL RELEASE RULE FOR ALL AI AGENTS & DEVELOPERS**:
> All official GitHub releases **MUST** be compiled and published automatically by the **GitHub Actions Workflow** (`.github/workflows/build.yml`) upon pushing a git tag (`v*`).
>
> **DO NOT** use local CLI tools (`gh release create` / local `app-release.apk`) to create or upload release binaries!

---

## 🛑 Why Local Uploads Are Forbidden

When local CLI binaries (`app/release/app-release.apk`) are uploaded manually at release creation:
1. Local builds lack GitHub secret environment variables (`KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`), resulting in local APKs being compiled with default `versionName = 1.0.0` and signed with `debug.keystore`.
2. Anyone downloading the APK in the first 0–2 minutes gets this temporary `debug.keystore` file, causing Android to block the installation with **"App Not Installed"** due to signature mismatch.
3. 2–3 minutes later, GitHub Actions overwrites the asset with the official `pixelautoaod.jks` signed APK (`versionName = X.Y.Z`).

---

## ✅ Correct Release Step-by-Step

To issue a clean, official release without any version or signature conflict:

1. **Increment Version** in `version.properties`:
   ```properties
   code=59
   major=1
   minor=4
   patch=36
   ```

2. **Commit & Tag**:
   ```bash
   git add .
   git commit -m "Release v1.4.36: Description of fixes"
   git tag v1.4.36
   ```

3. **Push Master & Tag to GitHub**:
   ```bash
   git push origin master v1.4.36
   ```

4. **Let GitHub Actions Handle the Rest**:
   - GitHub Actions workflow (`.github/workflows/build.yml`) automatically triggers on the `v1.4.36` push.
   - It builds the release APK with `-PversionName=1.4.36`, signs it with the official `pixelautoaod.jks` key from repository secrets, creates the GitHub Release, and attaches the single, official release binary.
   - Users get the official, perfectly signed release on their very first attempt!

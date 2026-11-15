# Building and releasing ClipForge

## Requirements

- Android Studio with Android SDK Platform 36 installed.
- JDK 17.
- Gradle 8.13, provided by the wrapper in the repository.
- An Android device or emulator with `arm64-v8a`; minimum Android API level is 29 (Android 10).
- Network access for Gradle/Maven dependencies and yt-dlp engine updates.

The app targets Android API 35 and compiles against API 36. Native libraries are restricted to `arm64-v8a`; yt-dlp and FFmpeg make the APK large.

## Build a debug APK

The full Android project is checked in directly at the repository root. Open this folder in Android Studio, or run the Gradle wrapper here:

```powershell
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```sh
chmod +x gradlew
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The debug package ID is `com.clipforge.app.debug`; the APK uses the local Gradle debug key.

Do not commit `local.properties`; it contains a machine-specific Android SDK path.

## Continuous integration

The GitHub Actions workflow builds `assembleDebug` directly from the repository checkout on pushes and pull requests, and stores the APK as a short-lived workflow artifact. It does not run device tests or publish a release.

## Create a release

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Rebuild and install-check the APK on a supported device.
3. Commit and push the source changes, then create a matching version tag.
4. Create a GitHub release and attach the APK from `app/build/outputs/apk/debug/`, or attach a separately release-signed APK.

The published APK is a debug build for direct installation/testing, not Google Play distribution. Configure a protected release signing key before publishing a store-ready build; never commit signing keys or passwords.

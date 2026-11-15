# Building and releasing ClipForge

## Requirements

- Android Studio with Android SDK Platform 36 installed.
- JDK 17.
- Gradle 8.13 (the included wrapper downloads the matching Gradle distribution).
- An Android device or emulator with `arm64-v8a`; the app has a minimum Android API level of 29 (Android 10).
- Network access for Gradle/Maven dependency resolution and yt-dlp engine updates.

The app targets Android API 35 and compiles against API 36. Native libraries are restricted to `arm64-v8a`. The yt-dlp/FFmpeg native dependencies make the APK large.

## Build a debug APK

From the project root:

```powershell
./gradlew.bat assembleDebug
```

On macOS/Linux:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device with Android Studio, or use `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The debug package ID is `com.clipforge.app.debug` and the APK is signed with the local Gradle debug key.

To open in Android Studio, open the repository root and let Gradle sync. Do not commit `local.properties`; it contains a machine-specific Android SDK path.

## Continuous integration

The GitHub Actions workflow builds `assembleDebug` on pushes and pull requests and stores the APK as a short-lived workflow artifact. It does not run device tests or publish a release.

## Create a release

1. Build and install-test the APK on a supported device.
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Create a Git tag such as `v1.0.1` and push it.
4. In GitHub, create a release for that tag and attach the APK from `app/build/outputs/apk/debug/` (or a separately prepared release-signed APK).

The checked-in APK is a debug build for direct installation/testing. It is not signed for Google Play distribution. Configure a protected release signing key separately before publishing a store-ready release; never commit signing keys or passwords.

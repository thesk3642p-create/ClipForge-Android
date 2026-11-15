# Building and releasing ClipForge

## Requirements

- Android Studio with Android SDK Platform 36 installed.
- JDK 17.
- Gradle 8.13, provided by the wrapper in the source archive.
- An Android device or emulator with `arm64-v8a`; minimum Android API level is 29 (Android 10).
- Network access for Gradle/Maven dependencies and yt-dlp engine updates.

The app targets Android API 35 and compiles against API 36. Native libraries are restricted to `arm64-v8a`; yt-dlp and FFmpeg make the APK large.

## Build a debug APK

The GitHub repository keeps the full Android project in `ClipForge-Android-project.zip`. Download it from the repository and extract it. Run the build from the extracted project root, the folder containing `gradlew`:

```powershell
Expand-Archive .\ClipForge-Android-project.zip -DestinationPath .\clipforge-source
Set-Location .\clipforge-source
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```sh
unzip ClipForge-Android-project.zip -d clipforge-source
cd clipforge-source
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The debug package ID is `com.clipforge.app.debug`; the APK uses the local Gradle debug key.

To use Android Studio, open the extracted project root and let Gradle sync. Do not commit `local.properties`; it contains a machine-specific Android SDK path.

## Continuous integration

The repository's GitHub Actions workflow unpacks the source archive, builds `assembleDebug` on pushes and pull requests, and stores the APK as a short-lived workflow artifact. It does not run device tests or publish a release.

## Create a release

1. Update `versionCode` and `versionName` in `app/build.gradle.kts` in the extracted project.
2. Rebuild and install-check the APK on a supported device.
3. Update the source archive in the GitHub repository and create/push a matching version tag.
4. Create a GitHub release and attach the APK from `app/build/outputs/apk/debug/`, or attach a separately release-signed APK.

The published APK is a debug build for direct installation/testing, not Google Play distribution. Configure a protected release signing key before publishing a store-ready build; never commit signing keys or passwords.

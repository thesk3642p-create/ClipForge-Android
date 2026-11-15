# ClipForge

ClipForge is a native Android workspace for preparing timestamped YouTube B-roll and scene clips. Organize clips into projects, paste a list of time ranges, download the requested segments, and export MP4 files into the phone's Movies library for use in video editors.

> Use this app only with videos and segments you are authorized to download and edit. YouTube and other source services may restrict downloading. ClipForge does not bypass DRM, private-video controls, or account restrictions.

## Features

- Projects keep each video's clip queue and exports grouped together.
- Bulk import accepts JSON or plain text; single-clip entry is also available.
- Review parsed clips, adjust cut points, and remove unwanted entries before saving.
- Background downloads use WorkManager, show per-clip progress, support retry/cancel, and resume pending clips.
- Choose 720p, 1080p, or best available source quality. Process one clip at a time or up to two at once.
- Preview exports in the app, share them, or open them in another video editor.
- Update the bundled stable yt-dlp engine from Settings.

## Get the app

Open the repository's **Releases** page and download `ClipForge-debug.apk`. On Android, open the downloaded APK and follow the install prompt. This arm64 debug build is signed with Android's development key and is not a Play Store release.

## Source and build

The complete Android Studio project is checked in directly at the repository root. Open this folder in Android Studio, or build from it with the included Gradle wrapper. The main source is in `app/`; dependency versions are in `gradle/libs.versions.toml`. See [Building and releasing](BUILDING.md) for setup and build instructions. The [project ZIP](ClipForge-Android-project.zip) remains available as a convenient download.

## Use the app

See the [user guide](USER_GUIDE.md) for project setup, supported import formats, queue controls, output location, and troubleshooting.

## Project notes

- App architecture and data flow: [Architecture](ARCHITECTURE.md)
- Dependency and media licensing notes: [Third-party notices](THIRD_PARTY_NOTICES.md)
- Version history: [Changelog](CHANGELOG.md)

## License

No license has been granted for the ClipForge source code. All rights are reserved unless the author adds a license file. Third-party components remain under their respective licenses; see [Third-party notices](THIRD_PARTY_NOTICES.md).

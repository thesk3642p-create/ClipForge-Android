# Architecture

ClipForge uses a small MVVM/repository structure around a Compose UI.

## Main modules

- `ui/screens` and `ui/components`: Compose Material 3 screens for projects, clip import, queue status, preview, and settings.
- `ui/ClipForgeViewModel.kt`: collects repository flows, saves project/clip changes, and starts queue/retry/cancel operations in `viewModelScope`.
- `domain/parser`: parses bulk JSON/plain-text scripts and normalizes timecodes/YouTube URLs into validated clip drafts.
- `data/ClipForgeRepository.kt`: coordinates Room operations for projects and clips.
- `data/local`: Room entities, DAO queries, and database configuration.
- `data/preferences`: DataStore values for quality and queue mode.
- `data/ClipQueue.kt`: creates constrained WorkManager requests and unique per-project work chains.
- `worker/ClipDownloadWorker.kt`: runs one segment download, publishes progress, retries errors, exports success, and updates clip state.
- `data/DownloadEngine.kt`: initializes yt-dlp/FFmpeg, builds section-download requests, copies completed MP4 segments into Android MediaStore, and cancels engine processes.

## Download lifecycle

1. An imported draft is validated and stored as a Room `ClipEntity` with `QUEUED` status.
2. **Download all** schedules `QUEUED` and `SCHEDULED` items through WorkManager. The unique chain serializes requests by default; the optional two-at-once setting groups requests in pairs.
3. A worker marks its clip `DOWNLOADING`, uses yt-dlp `--download-sections` with `--force-keyframes-at-cuts`, and writes temporary media in app cache.
4. The engine exports the MP4 through MediaStore to `Movies/ClipForge/<Project>/`.
5. Room records the media URI, byte count, and `COMPLETED` status. Failures retry and then become `FAILED`; pending states can be scheduled again.

## Persistence and permissions

Room stores project/clip metadata. DataStore stores preferences. WorkManager stores durable background work. MediaStore owns the exported video files; the app does not need broad storage write permission for its app-specific relative path. The manifest requests internet, foreground data-sync, notifications, and media-video read permissions.

## Dependencies and engine initialization

The app initializes the yt-dlp Android and FFmpeg libraries asynchronously from `ClipForgeApp`. Engine updates are explicit from Settings. Dependency coordinates and versions are in `gradle/libs.versions.toml`.

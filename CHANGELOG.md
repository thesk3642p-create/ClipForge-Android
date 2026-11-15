# Changelog

## 1.0.0

- Initial ClipForge Android project with project workspaces, bulk/single clip import, timestamp validation, WorkManager downloads, queue progress/retry/cancel, MediaStore exports, preview/share/open, quality settings, and yt-dlp engine update.
- Queue handling now includes pending scheduled clips when resuming and catches foreground-start errors inside worker retry handling so one clip's startup failure does not strand the rest of its chain.

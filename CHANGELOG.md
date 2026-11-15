# Changelog

## 1.0.2

- Export only yt-dlp's final merged `segment.mp4`; never select an intermediate format file that may contain audio only or video only.
- Validate the output video stream and segment duration before publishing it to Movies.
- Add a re-download action for ready clips so previously exported bad segments can be replaced without deleting the project entry first.

## 1.0.1

- Automatically refresh the stable yt-dlp engine on first launch and at least every 30 days, with background retry after 12 hours when an update attempt fails.
- If a segment fails with yt-dlp's 90-day outdated-engine warning, refresh the engine and retry that segment once before reporting failure.
- Show the actual yt-dlp `ERROR:` line in clip failure details instead of truncating the message at the outdated-engine warning.

## 1.0.0

- Initial ClipForge Android project with project workspaces, bulk/single clip import, timestamp validation, WorkManager downloads, queue progress/retry/cancel, MediaStore exports, preview/share/open, quality settings, and yt-dlp engine update.
- Queue handling now includes pending scheduled clips when resuming and catches foreground-start errors inside worker retry handling so one clip's startup failure does not strand the rest of its chain.

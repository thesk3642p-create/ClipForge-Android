# ClipForge user guide

## Create a project

1. Open ClipForge and create a project for the video you are editing.
2. Open the project and tap **Add clips**.
3. Choose bulk paste or single-clip entry.

Exports for a project are saved in `Movies/ClipForge/<Project Name>/` and indexed in the order they were added.

## Bulk paste

Paste one clip per line using pipes, or paste a JSON array/object. Lines that are not clip entries are ignored; malformed entries are reported in the preview.

```text
Alien Attack | https://www.youtube.com/watch?v=TAs7bFsnpNM | 00:02:46 - 00:06:47
Arrival | https://youtu.be/aqz-KE-bpKQ | 2:10 - 2:28
```

JSON accepts `label` (also `name` or `title`), `url` (also `link`, `videoUrl`, or `video_url`), `start` (also `startTime`, `start_time`, or `from`), and `end` (also `endTime`, `end_time`, or `to`). A top-level object can contain a `clips` array.

```json
[
  {"label":"Alien Attack","url":"https://www.youtube.com/watch?v=TAs7bFsnpNM","start":"00:02:46","end":"00:06:47"},
  {"label":"Arrival","url":"https://youtu.be/aqz-KE-bpKQ","start":"2:10","end":"2:28"}
]
```

Times may be `MM:SS` or `HH:MM:SS`; the end must be later than the start. Labels are cleaned for safe filenames and clips are numbered in script order. Review each parsed card before adding it to the project.

## Download and manage clips

- Tap **Download all** to enqueue pending clips. The default processes one clip at a time. Settings can allow up to two concurrent clips.
- Each card shows its queue/download state, progress, and estimated time when available.
- Cancel an individual queued or active clip from its card. Failed/canceled clips can be retried.
- If work was interrupted and cards remain queued, tap **Download all** again to resume pending work.
- Choose 720p, 1080p, or best available quality in Settings. Higher resolution can use more data and storage.
- Keep a working network connection and sufficient free space while downloading.

## Preview, share, and edit

Tap a completed clip or its **Preview** action. Use **Share** to send the MP4 to another app, or **Open in editor** to hand it to an installed video editor.

## Engine update

Open Settings and tap **Update yt-dlp Engine** if a source site changes and extraction stops working. This requires internet access.

## Troubleshooting

- **A clip fails:** Check that the URL is reachable and public, the timestamps are valid, and the source permits access. Try another quality, update the engine, then retry.
- **Queue appears idle:** Reopen the project and tap **Download all**. Android may defer background work while offline or under battery restrictions.
- **No editor appears:** Install an editor that accepts MP4 videos, then use **Share** or **Open in editor**.
- **Export missing:** Check `Movies/ClipForge/<Project Name>/` in a file/gallery app and confirm the phone has free storage.

ClipForge does not authenticate to private sources or bypass DRM or access controls. Respect copyright, platform terms, and permissions for every source clip.

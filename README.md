# SahraFlix 2.0

An Android TV–first IPTV player (Compose for TV, Media3/ExoPlayer, Room, Paging 3, Hilt, WorkManager)
for **your own** IPTV subscriptions (M3U, Xtream Codes, Stalker/Ministra portals), with a TMDB-powered
discovery layer that shows artwork, cast, trailers and **where each title is legally available**.

## Features
- **Sources:** M3U/M3U8 (incl. gzip, `#EXTVLCOPT` user-agent/referrer, `#EXTGRP`, `tvg-*`, catch-up attributes),
  Xtream Codes (auth/expiry check, categories, live/VOD/series, `get_series_info` episodes, `xmltv.php` guide,
  timeshift), Stalker portals (handshake → token → channels, on-demand `create_link`).
- **Setup:** type it on the TV, pair from a phone by QR code (PIN-protected, single-use, lockout), or one-press demo server.
- **Player:** shared OkHttp stack (http↔https redirects, per-stream headers), decoder fallback, automatic
  reconnect with backoff, live-window recovery, HLS fallback for extension-less links, audio/subtitle/quality
  selection, speed, aspect modes, external subtitles, PiP (API 26+), media session / notification controls,
  actionable error messages, "open in external player" (VLC/MX).
- **Home:** Continue watching (resume), favourite channels (long-press), On now (EPG), Live TV, Movies, Series,
  Trending movies & series (TMDB).
- **TV guide:** 8-hour grid (2 h back for catch-up), jump to live, play archived programmes.
- **Search:** channels, VOD, guide programmes and TMDB titles in one place.
- **Library matching:** a TMDB title page finds matching movies in your own IPTV VOD library ("Play from your library").
- **Background sync:** WorkManager, stale-channel cleanup, per-playlist status & error messages, configurable interval.
- **Profiles:** optional per-profile PIN (Android Keystore HMAC).

## Build
Requirements: Android Studio (Ladybug or newer), JDK 17, Android SDK 36.

1. Open the project and let Gradle sync.
2. *(Optional)* Get a free TMDB key at https://www.themoviedb.org/settings/api and add to `local.properties`:
   ```
   TMDB_API_KEY=your_v3_key_or_v4_read_token
   ```
3. Run on an Android TV emulator (API 30+ recommended) or device.

> **Note on versions.** Dependency versions were checked against upstream tags in Sept 2026, but this
> rebuild was written without access to Google's Maven repository, so it has **not been compiled yet**.
> Expect to fix a handful of small API mismatches on first build; Android Studio's upgrade assistant
> will flag them. Room schema JSONs are exported to `app/schemas/` on first build.

## Test server
```bash
python3 tools/test-server/server.py
```
Then in the app: **Settings → Add demo server** (emulator), or add Xtream `http://<LAN-IP>:8000`, `demo`/`demo`.
See [tools/test-server/README.md](tools/test-server/README.md).

## Tests
- JVM unit tests (`./gradlew test`): M3U parser, catch-up URL formatter, XMLTV time parsing, title matcher.
  These were compiled with kotlinc 2.2.21 and pass (17/17), including an end-to-end run against the test server.
- Room migration 7→8 should be verified with `MigrationTestHelper` on a device once the schema JSONs exist.

## Architecture
```
presentation/  Compose screens + ViewModels (PlayerViewModel is activity-scoped, shared via LocalPlayerViewModel)
domain/        models + repository contracts
data/
  parser/      pure-Kotlin M3U parser, title matcher (unit-tested)
  repository/  M3U / Xtream / Stalker providers, PlaylistSyncer, XMLTV sync, catch-up formatter
  local/       Room v8 (upserts, sync generations, playlist-scoped EPG)
  remote/      TMDB API (metadata, watch providers, videos)
  worker/      playlist + TMDB WorkManager jobs
player/        Media3 implementation, MediaSessionService, PiP actions
tools/test-server/  mock Xtream/M3U/XMLTV server
```

## Content & legal
SahraFlix is a player. It ships **no channels or content** and contains no scrapers for unlicensed
streaming sites. You are responsible for the sources you add. TMDB is used for metadata only; watch-provider
data is supplied by JustWatch via TMDB. *This product uses the TMDB API but is not endorsed or certified by TMDB.*

## Known limitations
- Stalker portals: live TV only (VOD/series catalogues are not synced).
- Xtream `get_vod_info` metadata (plot/cast per movie) is not fetched yet; TMDB covers movie details.
- Catch-up relies on provider support (`tv_archive`, `catchup-source`, or `catchup="shift"`).

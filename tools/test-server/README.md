# SahraFlix test IPTV server

A zero-dependency Python 3 mock of an IPTV provider for developing and testing SahraFlix.
It implements the parts of the **Xtream Codes** API the app uses, serves **M3U** playlists and an
**XMLTV** guide, and redirects streams to public, legally redistributable test media.

```bash
python3 tools/test-server/server.py            # http://0.0.0.0:8000
python3 tools/test-server/server.py --port 9000
```

| What | Address |
|---|---|
| Xtream login | server `http://<host>:8000`, user `demo`, password `demo` |
| M3U (Xtream style) | `/get.php?username=demo&password=demo&type=m3u_plus` |
| M3U (plain, gzipped EPG) | `/playlist.m3u` |
| XMLTV | `/xmltv.php?username=demo&password=demo`, `/epg.xml.gz` |
| Public free-to-air playlist | `/iptv-org` → https://iptv-org.github.io/iptv/index.m3u |

From the Android emulator the host is `10.0.2.2`, which is what **Settings → Add demo server** uses.
On a real TV/phone, set `TEST_SERVER_HOST=<your computer's LAN IP>` in `local.properties` or add the
server manually.

## What it exercises
- Live HLS (Akamai, Unified Streaming test streams), multi-audio + subtitles (Apple BipBop advanced), DASH (Akamai BBB).
- VOD (Blender open movies) with resume, series with seasons (`get_series_info`).
- Catch-up: channels 101/102 have `tv_archive=1`; `/timeshift/...` validates the URL format and serves a video.
- EPG: 48 h of hourly programmes (24 h past for catch-up, 24 h ahead).
- Failure paths: channel **901** → 404, **902** → 403 (max connections), wrong password → `auth: 0`,
  `expired/expired` → expired account, channel **903** → http→https redirect.

Stream targets are third-party test hosts; if one is down, the channel will fail in the app. That is
also a useful test of the error screen.

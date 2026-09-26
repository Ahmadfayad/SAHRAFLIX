#!/usr/bin/env python3
"""
SahraFlix mock IPTV server — Xtream Codes API + M3U + XMLTV for local testing.

Only uses the Python standard library. Streams are HTTP redirects to public, legally
redistributable test media (Apple/Mux HLS samples, Akamai/Unified live test streams,
Blender Foundation open movies on Google's sample bucket).

    python3 server.py                 # listens on 0.0.0.0:8000
    python3 server.py --port 9000

Emulator: the app reaches your computer at http://10.0.2.2:8000 ("Add demo server").
Real TV/phone: use your computer's LAN IP, e.g. http://192.168.1.20:8000.

Credentials: demo / demo   (expired / expired → expired account, anything else → auth failure)
"""
import argparse
import gzip
import io
import json
import time
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse
from xml.sax.saxutils import escape

USER, PASS = "demo", "demo"
GCS = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample"

LIVE_CATEGORIES = [("1", "News & Live Tests"), ("2", "HLS Samples"), ("3", "Diagnostics")]
# id, name, category, epg id, target url, archive(catch-up)
LIVE = [
    (101, "Akamai Live Test", "1", "akamai.live", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", 1),
    (102, "Unified Streaming Live", "1", "unified.live", "https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8", 1),
    (103, "Apple BipBop (multi-audio + subs)", "2", "apple.bipbop",
     "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8", 0),
    (104, "Mux Big Buck Bunny HLS", "2", "mux.bbb", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", 0),
    (105, "Akamai DASH (MPD)", "2", "akamai.dash", "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd", 0),
    # Diagnostics: exercise the app's error handling.
    (901, "Broken channel (404)", "3", "diag.404", None, 0),
    (902, "Forbidden channel (403)", "3", "diag.403", None, 0),
    (903, "Redirect http→https", "3", "diag.redirect", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", 0),
]

VOD_CATEGORIES = [("10", "Blender Open Movies")]
VOD = [  # id, name, category, url, year, plot
    (201, "Big Buck Bunny (2008)", "10", f"{GCS}/BigBuckBunny.mp4", 2008, "A giant rabbit takes revenge on three bullies."),
    (202, "Elephants Dream (2006)", "10", f"{GCS}/ElephantsDream.mp4", 2006, "Two men explore a surreal mechanical world."),
    (203, "Sintel (2010)", "10", f"{GCS}/Sintel.mp4", 2010, "A girl searches for her lost baby dragon."),
    (204, "Tears of Steel (2012)", "10", f"{GCS}/TearsOfSteel.mp4", 2012, "Scientists try to save the world from robots."),
]

SERIES_CATEGORIES = [("20", "Sample Series")]
SERIES = {  # series_id -> (name, category, {season: [(episode_id, title, url)]})
    301: ("Open Movie Shorts", "20", {
        1: [(3011, "For Bigger Blazes", f"{GCS}/ForBiggerBlazes.mp4"),
            (3012, "For Bigger Escapes", f"{GCS}/ForBiggerEscapes.mp4"),
            (3013, "For Bigger Fun", f"{GCS}/ForBiggerFun.mp4")],
        2: [(3021, "For Bigger Joyrides", f"{GCS}/ForBiggerJoyrides.mp4"),
            (3022, "For Bigger Meltdowns", f"{GCS}/ForBiggerMeltdowns.mp4")],
    }),
}

SHOWS = ["Morning Update", "Tech Talk", "World Report", "Nature Hour", "Sports Desk",
         "Film Club", "Science Now", "Late Edition"]


def xmltv(now=None) -> bytes:
    """Deterministic guide: 1-hour slots from 24 h ago to 24 h ahead for every live channel."""
    now = now or datetime.now(timezone.utc)
    base = now.replace(minute=0, second=0, microsecond=0) - timedelta(hours=24)
    fmt = "%Y%m%d%H%M%S +0000"
    out = io.StringIO()
    out.write('<?xml version="1.0" encoding="UTF-8"?>\n<tv generator-info-name="sahraflix-test-server">\n')
    for cid, name, _, epg, _, _ in LIVE:
        out.write(f'  <channel id="{escape(epg)}"><display-name>{escape(name)}</display-name></channel>\n')
    for i, (cid, name, _, epg, _, _) in enumerate(LIVE):
        for h in range(48):
            start = base + timedelta(hours=h)
            title = SHOWS[(h + i) % len(SHOWS)]
            out.write(
                f'  <programme start="{start.strftime(fmt)}" stop="{(start + timedelta(hours=1)).strftime(fmt)}" '
                f'channel="{escape(epg)}"><title>{escape(title)}</title>'
                f'<desc>{escape(title)} on {escape(name)} (test data).</desc></programme>\n')
    out.write("</tv>\n")
    return out.getvalue().encode("utf-8")


class Handler(BaseHTTPRequestHandler):
    server_version = "SahraFlixTestServer/1.0"

    # ---------- helpers ----------
    def host(self):
        return self.headers.get("Host") or f"127.0.0.1:{self.server.server_port}"

    def base(self):
        return f"http://{self.host()}"

    def send(self, code, body=b"", ctype="text/plain; charset=utf-8", extra=None):
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        for k, v in (extra or {}).items():
            self.send_header(k, v)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def json(self, obj):
        self.send(200, json.dumps(obj), "application/json")

    def redirect(self, url):
        self.send(302, "", extra={"Location": url})

    def authed(self, q):
        return q.get("username", [""])[0] == USER and q.get("password", [""])[0] == PASS

    def log_message(self, fmt, *args):
        print(f"[{self.log_date_time_string()}] {self.address_string()} {fmt % args}")

    # ---------- routing ----------
    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        url = urlparse(self.path)
        q = parse_qs(url.query)
        path = url.path.rstrip("/") or "/"
        parts = path.strip("/").split("/")
        try:
            if path == "/":
                return self.send(200, self.index(), "text/html; charset=utf-8")
            if path == "/player_api.php":
                return self.player_api(q)
            if path == "/get.php":
                if not self.authed(q):
                    return self.send(401, "Unauthorized")
                return self.send(200, self.m3u(xtream_urls=True), "audio/x-mpegurl")
            if path == "/playlist.m3u":  # plain M3U, no auth, gzipped EPG hint
                return self.send(200, self.m3u(xtream_urls=False), "audio/x-mpegurl")
            if path == "/xmltv.php":
                if not self.authed(q):
                    return self.send(401, "Unauthorized")
                return self.send(200, xmltv(), "application/xml")
            if path == "/epg.xml.gz":
                return self.send(200, gzip.compress(xmltv()), "application/gzip")
            if path == "/iptv-org":
                # Public, community-maintained list of free-to-air channels (github.com/iptv-org/iptv).
                return self.redirect("https://iptv-org.github.io/iptv/index.m3u")
            if len(parts) >= 4 and parts[0] in ("live", "movie", "series"):
                return self.stream(parts)
            if len(parts) >= 6 and parts[0] == "timeshift":
                return self.timeshift(parts)
            return self.send(404, "Not found")
        except BrokenPipeError:
            pass

    # ---------- Xtream API ----------
    def player_api(self, q):
        user, pwd = q.get("username", [""])[0], q.get("password", [""])[0]
        if user == "expired" and pwd == "expired":
            return self.json({"user_info": {"auth": 1, "status": "Expired", "exp_date": "1600000000"}})
        if not self.authed(q):
            return self.json({"user_info": {"auth": 0}})
        action = q.get("action", [""])[0]
        host, _, port = self.host().partition(":")
        if action == "":
            return self.json({
                "user_info": {
                    "username": USER, "password": PASS, "auth": 1, "status": "Active",
                    "exp_date": str(int(time.time()) + 365 * 86400), "is_trial": "0",
                    "active_cons": "0", "max_connections": "2",
                    "allowed_output_formats": ["m3u8", "ts"],
                },
                "server_info": {"url": host, "port": port or "80", "server_protocol": "http",
                                "timezone": "UTC", "timestamp_now": int(time.time())},
            })
        if action == "get_live_categories":
            return self.json([{"category_id": c, "category_name": n, "parent_id": 0} for c, n in LIVE_CATEGORIES])
        if action == "get_vod_categories":
            return self.json([{"category_id": c, "category_name": n, "parent_id": 0} for c, n in VOD_CATEGORIES])
        if action == "get_series_categories":
            return self.json([{"category_id": c, "category_name": n, "parent_id": 0} for c, n in SERIES_CATEGORIES])
        if action == "get_live_streams":
            return self.json([{
                "num": i + 1, "name": name, "stream_type": "live", "stream_id": sid,
                "stream_icon": "", "epg_channel_id": epg, "category_id": cat,
                "tv_archive": archive, "tv_archive_duration": 2 if archive else 0, "direct_source": "",
            } for i, (sid, name, cat, epg, _, archive) in enumerate(LIVE)])
        if action == "get_vod_streams":
            return self.json([{
                "num": i + 1, "name": name, "stream_type": "movie", "stream_id": vid, "stream_icon": "",
                "rating": "7.5", "category_id": cat, "container_extension": "mp4", "direct_source": "",
            } for i, (vid, name, cat, _, _, _) in enumerate(VOD)])
        if action == "get_vod_info":
            vid = int(q.get("vod_id", ["0"])[0])
            m = next((v for v in VOD if v[0] == vid), None)
            if not m:
                return self.json({})
            return self.json({"info": {"name": m[1], "plot": m[5], "releasedate": str(m[4])},
                              "movie_data": {"stream_id": vid, "container_extension": "mp4"}})
        if action == "get_series":
            return self.json([{"num": i + 1, "name": s[0], "series_id": sid, "cover": "", "category_id": s[1],
                               "plot": "Short open-movie clips.", "rating": "7"} for i, (sid, s) in enumerate(SERIES.items())])
        if action == "get_series_info":
            sid = int(q.get("series_id", ["0"])[0])
            s = SERIES.get(sid)
            if not s:
                return self.json({"episodes": {}})
            return self.json({
                "info": {"name": s[0], "plot": "Short open-movie clips (test data).", "rating": "7", "releaseDate": "2011-01-01"},
                "episodes": {str(season): [{
                    "id": str(eid), "episode_num": n + 1, "title": title, "container_extension": "mp4",
                    "season": season, "info": {"plot": f"{title} (sample)", "duration_secs": 15},
                } for n, (eid, title, _) in enumerate(eps)] for season, eps in s[2].items()},
            })
        if action == "get_short_epg":
            return self.json({"epg_listings": []})
        return self.json([])

    # ---------- streams ----------
    def stream(self, parts):
        kind, user, pwd, file = parts[0], parts[1], parts[2], parts[-1]
        if (user, pwd) != (USER, PASS):
            return self.send(401, "Unauthorized")
        sid = int(file.split(".")[0]) if file.split(".")[0].isdigit() else -1
        if kind == "live":
            ch = next((c for c in LIVE if c[0] == sid), None)
            if not ch:
                return self.send(404, "No such channel")
            if sid == 901:
                return self.send(404, "Channel offline")
            if sid == 902:
                return self.send(403, "Max connections reached")
            if sid == 903:  # http→https redirect: ExoPlayer's default HTTP stack refuses this
                return self.redirect(ch[4])
            return self.redirect(ch[4])
        if kind == "movie":
            m = next((v for v in VOD if v[0] == sid), None)
            return self.redirect(m[3]) if m else self.send(404, "No such movie")
        for _, (_, _, seasons) in SERIES.items():
            for eps in seasons.values():
                for eid, _, url in eps:
                    if eid == sid:
                        return self.redirect(url)
        return self.send(404, "No such episode")

    def timeshift(self, parts):
        # /timeshift/{user}/{pass}/{minutes}/{YYYY-MM-DD:HH-MM}/{id}.ts
        user, pwd, minutes, start = parts[1], parts[2], parts[3], parts[4]
        if (user, pwd) != (USER, PASS):
            return self.send(401, "Unauthorized")
        try:
            datetime.strptime(start, "%Y-%m-%d:%H-%M")
            int(minutes)
        except ValueError:
            return self.send(400, "Bad timeshift format (expected /{minutes}/{YYYY-MM-DD:HH-MM}/{id}.ts)")
        print(f"   ↳ catch-up request: start={start} duration={minutes}min")
        # No real archive exists; serve a VOD file so the catch-up path can be exercised end-to-end.
        return self.redirect(VOD[0][3])

    # ---------- M3U ----------
    def m3u(self, xtream_urls: bool) -> str:
        b = self.base()
        epg = f"{b}/xmltv.php?username={USER}&password={PASS}" if xtream_urls else f"{b}/epg.xml.gz"
        lines = [f'#EXTM3U url-tvg="{epg}"']
        cats = dict(LIVE_CATEGORIES)
        for i, (sid, name, cat, epg_id, _, archive) in enumerate(LIVE):
            catchup = (f' catchup="default" catchup-days="2" catchup-source="{b}/timeshift/{USER}/{PASS}/'
                       f'{{duration_min}}/{{Y}}-{{m}}-{{d}}:{{H}}-{{M}}/{sid}.ts"') if archive else ""
            lines.append(f'#EXTINF:-1 tvg-id="{epg_id}" tvg-chno="{i + 1}" group-title="{cats[cat]}"{catchup},{name}')
            lines.append(f"{b}/live/{USER}/{PASS}/{sid}.m3u8" if xtream_urls else f"{b}/live/{USER}/{PASS}/{sid}.m3u8")
        for vid, name, _, _, _, _ in VOD:
            lines.append(f'#EXTINF:-1 group-title="VOD: Movies",{name}')
            lines.append(f"{b}/movie/{USER}/{PASS}/{vid}.mp4")
        for _, (sname, _, seasons) in SERIES.items():
            for season, eps in seasons.items():
                for n, (eid, title, _) in enumerate(eps):
                    lines.append(f'#EXTINF:-1 group-title="Series: {sname}",{sname} S{season:02d}E{n + 1:02d} {title}')
                    lines.append(f"{b}/series/{USER}/{PASS}/{eid}.mp4")
        return "\n".join(lines) + "\n"

    def index(self):
        b = self.base()
        return f"""<!doctype html><meta charset=utf-8><title>SahraFlix test server</title>
<style>body{{font-family:sans-serif;max-width:720px;margin:40px auto;line-height:1.5}}code{{background:#eee;padding:2px 5px}}</style>
<h1>SahraFlix test IPTV server</h1>
<p><b>Xtream:</b> server <code>{b}</code>, user <code>demo</code>, password <code>demo</code></p>
<p><b>M3U (Xtream-style):</b> <code>{b}/get.php?username=demo&amp;password=demo&amp;type=m3u_plus</code></p>
<p><b>M3U (plain, gzipped EPG):</b> <code>{b}/playlist.m3u</code></p>
<p><b>XMLTV:</b> <code>{b}/xmltv.php?username=demo&amp;password=demo</code> · <code>{b}/epg.xml.gz</code></p>
<p><b>Public free-to-air list:</b> <code>{b}/iptv-org</code> → iptv-org/iptv on GitHub</p>
<p>Failure cases: login <code>expired/expired</code>, wrong password, channels 901 (404) &amp; 902 (403).</p>"""


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("--port", type=int, default=8000)
    args = ap.parse_args()
    srv = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"SahraFlix test server on http://{args.host}:{args.port}  (emulator: http://10.0.2.2:{args.port}, user demo / pass demo)")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()

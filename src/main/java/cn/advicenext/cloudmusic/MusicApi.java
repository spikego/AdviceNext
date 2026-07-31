package cn.advicenext.cloudmusic;

import cn.advicenext.cloudmusic.MusicModels.LyricResult;
import cn.advicenext.utility.client.http.HttpUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MusicApi {
    private String baseUrl = "https://music.163.com";
    private static final String BASE_COOKIE = "appver=2.7.1.198277; os=pc;";
    private String userCookie = "";
    private MusicModels.UserProfile userProfile = new MusicModels.UserProfile();
    private boolean debug = true;
    private static MusicApi instance;
    public static MusicApi get() { if (instance == null) instance = new MusicApi(); return instance; }
    public String getBaseUrl() { return baseUrl; }
    public void setCookie(String c) { this.userCookie = c; }
    public String getCookie() { return userCookie; }
    public boolean isLoggedIn() { return userCookie != null && !userCookie.isEmpty() && userCookie.contains("MUSIC_U"); }
    public MusicModels.UserProfile getUserProfile() { return userProfile; }
    public void setDebug(boolean debug) { this.debug = debug; }

    private String fullCookie() {
        if (userCookie != null && !userCookie.isEmpty()) return BASE_COOKIE + userCookie;
        return BASE_COOKIE;
    }

    private void log(String msg) {
        if (debug) System.out.println("[MusicApi] " + msg);
    }

    private void logUrl(String method, String url, String body) {
        if (!debug) return;
        log(method + " " + url);
        if (body != null && !body.isEmpty()) {
            String truncated = body.length() > 300 ? body.substring(0, 300) + "..." : body;
            log("  body: " + truncated);
        }
    }

    private void logResponse(HttpUtils.Response r) {
        if (!debug) return;
        if (r == null) { log("response: null"); return; }
        log("  -> " + r.code() + " (" + (r.isSuccess() ? "OK" : "FAIL") + ")");
        String body = r.body();
        if (body != null) {
            if (body.length() > 500) body = body.substring(0, 500) + "...";
            log("  body: " + body);
        }
    }

    private Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36");
        h.put("Accept", "*/*");
        h.put("Accept-Language", "zh-CN,zh;q=0.8,gl;q=0.6,zh-TW;q=0.4");
        h.put("Connection", "keep-alive");
        h.put("Referer", "http://music.163.com");
        h.put("Host", "music.163.com");
        h.put("Cookie", fullCookie());
        return h;
    }

    private Map<String, String> postHeaders() {
        Map<String, String> h = headers();
        h.put("Content-Type", "application/x-www-form-urlencoded");
        return h;
    }

    private void post(String path, Map<String, String> params, Consumer<JsonObject> cb) {
        post(path, params, cb, 0);
    }

    private void post(String path, Map<String, String> params, Consumer<JsonObject> cb, int retry) {
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                if (params != null) {
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        if (!sb.isEmpty()) sb.append('&');
                        sb.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
                    }
                }
                String url = baseUrl + path;
                if (retry == 0) logUrl("POST", url, sb.toString());
                else log("POST retry " + retry + "/2 " + url);
                HttpUtils.Response r = HttpUtils.request(url, "POST", sb.toString(), postHeaders());
                logResponse(r);
                extractCookieFromResponse(r);
                if (r != null && r.isSuccess() && r.body() != null)
                    cb.accept(JsonParser.parseString(r.body()).getAsJsonObject());
                else if (r != null && r.code() == -1 && retry < 2) {
                    try { Thread.sleep(1500 * (retry + 1)); } catch (InterruptedException ignored) {}
                    post(path, params, cb, retry + 1);
                }
                else log("post failed: " + (r != null ? r.code() : "null response"));
            } catch (Exception e) { log("post error: " + e.getMessage()); }
        });
    }

    private void extractCookieFromResponse(HttpUtils.Response r) {
        if (r == null) return;
        List<String> setCookies = r.getSetCookies();
        if (setCookies.isEmpty()) return;
        Map<String, String> cookieMap = new HashMap<>();
        if (userCookie != null && !userCookie.isEmpty()) {
            for (String part : userCookie.split("; ")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) cookieMap.put(kv[0], kv[1]);
                else if (kv.length == 1) cookieMap.put(kv[0], "");
            }
        }
        for (String cookie : setCookies) {
            String[] parts = cookie.split("; ");
            if (parts.length == 0) continue;
            String[] kv = parts[0].split("=", 2);
            if (kv.length == 2) cookieMap.put(kv[0], kv[1]);
            else if (kv.length == 1) cookieMap.put(kv[0], "");
        }
        StringBuilder newCookie = new StringBuilder();
        for (Map.Entry<String, String> e : cookieMap.entrySet()) {
            if (newCookie.length() > 0) newCookie.append("; ");
            newCookie.append(e.getKey()).append("=").append(e.getValue());
        }
        if (newCookie.length() > 0) {
            userCookie = newCookie.toString();
            if (debug) {
                boolean hasMusicU = userCookie.contains("MUSIC_U");
                log("  cookie: " + (userCookie.length() > 100 ? userCookie.substring(0, 100) + "..." : userCookie) + (hasMusicU ? " [MUSIC_U present]" : ""));
            }
        }
    }

    private void get(String path, Consumer<JsonObject> cb) {
        get(path, cb, 0);
    }

    private void get(String path, Consumer<JsonObject> cb, int retry) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = baseUrl + path;
                if (retry == 0) logUrl("GET", url, null);
                else log("GET retry " + retry + "/2 " + url);
                HttpUtils.Response r = HttpUtils.request(url, "GET", null, headers());
                logResponse(r);
                extractCookieFromResponse(r);
                if (r != null && r.isSuccess() && r.body() != null)
                    cb.accept(JsonParser.parseString(r.body()).getAsJsonObject());
                else if (r != null && r.code() == -1 && retry < 2) {
                    try { Thread.sleep(1500 * (retry + 1)); } catch (InterruptedException ignored) {}
                    get(path, cb, retry + 1);
                }
                else log("get failed: " + (r != null ? r.code() : "null response"));
            } catch (Exception e) { log("get error: " + e.getMessage()); }
        });
    }

    private void get(String path, Map<String, String> params, Consumer<JsonObject> cb) {
        StringBuilder sb = new StringBuilder(path).append('?');
        params.forEach((k, v) -> {
            if (sb.length() > path.length() + 1) sb.append('&');
            sb.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        get(sb.toString(), cb);
    }

    public void getQRKey(Consumer<String> cb) {
        post("/api/login/qrcode/unikey", Map.of("type", "1"), obj -> {
            String key = MusicModels.getString(obj, "unikey");
            if (key != null && !key.isEmpty()) {
                cb.accept(key);
            } else {
                log("getQRKey failed: " + obj);
            }
        });
    }

    public String getQRCodeUrl(String key) {
        return "https://music.163.com/login?codekey=" + key;
    }

    public void checkQR(String key, Consumer<MusicModels.QRLoginState> cb) {
        post("/api/login/qrcode/client/login", Map.of("key", key, "type", "1"), obj -> {
            MusicModels.QRLoginState s = new MusicModels.QRLoginState();
            s.key = key;
            int code = MusicModels.getInt(obj, "code");
            switch (code) {
                case 800: s.status = MusicModels.QRLoginState.Status.EXPIRED; break;
                case 801: s.status = MusicModels.QRLoginState.Status.WAITING; break;
                case 802: s.status = MusicModels.QRLoginState.Status.SCANNED;
                    s.nickname = MusicModels.getString(obj, "nickname");
                    s.avatarUrl = MusicModels.getString(obj, "avatarUrl"); break;
                case 803: s.status = MusicModels.QRLoginState.Status.CONFIRMED;
                    s.cookie = userCookie; break;
                default: s.status = MusicModels.QRLoginState.Status.ERROR;
                    s.message = MusicModels.getString(obj, "message");
            }
            cb.accept(s);
        });
    }

    public void getLoginStatus(Consumer<JsonObject> cb) {
        post("/api/w/nuser/account/get", Map.of(), obj -> {
            JsonElement accountEl = obj.get("account");
            JsonElement profileEl = obj.get("profile");
            JsonObject account = (accountEl != null && accountEl.isJsonObject()) ? accountEl.getAsJsonObject() : null;
            JsonObject profile = (profileEl != null && profileEl.isJsonObject()) ? profileEl.getAsJsonObject() : null;
            if (account != null && profile != null) {
                userProfile.userId = MusicModels.getLong(account, "id");
                userProfile.nickname = MusicModels.getString(profile, "nickname");
                userProfile.avatarUrl = MusicModels.getString(profile, "avatarUrl");
                userProfile.vipType = MusicModels.getInt(account, "vipType");
                userProfile.loaded = true;
                JsonObject result = new JsonObject();
                result.add("account", account);
                result.add("profile", profile);
                cb.accept(result);
            } else {
                cb.accept(obj);
            }
        });
    }

    public void search(String kw, int limit, int offset, Consumer<MusicModels.SearchResult> cb) {
        post("/api/cloudsearch/pc", Map.of("s", kw, "type", "1",
            "limit", String.valueOf(limit), "offset", String.valueOf(offset),
            "total", "true"), obj -> {
            MusicModels.SearchResult r = new MusicModels.SearchResult();
            JsonObject result = obj.getAsJsonObject("result");
            if (result != null) {
                r.totalCount = MusicModels.getInt(result, "songCount");
                JsonElement hm = result.get("hasMore");
                r.hasMore = hm != null && !hm.isJsonNull() && hm.getAsBoolean();
                JsonArray songs = result.getAsJsonArray("songs");
                if (songs != null) r.songs = MusicModels.parseSongList(songs);
            }
            cb.accept(r);
        });
    }

    public void search(String kw, Consumer<MusicModels.SearchResult> cb) { search(kw, 30, 0, cb); }

    public void getSongUrl(long id, Consumer<String> cb) {
        getSongUrl(id, "standard", cb);
    }

    public void getSongUrl(long id, String level, Consumer<String> cb) {
        songUrlApiPost("/api/song/enhance/player/url/v1", Map.of("ids", "[" + id + "]",
            "level", level, "encodeType", "mp3"), obj -> {
            JsonArray data = obj.getAsJsonArray("data");
            String url = null;
            if (data != null && !data.isEmpty()) {
                url = MusicModels.getString(data.get(0).getAsJsonObject(), "url");
            }
            cb.accept(url);
        });
    }

    private void songUrlApiPost(String path, Map<String, String> params, Consumer<JsonObject> cb) {
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                if (params != null) {
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        if (!sb.isEmpty()) sb.append('&');
                        sb.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
                    }
                }
                String url = "https://interface3.music.163.com" + path;
                logUrl("POST", url, sb.toString());
                Map<String, String> h = postHeaders();
                h.put("Host", "interface3.music.163.com");
                h.put("Referer", "https://music.163.com/");
                HttpUtils.Response r = HttpUtils.request(url, "POST", sb.toString(), h);
                logResponse(r);
                if (r != null && r.isSuccess() && r.body() != null)
                    cb.accept(JsonParser.parseString(r.body()).getAsJsonObject());
                else log("songUrlApi post failed: " + (r != null ? r.code() : "null response"));
            } catch (Exception e) { log("songUrlApi post error: " + e.getMessage()); }
        });
    }

    public void getSongDetail(long id, Consumer<MusicModels.Song> cb) {
        post("/api/v3/song/detail", Map.of("c", "[{\"id\":" + id + "}]"), obj -> {
            JsonArray songs = obj.getAsJsonArray("songs");
            if (songs != null && !songs.isEmpty())
                cb.accept(MusicModels.parseSong(songs.get(0).getAsJsonObject()));
        });
    }

    public void getSongUrlAndDetail(long id, Consumer<MusicModels.Song> cb) {
        getSongUrl(id, url -> {
            if (url == null) { cb.accept(null); return; }
            getSongDetail(id, song -> { song.url = url; cb.accept(song); });
        });
    }

    public void getLyric(long id, Consumer<MusicModels.LyricResult> cb) {
        post("/api/song/lyric", Map.of("id", String.valueOf(id),
            "lv", "0", "tv", "0"), obj -> {
            JsonObject lrc = obj.getAsJsonObject("lrc");
            if (lrc != null) {
                LyricResult r = MusicModels.parseLyric(lrc);
                JsonObject tlrc = obj.getAsJsonObject("tlyric");
                if (tlrc != null) {
                    r.rawTLyric = MusicModels.getRawLyricStr(tlrc);
                    r.hasTranslation = r.rawTLyric != null && !r.rawTLyric.isEmpty();
                }
                cb.accept(r);
            } else {
                log("getLyric: no lrc in response for id=" + id);
            }
        });
    }

    public void getDailyRecommend(Consumer<List<MusicModels.Song>> cb) {
        post("/api/v1/discovery/recommend/songs", Map.of(), obj -> {
            List<MusicModels.Song> songs = new ArrayList<>();
            JsonArray arr = null;
            JsonObject data = obj.getAsJsonObject("data");
            if (data != null) {
                arr = data.getAsJsonArray("dailySongs");
            }
            if (arr == null || arr.isEmpty()) arr = obj.getAsJsonArray("recommend");
            if (arr != null) for (JsonElement e : arr) {
                songs.add(MusicModels.parseSongSimple(e.getAsJsonObject()));
            }
            if (songs.isEmpty()) {
                log("daily recommend empty, trying /personalized/newsong...");
                getPersonalizedNewSongs(cb);
            } else {
                cb.accept(songs);
            }
        });
    }

    private void getPersonalizedNewSongs(Consumer<List<MusicModels.Song>> cb) {
        post("/api/personalized/newsong", Map.of("limit", "30"), obj -> {
            List<MusicModels.Song> songs = new ArrayList<>();
            JsonArray arr = obj.getAsJsonArray("result");
            if (arr != null) for (JsonElement e : arr) {
                JsonObject item = e.getAsJsonObject();
                JsonObject songObj = item.getAsJsonObject("song");
                if (songObj != null) {
                    MusicModels.Song s = MusicModels.parseSongSimple(songObj);
                    if (s.albumPicUrl.isEmpty()) {
                        s.albumPicUrl = MusicModels.getString(item, "picUrl");
                    }
                    songs.add(s);
                }
            }
            cb.accept(songs);
        });
    }

    public void getLikedSongs(long uid, Consumer<List<MusicModels.Song>> cb) {
        post("/api/user/playlist", Map.of("uid", String.valueOf(uid),
            "limit", "1", "offset", "0"), obj -> {
            JsonArray plist = obj.getAsJsonArray("playlist");
            if (plist == null || plist.isEmpty()) {
                log("getLikedSongs: no playlist found for uid " + uid);
                cb.accept(new ArrayList<>());
                return;
            }
            JsonObject fp = plist.get(0).getAsJsonObject();
            long likedId = fp.get("id").getAsLong();
            log("getLikedSongs: found liked playlist id=" + likedId + " name=" + fp.get("name").getAsString());
            getPlaylistDetail(likedId, pl -> {
                cb.accept(pl != null ? pl.tracks : new ArrayList<>());
            });
        });
    }

    private void getSongDetailList(String ids, Consumer<List<MusicModels.Song>> cb) {
        post("/api/v3/song/detail", Map.of("c", "[{\"id\":" + ids.replace(",", "},{\"id\":") + "}]"), obj -> {
            JsonArray songs = obj.getAsJsonArray("songs");
            cb.accept(songs != null ? MusicModels.parseSongList(songs) : new ArrayList<>());
        });
    }

    public void getDjRecommend(Consumer<List<MusicModels.RadioStation>> cb) {
        post("/api/djradio/hot/v1", Map.of("limit", "30"), obj -> {
            List<MusicModels.RadioStation> list = new ArrayList<>();
            JsonArray arr = obj.getAsJsonArray("djRadios");
            if (arr == null) arr = obj.getAsJsonArray("data");
            if (arr != null) for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                MusicModels.RadioStation rs = new MusicModels.RadioStation();
                rs.id = MusicModels.getLong(o, "id");
                rs.name = MusicModels.getString(o, "name");
                rs.picUrl = MusicModels.getString(o, "picUrl");
                rs.category = MusicModels.getString(o, "category");
                if (rs.category == null || rs.category.isEmpty()) {
                    rs.category = MusicModels.getString(o, "rcmdText");
                }
                list.add(rs);
            }
            cb.accept(list);
        });
    }

    public void getDjPrograms(long rid, Consumer<List<MusicModels.Song>> cb) {
        post("/api/dj/program/byradio", Map.of("radioId", String.valueOf(rid),
            "limit", "30", "offset", "0"), obj -> {
            List<MusicModels.Song> list = new ArrayList<>();
            JsonArray progs = obj.getAsJsonArray("programs");
            if (progs != null) for (JsonElement e : progs) {
                JsonObject mainSong = e.getAsJsonObject().getAsJsonObject("mainSong");
                if (mainSong != null) list.add(MusicModels.parseSong(mainSong));
            }
            cb.accept(list);
        });
    }

    public void getPlaylistDetail(long id, Consumer<MusicModels.Playlist> cb) {
        post("/api/v6/playlist/detail", Map.of("id", String.valueOf(id), "n", "1000"), obj -> {
            JsonObject pl = obj.getAsJsonObject("playlist");
            if (pl != null) cb.accept(MusicModels.parsePlaylist(pl));
        });
    }

    public void getRecommendPlaylists(Consumer<List<MusicModels.Playlist>> cb) {
        post("/api/toplist", Map.of(), obj -> {
            List<MusicModels.Playlist> list = new ArrayList<>();
            JsonArray arr = obj.getAsJsonArray("list");
            if (arr != null) for (JsonElement e : arr) {
                if (e == null || e.isJsonNull()) continue;
                JsonObject o = e.getAsJsonObject();
                MusicModels.Playlist pl = new MusicModels.Playlist();
                pl.id = MusicModels.getLong(o, "id");
                pl.name = MusicModels.getString(o, "name");
                pl.coverImgUrl = MusicModels.getString(o, "coverImgUrl");
                pl.trackCount = MusicModels.getInt(o, "trackCount");
                pl.playCount = MusicModels.getLong(o, "playCount");
                pl.description = MusicModels.getString(o, "description");
                JsonObject creator = o.getAsJsonObject("creator");
                if (creator != null) {
                    pl.creator = MusicModels.getString(creator, "nickname");
                }
                list.add(pl);
            }
            cb.accept(list);
        });
    }

    public void likeSong(long id, boolean like, Runnable cb) {
        post("/api/radio/like", Map.of("alg", "itembased",
            "trackId", String.valueOf(id), "like", like ? "true" : "false",
            "time", "25"), obj -> { if (cb != null) cb.run(); });
    }
}
package cn.advicenext.cloudmusic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class MusicModels {

    public static class Song {
        public long id;
        public String name = "";
        public String artist = "";
        public String album = "";
        public String albumPicUrl = "";
        public long duration;
        public String url;
        public boolean playable = true;
        public boolean liked;

        public String getArtistText() { return artist != null && !artist.isEmpty() ? artist : "Unknown"; }
        public String getDurationText() {
            long sec = duration / 1000;
            return String.format("%d:%02d", sec / 60, sec % 60);
        }
        @Override public boolean equals(Object o) { return o instanceof Song s && id == s.id; }
        @Override public int hashCode() { return Long.hashCode(id); }
    }

    public static class Playlist {
        public long id;
        public String name = "";
        public String coverImgUrl = "";
        public int trackCount;
        public long playCount;
        public String creator = "";
        public String description = "";
        public List<Song> tracks = new ArrayList<>();
        public boolean subscribed;
    }

    public static class RadioStation {
        public long id;
        public String name = "";
        public String picUrl = "";
        public String category = "";
    }

    public static class LyricLine {
        public long time;
        public String text;
        public LyricLine(long time, String text) { this.time = time; this.text = text; }
    }

    public static class LyricResult {
        public List<LyricLine> lines = new ArrayList<>();
        public String rawLyric;
        public String rawTLyric;
        public boolean hasTranslation;
    }

    public static class QRLoginState {
        public enum Status { WAITING, SCANNED, EXPIRED, CONFIRMED, ERROR }
        public Status status = Status.WAITING;
        public String key = "";
        public String qrImageUrl = "";
        public String cookie = "";
        public long userId;
        public String nickname = "";
        public String avatarUrl = "";
        public String message = "";
    }

    public static class SearchResult {
        public List<Song> songs = new ArrayList<>();
        public int totalCount;
        public boolean hasMore;
    }

    public static class UserProfile {
        public long userId;
        public String nickname = "";
        public String avatarUrl = "";
        public int vipType;
        public boolean loaded;
    }

    public static Song parseSong(JsonObject obj) {
        Song s = new Song();
        s.id = getLong(obj, "id");
        s.name = getString(obj, "name");
        s.duration = getLong(obj, "dt");
        JsonArray ar = obj.getAsJsonArray("ar");
        if (ar != null) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement e : ar) {
                if (!sb.isEmpty()) sb.append(" / ");
                sb.append(getString(e.getAsJsonObject(), "name"));
            }
            s.artist = sb.toString();
        }
        JsonObject al = obj.getAsJsonObject("al");
        if (al != null) { s.album = getString(al, "name"); s.albumPicUrl = getString(al, "picUrl"); }
        JsonObject priv = obj.getAsJsonObject("privilege");
        if (priv != null) s.playable = priv.has("st") && priv.get("st").getAsInt() >= 0;
        return s;
    }

    public static Song parseSongSimple(JsonObject obj) {
        Song s = new Song();
        s.id = getLong(obj, "id");
        s.name = getString(obj, "name");
        s.duration = getLong(obj, "duration");
        s.albumPicUrl = getString(obj, "picUrl");
        StringBuilder sb = new StringBuilder();
        JsonArray artists = obj.getAsJsonArray("artists");
        if (artists != null) for (JsonElement e : artists) {
            if (!sb.isEmpty()) sb.append(" / ");
            sb.append(getString(e.getAsJsonObject(), "name"));
        }
        s.artist = sb.toString();
        JsonObject album = obj.getAsJsonObject("album");
        if (album != null) {
            s.album = getString(album, "name");
            if (s.albumPicUrl.isEmpty()) s.albumPicUrl = getString(album, "picUrl");
        }
        return s;
    }

    public static List<Song> parseSongList(JsonArray arr) {
        List<Song> list = new ArrayList<>();
        if (arr == null) return list;
        for (JsonElement e : arr) list.add(parseSong(e.getAsJsonObject()));
        return list;
    }

    public static LyricResult parseLyric(JsonObject lrcObj) {
        LyricResult r = new LyricResult();
        r.rawLyric = getString(lrcObj, "lyric");
        String lrc = r.rawLyric;
        if (lrc != null) for (String line : lrc.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || !line.startsWith("[")) continue;
            int end = line.indexOf(']');
            if (end < 0) continue;
            String text = line.substring(end + 1).trim();
            if (text.isEmpty()) continue;
            r.lines.add(new LyricLine(parseLrcTime(line.substring(1, end)), text));
        }
        return r;
    }

    public static String getRawLyricStr(JsonObject lrcObj) {
        return getString(lrcObj, "lyric");
    }

    private static long parseLrcTime(String tag) {
        try {
            String[] p = tag.split(":");
            String[] s = p[1].split("\\.");
            return (Integer.parseInt(p[0]) * 60L + Integer.parseInt(s[0])) * 1000L + (s.length > 1 ? Integer.parseInt(s[1]) : 0);
        } catch (Exception e) { return 0; }
    }

    public static Playlist parsePlaylist(JsonObject obj) {
        Playlist p = new Playlist();
        p.id = getLong(obj, "id");
        p.name = getString(obj, "name");
        p.coverImgUrl = getString(obj, "coverImgUrl");
        p.trackCount = getInt(obj, "trackCount");
        p.playCount = getLong(obj, "playCount");
        p.subscribed = obj.has("subscribed") && obj.get("subscribed").getAsBoolean();
        JsonObject cr = obj.getAsJsonObject("creator");
        if (cr != null) p.creator = getString(cr, "nickname");
        JsonArray tr = obj.getAsJsonArray("tracks");
        if (tr != null) for (JsonElement e : tr) p.tracks.add(parseSong(e.getAsJsonObject()));
        return p;
    }

    public static String getString(JsonObject obj, String key) { return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : ""; }
    public static String getString(JsonObject obj, String key, String subKey) {
        if (!obj.has(key)) return "";
        JsonElement e = obj.get(key);
        return e.isJsonObject() ? getString(e.getAsJsonObject(), subKey) : e.getAsString();
    }
    public static long getLong(JsonObject obj, String key) { return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : 0; }
    public static int getInt(JsonObject obj, String key) { return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : 0; }
}
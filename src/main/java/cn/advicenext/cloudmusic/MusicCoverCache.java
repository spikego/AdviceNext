package cn.advicenext.cloudmusic;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

public class MusicCoverCache {
    private static final Map<String, BufferedImage> cache = new HashMap<>();
    private static final Map<String, Boolean> loading = new HashMap<>();
    private static final BufferedImage PLACEHOLDER;

    static {
        PLACEHOLDER = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++)
                PLACEHOLDER.setRGB(x, y, 0xFF2a2a2a);
    }

    public static BufferedImage get(String url) {
        if (url == null || url.isEmpty()) return PLACEHOLDER;
        BufferedImage cached = cache.get(url);
        if (cached != null) return cached;
        if (!loading.containsKey(url)) {
            loading.put(url, true);
            CompletableFuture.runAsync(() -> download(url));
        }
        return PLACEHOLDER;
    }

    public static boolean isLoaded(String url) {
        return cache.containsKey(url);
    }

    private static void download(String url) {
        try {
            String imgUrl = url + "?param=128y128";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(imgUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Referer", "https://music.163.com/");
            byte[] bytes;
            try (java.io.InputStream is = conn.getInputStream();
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                bytes = baos.toByteArray();
            }
            if (bytes != null) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) cache.put(url, img);
            }
        } catch (Exception e) {
            System.out.println("[MusicCoverCache] download failed: " + url);
        } finally {
            loading.remove(url);
        }
    }

    public static void clear() {
        cache.clear();
        loading.clear();
    }
}
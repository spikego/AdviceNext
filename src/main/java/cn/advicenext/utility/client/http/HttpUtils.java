package cn.advicenext.utility.client.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class HttpUtils {
    
    private static final int TIMEOUT = 10000;
    private static final String USER_AGENT = "AdviceNext/1.0";
    
    public static String get(String url) {
        return get(url, null);
    }
    
    public static String get(String url, Map<String, String> headers) {
        try {
            HttpURLConnection conn = createConnection(url, "GET");
            setHeaders(conn, headers);
            
            return readResponse(conn);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String post(String url, String data) {
        return post(url, data, null);
    }
    
    public static String post(String url, String data, Map<String, String> headers) {
        try {
            HttpURLConnection conn = createConnection(url, "POST");
            conn.setDoOutput(true);
            setHeaders(conn, headers);
            
            if (data != null) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            return readResponse(conn);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String put(String url, String data, Map<String, String> headers) {
        try {
            HttpURLConnection conn = createConnection(url, "PUT");
            conn.setDoOutput(true);
            setHeaders(conn, headers);
            
            if (data != null) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            return readResponse(conn);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String delete(String url, Map<String, String> headers) {
        try {
            HttpURLConnection conn = createConnection(url, "DELETE");
            setHeaders(conn, headers);
            
            return readResponse(conn);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static int getResponseCode(String url) {
        try {
            HttpURLConnection conn = createConnection(url, "HEAD");
            return conn.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }
    
    public static boolean isUrlReachable(String url) {
        return getResponseCode(url) == 200;
    }
    
    private static HttpURLConnection createConnection(String url, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        return conn;
    }
    
    private static void setHeaders(HttpURLConnection conn, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }
    
    private static String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
        }
        
        return response.toString().trim();
    }

    public record Response(int code, String body, Map<String, List<String>> rawHeaders) {
        public boolean isSuccess() {
            return code >= 200 && code < 300;
        }

        public Map<String, String> headers() {
            return rawHeaders.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey() != null ? e.getKey() : "Status",
                e -> String.join(", ", e.getValue()),
                (a, b) -> a));
        }

        public List<String> getSetCookies() {
            for (Map.Entry<String, List<String>> e : rawHeaders.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase("Set-Cookie")) {
                    return e.getValue();
                }
            }
            return Collections.emptyList();
        }
    }
    
    public static Response request(String url, String method, String data, Map<String, String> headers) {
        String currentUrl = url;
        int maxRedirects = 5;
        for (int redirect = 0; redirect < maxRedirects; redirect++) {
            try {
                HttpURLConnection conn = createConnection(currentUrl, method);
                conn.setInstanceFollowRedirects(false);
                setHeaders(conn, headers);

                if (data != null && ("POST".equals(method) || "PUT".equals(method))) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(data.getBytes(StandardCharsets.UTF_8));
                    }
                }

                int code = conn.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = conn.getHeaderField("Location");
                    if (location != null) {
                        currentUrl = location.startsWith("/") ? 
                            new URL(new URL(url), location).toString() : location;
                        // For GET/HEAD, follow redirects. For POST, only 303 changes to GET.
                        if (method.equals("POST") && code == 303) method = "GET";
                        data = null; // Don't resend POST data on redirect
                        continue;
                    }
                }

                String body = readResponse(conn);
                Map<String, List<String>> rawHeaders = conn.getHeaderFields();

                return new Response(code, body, rawHeaders);
            } catch (Exception e) {
                return new Response(-1, e.getMessage(), null);
            }
        }
        return new Response(-1, "Too many redirects", null);
    }

    public static byte[] downloadBytes(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
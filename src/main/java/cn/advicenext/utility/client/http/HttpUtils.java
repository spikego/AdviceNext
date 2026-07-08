package cn.advicenext.utility.client.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

    public record Response(int code, String body, Map<String, String> headers) {
        public boolean isSuccess() {
            return code >= 200 && code < 300;
        }
        }
    
    public static Response request(String url, String method, String data, Map<String, String> headers) {
        try {
            HttpURLConnection conn = createConnection(url, method);
            setHeaders(conn, headers);
            
            if (data != null && ("POST".equals(method) || "PUT".equals(method))) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            int code = conn.getResponseCode();
            String body = readResponse(conn);
            Map<String, String> responseHeaders = conn.getHeaderFields().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    e -> e.getKey() != null ? e.getKey() : "Status",
                    e -> String.join(", ", e.getValue())
                ));
            
            return new Response(code, body, responseHeaders);
        } catch (Exception e) {
            return new Response(-1, e.getMessage(), null);
        }
    }
}
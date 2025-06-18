package cn.advicenext.utility;

public class Vec2dUtils {

    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public static double dot(double x1, double y1, double x2, double y2) {
        return x1 * x2 + y1 * y2;
    }

    public static double angle(double x1, double y1, double x2, double y2) {
        double dot = dot(x1, y1, x2, y2);
        double len1 = length(x1, y1);
        double len2 = length(x2, y2);
        return Math.acos(dot / (len1 * len2));
    }

    // 可根据需要扩展更多2D向量操作
}
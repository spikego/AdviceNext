package cn.advicenext.gui.musicplayer;

public enum MusicTheme {
    CHROME_LIGHT(0xFFF8F9FA, 0xFF1A73E8, 0xFFEA4335, 0xFF34A853, 0xFFFBBC04, 0xFF3C4043, 0xFF5F6368, 0xFFDADCE0, 0xFFE8EAED),
    CHROME_DARK(0xFF202124, 0xFF8AB4F8, 0xFFF28B82, 0xFF81C995, 0xFFFDD663, 0xFFE8EAED, 0xFF9AA0A6, 0xFF3C4043, 0xFF303134),
    OCEAN(0xFFE8F5E9, 0xFF00897B, 0xFFE53935, 0xFF43A047, 0xFFFFA726, 0xFF263238, 0xFF546E7A, 0xFFB0BEC5, 0xFFCFD8DC),
    SUNSET(0xFFFFF3E0, 0xFFE65100, 0xFFD32F2F, 0xFF2E7D32, 0xFFF9A825, 0xFF3E2723, 0xFF5D4037, 0xFFBCAAA4, 0xFFD7CCC8),
    MIDNIGHT(0xFF121212, 0xFFBB86FC, 0xFFCF6679, 0xFF03DAC6, 0xFFFFD700, 0xFFE0E0E0, 0xFFA0A0A0, 0xFF2C2C2C, 0xFF1E1E1E),
    FOREST(0xFFF1F8E9, 0xFF33691E, 0xFFC62828, 0xFF558B2F, 0xFFF57F17, 0xFF1B5E20, 0xFF4E342E, 0xFFA5D6A7, 0xFFC8E6C9),
    SAKURA(0xFFFFF0F5, 0xFFE91E63, 0xFFFF1744, 0xFF00C853, 0xFFFFD600, 0xFF880E4F, 0xFFAD1457, 0xFFF8BBD0, 0xFFFCE4EC),
    MONOCHROME(0xFFF5F5F5, 0xFF212121, 0xFFFF5252, 0xFF4CAF50, 0xFFFFC107, 0xFF212121, 0xFF757575, 0xFFBDBDBD, 0xFFE0E0E0);

    public final int bg, accent, danger, success, warning, textPrimary, textSecondary, border, surface;

    MusicTheme(int bg, int accent, int danger, int success, int warning, int textPrimary, int textSecondary, int border, int surface) {
        this.bg = bg;
        this.accent = accent;
        this.danger = danger;
        this.success = success;
        this.warning = warning;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.border = border;
        this.surface = surface;
    }

    public int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
package cn.advicenext.gui.musicplayer;

import cn.advicenext.cloudmusic.MusicModels.LyricLine;
import cn.advicenext.cloudmusic.MusicModels.LyricResult;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;

public class LyricRenderer {
    private LyricResult lyric;
    private int currentIndex = -1;
    private long position;
    private float scrollOffset;
    private float targetScrollOffset;
    private MusicTheme theme = MusicTheme.CHROME_DARK;
    private float x, y, width, height;

    private final FontRenderer currentFont = Fonts.yaheiBold.get(16);
    private final FontRenderer normalFont = Fonts.yaheiBold.get(13);

    public void setLyric(LyricResult l) { this.lyric = l; currentIndex = -1; scrollOffset = 0; targetScrollOffset = 0; }
    public void setTheme(MusicTheme t) { this.theme = t; }
    public void setBounds(float x, float y, float w, float h) { this.x = x; this.y = y; this.width = w; this.height = h; }
    public void setPosition(long pos) { this.position = pos; updateCurrentLine(); }
    public LyricResult getLyric() { return lyric; }

    private void updateCurrentLine() {
        if (lyric == null || lyric.lines.isEmpty()) { currentIndex = -1; return; }
        for (int i = lyric.lines.size() - 1; i >= 0; i--) {
            if (lyric.lines.get(i).time <= position) {
                if (i != currentIndex) { currentIndex = i; targetScrollOffset = i * 36f; }
                return;
            }
        }
        currentIndex = -1;
    }

    public void render() {
        if (lyric == null || lyric.lines.isEmpty()) return;
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.15f;
        float lineHeight = 36f;
        float centerY = y + height / 2;
        float startY = centerY - scrollOffset - lineHeight;
        int visibleLines = (int) (height / lineHeight) + 2;
        int startIdx = Math.max(0, (int) (scrollOffset / lineHeight) - 1);

        for (int i = startIdx; i < Math.min(lyric.lines.size(), startIdx + visibleLines + 4); i++) {
            LyricLine line = lyric.lines.get(i);
            float ly = startY + i * lineHeight;
            if (ly < y - lineHeight || ly > y + height + lineHeight) continue;
            boolean isCurrent = i == currentIndex;
            int alpha = isCurrent ? 255 : (int) (180 * (1 - Math.min(1, Math.abs(ly - centerY) / (height / 2))));
            int textColor = isCurrent ? theme.accent : (theme.textPrimary & 0x00FFFFFF) | (alpha << 24);
            FontRenderer font = isCurrent ? currentFont : normalFont;
            float tw = font.getStringWidth(line.text);
            font.drawString(line.text, x + width / 2 - tw / 2, ly + lineHeight / 2 - font.getHeight() / 2, textColor);
        }
    }
}
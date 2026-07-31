package cn.advicenext.gui.hud.widgets;

import cn.advicenext.cloudmusic.MusicPlayerEngine;
import cn.advicenext.cloudmusic.MusicModels.Song;
import cn.advicenext.cloudmusic.MusicModels.LyricResult;
import cn.advicenext.cloudmusic.MusicModels.LyricLine;
import cn.advicenext.features.module.impl.misc.MusicPlayer;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.gui.musicplayer.MusicTheme;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.gui.DrawContext;

public class MusicPlayerInfo extends Widget {
    private final MusicPlayerEngine engine = MusicPlayerEngine.get();
    private static final int BAR_COUNT = 32;
    private static final int LYRIC_LINES = 5;
    private float[] smoothedBars = new float[BAR_COUNT];

    private final FontRenderer titleFont = Fonts.yaheiBold.get(13);
    private final FontRenderer subFont = Fonts.yaheiBold.get(11);
    private final FontRenderer timeFont = Fonts.yaheiBold.get(10);
    private final FontRenderer lyricFont = Fonts.yaheiBold.get(12);
    private final FontRenderer lyricCurrentFont = Fonts.yaheiBold.get(14);

    private LyricResult currentLyric;
    private int currentLyricIdx = -1;
    private float lyricScroll = 0;
    private float lyricScrollTarget = 0;
    private float[] lineAlphas = new float[LYRIC_LINES];
    private long lyricFadeTimer;

    public MusicPlayerInfo() {
        super("musicplayerinfo", 10, 100, 240, 110);
        engine.setLyricCallback(this::onLyricLine);
    }

    private void onLyricLine(LyricLine line) {
        if (currentLyric == null) return;
        int idx = currentLyric.lines.indexOf(line);
        if (idx >= 0 && idx != currentLyricIdx) {
            currentLyricIdx = idx;
            lyricScrollTarget = idx;
            lyricFadeTimer = System.currentTimeMillis();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        Song song = engine.getCurrentSong();
        MusicTheme theme = MusicPlayer.getCurrentTheme();

        if (song != null) {
            LyricResult engineLyric = engine.getCurrentLyric();
            if (currentLyric != engineLyric) {
                currentLyric = engineLyric;
                currentLyricIdx = -1;
                lyricScroll = 0;
                lyricScrollTarget = 0;
            }
            if (currentLyric != null && currentLyric.lines.isEmpty()) {
                currentLyric = null;
            }
        } else {
            currentLyric = null;
            currentLyricIdx = -1;
            lyricScroll = 0;
            lyricScrollTarget = 0;
        }

        float barW = 4f;
        float barGap = 2f;
        float totalBarW = BAR_COUNT * (barW + barGap) - barGap;
        float barStartX = x + (width - totalBarW) / 2;
        float barMaxH;
        float lyricH;
        float textH;

        if (song != null) {
            String name = truncate(song.name, 30);
            String artist = truncate(song.artist, 30);
            titleFont.drawString(name, x + 4, y + 2, theme.textPrimary);
            subFont.drawString(artist, x + 4, y + 18, theme.textSecondary);

            String timeText = formatTime(engine.getPosition()) + " / " + song.getDurationText();
            float tw = timeFont.getStringWidth(timeText);
            timeFont.drawString(timeText, x + width - tw - 4, y + 2, theme.textSecondary);

            float progY = y + 32;
            float progW = width - 4;
            context.fill((int) x + 2, (int) progY, (int) x + 2 + (int) progW, (int) progY + 2, theme.alpha(theme.border, 200));
            float progress = song.duration > 0 ? (float) engine.getPosition() / song.duration : 0;
            int fillW = (int) (progW * progress);
            if (fillW > 0) context.fill((int) x + 2, (int) progY, (int) x + 2 + fillW, (int) progY + 2, theme.accent);

            lyricH = renderLyrics(context, theme, x, y + 38, width, 54);
            textH = 34 + lyricH;
            barMaxH = 26;
            height = textH + barMaxH + 4;
        } else {
            subFont.drawString("No music playing", x + 4, y + 2, theme.textSecondary);
            textH = 14;
            lyricH = 0;
            barMaxH = 20;
            height = barMaxH + 4;
        }

        float[] spectrum = engine.getSpectrumData();
        float barY = y + textH + 4;
        for (int i = 0; i < BAR_COUNT; i++) {
            float raw = spectrum != null && i < spectrum.length ? spectrum[i] : 0;
            smoothedBars[i] = smoothedBars[i] * 0.8f + raw * 0.2f;
            float h = Math.max(2, smoothedBars[i] * barMaxH);
            float bx = barStartX + i * (barW + barGap);
            float by = barY + barMaxH - h;

            float t = (float) i / (BAR_COUNT - 1);
            int barColor = interpolateColor(theme.accent, theme.success, t);
            int alpha = (int) (180 + 75 * (1 - t));
            barColor = (barColor & 0x00FFFFFF) | (alpha << 24);

            context.fill((int) bx, (int) by, (int) (bx + barW), (int) (by + h), barColor);
        }
    }

    private float renderLyrics(DrawContext ctx, MusicTheme theme, float lx, float ly, float lw, float lh) {
        if (currentLyric == null || currentLyric.lines.isEmpty()) return 0;

        float lineH = 16f;
        float now = System.currentTimeMillis();
        float elapsed = (now - lyricFadeTimer) / 1000f;

        lyricScroll += (lyricScrollTarget - lyricScroll) * Math.min(1f, 0.12f);

        float centerY = ly + lineH * 2;
        int visibleCount = Math.min(LYRIC_LINES, currentLyric.lines.size());
        int centerIdx = currentLyricIdx >= 0 ? currentLyricIdx : 0;
        int startIdx = Math.max(0, centerIdx - 2);

        for (int i = 0; i < visibleCount; i++) {
            int lineIdx = startIdx + i;
            if (lineIdx >= currentLyric.lines.size()) break;

            LyricLine line = currentLyric.lines.get(lineIdx);
            float lineY = ly + i * lineH - (lyricScroll - startIdx) * lineH;

            if (lineY < ly - lineH || lineY > ly + lh + lineH) continue;

            boolean isCurrent = lineIdx == currentLyricIdx;
            float distFromCenter = Math.abs(lineY - centerY) / (lineH * 2);
            float baseAlpha = isCurrent ? 1f : Math.max(0.15f, 1f - distFromCenter);

            float fadeIn = Math.min(1f, elapsed / 0.6f);
            float targetAlpha = lineIdx == currentLyricIdx ? 1f : baseAlpha;
            int targetIdx = i;
            if (Math.abs(lineAlphas[targetIdx] - targetAlpha) > 0.001f) {
                lineAlphas[targetIdx] += (targetAlpha - lineAlphas[targetIdx]) * 0.1f;
            }

            int alpha = (int) (lineAlphas[targetIdx] * fadeIn * 255);
            if (alpha < 10) continue;

            int textColor;
            FontRenderer font;
            if (isCurrent) {
                textColor = (theme.accent & 0x00FFFFFF) | (alpha << 24);
                font = lyricCurrentFont;
            } else {
                textColor = (theme.textPrimary & 0x00FFFFFF) | (alpha << 24);
                font = lyricFont;
            }

            float tw = font.getStringWidth(line.text);
            float tx = lx + (lw - tw) / 2;
            if (tx < lx) tx = lx + 2;
            font.drawString(line.text, tx, lineY, textColor);
        }

        return lineH * visibleCount;
    }

    private int interpolateColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private String formatTime(long ms) {
        long sec = ms / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 1) + "..." : s != null ? s : "";
    }
}
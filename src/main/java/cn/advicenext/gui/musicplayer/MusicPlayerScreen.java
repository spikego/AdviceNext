package cn.advicenext.gui.musicplayer;

import cn.advicenext.cloudmusic.*;
import cn.advicenext.cloudmusic.MusicModels.*;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.QRCodeUtil;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import java.util.*;

public class MusicPlayerScreen extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static MusicPlayerScreen instance;

    private MusicTheme theme = MusicTheme.CHROME_DARK;
    private MusicPlayerEngine engine = MusicPlayerEngine.get();
    private MusicApi api = MusicApi.get();
    private LyricRenderer lyricRenderer = new LyricRenderer();

    private int panelX, panelY, panelW, panelH;
    private int sidebarW = 170;
    private int bottomBarH = 88;
    private int searchBarH = 40;
    private int padding = 10;
    private int cardW = 150, cardH = 195;

    private TextFieldWidget searchField;
    private List<Song> currentSongs = new ArrayList<>();
    private List<Playlist> playlists = new ArrayList<>();
    private List<RadioStation> stations = new ArrayList<>();
    private String currentPage = "search";
    private String statusText = "";
    private long statusTime;

    private boolean showQR;
    private QRLoginState qrState;
    private BufferedImage qrImage;
    private String qrKey = "";
    private int qrPollTimer;
    private long qrStartTime;

    private float scrollAmount;
    private int hoveredSongIdx = -1;
    private int hoveredPlaylistIdx = -1;
    private int hoveredStationIdx = -1;

    private long tickCounter;
    private boolean loadingRecommend;
    private boolean loadingPlaylists;
    private boolean loadingStations;
    private boolean loadingLiked;

    private final FontRenderer headerFont = Fonts.yaheiBold.get(15);
    private final FontRenderer navFont = Fonts.yaheiBold.get(14);
    private final FontRenderer cardTitleFont = Fonts.yaheiBold.get(12);
    private final FontRenderer cardSubFont = Fonts.yaheiBold.get(10);
    private final FontRenderer listTitleFont = Fonts.yaheiBold.get(13);
    private final FontRenderer listSubFont = Fonts.yaheiBold.get(11);
    private final FontRenderer bottomTitleFont = Fonts.yaheiBold.get(13);
    private final FontRenderer bottomSubFont = Fonts.yaheiBold.get(11);
    private final FontRenderer ctrlFont = Fonts.yaheiBold.get(18);
    private final FontRenderer qrFont = Fonts.yaheiBold.get(12);
    private final FontRenderer statusFont = Fonts.yaheiBold.get(11);
    private final FontRenderer settingsFont = Fonts.yaheiBold.get(13);
    private final FontRenderer emptyFont = Fonts.yaheiBold.get(12);
    private final FontRenderer timeFont = Fonts.yaheiBold.get(10);
    private final FontRenderer artistFont = Fonts.yaheiBold.get(12);

    private BufferedImage iconCancel, iconPlay, iconPause, iconNext, iconVolDown, iconVolUp, iconNetwork;
    private boolean iconsLoaded;

    public static MusicPlayerScreen get() { return instance; }
    public MusicTheme getTheme() { return theme; }

    public MusicPlayerScreen() {
        super(Text.literal("Music Player"));
        instance = this;
    }

    private void loadIcons() {
        if (iconsLoaded) return;
        iconsLoaded = true;
        iconCancel = loadIcon("cancel");
        iconPlay = loadIcon("play");
        iconPause = loadIcon("pause");
        iconNext = loadIcon("next");
        iconVolDown = loadIcon("volumn_down");
        iconVolUp = loadIcon("volumn_up");
        iconNetwork = loadIcon("network");
    }

    private BufferedImage loadIcon(String name) {
        try {
            return ImageIO.read(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/advicenext/icon/music/" + name + ".png")));
        } catch (Exception e) {
            System.err.println("[MusicPlayer] Failed to load icon: " + name + " - " + e.getMessage());
            return null;
        }
    }

    @Override protected void init() {
        loadIcons();
        panelW = Math.min(780, width - 24);
        panelH = Math.min(520, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        lyricRenderer.setTheme(theme);

        int sx = panelX + sidebarW + padding * 2;
        int sy = panelY + 38 + padding;
        int sw = panelW - sidebarW - padding * 3;
        searchField = new TextFieldWidget(textRenderer, sx, sy, sw, searchBarH, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("搜索歌曲..."));
        searchField.setChangedListener(s -> { if (!s.isEmpty()) doSearch(); });
        addDrawableChild(searchField);

        if (currentSongs.isEmpty()) loadRecommend();
        if (api.isLoggedIn() && !api.getUserProfile().loaded) {
            api.getLoginStatus(obj -> {});
        }
    }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        tickCounter++;
        renderPanel(mouseX, mouseY);
        renderSidebar(mouseX, mouseY);
        renderSearchBar(ctx);
        renderContent(mouseX, mouseY);
        renderBottomBar(mouseX, mouseY);
        if (showQR) renderQRPanel(mouseX, mouseY);
        if (statusText != null && !statusText.isEmpty() && System.currentTimeMillis() - statusTime > 2500) statusText = "";
        if (qrPollTimer > 0 && tickCounter % 15 == 0) pollQR();
        if (qrPollTimer < 0 && tickCounter % 20 == 0) {
            qrPollTimer++;
            if (qrPollTimer == 0) {
                showQR = false;
                qrState = null;
                qrImage = null;
            }
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPanel(int mx, int my) {
        int x = panelX, y = panelY, w = panelW, h = panelH;
        SkijaUIRenderer.drawRoundedRect("mp_panel_bg", x, y, w, h, 14, theme.alpha(theme.bg, 250));
        SkijaUIRenderer.drawRoundedRect("mp_panel_border", x, y, w, h, 14, theme.alpha(theme.border, 50));

        int headerH = 38;
        SkijaUIRenderer.drawRoundedRect("mp_header", x, y, w, headerH, 14, theme.alpha(theme.surface, 200));
        SkijaUIRenderer.drawRect("mp_header_fill", x, y + headerH - 14, w, 14, theme.alpha(theme.surface, 200));
        headerFont.drawString("AdviceNext Music", x + 16, y + 10, theme.textPrimary);

        String loginLabel = api.isLoggedIn() ? "已登录" : "未登录";
        int loginColor = api.isLoggedIn() ? theme.success : theme.textSecondary;
        float lw = statusFont.getStringWidth(loginLabel);
        statusFont.drawString(loginLabel, x + w - lw - 36, y + 12, loginColor);

        int closeX = x + w - 28, closeY = y + 8, closeSize = 18;
        if (isHovered(mx, my, closeX, closeY, closeSize, closeSize)) {
            SkijaUIRenderer.drawRoundedRect(closeX, closeY, closeSize, closeSize, 4, theme.alpha(theme.danger, 180));
        }
        if (iconCancel != null) {
            SkijaUIRenderer.drawImage("icon_cancel", iconCancel, closeX + 3, closeY + 3, 12, 12);
        } else {
            ctrlFont.drawString("x", closeX + 3, closeY + 1, theme.textPrimary);
        }
    }

    private void renderSidebar(int mx, int my) {
        int sx = panelX + padding;
        int sy = panelY + 38 + padding;
        int sw = sidebarW;
        int sh = panelH - 38 - bottomBarH - padding * 2;
        SkijaUIRenderer.drawRoundedRect("mp_sidebar", sx, sy, sw, sh, 10, theme.alpha(theme.surface, 60));

        String[][] navItems = {
            {"search", "搜索"},
            {"recommend", "推荐"},
            {"playlist", "歌单"},
            {"liked", "我喜欢"},
            {"radio", "电台"},
            {"settings", "设置"}
        };
        int itemY = sy + 8;
        for (String[] item : navItems) {
            boolean sel = currentPage.equals(item[0]);
            boolean hover = isHovered(mx, my, sx + 4, itemY, sw - 8, 34);
            int bg = sel ? theme.accent : (hover ? theme.alpha(theme.surface, 150) : 0);
            if (bg != 0) SkijaUIRenderer.drawRoundedRect(sx + 4, itemY, sw - 8, 34, 8, bg);
            navFont.drawString(item[1], sx + 18, itemY + 8, sel ? 0xFFFFFFFF : theme.textPrimary);
            itemY += 38;
        }

        MusicModels.UserProfile profile = api.getUserProfile();
        if (profile.loaded) {
            int infoY = itemY + 8;
            SkijaUIRenderer.drawRoundedRect("mp_userinfo", sx + 4, infoY, sw - 8, 52, 8, theme.alpha(theme.surface, 100));
            if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty()) {
                BufferedImage avatar = MusicCoverCache.get(profile.avatarUrl);
                SkijaUIRenderer.drawImage("avatar_" + profile.avatarUrl, avatar, sx + 12, infoY + 6, 36, 36);
            }
            String nick = profile.nickname != null && !profile.nickname.isEmpty() ? profile.nickname : "用户";
            navFont.drawString(truncate(nick, 14), sx + 56, infoY + 8, theme.textPrimary);
            String uidText = "ID: " + profile.userId;
            cardSubFont.drawString(uidText, sx + 56, infoY + 28, theme.textSecondary);
        }

        int loginY = sy + sh - 48;
        boolean loginHover = isHovered(mx, my, sx + 4, loginY, sw - 8, 38);
        int loginBg = loginHover ? theme.alpha(theme.accent, 220) : theme.alpha(theme.accent, 80);
        SkijaUIRenderer.drawRoundedRect("mp_loginbtn", sx + 4, loginY, sw - 8, 38, 8, loginBg);
        String loginLabel = api.isLoggedIn() ? "已登录" : "扫码登录";
        float llw = navFont.getStringWidth(loginLabel);
        navFont.drawString(loginLabel, sx + (sw - llw) / 2, loginY + 10, 0xFFFFFFFF);
    }

    private void renderSearchBar(DrawContext ctx) {
        int sx = panelX + sidebarW + padding * 2;
        int sy = panelY + 38 + padding;
        int sw = panelW - sidebarW - padding * 3;
        SkijaUIRenderer.drawRoundedRect("mp_searchbar", sx, sy, sw, searchBarH, 20, theme.alpha(theme.surface, 140));
        searchField.setPosition(sx, sy);
        searchField.setDimensions(sw, searchBarH);
    }

    private void renderContent(int mx, int my) {
        int cx = panelX + sidebarW + padding * 2;
        int cy = panelY + 38 + padding + searchBarH + padding;
        int cw = panelW - sidebarW - padding * 3;
        int ch = panelH - 38 - bottomBarH - searchBarH - padding * 4;

        switch (currentPage) {
            case "search", "recommend", "liked" -> renderSongGrid(mx, my, cx, cy, cw, ch);
            case "playlist" -> renderPlaylistView(mx, my, cx, cy, cw, ch);
            case "radio" -> renderRadioView(mx, my, cx, cy, cw, ch);
            case "lyrics" -> renderLyricsView(cx, cy, cw, ch);
            case "settings" -> renderSettings(mx, my, cx, cy, cw, ch);
        }
    }

    private void renderSongGrid(int mx, int my, int cx, int cy, int cw, int ch) {
        if (currentSongs.isEmpty()) {
            float tw = emptyFont.getStringWidth("暂无歌曲");
            emptyFont.drawString("暂无歌曲", cx + (cw - tw) / 2, cy + ch / 2, theme.textSecondary);
            return;
        }
        int cols = Math.max(1, cw / (cardW + 10));
        int rows = (currentSongs.size() + cols - 1) / cols;
        int startRow = Math.max(0, (int) (-scrollAmount / (cardH + 10)));
        int endRow = Math.min(rows, startRow + (ch / (cardH + 10)) + 2);
        hoveredSongIdx = -1;
        for (int r = startRow; r < endRow; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= currentSongs.size()) break;
                Song song = currentSongs.get(idx);
                int sx = cx + c * (cardW + 10);
                int sy = cy + (int) scrollAmount + r * (cardH + 10);
                if (sy + cardH < cy || sy > cy + ch) continue;
                boolean hover = isHovered(mx, my, sx, sy, cardW, cardH);
                if (hover) hoveredSongIdx = idx;
                boolean playing = engine.getCurrentSong() != null && engine.getCurrentSong().equals(song);
                renderSongCard(song, sx, sy, cardW, cardH, hover, playing);
            }
        }
    }

    private void renderSongCard(Song song, int x, int y, int w, int h, boolean hover, boolean playing) {
        int bgColor = hover ? theme.alpha(theme.surface, 220) : theme.alpha(theme.surface, 90);
        String cardBgKey = "sc_bg_" + w + "_" + h + "_" + (hover ? "h" : "n");
        SkijaUIRenderer.drawRoundedRect(cardBgKey, x, y, w, h, 10, bgColor);
        if (playing) {
            SkijaUIRenderer.drawRoundedRect("sc_playing_" + w + "_" + h, x, y, w, h, 10, theme.alpha(theme.accent, 50));
            SkijaUIRenderer.drawRoundedRect("sc_playing_border_" + w + "_" + h, x, y, w, h, 10, theme.alpha(theme.accent, 100));
        }

        int imgSize = Math.min(w - 20, 110);
        int imgX = x + (w - imgSize) / 2;
        int imgY = y + 8;

        if (song.albumPicUrl != null && !song.albumPicUrl.isEmpty()) {
                BufferedImage cover = MusicCoverCache.get(song.albumPicUrl);
                SkijaUIRenderer.drawImage("cover_" + song.albumPicUrl, cover, imgX, imgY, imgSize, imgSize);
            } else {
            int imgColor = playing ? theme.accent : theme.alpha(theme.surface, 200);
            SkijaUIRenderer.drawRoundedRect("sc_imgfg_" + imgSize + "_" + (playing ? "p" : "n"), imgX, imgY, imgSize, imgSize, 8, imgColor);
            String label = playing ? "▶" : "♫";
            float lw = ctrlFont.getStringWidth(label);
            ctrlFont.drawString(label, imgX + imgSize / 2 - lw / 2, imgY + imgSize / 2 - 8, 0x40FFFFFF);
        }

        String name = truncate(song.name, 14);
        String artist = truncate(song.artist, 16);
        cardTitleFont.drawString(name, x + 8, y + imgSize + 14, theme.textPrimary);
        cardSubFont.drawString(artist, x + 8, y + imgSize + 28, theme.textSecondary);
        cardSubFont.drawString(song.getDurationText(), x + 8, y + imgSize + 42, theme.textSecondary);
    }

    private void renderPlaylistView(int mx, int my, int cx, int cy, int cw, int ch) {
        if (playlists.isEmpty()) {
            loadPlaylists();
            float tw = emptyFont.getStringWidth("加载中...");
            emptyFont.drawString("加载中...", cx + (cw - tw) / 2, cy + ch / 2, theme.textSecondary);
            return;
        }
        int itemH = 56;
        hoveredPlaylistIdx = -1;
        for (int i = 0; i < playlists.size(); i++) {
            Playlist pl = playlists.get(i);
            int iy = cy + (int) scrollAmount + i * itemH;
            if (iy + itemH < cy || iy > cy + ch) continue;
            boolean hover = isHovered(mx, my, cx, iy, cw, itemH);
            if (hover) hoveredPlaylistIdx = i;
            if (hover) SkijaUIRenderer.drawRoundedRect(cx, iy, cw, itemH, 6, theme.alpha(theme.surface, 120));
            if (pl.coverImgUrl != null && !pl.coverImgUrl.isEmpty()) {
                BufferedImage cover = MusicCoverCache.get(pl.coverImgUrl);
                SkijaUIRenderer.drawImage("cover_" + pl.coverImgUrl, cover, cx + 4, iy + 4, 44, 44);
            } else {
                SkijaUIRenderer.drawRoundedRect("pl_fb_" + 44, cx + 4, iy + 4, 44, 44, 8, theme.alpha(theme.accent, 80));
                cardTitleFont.drawString("♫", cx + 20, iy + 18, theme.textPrimary);
            }
            listTitleFont.drawString(truncate(pl.name, 28), cx + 56, iy + 8, theme.textPrimary);
            listSubFont.drawString(pl.trackCount + " 首 | " + (pl.creator != null ? pl.creator : ""), cx + 56, iy + 28, theme.textSecondary);
        }
    }

    private void renderRadioView(int mx, int my, int cx, int cy, int cw, int ch) {
        if (stations.isEmpty()) {
            loadStations();
            float tw = emptyFont.getStringWidth("加载中...");
            emptyFont.drawString("加载中...", cx + (cw - tw) / 2, cy + ch / 2, theme.textSecondary);
            return;
        }
        int itemH = 56;
        hoveredStationIdx = -1;
        for (int i = 0; i < stations.size(); i++) {
            RadioStation rs = stations.get(i);
            int iy = cy + (int) scrollAmount + i * itemH;
            if (iy + itemH < cy || iy > cy + ch) continue;
            boolean hover = isHovered(mx, my, cx, iy, cw, itemH);
            if (hover) hoveredStationIdx = i;
            if (hover) SkijaUIRenderer.drawRoundedRect(cx, iy, cw, itemH, 6, theme.alpha(theme.surface, 120));
            if (rs.picUrl != null && !rs.picUrl.isEmpty()) {
                BufferedImage cover = MusicCoverCache.get(rs.picUrl);
                SkijaUIRenderer.drawImage("cover_" + rs.picUrl, cover, cx + 4, iy + 4, 44, 44);
            } else {
                SkijaUIRenderer.drawRoundedRect("rs_fb_" + 44, cx + 4, iy + 4, 44, 44, 8, theme.alpha(theme.warning, 80));
                cardTitleFont.drawString("◆", cx + 20, iy + 18, theme.textPrimary);
            }
            listTitleFont.drawString(truncate(rs.name, 28), cx + 56, iy + 8, theme.textPrimary);
            listSubFont.drawString(rs.category, cx + 56, iy + 28, theme.textSecondary);
        }
    }

    private void renderLyricsView(int cx, int cy, int cw, int ch) {
        lyricRenderer.setBounds(cx, cy, cw, ch);
        lyricRenderer.setPosition(engine.getPosition());
        lyricRenderer.render();
    }

    private void renderSettings(int mx, int my, int cx, int cy, int cw, int ch) {
        int sy = cy + 8;
        settingsFont.drawString("主题", cx + 8, sy, theme.textPrimary);
        sy += 26;
        MusicTheme[] themes = MusicTheme.values();
        int selW = 125, selH = 34;
        int cols = Math.max(1, (cw - 16) / (selW + 8));
        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int ttx = cx + 8 + col * (selW + 8);
            int tty = sy + row * (selH + 8);
            boolean sel = theme == themes[i];
            boolean hover = isHovered(mx, my, ttx, tty, selW, selH);
            int bg = sel ? themes[i].accent : (hover ? themes[i].alpha(themes[i].surface, 220) : themes[i].alpha(themes[i].surface, 120));
            SkijaUIRenderer.drawRoundedRect(ttx, tty, selW, selH, 8, bg);
            String name = themes[i].name().replace('_', ' ');
            float nw = settingsFont.getStringWidth(name);
            settingsFont.drawString(name, ttx + (selW - nw) / 2, tty + 9, sel ? 0xFFFFFFFF : themes[i].textPrimary);
        }
        sy += ((themes.length + cols - 1) / cols) * (selH + 8) + 16;
        listSubFont.drawString("API: " + api.getBaseUrl(), cx + 8, sy, theme.textSecondary);
    }

    private void renderBottomBar(int mx, int my) {
        int bx = panelX + padding;
        int by = panelY + panelH - bottomBarH - padding;
        int bw = panelW - padding * 2;
        SkijaUIRenderer.drawRoundedRect("mp_bottombar", bx, by, bw, bottomBarH, 10, theme.alpha(theme.surface, 200));

        Song song = engine.getCurrentSong();
        if (song != null) {
            int artSize = 64;
            if (song.albumPicUrl != null && !song.albumPicUrl.isEmpty()) {
                BufferedImage cover = MusicCoverCache.get(song.albumPicUrl);
                SkijaUIRenderer.drawImage("cover_" + song.albumPicUrl, cover, bx + 10, by + 10, artSize, artSize);
            } else {
                SkijaUIRenderer.drawRoundedRect(bx + 10, by + 10, artSize, artSize, 8, theme.alpha(theme.accent, 60));
                String artLabel = engine.isPlaying() && !engine.isPaused() ? "▶" : "♫";
                float alw = ctrlFont.getStringWidth(artLabel);
                ctrlFont.drawString(artLabel, bx + 10 + artSize / 2 - alw / 2, by + 10 + artSize / 2 - 8, 0x40FFFFFF);
            }

            bottomTitleFont.drawString(truncate(song.name, 28), bx + 86, by + 10, theme.textPrimary);
            bottomSubFont.drawString(truncate(song.artist, 28), bx + 86, by + 30, theme.textSecondary);

            int ctrlY = by + 26;
            int ctrlX = bx + 86;
            int ctrlSpacing = 44;

            boolean prevHover = isHovered(mx, my, ctrlX, ctrlY, 30, 30);
            boolean playHover = isHovered(mx, my, ctrlX + ctrlSpacing, ctrlY, 30, 30);
            boolean nextHover = isHovered(mx, my, ctrlX + ctrlSpacing * 2, ctrlY, 30, 30);
            boolean lyricHover = isHovered(mx, my, ctrlX + ctrlSpacing * 3, ctrlY, 30, 30);

            ctrlFont.drawString("⏮", ctrlX + 2, ctrlY + 2, prevHover ? theme.accent : theme.textPrimary);
            if (engine.isPlaying() && !engine.isPaused()) {
                if (iconPause != null) {
                    SkijaUIRenderer.drawImage("icon_pause", iconPause, ctrlX + ctrlSpacing + 5, ctrlY + 5, 20, 20);
                } else {
                    ctrlFont.drawString("⏸", ctrlX + ctrlSpacing + 2, ctrlY + 2, playHover ? theme.accent : theme.textPrimary);
                }
            } else {
                if (iconPlay != null) {
                    SkijaUIRenderer.drawImage("icon_play", iconPlay, ctrlX + ctrlSpacing + 5, ctrlY + 5, 20, 20);
                } else {
                    ctrlFont.drawString("▶", ctrlX + ctrlSpacing + 2, ctrlY + 2, playHover ? theme.accent : theme.textPrimary);
                }
            }
            if (iconNext != null) {
                SkijaUIRenderer.drawImage("icon_next", iconNext, ctrlX + ctrlSpacing * 2 + 5, ctrlY + 5, 20, 20);
            } else {
                ctrlFont.drawString("⏭", ctrlX + ctrlSpacing * 2 + 2, ctrlY + 2, nextHover ? theme.accent : theme.textPrimary);
            }
            ctrlFont.drawString("♬", ctrlX + ctrlSpacing * 3 + 2, ctrlY + 2, lyricHover ? theme.accent : theme.textPrimary);

            int progX = bx + 86, progY = by + 62, progW = bw - 200;
            SkijaUIRenderer.drawRoundedRect(progX, progY, progW, 4, 2, theme.alpha(theme.border, 180));
            float progress = song.duration > 0 ? (float) engine.getPosition() / song.duration : 0;
            int fillW = (int) (progW * progress);
            if (fillW > 2) SkijaUIRenderer.drawRoundedRect(progX, progY, Math.max(fillW, 4), 4, 2, theme.accent);
            if (fillW > 0) SkijaUIRenderer.drawCircle(progX + fillW, progY + 2, 10, theme.accent);

            long pos = engine.getPosition();
            long dur = song.duration;
            String posStr = formatTime(pos);
            String durStr = formatTime(dur);
            timeFont.drawString(posStr, progX - 40, progY - 2, theme.textSecondary);
            timeFont.drawString(durStr, progX + progW + 4, progY - 2, theme.textSecondary);

            int volX = bx + bw - 130;
            int volIconSize = 16;
            int volBarX = volX + 20, volBarY = ctrlY + 12;
            int volBarW = 70, volBarH = 4;

            if (iconVolDown != null) {
                SkijaUIRenderer.drawImage("icon_voldown", iconVolDown, volX, ctrlY + 5, volIconSize, volIconSize);
            }
            SkijaUIRenderer.drawRoundedRect(volBarX, volBarY, volBarW, volBarH, 2, theme.alpha(theme.border, 180));
            int volFill = (int) (volBarW * engine.getVolume());
            if (volFill > 0) SkijaUIRenderer.drawRoundedRect(volBarX, volBarY, volFill, volBarH, 2, theme.accent);
            if (iconVolUp != null) {
                SkijaUIRenderer.drawImage("icon_volup", iconVolUp, volBarX + volBarW + 4, ctrlY + 5, volIconSize, volIconSize);
            }
        } else {
            float tw = emptyFont.getStringWidth("未播放歌曲");
            emptyFont.drawString("未播放歌曲", bx + (bw - tw) / 2, by + bottomBarH / 2 - 4, theme.textSecondary);
        }
    }

    private void renderQRPanel(int mx, int my) {
        if (!showQR) return;
        int qw = 320, qh = 380;
        int qx = (width - qw) / 2, qy = (height - qh) / 2;
        SkijaUIRenderer.drawRect(0, 0, width, height, theme.alpha(0, 160));
        SkijaUIRenderer.drawRoundedRect("mp_qrpanel", qx, qy, qw, qh, 16, theme.alpha(theme.bg, 255));

        float tw = headerFont.getStringWidth("扫码登录网易云音乐");
        headerFont.drawString("扫码登录网易云音乐", qx + (qw - tw) / 2, qy + 20, theme.textPrimary);

        if (qrState == null) {
            String msg = "正在生成二维码...";
            float mw = qrFont.getStringWidth(msg);
            qrFont.drawString(msg, qx + (qw - mw) / 2, qy + qh / 2 - 8, theme.textSecondary);
            if (System.currentTimeMillis() - qrStartTime > 8000) {
                String err = "生成超时，请重试";
                float ew = qrFont.getStringWidth(err);
                qrFont.drawString(err, qx + (qw - ew) / 2, qy + qh / 2 + 16, theme.warning);
            }
        } else if (qrState.status == QRLoginState.Status.WAITING) {
            if (qrImage != null) {
                SkijaUIRenderer.drawImage("qr_" + qrKey, qrImage, qx + 60, qy + 50, 200, 200);
                String hint = "请用网易云音乐APP扫码";
                float hw = qrFont.getStringWidth(hint);
                qrFont.drawString(hint, qx + (qw - hw) / 2, qy + 270, theme.textSecondary);
            } else {
                String hint = "请用网易云音乐APP扫码";
                float hw = qrFont.getStringWidth(hint);
                qrFont.drawString(hint, qx + (qw - hw) / 2, qy + 140, theme.textSecondary);
            }
        } else if (qrState.status == QRLoginState.Status.SCANNED) {
            SkijaUIRenderer.drawCircle(qx + qw / 2, qy + 120, 60, theme.alpha(theme.success, 40));
            String msg = "已扫描，请在手机上确认";
            float mw = qrFont.getStringWidth(msg);
            qrFont.drawString(msg, qx + (qw - mw) / 2, qy + 200, theme.success);
            if (qrState.nickname != null && !qrState.nickname.isEmpty()) {
                String nick = "账号: " + qrState.nickname;
                float nw = qrFont.getStringWidth(nick);
                qrFont.drawString(nick, qx + (qw - nw) / 2, qy + 220, theme.textSecondary);
            }
        } else if (qrState.status == QRLoginState.Status.CONFIRMED) {
            SkijaUIRenderer.drawCircle(qx + qw / 2, qy + 120, 60, theme.alpha(theme.success, 60));
            String msg = "登录成功!";
            float mw = qrFont.getStringWidth(msg);
            qrFont.drawString(msg, qx + (qw - mw) / 2, qy + 200, theme.success);
        } else if (qrState.status == QRLoginState.Status.EXPIRED) {
            String msg = "二维码已过期";
            float mw = qrFont.getStringWidth(msg);
            qrFont.drawString(msg, qx + (qw - mw) / 2, qy + 140, theme.warning);
        } else if (qrState.status == QRLoginState.Status.ERROR) {
            String msg = "错误: " + (qrState.message != null ? qrState.message : "未知错误");
            float mw = qrFont.getStringWidth(msg);
            qrFont.drawString(msg, qx + (qw - mw) / 2, qy + 140, theme.danger);
        }

        int closeX = qx + qw - 24, closeY = qy + 6;
        if (isHovered(mx, my, closeX, closeY, 18, 18)) {
            SkijaUIRenderer.drawRoundedRect(closeX, closeY, 18, 18, 4, theme.alpha(theme.danger, 180));
        }
        ctrlFont.drawString("x", closeX + 2, closeY - 2, theme.textPrimary);

        if (qrState != null && (qrState.status == QRLoginState.Status.EXPIRED || qrState.status == QRLoginState.Status.ERROR)) {
            int btnX = qx + qw / 2 - 50, btnY = qy + qh - 52;
            boolean btnHover = isHovered(mx, my, btnX, btnY, 100, 36);
            int btnBg = btnHover ? theme.alpha(theme.accent, 220) : theme.alpha(theme.accent, 160);
            SkijaUIRenderer.drawRoundedRect(btnX, btnY, 100, 36, 8, btnBg);
            String refresh = "重新生成";
            float rw = qrFont.getStringWidth(refresh);
            qrFont.drawString(refresh, btnX + (100 - rw) / 2, btnY + 10, 0xFFFFFFFF);
        }
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int mxi = (int) click.x(), myi = (int) click.y();

        if (showQR) {
            int qw = 320, qh = 380, qx = (width - qw) / 2, qy = (height - qh) / 2;
            if (isHovered(mxi, myi, qx + qw - 24, qy + 6, 18, 18)) { showQR = false; return true; }
            if (qrState != null && (qrState.status == QRLoginState.Status.EXPIRED || qrState.status == QRLoginState.Status.ERROR)) {
                int btnX = qx + qw / 2 - 50, btnY = qy + qh - 52;
                if (isHovered(mxi, myi, btnX, btnY, 100, 36)) { startQRLogin(); return true; }
            }
            return true;
        }

        if (isHovered(mxi, myi, panelX + panelW - 28, panelY + 8, 18, 18)) { close(); return true; }

        int sx = panelX + padding, sy = panelY + 38 + padding;
        int loginY = sy + panelH - 38 - bottomBarH - padding * 2 - 48;
        if (isHovered(mxi, myi, sx + 4, loginY, sidebarW - 8, 38)) {
            if (api.isLoggedIn()) { setStatus("已登录"); } else { startQRLogin(); }
            return true;
        }

        String[][] navItems = {{"search"}, {"recommend"}, {"playlist"}, {"liked"}, {"radio"}, {"settings"}};
        int itemY = sy + 8;
        for (String[] nav : navItems) {
            if (isHovered(mxi, myi, sx + 4, itemY, sidebarW - 8, 34)) {
                navigateTo(nav[0]);
                return true;
            }
            itemY += 38;
        }

        int searchX = panelX + sidebarW + padding * 2, searchY = panelY + 38 + padding;
        int searchW = panelW - sidebarW - padding * 3;
        if (isHovered(mxi, myi, searchX, searchY, searchW, searchBarH)) {
            setFocused(searchField);
        }

        if (hoveredSongIdx >= 0 && hoveredSongIdx < currentSongs.size()) {
            playSong(currentSongs.get(hoveredSongIdx));
            return true;
        }
        if (hoveredPlaylistIdx >= 0 && hoveredPlaylistIdx < playlists.size()) {
            loadPlaylist(playlists.get(hoveredPlaylistIdx));
            return true;
        }
        if (hoveredStationIdx >= 0 && hoveredStationIdx < stations.size()) {
            loadStation(stations.get(hoveredStationIdx));
            return true;
        }

        if (currentPage.equals("settings")) {
            int cx = panelX + sidebarW + padding * 2, cy = panelY + 38 + padding + searchBarH + padding;
            MusicTheme[] themes = MusicTheme.values();
            int selW = 125, selH = 34, cols = Math.max(1, (panelW - sidebarW - padding * 3 - 16) / (selW + 8));
            for (int i = 0; i < themes.length; i++) {
                int col = i % cols, row = i / cols;
                int ttx = cx + 8 + col * (selW + 8), tty = cy + 34 + row * (selH + 8);
                if (isHovered(mxi, myi, ttx, tty, selW, selH)) {
                    theme = themes[i];
                    lyricRenderer.setTheme(theme);
                    return true;
                }
            }
        }

        Song song = engine.getCurrentSong();
        if (song != null) {
            int bx = panelX + padding, by = panelY + panelH - bottomBarH - padding;
            int bw = panelW - padding * 2;
            int ctrlY = by + 26, ctrlX = bx + 86, ctrlSpacing = 44;
            if (isHovered(mxi, myi, ctrlX, ctrlY, 30, 30)) { prevSong(); return true; }
            if (isHovered(mxi, myi, ctrlX + ctrlSpacing, ctrlY, 30, 30)) { engine.togglePause(); return true; }
            if (isHovered(mxi, myi, ctrlX + ctrlSpacing * 2, ctrlY, 30, 30)) { nextSong(); return true; }
            if (isHovered(mxi, myi, ctrlX + ctrlSpacing * 3, ctrlY, 30, 30)) { navigateTo("lyrics"); return true; }

            int volX = bx + bw - 130;
            int volBarW = 70;
            int volBarX = volX + 20;
            int volBarHitY = ctrlY + 8;
            if (isHovered(mxi, myi, volX, ctrlY + 5, 16, 16)) { engine.adjustVolume(-0.05f); return true; }
            if (isHovered(mxi, myi, volBarX + volBarW + 4, ctrlY + 5, 16, 16)) { engine.adjustVolume(0.05f); return true; }
            if (isHovered(mxi, myi, volBarX, volBarHitY, volBarW, 16)) {
                float frac = (float)(mxi - volBarX) / volBarW;
                engine.setVolume(Math.max(0, Math.min(1, frac)));
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        int mxi = (int) mx, myi = (int) my;

        Song song = engine.getCurrentSong();
        if (song != null) {
            int bx = panelX + padding, by = panelY + panelH - bottomBarH - padding;
            int bw = panelW - padding * 2;
            int ctrlY = by + 26;
            int volX = bx + bw - 130;
            int volBarX = volX + 20;
            int volBarW = 70;
            if (isHovered(mxi, myi, volBarX, ctrlY + 8, volBarW, 16)) {
                engine.adjustVolume((float) v * 0.05f);
                return true;
            }
        }

        scrollAmount += v * 36;
        scrollAmount = Math.min(0, Math.max(-3000, scrollAmount));
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(input);
    }

    private void startQRLogin() {
        showQR = true;
        qrState = null;
        qrImage = null;
        qrStartTime = System.currentTimeMillis();
        setStatus("正在获取二维码...");
        api.getQRKey(key -> {
            if (key.isEmpty()) {
                qrState = new QRLoginState();
                qrState.status = QRLoginState.Status.ERROR;
                qrState.message = "无法获取QR Key";
                setStatus("QR Key 获取失败");
                return;
            }
            qrImage = QRCodeUtil.generate(api.getQRCodeUrl(key), 200);
            qrKey = String.valueOf(System.currentTimeMillis());
            qrState = new QRLoginState();
            qrState.status = QRLoginState.Status.WAITING;
            qrState.key = key;
            qrPollTimer = 1;
            setStatus("二维码已生成，请扫码");
        });
    }

    private void pollQR() {
        if (qrState == null || qrState.key.isEmpty()) return;
        api.checkQR(qrState.key, state -> {
            qrState = state;
            if (state.status == QRLoginState.Status.CONFIRMED) {
                qrPollTimer = -1;
                setStatus("登录成功");
                api.getLoginStatus(obj -> {
                    loadRecommend();
                });
            } else if (state.status == QRLoginState.Status.EXPIRED) {
                qrPollTimer = 0;
                setStatus("二维码已过期");
            }
        });
    }

    private void navigateTo(String page) {
        currentPage = page;
        scrollAmount = 0;
        switch (page) {
            case "search" -> currentSongs = new ArrayList<>();
            case "recommend" -> { currentSongs = new ArrayList<>(); loadRecommend(); }
            case "playlist" -> { playlists = new ArrayList<>(); loadPlaylists(); }
            case "liked" -> { currentSongs = new ArrayList<>(); loadLiked(); }
            case "radio" -> { stations = new ArrayList<>(); loadStations(); }
        }
    }

    private void doSearch() {
        String query = searchField.getText();
        if (query.isEmpty()) return;
        setStatus("搜索: " + query);
        api.search(query, r -> {
            currentSongs = r.songs;
            currentPage = "search";
            setStatus("找到 " + r.totalCount + " 首歌曲");
        });
    }

    private void loadRecommend() {
        if (loadingRecommend) return;
        loadingRecommend = true;
        setStatus("加载推荐中...");
        api.getDailyRecommend(songs -> {
            currentSongs = songs;
            currentPage = "recommend";
            loadingRecommend = false;
            setStatus("推荐 " + songs.size() + " 首歌曲");
        });
    }

    private void loadPlaylists() {
        if (loadingPlaylists) return;
        loadingPlaylists = true;
        api.getRecommendPlaylists(pls -> {
            playlists = pls;
            loadingPlaylists = false;
        });
    }

    private void loadPlaylist(Playlist pl) {
        setStatus("加载歌单: " + pl.name);
        api.getPlaylistDetail(pl.id, detail -> {
            currentSongs = detail.tracks;
            currentPage = "search";
            setStatus("歌单: " + detail.name + " (" + detail.tracks.size() + "首)");
        });
    }

    private void loadLiked() {
        if (loadingLiked) return;
        loadingLiked = true;
        setStatus("加载我喜欢...");
        MusicModels.UserProfile profile = api.getUserProfile();
        if (!profile.loaded) {
            api.getLoginStatus(detail -> {
                long uid = api.getUserProfile().userId;
                if (uid == 0) { setStatus("请先登录"); loadingLiked = false; return; }
                api.getLikedSongs(uid, songs -> {
                    currentSongs = songs;
                    loadingLiked = false;
                    setStatus("我喜欢: " + songs.size() + " 首");
                });
            });
        } else {
            api.getLikedSongs(profile.userId, songs -> {
                currentSongs = songs;
                loadingLiked = false;
                setStatus("我喜欢: " + songs.size() + " 首");
            });
        }
    }

    private void loadStations() {
        if (loadingStations) return;
        loadingStations = true;
        api.getDjRecommend(sts -> {
            stations = sts;
            loadingStations = false;
        });
    }

    private void loadStation(RadioStation rs) {
        setStatus("加载电台: " + rs.name);
        api.getDjPrograms(rs.id, songs -> {
            currentSongs = songs;
            currentPage = "search";
            setStatus("电台: " + rs.name + " (" + songs.size() + "首)");
        });
    }

    private void playSong(Song song) {
        setStatus("加载: " + song.name);
        api.getSongUrlAndDetail(song.id, s -> {
            if (s == null || s.url == null || s.url.isEmpty()) {
                setStatus("无法播放: " + song.name);
                return;
            }
            engine.play(s);
            engine.loadLyric(s);
            lyricRenderer.setLyric(engine.getCurrentLyric());
            setStatus("正在播放: " + s.name + " - " + s.artist);
        });
    }

    private void prevSong() {
        if (currentSongs.isEmpty()) return;
        Song cur = engine.getCurrentSong();
        int idx = cur != null ? currentSongs.indexOf(cur) : -1;
        if (idx > 0) playSong(currentSongs.get(idx - 1));
        else playSong(currentSongs.get(currentSongs.size() - 1));
    }

    private void nextSong() {
        if (currentSongs.isEmpty()) return;
        Song cur = engine.getCurrentSong();
        int idx = cur != null ? currentSongs.indexOf(cur) : -1;
        if (idx >= 0 && idx < currentSongs.size() - 1) playSong(currentSongs.get(idx + 1));
        else playSong(currentSongs.get(0));
    }

    private void setStatus(String msg) { statusText = msg; statusTime = System.currentTimeMillis(); }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 1) + "…" : s != null ? s : "";
    }

    private String formatTime(long ms) {
        long sec = ms / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { mc.setScreen(null); }
}
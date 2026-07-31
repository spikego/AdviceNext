package cn.advicenext.gui.clientmenu;

import cn.advicenext.AdviceNext;
import cn.advicenext.gui.mainmenu.MainMenuScreen;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.ccbluex.liquidbounce.authlib.account.CrackedAccount;
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount;
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount;
import net.ccbluex.liquidbounce.authlib.compat.OAuthServer;
import net.ccbluex.liquidbounce.authlib.manage.AccountSerializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.session.Session;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.awt.Desktop;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AltManagerGui extends Screen {
    private final List<MinecraftAccount> accountList = new ArrayList<>();
    private int selectedAccount = -1;
    private int scrollOffset = 0;
    private final Path altFile = Paths.get(System.getProperty("user.home"), ".advicenext", "alts.json");

    private String status = "";
    private long statusTime = 0;
    private boolean isLoading = false;

    private OAuthServer currentOAuthServer = null;

    // 皮肤纹理缓存: key = account identifier (username or UUID as string)
    private final Map<String, SkinTextures> skinTexturesCache = new HashMap<>();

    private static final int BG_COLOR = 0x80000000;
    private static final int PANEL_COLOR = 0xAA2D2D30;
    private static final int SELECTED_COLOR = 0x88007ACC;
    private static final int HOVER_COLOR = 0xAA3E3E42;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SUB_TEXT_COLOR = 0xFFCCCCCC;
    private static final int MICROSOFT_COLOR = 0xFF00AA00;
    private static final int OFFLINE_COLOR = 0xFFFFAA00;
    private static final int CAPE_BG_COLOR = 0xAA000000;

    // 头像大小
    private static final int HEAD_SIZE = 22;
    // 披风绘制尺寸
    private static final int CAPE_WIDTH = 20;
    private static final int CAPE_HEIGHT = 14;

    public AltManagerGui() {
        super(Text.literal("Alt Manager"));
        loadAccounts();
    }

    @Override
    public void removed() {
        // 关闭屏幕时停止OAuthServer，释放端口
        if (currentOAuthServer != null) {
            try {
                currentOAuthServer.stop(false);
            } catch (Exception ignored) {}
            currentOAuthServer = null;
        }
        super.removed();
    }


    /**
     * 根据用户名判断是否使用Alex模型（Slim）
     * 简单规则：用户名的哈希值决定
     */
    private boolean isAlexSkin(String username) {
        if (username == null || username.isEmpty()) return false;
        // Minecraft使用UUID的哈希决定Steve/Alex，这里简单模拟
        return (username.hashCode() & 1) == 0;
    }

    // ========== 渲染 ==========

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 先渲染原版背景（含模糊效果）
        super.render(context, mouseX, mouseY, delta);
        FontRenderer.setRenderState(context.state, context.getMatrices());
        FontRenderer fr1 = Fonts.monoBold.get(18);

        // 半透明覆盖层
        context.fill(0, 0, width, height, BG_COLOR);

        // 标题
        //context.drawCenteredTextWithShadow(textRenderer, "Alt Manager", width / 2, 15, TEXT_COLOR);
        fr1.drawCenteredString("Alt Manager", (float) width / 2, 15, TEXT_COLOR);

        // 状态信息
        if (isLoading) {
            context.drawCenteredTextWithShadow(textRenderer, "§eProcessing...", width / 2, 35, TEXT_COLOR);
        } else if (!status.isEmpty() && System.currentTimeMillis() - statusTime < 5000) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, 35, TEXT_COLOR);
        }

        // 账户列表区域
        int px = 30, py = 55, pw = width - 60, lh = height - 130;
        context.fill(px, py, px + pw, py + lh, PANEL_COLOR);

        renderAccountList(context, px, py, pw, lh, mouseX, mouseY);

        // 底部按钮
        renderBottomButtons(context, mouseX, mouseY);
    }

    private void renderAccountList(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (accountList.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "§7No accounts. Add one below.", x + w / 2, y + h / 2 - 10, SUB_TEXT_COLOR);
            return;
        }

        int itemHeight = 34; // 稍微增加高度以容纳头像和披风
        int maxVisible = h / itemHeight;
        int totalItems = accountList.size();

        // 限制滚动范围
        if (scrollOffset > totalItems - maxVisible) {
            scrollOffset = Math.max(0, totalItems - maxVisible);
        }
        if (scrollOffset < 0) scrollOffset = 0;

        // 绘制可见条目
        for (int i = 0; i < maxVisible && (i + scrollOffset) < totalItems; i++) {
            int idx = i + scrollOffset;
            MinecraftAccount account = accountList.get(idx);
            int itemY = y + i * itemHeight;

            // 选中高亮
            if (idx == selectedAccount) {
                context.fill(x, itemY, x + w, itemY + itemHeight, SELECTED_COLOR);
            } else if (mouseX >= x && mouseX <= x + w && mouseY >= itemY && mouseY <= itemY + itemHeight) {
                context.fill(x, itemY, x + w, itemY + itemHeight, HOVER_COLOR);
            }

            // 分隔线
            context.fill(x, itemY + itemHeight - 1, x + w, itemY + itemHeight, 0xAA1E1E1E);

            // ===== 绘制头像 =====
            int headX = x + 5;
            int headY = itemY + (itemHeight - HEAD_SIZE) / 2;

            // ===== 绘制披风（如果有） =====

            // ===== 账户信息 =====
            int textX = headX + HEAD_SIZE + 4;
            // 如果有披风，调整文本位置

            // 类型标签
            boolean isMs = account instanceof MicrosoftAccount;
            String typeLabel = isMs ? "MS" : "Off";
            int typeColor = isMs ? MICROSOFT_COLOR : OFFLINE_COLOR;
            context.fill(textX, itemY + 5, textX + 22, itemY + 21, typeColor);
            context.drawCenteredTextWithShadow(textRenderer, typeLabel, textX + 11, itemY + 8, TEXT_COLOR);

            // 用户名
            String name = account.getProfile() != null ? account.getProfile().getUsername() : "Unknown";
            context.drawTextWithShadow(textRenderer, name, textX + 28, itemY + 4, TEXT_COLOR);

            // 类型文字
            String typeText = "§7" + account.getType();
            context.drawTextWithShadow(textRenderer, typeText, textX + 28, itemY + 16, SUB_TEXT_COLOR);

            // ===== 登录按钮 =====
            int btnX = x + w - 45;
            int btnY = itemY + (itemHeight - 22) / 2;
            int btnW = 40;
            int btnH = 22;
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnHovered ? SELECTED_COLOR : 0xFF555555);
            context.drawCenteredTextWithShadow(textRenderer, "Login", btnX + btnW / 2, btnY + 6, TEXT_COLOR);

            // 当前登录账户高亮标记
            Session currentSession = MinecraftClient.getInstance().getSession();
            if (currentSession != null && account.getProfile() != null
                && currentSession.getUsername() != null
                && currentSession.getUsername().equals(account.getProfile().getUsername())) {
                context.drawCenteredTextWithShadow(textRenderer, "§a✓", btnX - 10, btnY + 6, TEXT_COLOR);
            }
        }

        // 滚动条（如果有滚动）
        if (totalItems > maxVisible) {
            int scrollBarHeight = (int) ((float) maxVisible / totalItems * h);
            int scrollBarY = y + (int) ((float) scrollOffset / totalItems * h);
            context.fill(x + w - 4, scrollBarY, x + w - 1, scrollBarY + Math.max(scrollBarHeight, 10), 0xFF888888);
        }
    }

    private void renderBottomButtons(DrawContext context, int mouseX, int mouseY) {
        int by = height - 55, bw = 90, bh = 25, sp = 100;
        int totalW = sp * 4 - 10;
        int sx = (width - totalW) / 2;

        drawButton(context, "§aMicrosoft", sx, by, bw, bh, mouseX, mouseY, MICROSOFT_COLOR);
        drawButton(context, "§6Offline", sx + sp, by, bw, bh, mouseX, mouseY, OFFLINE_COLOR);
        drawButton(context, "§cDelete", sx + sp * 2, by, bw, bh, mouseX, mouseY, 0xFFAA0000);
        drawButton(context, "§7Back", sx + sp * 3, by, bw, bh, mouseX, mouseY, 0xFF666666);
    }

    private void drawButton(DrawContext context, String text, int x, int y, int w, int h, int mx, int my, int color) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg = hovered ? color : (color & 0x80FFFFFF);
        context.fill(x, y, x + w, y + h, bg);
        // 边框
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
        context.drawCenteredTextWithShadow(textRenderer, text, x + w / 2, y + 6, TEXT_COLOR);
    }

    // ========== 鼠标事件 ==========

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();

        int px = 30, py = 55, pw = width - 60, lh = height - 130;

        // 点击账户列表区域
        if (mx >= px && mx <= px + pw && my >= py && my <= py + lh) {
            int itemHeight = 34;
            int maxVisible = lh / itemHeight;
            int clickedIndex = (int) ((my - py) / itemHeight) + scrollOffset;

            if (clickedIndex >= 0 && clickedIndex < accountList.size()) {
                int itemY = py + (clickedIndex - scrollOffset) * itemHeight;
                // 检查是否点击了登录按钮
                int btnX = px + pw - 45;
                int btnY = itemY + (itemHeight - 22) / 2;
                int btnW = 40;
                int btnH = 22;

                if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                    loginWithAccount(clickedIndex);
                    return true;
                }

                selectedAccount = clickedIndex;
                return true;
            }
        }

        // 底部按钮
        int by = height - 55, bw = 90, bh = 25, sp = 100;
        int totalW = sp * 4 - 10;
        int sx = (width - totalW) / 2;

        if (my >= by && my <= by + bh) {
            if (mx >= sx && mx <= sx + bw) {
                loginMicrosoft();
            } else if (mx >= sx + sp && mx <= sx + sp + bw) {
                openOfflineDialog();
            } else if (mx >= sx + sp * 2 && mx <= sx + sp * 2 + bw) {
                deleteAccount();
            } else if (mx >= sx + sp * 3 && mx <= sx + sp * 3 + bw) {
                client.setScreen(new MainMenuScreen());
            }
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    // ========== 鼠标滚轮事件 ==========

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int px = 30, py = 55, pw = width - 60, lh = height - 130;
        if (mouseX >= px && mouseX <= px + pw && mouseY >= py && mouseY <= py + lh) {
            int itemHeight = 34;
            int maxVisible = lh / itemHeight;
            int totalItems = accountList.size();

            scrollOffset -= (int) verticalAmount;
            if (scrollOffset > totalItems - maxVisible) {
                scrollOffset = Math.max(0, totalItems - maxVisible);
            }
            if (scrollOffset < 0) scrollOffset = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ========== 登录逻辑 ==========

    private void loginWithAccount(int index) {
        if (index < 0 || index >= accountList.size() || isLoading) return;

        MinecraftAccount account = accountList.get(index);
        isLoading = true;
        setStatus("§7Logging in...");

        CompletableFuture.runAsync(() -> {
            try {
                var result = account.login();
                var authSession = result.getFirst();

                MinecraftClient.getInstance().execute(() -> {
                    try {
                        String clientId = account instanceof MicrosoftAccount ? "msa" : "legacy";
                        Session session = new Session(
                            authSession.getUsername(),
                            authSession.getUuid(),
                            authSession.getToken(),
                            Optional.empty(), // xuid
                            Optional.of(clientId) // clientId: "msa" for Microsoft, "legacy" for offline
                        );
                        setMinecraftSession(session);
                        setStatus("§aLogged in as " + authSession.getUsername());
                        isLoading = false;
                        saveAccounts();
                    } catch (Exception e) {
                        e.printStackTrace();
                        setStatus("§cFailed to set session: " + e.getMessage());
                        isLoading = false;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                MinecraftClient.getInstance().execute(() -> {
                    setStatus("§cLogin failed: " + e.getMessage());
                    isLoading = false;
                });
            }
        });
    }

    private void loginMicrosoft() {
        if (isLoading) return;
        isLoading = true;
        setStatus("§7Opening browser for Microsoft login...");

        // 停止之前的OAuthServer，释放端口
        if (currentOAuthServer != null) {
            try {
                currentOAuthServer.stop(false);
            } catch (Exception ignored) {}
            currentOAuthServer = null;
        }

        try {
            MicrosoftAccount.OAuthHandler handler = new MicrosoftAccount.OAuthHandler() {
                @Override
                public void openUrl(String url) {
                    AdviceNext.LOGGER.info("[AltManager] Opening URL: {}", url);
                    AdviceNext.LOGGER.info("[AltManager] AuthMethod: {}", MicrosoftAccount.AuthMethod.AZURE_APP);
                    AdviceNext.LOGGER.info("[AltManager] AZURE_APP clientId: {}", MicrosoftAccount.AuthMethod.AZURE_APP.getClientId());
                    AdviceNext.LOGGER.info("[AltManager] AZURE_APP redirectUri: {}", MicrosoftAccount.AuthMethod.AZURE_APP.getRedirectUri());
                    try {
                        openBrowser(url);
                        MinecraftClient.getInstance().execute(() -> {
                            setStatus("§aBrowser opened. Please complete login in the browser.");
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        MinecraftClient.getInstance().execute(() -> {
                            setStatus("§cFailed to open browser: " + e.getMessage());
                            isLoading = false;
                        });
                    }
                }

                @Override
                public void authResult(MicrosoftAccount account) {
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            var result = account.login();
                            var authSession = result.getFirst();

                            Session session = new Session(
                                authSession.getUsername(),
                                authSession.getUuid(),
                                authSession.getToken(),
                                Optional.empty(),  // xuid
                                Optional.of("msa") // clientId = "msa" for Microsoft account
                            );
                            setMinecraftSession(session);
                            setStatus("§aLogged in as " + authSession.getUsername());

                            // 移除已存在的同名账户
                            accountList.removeIf(a ->
                                a instanceof MicrosoftAccount
                                && a.getProfile() != null
                                && account.getProfile() != null
                                && a.getProfile().getUsername().equals(account.getProfile().getUsername())
                            );
                            accountList.add(0, account);
                            saveAccounts();
                            isLoading = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                            setStatus("§cLogin failed: " + e.getMessage());
                            isLoading = false;
                        }
                    });
                }

                @Override
                public void authError(String error) {
                    System.err.println("Microsoft auth error: " + error);
                    MinecraftClient.getInstance().execute(() -> {
                        setStatus("§cAuth error: " + error);
                        isLoading = false;
                    });
                }
            };

            // 启动OAuth服务器并打开浏览器
            currentOAuthServer = MicrosoftAccount.Companion.buildFromOpenBrowser(handler, MicrosoftAccount.AuthMethod.AZURE_APP);
        } catch (Exception e) {
            AdviceNext.LOGGER.error("[AltManager] Failed to start Microsoft login: {}", e.getMessage());
            setStatus("§cFailed to start Microsoft login: " + e.getMessage());
            isLoading = false;
        }
    }

    private void loginOffline(String username) {
        if (username == null || username.trim().isEmpty()) {
            setStatus("§cUsername cannot be empty");
            return;
        }
        if (isLoading) return;

        isLoading = true;
        setStatus("§7Logging in offline...");
        AdviceNext.LOGGER.info("[AltManager] Logging in offline...");

        CompletableFuture.runAsync(() -> {
            try {
                CrackedAccount account = new CrackedAccount(username.trim(), false);
                var result = account.login();
                var authSession = result.getFirst();

                MinecraftClient.getInstance().execute(() -> {
                    try {
                        Session session = new Session(
                            authSession.getUsername(),
                            authSession.getUuid(),
                            authSession.getToken(),
                            Optional.empty(),  // xuid
                            Optional.of("legacy") // clientId = "legacy" for offline account
                        );
                        setMinecraftSession(session);
                        setStatus("§aLogged in as " + authSession.getUsername());

                        // 添加到账户列表
                        accountList.removeIf(a ->
                            a instanceof CrackedAccount
                            && a.getProfile() != null
                            && a.getProfile().getUsername().equals(username.trim())
                        );
                        accountList.add(0, account);
                        saveAccounts();
                        isLoading = false;
                    } catch (Exception e) {
                        AdviceNext.LOGGER.error("[AltManager] Failed to set session: {}", e.getMessage());
                        setStatus("§cFailed to set session: " + e.getMessage());
                        isLoading = false;
                    }
                });
            } catch (Exception e) {
                AdviceNext.LOGGER.error("[AltManager] Offline login failed: {}", e.getMessage());
                MinecraftClient.getInstance().execute(() -> {
                    setStatus("§cOffline login failed: " + e.getMessage());
                    isLoading = false;
                });
            }
        });
    }

    private void deleteAccount() {
        if (selectedAccount < 0 || selectedAccount >= accountList.size()) {
            setStatus("§cNo account selected");
            return;
        }

        // 从皮肤缓存中移除
        MinecraftAccount account = accountList.get(selectedAccount);

        accountList.remove(selectedAccount);
        selectedAccount = -1;
        saveAccounts();
        setStatus("§aAccount deleted");
    }

    private void openOfflineDialog() {
        client.setScreen(new OfflineLoginDialog(this));
    }

    // ========== Session设置 ==========

    private void setMinecraftSession(Session session) {
        try {
            java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(MinecraftClient.getInstance(), session);
            System.out.println("Session set successfully: " + session.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set Minecraft session", e);
        }
    }

    // ========== 浏览器打开 ==========

    private void openBrowser(String url) {
        try {
            // 使用 Desktop.getDesktop().browse() (Java 标准方式)
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {}

        // 回退到 Runtime.exec()
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                // 注意: 必须给 URL 加引号，否则 & 会被 cmd.exe 解释为命令分隔符
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "\"\"", "\"" + url + "\""});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to open browser", e);
        }
    }

    // ========== 持久化 ==========

    private void saveAccounts() {
        try {
            Files.createDirectories(altFile.getParent());
            JsonArray jsonArray = new JsonArray();
            AccountSerializer serializer = AccountSerializer.INSTANCE;

            for (MinecraftAccount account : accountList) {
                JsonObject json = serializer.toJson(account);
                jsonArray.add(json);
            }

            Files.writeString(altFile, new GsonBuilder().setPrettyPrinting().create().toJson(jsonArray), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAccounts() {
        accountList.clear();
        try {
            if (Files.exists(altFile)) {
                String content = Files.readString(altFile, StandardCharsets.UTF_8);
                JsonArray jsonArray = JsonParser.parseString(content).getAsJsonArray();
                AccountSerializer serializer = AccountSerializer.INSTANCE;

                for (var element : jsonArray) {
                    JsonObject json = element.getAsJsonObject();
                    MinecraftAccount account = serializer.fromJson(json);
                    if (account != null) {
                        accountList.add(account);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========== 工具方法 ==========

    private void setStatus(String msg) {
        status = msg;
        statusTime = System.currentTimeMillis();
    }

    // ========== 离线登录对话框 ==========

    private static class OfflineLoginDialog extends Screen {
        private final AltManagerGui parent;
        private TextFieldWidget usernameField;

        public OfflineLoginDialog(AltManagerGui parent) {
            super(Text.literal("Offline Login"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            usernameField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 10, 200, 20, Text.literal("Username"));
            usernameField.setPlaceholder(Text.literal("Enter username..."));
            addDrawableChild(usernameField);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(0, 0, width, height, 0xCC000000);
            context.drawCenteredTextWithShadow(textRenderer, "Offline Login", width / 2, height / 2 - 40, TEXT_COLOR);

            int by = height / 2 + 20;
            drawButton(context, "Login", width / 2 - 50, by, 100, 20, mouseX, mouseY, SELECTED_COLOR);
            int cy = by + 30;
            drawButton(context, "Cancel", width / 2 - 50, cy, 100, 20, mouseX, mouseY, 0xFFAA0000);

            super.render(context, mouseX, mouseY, delta);
        }

        private void drawButton(DrawContext context, String text, int x, int y, int w, int h, int mx, int my, int color) {
            boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
            context.fill(x, y, x + w, y + h, hovered ? color : (color & 0x80FFFFFF));
            if (hovered) {
                context.fill(x, y, x + w, y + 1, color);
                context.fill(x, y + h - 1, x + w, y + h, color);
                context.fill(x, y, x + 1, y + h, color);
                context.fill(x + w - 1, y, x + w, y + h, color);
            }
            context.drawCenteredTextWithShadow(textRenderer, text, x + w / 2, y + 6, TEXT_COLOR);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            double mx = click.x(), my = click.y();
            int by = height / 2 + 20;
            int cy = by + 30;

            if (mx >= width / 2 - 50 && mx <= width / 2 + 50) {
                if (my >= by && my <= by + 20) {
                    parent.loginOffline(usernameField.getText());
                    client.setScreen(parent);
                    return true;
                }
                if (my >= cy && my <= cy + 20) {
                    client.setScreen(parent);
                    return true;
                }
            }
            return super.mouseClicked(click, doubled);
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (input.key() == 257) { // Enter
                parent.loginOffline(usernameField.getText());
                client.setScreen(parent);
                return true;
            }
            return super.keyPressed(input);
        }
    }
}
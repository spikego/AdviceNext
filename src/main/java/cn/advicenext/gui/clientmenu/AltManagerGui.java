package cn.advicenext.gui.clientmenu;

import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.mainmenu.MainMenuScreen;
import net.ccbluex.liquidbounce.authlib.account.CrackedAccount;
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class AltManagerGui extends Screen {
    private List<String> accountList = new ArrayList<>();
    private int selectedAccount = -1;
    private int scrollOffset = 0;
    private final Gson gson = new Gson();
    private final Path altFile = Paths.get(System.getProperty("user.home"), ".advicenext", "alt.json");

    private String status = "";
    private boolean isLoading = false;

    // 双击登录相关变量
    private int lastClickedAccount = -1;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME = 500;

    // Rise style colors
    private final int backgroundColor = 0xFF1E1E1E;
    private final int panelColor = 0xFF2D2D30;
    private final int selectedColor = 0xFF007ACC;
    private final int hoverColor = 0xFF3E3E42;
    private final int textColor = 0xFFFFFFFF;
    private final int subTextColor = 0xFFCCCCCC;

    public AltManagerGui() {
        super(Text.literal("Alt Manager"));
        loadAccounts();
    }

    @Override
    protected void init() {
        // No text field in main view
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(textRenderer, "Alt Manager", width / 2, 15, Colors.currentColor().getRGB());

        int panelX = 20;
        int panelY = 50;
        int panelWidth = width - 40;
        int panelHeight = height - 120;

        renderAccountList(context, panelX + 10, panelY + 10, panelWidth - 20, panelHeight - 20, mouseX, mouseY);
        renderBottomButtons(context, mouseX, mouseY);

        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 15,
                    status.contains("Success") ? 0x00FF00 : 0xFF0000);
        }

        context.drawTextWithShadow(textRenderer, "Double-click to login", 30, height - 15, subTextColor);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderAccountList(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.drawTextWithShadow(textRenderer, "Accounts (" + accountList.size() + ")", x, y, textColor);

        int listY = y + 20;
        int listHeight = height - 20;
        int itemHeight = 30;
        int visibleItems = listHeight / itemHeight;

        int maxScroll = Math.max(0, accountList.size() - visibleItems);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < Math.min(visibleItems, accountList.size()); i++) {
            int index = i + scrollOffset;
            if (index >= accountList.size()) break;

            String account = accountList.get(index);
            int itemY = listY + i * itemHeight;

            boolean isSelected = index == selectedAccount;
            boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY <= itemY + itemHeight;

            if (isSelected) {
                context.fill(x, itemY, x + width, itemY + itemHeight, selectedColor);
            } else if (isHovered) {
                context.fill(x, itemY, x + width, itemY + itemHeight, hoverColor);
            }

            String displayName = account.length() > 40 ? account.substring(0, 37) + "..." : account;
            context.drawTextWithShadow(textRenderer, displayName, x + 10, itemY + 8, textColor);

            String type = account.contains("@") ? "Microsoft" : "Offline";
            context.drawTextWithShadow(textRenderer, type, x + width - 80, itemY + 8, subTextColor);

            int statusColor = account.contains("@") ? 0x00AA00 : 0xFFAA00;
            context.fill(x + 5, itemY + 10, x + 8, itemY + 20, statusColor);
        }
    }

    private void renderBottomButtons(DrawContext context, int mouseX, int mouseY) {
        int buttonY = height - 60;
        int buttonWidth = 100;
        int buttonHeight = 25;
        int spacing = 110;
        int startX = (width - (spacing * 4 - 10)) / 2;

        renderButton(context, "Microsoft", startX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY, selectedColor);
        renderButton(context, "Offline", startX + spacing, buttonY, buttonWidth, buttonHeight, mouseX, mouseY, 0xFF666666);
        renderButton(context, "Delete", startX + spacing * 2, buttonY, buttonWidth, buttonHeight, mouseX, mouseY, 0xFFAA0000);
        renderButton(context, "Back", startX + spacing * 3, buttonY, buttonWidth, buttonHeight, mouseX, mouseY, 0xFF333333);
    }

    private void renderButton(DrawContext context, String text, int x, int y, int width, int height,
                              int mouseX, int mouseY, int color) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor = hovered ? (color | 0xFF000000) : (color & 0x80FFFFFF);
        context.fill(x, y, x + width, y + height, bgColor);

        if (hovered) {
            context.drawBorder(x, y, width, height, color);
        }

        context.drawCenteredTextWithShadow(textRenderer, text, x + width / 2, y + 8, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = 30;
        int panelY = 70;
        int panelWidth = width - 60;
        int listHeight = height - 140;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + listHeight) {
            int itemHeight = 30;
            int clickedIndex = (int) ((mouseY - panelY) / itemHeight) + scrollOffset;

            if (clickedIndex >= 0 && clickedIndex < accountList.size()) {
                long currentTime = System.currentTimeMillis();
                if (clickedIndex == lastClickedAccount && currentTime - lastClickTime < DOUBLE_CLICK_TIME) {
                    loginWithAccount(clickedIndex);
                    return true;
                }

                selectedAccount = clickedIndex;
                lastClickedAccount = clickedIndex;
                lastClickTime = currentTime;
            }
            return true;
        }

        int buttonY = height - 60;
        int buttonWidth = 100;
        int buttonHeight = 25;
        int spacing = 110;
        int startX = (width - (spacing * 4 - 10)) / 2;

        if (mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            if (mouseX >= startX && mouseX <= startX + buttonWidth) {
                loginUsingMicrosoft();
            } else if (mouseX >= startX + spacing && mouseX <= startX + spacing + buttonWidth) {
                openOfflineDialog();
            } else if (mouseX >= startX + spacing * 2 && mouseX <= startX + spacing * 2 + buttonWidth) {
                deleteAccount();
            } else if (mouseX >= startX + spacing * 3 && mouseX <= startX + spacing * 3 + buttonWidth) {
                if (client != null) client.setScreen(new MainMenuScreen());
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void loginWithAccount(int index) {
        if (index < 0 || index >= accountList.size()) return;

        String account = accountList.get(index);
        isLoading = true;
        status = "Logging in...";

        if (account.contains("@")) {
            loginMicrosoftAccount(account);
        } else {
            loginOfflineAccount(account);
        }
    }

    private void loginMicrosoftAccount(String email) {
        isLoading = true;
        status = "Opening browser for Microsoft login...";
        
        try {
            MicrosoftAccount.Companion.buildFromOpenBrowser(new MicrosoftAccount.OAuthHandler() {
                @Override
                public void openUrl(String url) {
                    try {
                        openBrowser(url);
                        status = "Complete login in browser...";
                    } catch (Exception e) {
                        status = "Failed to open browser: " + e.getMessage();
                        isLoading = false;
                    }
                }

                @Override
                public void authResult(MicrosoftAccount account) {
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            var loginResult = account.login();
                            var session = loginResult.getFirst();
                            
                            Session mcSession = new Session(
                                session.getUsername(),
                                session.getUuid(),
                                session.getToken(),
                                Optional.empty(),
                                Optional.empty(),
                                Session.AccountType.MSA
                            );
                            
                            setMinecraftSession(mcSession);
                            
                            String accountName = session.getUsername() + "@microsoft.com";
                            addAccount(accountName);
                            
                            status = "Success: Logged in as " + session.getUsername();
                            isLoading = false;
                            
                        } catch (Exception e) {
                            status = "Failed to set session: " + e.getMessage();
                            isLoading = false;
                        }
                    });
                }

                @Override
                public void authError(String error) {
                    MinecraftClient.getInstance().execute(() -> {
                        status = "Login failed: " + error;
                        isLoading = false;
                    });
                }
            }, MicrosoftAccount.AuthMethod.AZURE_APP);
        } catch (Exception e) {
            status = "Error: " + e.getMessage();
            isLoading = false;
        }
    }
    
    private void openBrowser(String url) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows - try multiple methods
            try {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } catch (Exception e1) {
                try {
                    Runtime.getRuntime().exec("cmd /c start \"\" \"" + url + "\"");
                } catch (Exception e2) {
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
                }
            }
        } else if (os.contains("mac")) {
            // macOS
            Runtime.getRuntime().exec("open " + url);
        } else {
            // Linux/Unix
            try {
                Runtime.getRuntime().exec("xdg-open " + url);
            } catch (Exception e1) {
                try {
                    Runtime.getRuntime().exec("firefox " + url);
                } catch (Exception e2) {
                    Runtime.getRuntime().exec("google-chrome " + url);
                }
            }
        }
    }

    private void loginOfflineAccount(String username) {
        try {
            CrackedAccount account = new CrackedAccount(username, false);
            var loginResult = account.login();
            var session = loginResult.getFirst();

            Session mcSession = new Session(
                    session.getUsername(),
                    session.getUuid(),
                    session.getToken(),
                    Optional.empty(),
                    Optional.empty(),
                    Session.AccountType.LEGACY
            );

            setMinecraftSession(mcSession);
            status = "Success: Logged in as " + username;
        } catch (Exception e) {
            status = "Error: " + e.getMessage();
        }
        isLoading = false;
    }

    private void setMinecraftSession(Session session) {
        try {
            java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(MinecraftClient.getInstance(), session);
        } catch (Exception e) {
            status = "Failed to set session: " + e.getMessage();
        }
    }

    private void saveAccounts() {
        try {
            Files.createDirectories(altFile.getParent());
            String json = gson.toJson(accountList);
            Files.write(altFile, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAccounts() {
        try {
            if (Files.exists(altFile)) {
                String json = new String(Files.readAllBytes(altFile), StandardCharsets.UTF_8);
                accountList = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
                if (accountList == null) accountList = new ArrayList<>();
            }
        } catch (Exception e) {
            accountList = new ArrayList<>();
        }
    }

    private void addAccount(String account) {
        if (!accountList.contains(account)) {
            accountList.add(account);
            saveAccounts();
        }
    }

    private void deleteAccount() {
        if (selectedAccount >= 0 && selectedAccount < accountList.size()) {
            accountList.remove(selectedAccount);
            saveAccounts();
            selectedAccount = -1;
        }
    }

    private void loginUsingMicrosoft() {
        loginMicrosoftAccount("");
    }

    private void openOfflineDialog() {
        client.setScreen(new OfflineLoginDialog(this));
    }

    public void loginOffline(String username) {
        if (username.isEmpty()) return;

        addAccount(username);
        loginOfflineAccount(username);
    }

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
            context.drawCenteredTextWithShadow(textRenderer, "Offline Login", width / 2, height / 2 - 40, 0xFFFFFF);

            int buttonY = height / 2 + 20;
            boolean loginHovered = mouseX >= width / 2 - 50 && mouseX <= width / 2 + 50 && mouseY >= buttonY && mouseY <= buttonY + 20;
            context.fill(width / 2 - 50, buttonY, width / 2 + 50, buttonY + 20, loginHovered ? 0xFF007ACC : 0xFF333333);
            context.drawCenteredTextWithShadow(textRenderer, "Login", width / 2, buttonY + 6, 0xFFFFFF);

            int cancelY = buttonY + 30;
            boolean cancelHovered = mouseX >= width / 2 - 50 && mouseX <= width / 2 + 50 && mouseY >= cancelY && mouseY <= cancelY + 20;
            context.fill(width / 2 - 50, cancelY, width / 2 + 50, cancelY + 20, cancelHovered ? 0xFFAA0000 : 0xFF333333);
            context.drawCenteredTextWithShadow(textRenderer, "Cancel", width / 2, cancelY + 6, 0xFFFFFF);

            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int buttonY = height / 2 + 20;
            int cancelY = buttonY + 30;

            if (mouseX >= width / 2 - 50 && mouseX <= width / 2 + 50) {
                if (mouseY >= buttonY && mouseY <= buttonY + 20) {
                    parent.loginOffline(usernameField.getText());
                    client.setScreen(parent);
                    return true;
                } else if (mouseY >= cancelY && mouseY <= cancelY + 20) {
                    client.setScreen(parent);
                    return true;
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 257) {
                parent.loginOffline(usernameField.getText());
                client.setScreen(parent);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
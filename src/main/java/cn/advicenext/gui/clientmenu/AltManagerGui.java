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
import net.minecraft.util.Util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.awt.Color;
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
        // Background
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // Header - no background fill
        context.drawCenteredTextWithShadow(textRenderer, "Alt Manager", width / 2, 15, Colors.currentColor().getRGB());
        
        // Main panel - no background fill
        int panelX = 20;
        int panelY = 50;
        int panelWidth = width - 40;
        int panelHeight = height - 120;
        
        // Account list
        renderAccountList(context, panelX + 10, panelY + 10, panelWidth - 20, panelHeight - 20, mouseX, mouseY);
        
        // Bottom buttons
        renderBottomButtons(context, mouseX, mouseY);
        
        // Status
        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 15, 
                status.contains("Success") ? 0x00FF00 : 0xFF0000);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderAccountList(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Header
        context.drawTextWithShadow(textRenderer, "Accounts (" + accountList.size() + ")", x, y, textColor);
        
        // List area
        int listY = y + 20;
        int listHeight = height - 20;
        int itemHeight = 30;
        int visibleItems = listHeight / itemHeight;
        
        // Scroll handling
        int maxScroll = Math.max(0, accountList.size() - visibleItems);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        // Render accounts
        for (int i = 0; i < Math.min(visibleItems, accountList.size()); i++) {
            int index = i + scrollOffset;
            if (index >= accountList.size()) break;
            
            String account = accountList.get(index);
            int itemY = listY + i * itemHeight;
            
            boolean isSelected = index == selectedAccount;
            boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY <= itemY + itemHeight;
            
            // Background
            if (isSelected) {
                context.fill(x, itemY, x + width, itemY + itemHeight, selectedColor);
            } else if (isHovered) {
                context.fill(x, itemY, x + width, itemY + itemHeight, hoverColor);
            }
            
            // Account info
            String displayName = account.length() > 40 ? account.substring(0, 37) + "..." : account;
            context.drawTextWithShadow(textRenderer, displayName, x + 10, itemY + 8, textColor);
            
            String type = account.contains("@") ? "Microsoft" : "Offline";
            context.drawTextWithShadow(textRenderer, type, x + width - 80, itemY + 8, subTextColor);
            
            // Status indicator
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
        
        // Button background
        int bgColor = hovered ? (color | 0xFF000000) : (color & 0x80FFFFFF);
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Button border
        if (hovered) {
            context.drawBorder(x, y, width, height, color);
        }
        
        // Button text
        context.drawCenteredTextWithShadow(textRenderer, text, x + width / 2, y + 8, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Account list click
        int panelX = 30;
        int panelY = 70;
        int panelWidth = width - 60;
        int listHeight = height - 140;
        
        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + listHeight) {
            int itemHeight = 30;
            int clickedIndex = (int) ((mouseY - panelY) / itemHeight) + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < accountList.size()) {
                selectedAccount = clickedIndex;
            }
            return true;
        }
        
        // Button clicks
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
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelX = 30;
        int panelY = 70;
        int panelWidth = width - 60;
        int listHeight = height - 140;
        
        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + listHeight) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) verticalAmount, 
                Math.max(0, accountList.size() - (listHeight / 30))));
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void loginUsingMicrosoft() {
        if (isLoading) return;
        isLoading = true;
        status = "Opening browser...";
        
        MicrosoftAccount.Companion.buildFromOpenBrowser(new MicrosoftAccount.OAuthHandler() {
            @Override
            public void openUrl(String url) {
                try {
                    Util.getOperatingSystem().open(url);
                    status = "Complete login in browser...";
                } catch (Exception e) {
                    status = "Failed to open browser";
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
                        
                        java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
                        sessionField.setAccessible(true);
                        sessionField.set(MinecraftClient.getInstance(), mcSession);
                        
                        String accountName = session.getUsername() + "@microsoft.com";
                        if (!accountList.contains(accountName)) {
                            accountList.add(accountName);
                            saveAccounts();
                        }
                        
                        status = "Success: Logged in as " + session.getUsername();
                        isLoading = false;
                        
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                MinecraftClient.getInstance().execute(() -> {
                                    if (client != null) client.setScreen(null);
                                });
                            } catch (InterruptedException ignored) {}
                        }).start();
                        
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
    }
    
    private void openOfflineDialog() {
        if (client != null) {
            client.setScreen(new OfflineLoginDialog(this));
        }
    }
    
    private void deleteAccount() {
        if (selectedAccount >= 0 && selectedAccount < accountList.size()) {
            String removed = accountList.remove(selectedAccount);
            saveAccounts();
            status = "Deleted: " + removed;
            
            if (accountList.isEmpty()) {
                selectedAccount = -1;
            } else {
                selectedAccount = Math.max(0, selectedAccount - 1);
            }
        } else {
            status = "Select account to delete";
        }
    }

    private void loadAccounts() {
        try {
            if (Files.exists(altFile)) {
                String json = Files.readString(altFile, StandardCharsets.UTF_8);
                accountList = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
                if (accountList == null) accountList = new ArrayList<>();
            }
        } catch (Exception e) {
            accountList = new ArrayList<>();
        }
    }

    public void saveAccounts() {
        try {
            Files.createDirectories(altFile.getParent());
            String json = gson.toJson(accountList);
            Files.writeString(altFile, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
    
    // Offline login dialog
    private static class OfflineLoginDialog extends Screen {
        private final Screen parent;
        private TextFieldWidget usernameField;
        private String status = "";

        public OfflineLoginDialog(Screen parent) {
            super(Text.literal("Offline Login"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            usernameField = new TextFieldWidget(
                textRenderer,
                width / 2 - 100,
                height / 2 - 10,
                200,
                20,
                Text.literal("Username")
            );
            usernameField.setMaxLength(48);
            usernameField.setFocused(true);
            addDrawableChild(usernameField);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            
            int dialogWidth = 300;
            int dialogHeight = 150;
            int dialogX = (width - dialogWidth) / 2;
            int dialogY = (height - dialogHeight) / 2;
            
            context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xE0000000);
            context.drawBorder(dialogX, dialogY, dialogWidth, dialogHeight, Colors.currentColor().getRGB());
            
            context.drawCenteredTextWithShadow(textRenderer, "Offline Login", width / 2, dialogY + 20, 0xFFFFFF);
            
            renderButton(context, "Login", dialogX + 50, dialogY + 100, 80, 20, mouseX, mouseY, 0xFF007ACC);
            renderButton(context, "Cancel", dialogX + 170, dialogY + 100, 80, 20, mouseX, mouseY, 0xFFAA0000);
            
            if (!status.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, status, width / 2, dialogY + 70, 
                    status.contains("Success") ? 0x00FF00 : 0xFF0000);
            }
            
            super.render(context, mouseX, mouseY, delta);
        }
        
        private void renderButton(DrawContext context, String text, int x, int y, int width, int height, 
                                 int mouseX, int mouseY, int color) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            
            context.fill(x, y, x + width, y + height, hovered ? color : (color & 0x80FFFFFF));
            context.drawCenteredTextWithShadow(textRenderer, text, x + width / 2, y + 6, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (usernameField != null && usernameField.mouseClicked(mouseX, mouseY, button)) {
                setFocused(usernameField);
                return true;
            }
            
            int dialogWidth = 300;
            int dialogHeight = 150;
            int dialogX = (width - dialogWidth) / 2;
            int dialogY = (height - dialogHeight) / 2;
            
            if (mouseX >= dialogX + 50 && mouseX <= dialogX + 130 && 
                mouseY >= dialogY + 100 && mouseY <= dialogY + 120) {
                handleLogin();
                return true;
            }
            
            if (mouseX >= dialogX + 170 && mouseX <= dialogX + 250 && 
                mouseY >= dialogY + 100 && mouseY <= dialogY + 120) {
                if (client != null) client.setScreen(parent);
                return true;
            }
            
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        private void handleLogin() {
            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                status = "Enter username";
                return;
            }
            
            try {
                CrackedAccount offlineAccount = new CrackedAccount(username, false);
                var loginResult = offlineAccount.login();
                var session = loginResult.getFirst();
                
                Session mcSession = new Session(
                    session.getUsername(),
                    session.getUuid(),
                    session.getToken(),
                    Optional.empty(),
                    Optional.empty(),
                    Session.AccountType.LEGACY
                );
                
                java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
                sessionField.setAccessible(true);
                sessionField.set(MinecraftClient.getInstance(), mcSession);
                
                // Add to account list
                if (parent instanceof AltManagerGui) {
                    AltManagerGui altManager = (AltManagerGui) parent;
                    if (!altManager.accountList.contains(username)) {
                        altManager.accountList.add(username);
                        altManager.saveAccounts();
                    }
                }
                
                status = "Success: Logged in as " + username;
                
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        MinecraftClient.getInstance().execute(() -> {
                            if (client != null) client.setScreen(new MainMenuScreen());
                        });
                    } catch (InterruptedException ignored) {}
                }).start();
                
            } catch (Exception e) {
                status = "Login failed: " + e.getMessage();
            }
        }
    }
}
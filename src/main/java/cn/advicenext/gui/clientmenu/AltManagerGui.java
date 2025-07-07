package cn.advicenext.gui.clientmenu;

import cn.advicenext.gui.mainmenu.MainMenuScreen;
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import java.util.Optional;
import java.util.UUID;

public class AltManagerGui extends Screen {
    private TextFieldWidget usernameField;
    private ButtonWidget microsoftButton;
    private ButtonWidget crackedLoginButton;
    private ButtonWidget manageButton;
    private ButtonWidget backButton;

    // 账号列表
    private java.util.List<String> accountList = new java.util.ArrayList<>();
    private int selectedAccount = -1;
    private ButtonWidget selectAccountButton;
    private ButtonWidget addAccountButton;
    private ButtonWidget deleteAccountButton;
    private final Gson gson = new Gson();
    private final Path altFile = Paths.get(System.getProperty("user.home"), ".advicenext", "alt.json");

    private void loadAccounts() {
        try {
            if (Files.exists(altFile)) {
                String json = Files.readString(altFile, StandardCharsets.UTF_8);
                accountList = gson.fromJson(json, new TypeToken<java.util.List<String>>(){}.getType());
                if (accountList == null) accountList = new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            accountList = new java.util.ArrayList<>();
        }
    }
    private void saveAccounts() {
        try {
            Files.createDirectories(altFile.getParent());
            String json = gson.toJson(accountList);
            Files.writeString(altFile, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    public AltManagerGui() {
        super(Text.literal("Alt Manager"));
    }

    @Override
    protected void init() {
        int leftX = width / 4;
        int centerY = height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;
        loadAccounts();

        usernameField = new TextFieldWidget(
            textRenderer,
            leftX - 100,
            centerY - spacing * 3,
            buttonWidth,
            buttonHeight,
            Text.literal("Crack Username")
        );
        usernameField.setMaxLength(48);
        usernameField.setText("");

        microsoftButton = ButtonWidget.builder(
            Text.literal("§9Login with Microsoft"),
            btn -> loginUsingMicrosoft()
        ).dimensions(
            leftX - 100,
            centerY - spacing * 2,
            buttonWidth,
            buttonHeight
        ).build();

        crackedLoginButton = ButtonWidget.builder(
            Text.literal("§7Login as Cracked"),
            btn -> {
                if (!usernameField.getText().isEmpty()) {
                    handleCrackedLogin(usernameField.getText());
                }
            }
        ).dimensions(
            leftX - 100,
            centerY - spacing,
            buttonWidth,
            buttonHeight
        ).build();

        manageButton = ButtonWidget.builder(
            Text.literal("§eManage Accounts"),
            btn -> handleManageAccounts()
        ).dimensions(
            leftX - 100,
            centerY,
            buttonWidth,
            buttonHeight
        ).build();

        backButton = ButtonWidget.builder(
            Text.literal("Back"),
            btn -> {
                if (client != null) client.setScreen(new MainMenuScreen());
            }
        ).dimensions(
            leftX - 100,
            centerY + spacing,
            buttonWidth,
            buttonHeight
        ).build();

        // 添加演示账号（实际可从本地存储加载）
        accountList.clear();
        accountList.add("demo1@example.com");
        accountList.add("demo2@example.com");
        accountList.add("离线账号");
        // 选择账号按钮
        selectAccountButton = ButtonWidget.builder(
            Text.literal("选择账号"),
            btn -> {
                if (!accountList.isEmpty()) {
                    selectedAccount = (selectedAccount + 1) % accountList.size();
                    usernameField.setText(accountList.get(selectedAccount));
                }
            }
        ).dimensions(
            leftX + 110,
            centerY - spacing * 3,
            100,
            20
        ).build();
        // 添加账号按钮
        addAccountButton = ButtonWidget.builder(
            Text.literal("添加账号"),
            btn -> {
                String name = usernameField.getText();
                if (!name.isEmpty() && !accountList.contains(name)) {
                    accountList.add(name);
                    saveAccounts();
                }
            }
        ).dimensions(
            leftX + 110,
            centerY - spacing * 2,
            100,
            20
        ).build();
        // 删除账号按钮
        deleteAccountButton = ButtonWidget.builder(
            Text.literal("删除账号"),
            btn -> {
                if (selectedAccount >= 0 && selectedAccount < accountList.size()) {
                    accountList.remove(selectedAccount);
                    if (accountList.isEmpty()) {
                        selectedAccount = -1;
                        usernameField.setText("");
                    } else {
                        selectedAccount = Math.max(0, selectedAccount - 1);
                        usernameField.setText(accountList.get(selectedAccount));
                    }
                    saveAccounts();
                }
            }
        ).dimensions(
            leftX + 110,
            centerY - spacing,
            100,
            20
        ).build();
        addDrawableChild(usernameField);
        addDrawableChild(microsoftButton);
        addDrawableChild(crackedLoginButton);
        addDrawableChild(manageButton);
        addDrawableChild(backButton);
        addDrawableChild(selectAccountButton);
        addDrawableChild(addAccountButton);
        addDrawableChild(deleteAccountButton);
    }

    private void handleCrackedLogin(String username) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
            Session session = new Session(
                username,
                uuid,
                "",
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.LEGACY
            );
            java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(MinecraftClient.getInstance(), session);
            if (client != null) client.setScreen(null);
        } catch (Exception e) {
            System.out.println("Cracked login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loginUsingMicrosoft() {
        MicrosoftAccount.Companion.buildFromOpenBrowser(new MicrosoftAccount.OAuthHandler() {
            @Override
            public void openUrl(String url) {
                System.out.println("Open url: " + url);
                boolean opened = false;
                try {
                    MinecraftClient.getInstance().execute(() -> Util.getOperatingSystem().open(url));
                    opened = true;
                } catch (Exception e) {
                    System.out.println("Failed to open URL: " + e.getMessage());
                }
                if (!opened) {
                    MinecraftClient.getInstance().execute(() -> {
                        var player = MinecraftClient.getInstance().player;
                        if (player != null) {
                            player.sendMessage(
                                Text.literal("§6Click here to open Microsoft login").styled(style ->
                                    style.withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(url)))
                                         .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click here to open Microsoft login")))
                                ), false
                            );
                        }
                    });
                }
            }

            @Override
            public void authResult(MicrosoftAccount account) {
                try {
                    var pair = account.login();
                    net.ccbluex.liquidbounce.authlib.compat.Session session = pair.getFirst();
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
                            sessionField.setAccessible(true);
                            sessionField.set(MinecraftClient.getInstance(), session);
                            var player = MinecraftClient.getInstance().player;
                            if (player != null) {
                                player.sendMessage(Text.literal("§aSuccessfully logged in as " + session.getUsername()), false);
                            }
                            if (client != null) client.setScreen(null);
                        } catch (Exception e) {
                            System.out.println("Failed to update session: " + e.getMessage());
                            e.printStackTrace();
                            var player = MinecraftClient.getInstance().player;
                            if (player != null) {
                                player.sendMessage(Text.literal("§cFailed to update session: " + e.getMessage()), false);
                            }
                        }
                    });
                } catch (Exception e) {
                    System.out.println("Auth result error: " + e.getMessage());
                    e.printStackTrace();
                    MinecraftClient.getInstance().execute(() -> {
                        var player = MinecraftClient.getInstance().player;
                        if (player != null) {
                            player.sendMessage(Text.literal("§cLogin failed: " + e.getMessage()), false);
                        }
                    });
                }
            }

            @Override
            public void authError(String error) {
                System.out.println("Auth error: " + error);
                MinecraftClient.getInstance().execute(() -> {
                    var player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        player.sendMessage(Text.literal("§cLogin failed: " + error), false);
                    }
                });
            }
        }, MicrosoftAccount.AuthMethod.AZURE_APP);
    }

    private void handleManageAccounts() {
        System.out.println("Managing accounts...");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("§l§6Alt Manager"),
            width / 4,
            20,
            0xFFFFFF
        );
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Email/Username:"),
            width / 4 - 100,
            height / 2 - 87,
            0xFFFFFF
        );
        // 渲染账号列表
        int listX = width / 4 + 120;
        int listY = height / 2 - 87 + 30;
        for (int i = 0; i < accountList.size(); i++) {
            int y = listY + i * 18;
            int color = (i == selectedAccount) ? 0xFFAA00 : 0xAAAAAA;
            context.drawTextWithShadow(textRenderer, Text.literal(accountList.get(i)), listX, y, color);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 支持点击账号列表切换
        int listX = width / 4 + 120;
        int listY = height / 2 - 87 + 30;
        for (int i = 0; i < accountList.size(); i++) {
            int y = listY + i * 18;
            if (mouseX >= listX && mouseX <= listX + 120 && mouseY >= y && mouseY <= y + 16) {
                selectedAccount = i;
                usernameField.setText(accountList.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

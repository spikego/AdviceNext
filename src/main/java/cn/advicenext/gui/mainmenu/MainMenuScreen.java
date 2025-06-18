package cn.advicenext.gui.mainmenu;

import cn.advicenext.gui.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MainMenuScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    // Animation
    private long openTime;
    private float openAnimation = 0f;
    private boolean closingAnimation = false;
    private long closeTime;
    private float closeAnimation = 0f;
    private Screen nextScreen = null;
    
    // Buttons
    private final List<MenuButton> buttons = new ArrayList<>();
    private int hoveredButton = -1;
    
    // Colors - Neverlose inspired
    private final Color backgroundColor = new Color(17, 17, 17, 200);
    private final Color buttonColor = new Color(25, 25, 25, 220);
    private final Color buttonHoverColor = new Color(35, 35, 35, 220);
    private final Color accentColor = Colors.currentColor();
    private final Color textColor = Color.WHITE;
    private final Color subTextColor = new Color(170, 170, 170);
    
    // Logo
    private final String clientName = "AdviceNext";
    private final String clientVersion = "v1.0";
    private final String clientTagline = "Premium Minecraft Experience";
    
    // Background
    private final Identifier backgroundTexture = Identifier.of("textures/gui/title/background/panorama_0.png");
    
    public MainMenuScreen() {
        super(Text.literal("Main Menu"));
        this.openTime = System.currentTimeMillis();
        
        // Initialize buttons
        buttons.add(new MenuButton("Singleplayer", () -> openScreen(() -> new CustomSelectWorldScreen(this))));
        buttons.add(new MenuButton("Multiplayer", () -> openScreen(() -> new CustomMultiplayerScreen(this))));
        buttons.add(new MenuButton("Realms", () -> openScreen(() -> new CustomRealmsScreen(this))));
        buttons.add(new MenuButton("Options", () -> openScreen(() -> new CustomOptionsScreen(this, mc.options))));
        buttons.add(new MenuButton("Quit", () -> mc.stop()));
    }
    
    private void openScreen(Supplier<Screen> screenSupplier) {
        startClosingAnimation(screenSupplier);
    }
    
    private void startClosingAnimation(Supplier<Screen> nextScreenSupplier) {
        closingAnimation = true;
        closeTime = System.currentTimeMillis();
        nextScreen = nextScreenSupplier.get();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update animations
        long currentTime = System.currentTimeMillis();
        if (!closingAnimation) {
            openAnimation = Math.min(1f, (currentTime - openTime) / 500f);
        } else {
            closeAnimation = Math.min(1f, (currentTime - closeTime) / 300f);
            if (closeAnimation >= 1f && nextScreen != null) {
                mc.setScreen(nextScreen);
                return;
            }
        }
        
        // Draw background
        renderBackground(context, mouseX, mouseY, delta);
        
        // Draw semi-transparent overlay
        context.fill(0, 0, width, height, backgroundColor.getRGB());
        
        // Calculate center area
        int centerWidth = 400;
        int centerHeight = 300;
        
        // Apply animation to center area
        float scale = closingAnimation ? 
            1f - (0.2f * closeAnimation) : 
            0.8f + (0.2f * openAnimation);
        
        float alpha = closingAnimation ? 
            1f - closeAnimation : 
            openAnimation;
        
        // Draw center panel with animation
        int animatedWidth = (int)(centerWidth * scale);
        int animatedHeight = (int)(centerHeight * scale);
        int animatedX = (width - animatedWidth) / 2;
        int animatedY = (height - animatedHeight) / 2;
        
        // Draw center panel background
        Color panelColor = new Color(25, 25, 25, (int)(220 * alpha));
        context.fill(animatedX, animatedY, animatedX + animatedWidth, animatedY + animatedHeight, panelColor.getRGB());
        
        // Draw accent line at top
        context.fill(animatedX, animatedY, animatedX + animatedWidth, animatedY + 2, accentColor.getRGB());
        
        // Draw client name
        float nameScale = 2.0f * openAnimation;
        context.getMatrices().push();
        context.getMatrices().scale(nameScale, nameScale, 1.0f);
        int nameWidth = mc.textRenderer.getWidth(clientName);
        context.drawTextWithShadow(mc.textRenderer, clientName, 
                                  (int)((animatedX + 20) / nameScale), 
                                  (int)((animatedY + 20) / nameScale), 
                                  accentColor.getRGB());
        context.getMatrices().pop();
        
        // Draw version and tagline
        context.drawTextWithShadow(mc.textRenderer, clientVersion, 
                                  animatedX + 20 + (int)(nameWidth * nameScale) + 5, 
                                  animatedY + 20, 
                                  subTextColor.getRGB());
        
        context.drawTextWithShadow(mc.textRenderer, clientTagline, 
                                  animatedX + 20, 
                                  animatedY + 20 + (int)(10 * nameScale), 
                                  subTextColor.getRGB());
        
        // Draw buttons
        int buttonY = animatedY + 80;
        int buttonWidth = 200;
        int buttonHeight = 30;
        int buttonSpacing = 10;
        
        // Check for hovered button
        hoveredButton = -1;
        for (int i = 0; i < buttons.size(); i++) {
            int y = buttonY + i * (buttonHeight + buttonSpacing);
            if (mouseX >= animatedX + (animatedWidth - buttonWidth) / 2 && 
                mouseX <= animatedX + (animatedWidth + buttonWidth) / 2 &&
                mouseY >= y && mouseY <= y + buttonHeight) {
                hoveredButton = i;
                break;
            }
        }
        
        // Draw buttons with animation
        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);
            int y = buttonY + i * (buttonHeight + buttonSpacing);
            
            // Apply animation delay for each button
            float buttonAnimation = Math.max(0, Math.min(1, (openAnimation * 2) - (i * 0.15f)));
            
            // Calculate button position with animation
            int animatedButtonWidth = (int)(buttonWidth * buttonAnimation);
            int buttonX = animatedX + (animatedWidth - animatedButtonWidth) / 2;
            
            // Draw button background
            Color bgColor = i == hoveredButton ? buttonHoverColor : buttonColor;
            context.fill(buttonX, y, buttonX + animatedButtonWidth, y + buttonHeight, bgColor.getRGB());
            
            // Draw button accent if hovered
            if (i == hoveredButton) {
                context.fill(buttonX, y, buttonX + 2, y + buttonHeight, accentColor.getRGB());
            }
            
            // Draw button text if animation is complete enough
            if (buttonAnimation > 0.5f) {
                int textWidth = mc.textRenderer.getWidth(button.text);
                float textAlpha = (buttonAnimation - 0.5f) * 2; // Fade in text
                int textX = buttonX + (animatedButtonWidth - textWidth) / 2;
                int textY = y + (buttonHeight - 8) / 2;
                
                context.drawTextWithShadow(mc.textRenderer, button.text, textX, textY, textColor.getRGB());
            }
        }
        
        // Draw bottom info
        String info = "AdviceNext Client • " + mc.getGameVersion();
        context.drawTextWithShadow(mc.textRenderer, info, 
                                  animatedX + 10, 
                                  animatedY + animatedHeight - 15, 
                                  subTextColor.getRGB());
        
        // Draw user info
        String user = "User: " + mc.getSession().getUsername();
        int userWidth = mc.textRenderer.getWidth(user);
        context.drawTextWithShadow(mc.textRenderer, user, 
                                  animatedX + animatedWidth - userWidth - 10, 
                                  animatedY + animatedHeight - 15, 
                                  subTextColor.getRGB());
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredButton >= 0 && hoveredButton < buttons.size() && openAnimation >= 0.9f && !closingAnimation) {
            buttons.get(hoveredButton).action.run();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public void close() {
        // Override close to prevent going back to TitleScreen
        // Do nothing, stay on this screen
    }
    
    private static class MenuButton {
        public final String text;
        public final Runnable action;
        
        public MenuButton(String text, Runnable action) {
            this.text = text;
            this.action = action;
        }
    }
}
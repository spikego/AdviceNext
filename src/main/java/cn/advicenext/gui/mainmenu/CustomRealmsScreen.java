package cn.advicenext.gui.mainmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;

/**
 * A wrapper for RealmsMainScreen that returns to our custom main menu
 */
public class CustomRealmsScreen extends RealmsMainScreen {
    private final Screen parent;
    
    public CustomRealmsScreen(Screen parent) {
        super(parent);
        this.parent = parent;
    }
    
    @Override
    public void init() {
        super.init();
        
        // Find and replace the back button
        for (var child : this.children()) {
            if (child instanceof ButtonWidget button) {
                if (button.getMessage().getString().equals("Back")) {
                    int x = button.getX();
                    int y = button.getY();
                    int width = button.getWidth();
                    int height = button.getHeight();
                    
                    this.remove(button);
                    
                    ButtonWidget customButton = ButtonWidget.builder(Text.literal("Back"), b -> {
                        MinecraftClient.getInstance().setScreen(new MainMenuScreen());
                    }).dimensions(x, y, width, height).build();
                    
                    this.addDrawableChild(customButton);
                    break;
                }
            }
        }
    }
    
    @Override
    public void close() {
        // Return to our custom main menu instead of the default behavior
        MinecraftClient.getInstance().setScreen(new MainMenuScreen());
    }
}
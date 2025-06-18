package cn.spikego.advicenext.gui.mainmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A wrapper for SelectWorldScreen that returns to our custom main menu
 */
public class CustomSelectWorldScreen extends SelectWorldScreen {
    private final Screen parent;
    
    public CustomSelectWorldScreen(Screen parent) {
        super(parent);
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Find and replace the cancel button
        for (var child : this.children()) {
            if (child instanceof ButtonWidget button) {
                if (button.getMessage().getString().equals("Cancel")) {
                    int x = button.getX();
                    int y = button.getY();
                    int width = button.getWidth();
                    int height = button.getHeight();
                    
                    this.remove(button);
                    
                    ButtonWidget customButton = ButtonWidget.builder(Text.literal("Cancel"), b -> {
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
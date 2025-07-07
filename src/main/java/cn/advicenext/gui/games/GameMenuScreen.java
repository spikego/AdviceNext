package cn.advicenext.gui.games;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GameMenuScreen extends Screen {
    
    public GameMenuScreen() {
        super(Text.literal("Mini Games"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Pacman"), button -> {
            this.client.setScreen(new PacmanGame());
        }).dimensions(centerX - 100, startY, 200, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Snake"), button -> {
            this.client.setScreen(new SnakeGame());
        }).dimensions(centerX - 100, startY + 30, 200, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Space Shooter"), button -> {
            this.client.setScreen(new SpaceShooterGame());
        }).dimensions(centerX - 100, startY + 60, 200, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            this.close();
        }).dimensions(centerX - 50, startY + 100, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        context.drawCenteredTextWithShadow(textRenderer, "Mini Games", this.width / 2, 50, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Choose a game to play:", this.width / 2, 80, 0xFFAAAAAA);
        
        super.render(context, mouseX, mouseY, delta);
    }
}
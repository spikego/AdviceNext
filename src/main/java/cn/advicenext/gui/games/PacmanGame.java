package cn.advicenext.gui.games;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PacmanGame extends Screen {
    private int pacmanX = 100, pacmanY = 100;
    private int direction = 0; // 0=right, 1=down, 2=left, 3=up
    private List<int[]> dots = new ArrayList<>();
    private int score = 0;
    private boolean gameRunning = true;

    public PacmanGame() {
        super(Text.literal("Pacman Game"));
        initDots();
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Exit"), button -> this.close())
                .dimensions(this.width - 60, 10, 50, 20)
                .build());
    }

    private void initDots() {
        for (int x = 50; x < 400; x += 30) {
            for (int y = 50; y < 300; y += 30) {
                dots.add(new int[]{x, y});
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        if (gameRunning) {
            updateGame();
        }
        
        // 绘制豆子
        for (int[] dot : dots) {
            context.fill(dot[0], dot[1], dot[0] + 4, dot[1] + 4, 0xFFFFFF00);
        }
        
        // 绘制吃豆人
        context.fill(pacmanX, pacmanY, pacmanX + 20, pacmanY + 20, 0xFFFFFF00);
        
        // 绘制分数
        context.drawText(textRenderer, "Score: " + score, 10, 10, 0xFFFFFFFF, true);
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void updateGame() {
        // 移动吃豆人
        switch (direction) {
            case 0: pacmanX += 2; break;
            case 1: pacmanY += 2; break;
            case 2: pacmanX -= 2; break;
            case 3: pacmanY -= 2; break;
        }
        
        // 边界检查
        if (pacmanX < 0) pacmanX = width - 20;
        if (pacmanX > width) pacmanX = 0;
        if (pacmanY < 0) pacmanY = height - 20;
        if (pacmanY > height) pacmanY = 0;
        
        // 检查吃豆子
        dots.removeIf(dot -> {
            if (Math.abs(dot[0] - pacmanX) < 20 && Math.abs(dot[1] - pacmanY) < 20) {
                score += 10;
                return true;
            }
            return false;
        });
        
        if (dots.isEmpty()) {
            gameRunning = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 262: direction = 0; break; // Right
            case 264: direction = 1; break; // Down
            case 263: direction = 2; break; // Left
            case 265: direction = 3; break; // Up
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
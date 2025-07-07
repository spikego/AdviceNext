package cn.advicenext.gui.games;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGame extends Screen {
    private List<int[]> snake = new ArrayList<>();
    private int[] food = new int[2];
    private int direction = 0; // 0=right, 1=down, 2=left, 3=up
    private int score = 0;
    private boolean gameRunning = true;
    private int tickCounter = 0;
    private Random random = new Random();

    public SnakeGame() {
        super(Text.literal("Snake Game"));
        initGame();
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Exit"), button -> this.close())
                .dimensions(this.width - 60, 10, 50, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Restart"), button -> {
            initGame();
            gameRunning = true;
        }).dimensions(this.width - 120, 10, 50, 20).build());
    }

    private void initGame() {
        snake.clear();
        snake.add(new int[]{100, 100});
        snake.add(new int[]{80, 100});
        snake.add(new int[]{60, 100});
        spawnFood();
        score = 0;
        direction = 0;
    }

    private void spawnFood() {
        food[0] = random.nextInt(30) * 20 + 20;
        food[1] = random.nextInt(20) * 20 + 50;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        if (gameRunning && ++tickCounter % 5 == 0) {
            updateGame();
        }
        
        // 绘制蛇
        for (int i = 0; i < snake.size(); i++) {
            int[] segment = snake.get(i);
            int color = i == 0 ? 0xFF00FF00 : 0xFF008800;
            context.fill(segment[0], segment[1], segment[0] + 18, segment[1] + 18, color);
        }
        
        // 绘制食物
        context.fill(food[0], food[1], food[0] + 18, food[1] + 18, 0xFFFF0000);
        
        // 绘制分数
        context.drawText(textRenderer, "Score: " + score, 10, 10, 0xFFFFFFFF, true);
        
        if (!gameRunning) {
            context.drawText(textRenderer, "Game Over! Press Restart", width/2 - 80, height/2, 0xFFFF0000, true);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void updateGame() {
        if (!gameRunning) return;
        
        int[] head = snake.get(0).clone();
        
        // 移动蛇头
        switch (direction) {
            case 0: head[0] += 20; break;
            case 1: head[1] += 20; break;
            case 2: head[0] -= 20; break;
            case 3: head[1] -= 20; break;
        }
        
        // 边界检查
        if (head[0] < 0 || head[0] >= width || head[1] < 0 || head[1] >= height) {
            gameRunning = false;
            return;
        }
        
        // 自撞检查
        for (int[] segment : snake) {
            if (head[0] == segment[0] && head[1] == segment[1]) {
                gameRunning = false;
                return;
            }
        }
        
        snake.add(0, head);
        
        // 检查吃食物
        if (head[0] == food[0] && head[1] == food[1]) {
            score += 10;
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!gameRunning) return super.keyPressed(keyCode, scanCode, modifiers);
        
        switch (keyCode) {
            case 262: if (direction != 2) direction = 0; break; // Right
            case 264: if (direction != 3) direction = 1; break; // Down
            case 263: if (direction != 0) direction = 2; break; // Left
            case 265: if (direction != 1) direction = 3; break; // Up
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
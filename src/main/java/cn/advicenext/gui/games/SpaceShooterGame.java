package cn.advicenext.gui.games;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpaceShooterGame extends Screen {
    private int playerX = 200, playerY = 400;
    private List<int[]> bullets = new ArrayList<>();
    private List<int[]> enemies = new ArrayList<>();
    private int score = 0;
    private boolean gameRunning = true;
    private int tickCounter = 0;
    private Random random = new Random();
    private boolean[] keys = new boolean[4]; // left, right, up, down

    public SpaceShooterGame() {
        super(Text.literal("Space Shooter"));
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
        playerX = width / 2;
        playerY = height - 50;
        bullets.clear();
        enemies.clear();
        score = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        if (gameRunning) {
            updateGame();
        }
        
        // 绘制玩家飞机
        context.fill(playerX - 10, playerY - 10, playerX + 10, playerY + 10, 0xFF00FF00);
        context.fill(playerX - 5, playerY - 15, playerX + 5, playerY - 5, 0xFF00FF00);
        
        // 绘制子弹
        for (int[] bullet : bullets) {
            context.fill(bullet[0] - 2, bullet[1] - 5, bullet[0] + 2, bullet[1] + 5, 0xFFFFFF00);
        }
        
        // 绘制敌机
        for (int[] enemy : enemies) {
            context.fill(enemy[0] - 8, enemy[1] - 8, enemy[0] + 8, enemy[1] + 8, 0xFFFF0000);
        }
        
        // 绘制分数
        context.drawText(textRenderer, "Score: " + score, 10, 10, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "WASD: Move, Space: Shoot", 10, 25, 0xFFAAAAAA, true);
        
        if (!gameRunning) {
            context.drawText(textRenderer, "Game Over! Press Restart", width/2 - 80, height/2, 0xFFFF0000, true);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void updateGame() {
        if (!gameRunning) return;
        
        tickCounter++;
        
        // 移动玩家
        if (keys[0] && playerX > 20) playerX -= 3; // A - left
        if (keys[1] && playerX < width - 20) playerX += 3; // D - right
        if (keys[2] && playerY > 20) playerY -= 3; // W - up
        if (keys[3] && playerY < height - 20) playerY += 3; // S - down
        
        // 移动子弹
        Iterator<int[]> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            int[] bullet = bulletIter.next();
            bullet[1] -= 5;
            if (bullet[1] < 0) {
                bulletIter.remove();
            }
        }
        
        // 生成敌机
        if (tickCounter % 30 == 0) {
            enemies.add(new int[]{random.nextInt(width - 40) + 20, 0});
        }
        
        // 移动敌机
        Iterator<int[]> enemyIter = enemies.iterator();
        while (enemyIter.hasNext()) {
            int[] enemy = enemyIter.next();
            enemy[1] += 2;
            if (enemy[1] > height) {
                enemyIter.remove();
            }
            
            // 检查玩家碰撞
            if (Math.abs(enemy[0] - playerX) < 15 && Math.abs(enemy[1] - playerY) < 15) {
                gameRunning = false;
            }
        }
        
        // 检查子弹击中敌机
        bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            int[] bullet = bulletIter.next();
            enemyIter = enemies.iterator();
            while (enemyIter.hasNext()) {
                int[] enemy = enemyIter.next();
                if (Math.abs(bullet[0] - enemy[0]) < 10 && Math.abs(bullet[1] - enemy[1]) < 10) {
                    bulletIter.remove();
                    enemyIter.remove();
                    score += 100;
                    break;
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 65: keys[0] = true; break; // A
            case 68: keys[1] = true; break; // D
            case 87: keys[2] = true; break; // W
            case 83: keys[3] = true; break; // S
            case 32: // Space - shoot
                if (gameRunning) {
                    bullets.add(new int[]{playerX, playerY - 15});
                }
                break;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 65: keys[0] = false; break; // A
            case 68: keys[1] = false; break; // D
            case 87: keys[2] = false; break; // W
            case 83: keys[3] = false; break; // S
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
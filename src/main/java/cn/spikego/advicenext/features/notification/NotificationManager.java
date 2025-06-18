package cn.spikego.advicenext.features.notification;

import cn.spikego.advicenext.event.impl.Render2DEvent;
import cn.spikego.advicenext.gui.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static final NotificationManager INSTANCE = new NotificationManager();
    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    // 最大显示的通知数量
    private static final int MAX_NOTIFICATIONS = 5;
    
    // 通知的尺寸
    private static final int NOTIFICATION_WIDTH = 200;
    private static final int NOTIFICATION_HEIGHT = 30;
    private static final int NOTIFICATION_SPACING = 5;
    
    private NotificationManager() {}
    
    public static NotificationManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 添加一个通知
     */
    public void addNotification(String title, String message, NotificationType type, int duration) {
        notifications.add(new Notification(title, message, type, duration));
        
        // 如果通知数量超过最大值，移除最早的通知
        while (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }
    }
    
    /**
     * 渲染所有通知
     */
    public void render(Render2DEvent event) {
        DrawContext context = event.getContext();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        
        int y = screenHeight - NOTIFICATION_HEIGHT - 10;
        
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notification = notifications.get(i);
            
            // 更新通知状态
            notification.update();
            
            // 如果通知已过期，移除它
            if (notification.isExpired()) {
                notifications.remove(i);
                continue;
            }
            
            // 计算通知的位置和动画
            float animation = notification.getAnimation();
            int x = (int)(screenWidth - NOTIFICATION_WIDTH * animation);
            
            // 绘制通知背景
            Color bgColor = new Color(25, 25, 25, (int)(220 * animation));
            context.fill(x, y, x + NOTIFICATION_WIDTH, y + NOTIFICATION_HEIGHT, bgColor.getRGB());
            
            // 绘制通知类型颜色条
            Color typeColor = notification.getType().getColor();
            context.fill(x, y, x + 3, y + NOTIFICATION_HEIGHT, typeColor.getRGB());
            
            // 绘制通知标题
            context.drawTextWithShadow(mc.textRenderer, notification.getTitle(), 
                                      x + 8, y + 5, Colors.currentColor().getRGB());
            
            // 绘制通知消息
            context.drawTextWithShadow(mc.textRenderer, notification.getMessage(), 
                                      x + 8, y + 18, Color.WHITE.getRGB());
            
            // 绘制进度条
            float progress = notification.getProgress();
            int progressWidth = (int)(NOTIFICATION_WIDTH * progress);
            context.fill(x, y + NOTIFICATION_HEIGHT - 2, x + progressWidth, y + NOTIFICATION_HEIGHT, typeColor.getRGB());
            
            // 更新下一个通知的Y坐标
            y -= NOTIFICATION_HEIGHT + NOTIFICATION_SPACING;
        }
    }
    
    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        INFO(new Color(0, 150, 255)),
        SUCCESS(new Color(0, 200, 0)),
        WARNING(new Color(255, 200, 0)),
        ERROR(new Color(255, 0, 0));
        
        private final Color color;
        
        NotificationType(Color color) {
            this.color = color;
        }
        
        public Color getColor() {
            return color;
        }
    }
    
    /**
     * 通知类
     */
    private static class Notification {
        private final String title;
        private final String message;
        private final NotificationType type;
        private final long creationTime;
        private final int duration; // 持续时间（毫秒）
        private float animation = 0f; // 动画进度 (0-1)
        
        public Notification(String title, String message, NotificationType type, int duration) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.duration = duration;
            this.creationTime = System.currentTimeMillis();
        }
        
        /**
         * 更新通知状态和动画
         */
        public void update() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - creationTime;
            
            // 淡入动画
            if (elapsedTime < 500) {
                animation = Math.min(1f, elapsedTime / 500f);
            } 
            // 淡出动画
            else if (elapsedTime > duration - 500) {
                animation = Math.max(0f, 1f - (elapsedTime - (duration - 500)) / 500f);
            } 
            // 完全显示
            else {
                animation = 1f;
            }
        }
        
        /**
         * 检查通知是否已过期
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime > duration;
        }
        
        /**
         * 获取通知的进度 (0-1)
         */
        public float getProgress() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - creationTime;
            return 1f - Math.min(1f, (float)elapsedTime / duration);
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getMessage() {
            return message;
        }
        
        public NotificationType getType() {
            return type;
        }
        
        public float getAnimation() {
            return animation;
        }
    }
}

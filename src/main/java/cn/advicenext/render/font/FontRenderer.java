package cn.advicenext.render.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FontRenderer {
    private final Map<Character, CharInfo> charMap = new HashMap<>();
    private int fontHeight = 0;
    private int textureId;
    private final String name;

    public FontRenderer(String fontPath, float fontSize) {
        this.name = fontPath.substring(fontPath.lastIndexOf('/') + 1);
        loadFont(fontPath, fontSize);
    }

    private void loadFont(String fontPath, float fontSize) {
        // 加载字体
        Font font;
        try {
            InputStream is = MinecraftClient.getInstance().getResourceManager()
                    .getResource(Identifier.of(fontPath)).get().getInputStream();
            font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(fontSize);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            // 如果加载失败，使用默认字体
            font = new Font("Arial", Font.PLAIN, (int) fontSize);
        }

        // 创建字体图像
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.dispose();

        // 计算所有字符的总宽度和高度
        int imageWidth = 0;
        int imageHeight = 0;
        
        // 支持ASCII字符和一些常用中文字符
        for (int i = 32; i < 256; i++) {
            char c = (char) i;
            int charWidth = metrics.charWidth(c);
            imageWidth += charWidth;
            imageHeight = Math.max(imageHeight, metrics.getHeight());
        }

        // 创建足够大的图像
        image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);
        metrics = g.getFontMetrics();
        g.setColor(Color.WHITE);

        int x = 0;
        for (int i = 32; i < 256; i++) {
            char c = (char) i;
            int charWidth = metrics.charWidth(c);
            
            g.drawString(String.valueOf(c), x, metrics.getAscent());
            
            charMap.put(c, new CharInfo(x, 0, charWidth, metrics.getHeight()));
            x += charWidth;
        }
        
        fontHeight = metrics.getHeight();
        g.dispose();

        // 创建OpenGL纹理
        textureId = createTexture(image);
    }

    private int createTexture(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        
        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        // 设置纹理参数
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // 上传纹理数据
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, image.getWidth(), image.getHeight(), 
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        
        return textureId;
    }

    public void drawString(MatrixStack matrices, String text, float x, float y, int color, boolean shadow) {
        if (text == null) return;
        
        // 保存当前OpenGL状态
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // 绑定纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        
        if (a == 0) a = 1.0f; // 如果没有指定alpha，默认为1
        
        GL11.glColor4f(r, g, b, a);
        
        float startX = x;
        
        // 如果需要阴影，先绘制阴影
        if (shadow) {
            GL11.glColor4f(r * 0.3f, g * 0.3f, b * 0.3f, a);
            renderText(text, startX + 1, y + 1);
            GL11.glColor4f(r, g, b, a);
        }
        
        // 绘制文本
        renderText(text, startX, y);
        
        // 恢复OpenGL状态
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
    
    private void renderText(String text, float x, float y) {
        float currentX = x;
        
        for (char c : text.toCharArray()) {
            if (charMap.containsKey(c)) {
                CharInfo charInfo = charMap.get(c);
                
                float texX1 = (float) charInfo.x / (float) getTextureWidth();
                float texY1 = (float) charInfo.y / (float) getTextureHeight();
                float texX2 = (float) (charInfo.x + charInfo.width) / (float) getTextureWidth();
                float texY2 = (float) (charInfo.y + charInfo.height) / (float) getTextureHeight();
                
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(texX1, texY1);
                GL11.glVertex2f(currentX, y);
                
                GL11.glTexCoord2f(texX1, texY2);
                GL11.glVertex2f(currentX, y + charInfo.height);
                
                GL11.glTexCoord2f(texX2, texY2);
                GL11.glVertex2f(currentX + charInfo.width, y + charInfo.height);
                
                GL11.glTexCoord2f(texX2, texY1);
                GL11.glVertex2f(currentX + charInfo.width, y);
                GL11.glEnd();
                
                currentX += charInfo.width;
            }
        }
    }
    
    public int getStringWidth(String text) {
        int width = 0;
        
        for (char c : text.toCharArray()) {
            if (charMap.containsKey(c)) {
                width += charMap.get(c).width;
            }
        }
        
        return width;
    }
    
    public int getHeight() {
        return fontHeight;
    }
    
    private int getTextureWidth() {
        int width = 0;
        for (CharInfo info : charMap.values()) {
            width += info.width;
        }
        return width;
    }
    
    private int getTextureHeight() {
        return fontHeight;
    }
    
    private static class CharInfo {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        
        public CharInfo(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
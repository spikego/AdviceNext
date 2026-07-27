package cn.advicenext.utility.client.render.engine;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * 顶点构建器 - 构建自定义顶点数据
 * 用于创建我们自己的顶点缓冲区，配合自定义着色器使用
 */
public class VertexBuilder {

    private final VertexFormat format;
    private final int vertexCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;
    private int currentVertex = 0;

    /**
     * 创建一个顶点构建器
     * @param format 顶点格式
     * @param vertexCount 顶点数量
     */
    public VertexBuilder(VertexFormat format, int vertexCount) {
        this.format = format;
        this.vertexCount = vertexCount;
        int size = vertexCount * format.getVertexSize();
        this.buffer = MemoryUtil.memAlloc(size);
        this.floatBuffer = this.buffer.asFloatBuffer();
    }

    /**
     * 添加 Position 属性
     */
    public VertexBuilder pos(float x, float y, float z) {
        floatBuffer.put(x).put(y).put(z);
        return this;
    }

    /**
     * 添加 UV 属性
     */
    public VertexBuilder uv(float u, float v) {
        floatBuffer.put(u).put(v);
        return this;
    }

    /**
     * 添加 Color 属性
     */
    public VertexBuilder color(float r, float g, float b, float a) {
        floatBuffer.put(r).put(g).put(b).put(a);
        return this;
    }

    /**
     * 添加 Color 属性（整数格式）
     */
    public VertexBuilder color(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        floatBuffer.put(r).put(g).put(b).put(a);
        return this;
    }

    /**
     * 结束当前顶点
     */
    public VertexBuilder endVertex() {
        currentVertex++;
        return this;
    }

    /**
     * 获取构建的顶点数据
     */
    public ByteBuffer build() {
        floatBuffer.flip();
        buffer.position(floatBuffer.position() * 4);
        buffer.limit(floatBuffer.position() * 4);
        return buffer;
    }

    /**
     * 获取顶点数量
     */
    public int getVertexCount() {
        return currentVertex;
    }

    /**
     * 释放资源
     */
    public void free() {
        MemoryUtil.memFree(buffer);
    }

    /**
     * 获取顶点格式的字节大小
     */
    public static int getVertexSize(VertexFormat format) {
        return format.getVertexSize();
    }

    /**
     * 创建索引缓冲区
     */
    public static ByteBuffer createQuadIndices(int quadCount) {
        ByteBuffer buffer = MemoryUtil.memAlloc(quadCount * 6 * 4); // 6 indices per quad, int = 4 bytes
        for (int i = 0; i < quadCount; i++) {
            int base = i * 4;
            buffer.putInt(base).putInt(base + 1).putInt(base + 2);
            buffer.putInt(base + 2).putInt(base + 1).putInt(base + 3);
        }
        buffer.flip();
        return buffer;
    }
}

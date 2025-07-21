package cn.advicenext.render.shader;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

/**
 * 着色器缓冲区工具类
 */
public class BUFFER {
    /**
     * Uniform提供者接口
     */
    public static class UniformProvider {
        public int pointer;
        public final Consumer<Integer> set;
        
        private UniformProvider(int pointer, Consumer<Integer> set) {
            this.pointer = pointer;
            this.set = set;
        }
        
        public void init(int program) {
            // 初始化Uniform
        }
        
        /**
         * 创建整数Uniform
         * @param name Uniform名称
         * @param value 整数值
         * @return UniformProvider实例
         */
        public static UniformProvider integer(String name, int value) {
            return new UniformProvider(-1, (pointer) -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer buffer = stack.mallocInt(1);
                    buffer.put(0, value);
                    GlStateManager._glUniform1(pointer, buffer);
                }
            }) {
                @Override
                public void init(int program) {
                    this.pointer = GlStateManager._glGetUniformLocation(program, name);
                }
            };
        }
        
        /**
         * 创建浮点数Uniform
         * @param name Uniform名称
         * @param value 浮点数值
         * @return UniformProvider实例
         */
        public static UniformProvider floatValue(String name, float value) {
            return new UniformProvider(-1, (pointer) -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer buffer = stack.mallocFloat(1);
                    buffer.put(0, value);
                    GlStateManager._glUniform1(pointer, buffer);
                }
            }) {
                @Override
                public void init(int program) {
                    this.pointer = GlStateManager._glGetUniformLocation(program, name);
                }
            };
        }
        
        /**
         * 创建二维向量Uniform
         * @param name Uniform名称
         * @param value 二维向量
         * @return UniformProvider实例
         */
        public static UniformProvider vec2(String name, Vector2f value) {
            return new UniformProvider(-1, (pointer) -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer buffer = stack.mallocFloat(2);
                    buffer.put(0, value.x);
                    buffer.put(1, value.y);
                    GlStateManager._glUniform2(pointer, buffer);
                }
            }) {
                @Override
                public void init(int program) {
                    this.pointer = GlStateManager._glGetUniformLocation(program, name);
                }
            };
        }
        
        /**
         * 创建三维向量Uniform
         * @param name Uniform名称
         * @param value 三维向量
         * @return UniformProvider实例
         */
        public static UniformProvider vec3(String name, Vector3f value) {
            return new UniformProvider(-1, (pointer) -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer buffer = stack.mallocFloat(3);
                    buffer.put(0, value.x);
                    buffer.put(1, value.y);
                    buffer.put(2, value.z);
                    GlStateManager._glUniform3(pointer, buffer);
                }
            }) {
                @Override
                public void init(int program) {
                    this.pointer = GlStateManager._glGetUniformLocation(program, name);
                }
            };
        }
        
        /**
         * 创建四维向量Uniform
         * @param name Uniform名称
         * @param value 四维向量
         * @return UniformProvider实例
         */
        public static UniformProvider vec4(String name, Vector4f value) {
            return new UniformProvider(-1, (pointer) -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer buffer = stack.mallocFloat(4);
                    buffer.put(0, value.x);
                    buffer.put(1, value.y);
                    buffer.put(2, value.z);
                    buffer.put(3, value.w);
                    GlStateManager._glUniform4(pointer, buffer);
                }
            }) {
                @Override
                public void init(int program) {
                    this.pointer = GlStateManager._glGetUniformLocation(program, name);
                }
            };
        }
        
        /**
         * 创建矩阵Uniform
         * @param name Uniform名称
         * @param value 4x4矩阵
         * @return UniformProvider实例
         */
        public static UniformProvider matrix4(String name, Matrix4f value) {
            return new UniformProvider(-1, (pointer) -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer buffer = stack.mallocFloat(16);
                    value.get(buffer);
                    GlStateManager._glUniformMatrix4(pointer, buffer);
                }
            }) {
                @Override
                public void init(int program) {
                    this.pointer = GlStateManager._glGetUniformLocation(program, name);
                }
            };
        }
    }
}
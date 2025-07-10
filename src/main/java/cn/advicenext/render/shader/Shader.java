package cn.advicenext.render.shader;


import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;

import java.io.Closeable;

public class Shader implements Closeable {
    protected int program;
    protected final BUFFER.UniformProvider[] uniforms;

    public Shader(String vertex, String fragment, BUFFER.UniformProvider[] uniforms) {
        this.uniforms = uniforms != null ? uniforms : new BUFFER.UniformProvider[0];

        int vertProgram = compileShader(vertex, GlConst.GL_VERTEX_SHADER);
        int fragProgram = compileShader(fragment, GlConst.GL_FRAGMENT_SHADER);

        this.program = GlStateManager.glCreateProgram();

        GlStateManager.glAttachShader(program, vertProgram);
        GlStateManager.glAttachShader(program, fragProgram);
        GlStateManager.glLinkProgram(program);

        // 检查链接状态
        if (GlStateManager.glGetProgrami(program, GlConst.GL_LINK_STATUS) == GlConst.GL_FALSE) {
            String log = GlStateManager.glGetProgramInfoLog(program, 1024);
            throw new RuntimeException("Failed to link shader program! Caused by: " + log);
        }

        // 清理
        GlStateManager.glDeleteShader(vertProgram);
        GlStateManager.glDeleteShader(fragProgram);

        for (BUFFER.UniformProvider uniform : this.uniforms) {
            uniform.init(program);
        }
    }

    private int compileShader(String source, int type) {
        int shader = GlStateManager.glCreateShader(type);
        GlStateManager.glShaderSource(shader, source);
        GlStateManager.glCompileShader(shader);

        // 检查编译状态
        if (GlStateManager.glGetShaderi(shader, GlConst.GL_COMPILE_STATUS) == GlConst.GL_FALSE) {
            String log = GlStateManager.glGetShaderInfoLog(shader, 1024);
            throw new RuntimeException("Failed to compile shader! Caused by: " + log);
        }

        return shader;
    }

    public void use() {
        GlStateManager._glUseProgram(this.program);
        for (BUFFER.UniformProvider uniform : uniforms) {
            uniform.set.accept(uniform.pointer);
        }
    }

    public void stop() {
        GlStateManager._glUseProgram(0);
    }

    @Override
    public void close() {
        GlStateManager.glDeleteProgram(this.program);
    }
}
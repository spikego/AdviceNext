package cn.advicenext.render.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUniform;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.function.IntConsumer;

public class BUFFER {
    private static final FloatBuffer BUFFER = MemoryUtil.memAllocFloat(16);

    public static final UniformProvider ProjMatUniform = new UniformProvider("projMat", pointer -> {
        BUFFER.position(0);
        RenderSystem.getProjectionMatrix().get(BUFFER);
        GL20.glUniformMatrix4fv(pointer, false, BUFFER);
    });

    public static class UniformProvider {
        public final String name;
        public final IntConsumer set;
        public int pointer = -1;

        public UniformProvider(String name, IntConsumer set) {
            this.name = name;
            this.set = set;
        }

        public void init(int program) {
            pointer = GlUniform.getUniformLocation(program, name);
        }
    }
}
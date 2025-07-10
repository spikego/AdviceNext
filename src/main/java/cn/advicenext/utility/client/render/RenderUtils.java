package cn.advicenext.utility.client.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.Identifier;

public class RenderUtils {
    
    public static void enableBlend() {
        RenderSystem.assertOnRenderThread();
        GlStateManager._enableBlend();
    }
    
    public static void disableBlend() {
        RenderSystem.assertOnRenderThread();
        GlStateManager._disableBlend();
    }
}
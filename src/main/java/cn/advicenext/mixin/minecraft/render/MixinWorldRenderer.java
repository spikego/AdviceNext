package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.event.impl.Render3DEvent;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.advicenext.event.EventBus.post;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Shadow @Final
    private DefaultFramebufferSet framebufferSet;

    @Shadow @Final
    private BufferBuilderStorage bufferBuilders;

    @Inject(
        method = "renderLateDebug",
        at = @At("TAIL")
    )
    private void onRenderLateDebugTail(
        FrameGraphBuilder frameGraphBuilder,
        CameraRenderState cameraRenderState,
        GpuBufferSlice fogBuffer,
        Matrix4f posMatrix,
        CallbackInfo ci
    ) {
        FramePass framePass = frameGraphBuilder.createPass("advice_3d");
        this.framebufferSet.mainFramebuffer = framePass.transfer(this.framebufferSet.mainFramebuffer);
        Handle<Framebuffer> handle = this.framebufferSet.mainFramebuffer;

        framePass.setRenderer(() -> {
            RenderSystem.setShaderFog(fogBuffer);
            MatrixStack matrixStack = new MatrixStack();
            VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
            RenderSystem.outputColorTextureOverride = handle.get().getColorAttachmentView();
            RenderSystem.outputDepthTextureOverride = handle.get().getDepthAttachmentView();

            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(framebuffer.getDepthAttachment(), 1.0);

            Render3DEvent event = new Render3DEvent(
                matrixStack, 0.0F, posMatrix, null, immediate, cameraRenderState
            );
            post(event);

            immediate.drawCurrentLayer();

            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
        });
    }
}
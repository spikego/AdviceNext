package cn.advicenext.event.impl;

import cn.advicenext.event.Event;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class Render3DEvent extends Event {
    private final MatrixStack matrices;
    private final float tickDelta;
    private final Matrix4f positionMatrix;
    private final Camera camera;
    private final VertexConsumerProvider.Immediate vertexConsumers;
    private final CameraRenderState cameraRenderState;

    public Render3DEvent(MatrixStack matrices, float tickDelta, Matrix4f positionMatrix, Camera camera,
                         VertexConsumerProvider.Immediate vertexConsumers, CameraRenderState cameraRenderState) {
        this.matrices = matrices;
        this.tickDelta = tickDelta;
        this.positionMatrix = positionMatrix;
        this.camera = camera;
        this.vertexConsumers = vertexConsumers;
        this.cameraRenderState = cameraRenderState;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public Matrix4f getPositionMatrix() {
        return positionMatrix;
    }

    public Camera getCamera() {
        return camera;
    }

    public VertexConsumerProvider.Immediate getVertexConsumers() {
        return vertexConsumers;
    }

    public CameraRenderState getCameraRenderState() {
        return cameraRenderState;
    }
}
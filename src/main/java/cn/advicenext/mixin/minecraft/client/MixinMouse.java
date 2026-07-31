package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.utility.client.input.MouseAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mouse.class)
public abstract class MixinMouse implements MouseAccessor {

    @Shadow
    @Final
    private MinecraftClient client;

    @Invoker("onMouseButton")
    public abstract void invokeOnMouseButton(long window, MouseInput input, int action);

    @Unique
    @Override
    public void adviceNext$simulateLeftClick() {
        long window = client.getWindow().getHandle();
        this.invokeOnMouseButton(window, new MouseInput(GLFW.GLFW_MOUSE_BUTTON_1, 0), GLFW.GLFW_PRESS);
        this.invokeOnMouseButton(window, new MouseInput(GLFW.GLFW_MOUSE_BUTTON_1, 0), GLFW.GLFW_RELEASE);
    }
}
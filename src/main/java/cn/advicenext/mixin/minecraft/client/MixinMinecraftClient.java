package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.impl.world.FastPlace;
import cn.advicenext.gui.mainmenu.MainMenuScreen;

import cn.advicenext.utility.minecraft.player.RotationManager;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow
    @Final
    private Window window;

    @Shadow private Screen currentScreen;

    @Shadow
    private int itemUseCooldown;

    @Unique
    private float adviceNext$visualYaw;

    @Unique
    private float adviceNext$visualPitch;

    @Inject(at = @At("HEAD"), method = "tick")
    private void init(CallbackInfo Info) {
        EventBus.post(new TickEvent());

        // RotationManager 每 tick 推进 + AntiAim 提交旋转请求
        if (MinecraftClient.getInstance().player != null) {
            RotationManager.INSTANCE.tick();
        }
    }

    @Redirect(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
    public void setTitle(Window instance, String title) {
        String pageTitle = currentScreen != null ? currentScreen.getTitle().getString() : "Game";
        this.window.setTitle("AdviceNext # " + pageTitle);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (FastPlace.isEnabled()) {
            this.itemUseCooldown = FastPlace.getDelay();
        }
    }


    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        // Replace TitleScreen with our custom main menu
        if (screen instanceof TitleScreen) {
            MinecraftClient.getInstance().setScreen(new MainMenuScreen());
            ci.cancel();
        }
    }

    // ==================== 交互旋转修复 (Interaction Rotation Fix) ====================

    /**
     * 在 doItemUse 之前：保存视觉旋转，替换为服务端旋转
     * 这样放置方块、使用物品、开关门等交互会使用正确的旋转角度
     */
    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onPreDoItemUse(CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (!RotationManager.INSTANCE.hasActiveRequest() && !RotationUtils.isFakeRotation()) return;

        adviceNext$visualYaw = player.getYaw();
        adviceNext$visualPitch = player.getPitch();

        float serverYaw = RotationManager.INSTANCE.hasActiveRequest()
                ? RotationManager.INSTANCE.getServerYaw()
                : RotationUtils.getServerYaw();
        float serverPitch = RotationManager.INSTANCE.hasActiveRequest()
                ? RotationManager.INSTANCE.getServerPitch()
                : RotationUtils.getServerPitch();

        player.setYaw(serverYaw);
        player.setPitch(serverPitch);
    }

    /**
     * 在 doItemUse 之后：恢复视觉旋转
     */
    @Inject(method = "doItemUse", at = @At("TAIL"))
    private void onPostDoItemUse(CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (!RotationManager.INSTANCE.hasActiveRequest() && !RotationUtils.isFakeRotation()) return;

        player.setYaw(adviceNext$visualYaw);
        player.setPitch(adviceNext$visualPitch);
    }
}
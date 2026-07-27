package cn.advicenext.mixin.minecraft.client;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 强制关闭冲刺：只改 sprint boolean，绝对不碰 forward/backward/left/right
 *
 * 当 AntiAim Backward 激活且玩家有 WASD 方向输入时，
 * 把 PlayerInput 的 sprint 字段强制设为 false。
 *
 * 注意：@Mixin 目标必须是 KeyboardInput.class 而非 Input.class。
 * Input.tick() 是空方法且被 KeyboardInput 覆写（未调用 super.tick()），
 * 注入到 Input.tick 的代码永远不会执行。playerInput 是 public 字段，
 * 可直接通过强转访问。
 */
@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInputSprint {

    @Inject(method = "tick", at = @At("TAIL"))
    private void afterTick(CallbackInfo ci) {
        Input self = (Input) (Object) this;
        PlayerInput playerInput = self.playerInput;
        if (playerInput == null) return;

        // 只在玩家有方向输入时强制关冲刺（静止时无所谓）
        if (!playerInput.forward() && !playerInput.backward() && !playerInput.left() && !playerInput.right()) {
            return;
        }
        if (!playerInput.sprint()) return; // 本来就没冲刺，不用改

        // 强制 sprint=false，其余输入完全不动
        self.playerInput = new PlayerInput(
            playerInput.forward(), playerInput.backward(),
            playerInput.left(), playerInput.right(),
            playerInput.jump(), playerInput.sneak(),
            false // sprint = false
        );
    }
}

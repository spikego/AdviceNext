package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.movement.Sprint;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInputSprint {

    @Inject(method = "tick", at = @At("TAIL"))
    private void afterTick(CallbackInfo ci) {
        Sprint sprint = ModuleManager.getModule(Sprint.class);
        if (sprint == null || !sprint.getEnabled()) return;

        Input self = (Input) (Object) this;
        PlayerInput playerInput = self.playerInput;
        if (playerInput == null) return;

        if (!playerInput.forward() && !playerInput.backward() && !playerInput.left() && !playerInput.right()) {
            return;
        }
        if (!playerInput.sprint()) return;

        self.playerInput = new PlayerInput(
            playerInput.forward(), playerInput.backward(),
            playerInput.left(), playerInput.right(),
            playerInput.jump(), playerInput.sneak(),
            false
        );
    }
}
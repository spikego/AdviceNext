package cn.spikego.advicenext.features.module.impl.combat;

import cn.spikego.advicenext.event.impl.TickEvent;
import cn.spikego.advicenext.features.module.Module;
import cn.spikego.advicenext.features.module.Category;
import cn.spikego.advicenext.features.value.slider.DoubleSetting;
import cn.spikego.advicenext.features.value.slider.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.consume.UseAction;

import static cn.spikego.advicenext.features.command.CommandManager.mc;

public class AutoClicker extends Module {
    private final DoubleSetting cps = new DoubleSetting("CPS", "15", 12.0, 30.0, 0.0, 0.3);
    private long lastClick = 0;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks for you", Category.COMBAT);
        this.settings.add(cps);
    }

    @Override
    public void onTick(TickEvent event) {
        if (!this.enabled) return;

        double cpsValue = ((Number)cps.getValue()).doubleValue();
        long delay = (long)(1000.0 / cpsValue);


        long currentTime = System.currentTimeMillis();

        if (currentTime - lastClick >= delay) {
            if (mc.player == null) return;

            // 检查是否按下攻击键且未打开界面
            if (mc.options.attackKey.isPressed() && mc.currentScreen == null) {
                // 阻止格挡时点击
                if (mc.player.isUsingItem() && mc.player.getActiveItem().getUseAction() == UseAction.BLOCK) {
                    return;
                }

                // 执行点击
                mc.player.swingHand(Hand.MAIN_HAND);
                if (mc.crosshairTarget != null && mc.crosshairTarget.getType().name().equals("ENTITY")) {
                    // 需要类型转换
                    net.minecraft.util.hit.EntityHitResult entityHit = (net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget;
                    if (mc.interactionManager != null) {
                        mc.interactionManager.attackEntity(mc.player, entityHit.getEntity());
                    }
                }
                lastClick = currentTime;
            }
        }
    }

    @Override
    public void onEnable() {
        lastClick = 0;
    }

    @Override
    public void onDisable() {}
}
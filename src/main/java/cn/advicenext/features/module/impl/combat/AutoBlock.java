package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.Listener;
import cn.advicenext.event.impl.AttackEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.minecraft.combat.AttackUtils;
import cn.advicenext.utility.minecraft.network.LagUtils;
import cn.advicenext.utility.minecraft.network.lag.LagDirection;
import cn.advicenext.utility.minecraft.network.lag.LagRequest;
import cn.advicenext.utility.minecraft.network.lag.TimedTimeout;
import net.minecraft.util.Hand;

public class AutoBlock extends Module {

    public static AutoBlock INSTANCE;

    private final ModeSetting mode = new ModeSetting("Mode", "Block mode", "Normal",
            java.util.List.of("Normal", "Packet", "Lag"));
    private final IntSetting blockDuration = new IntSetting("BlockDuration", "Block hold duration (ms)", 100, 500, 0, 25);
    private final IntSetting lagDelay = new IntSetting("LagDelay", "Lag delay (ms)", 200, 1000, 50, 25,
            () -> mode.is("Lag"));
    private final BooleanSetting withKA = new BooleanSetting("WithKA", "Only block when KA is attacking", false);
    private final BooleanSetting weaponOnly = new BooleanSetting("WeaponOnly", "Only when holding sword", true);

    private boolean blocking;
    private long scheduledUnblock;

    public AutoBlock() {
        super("AutoBlock", "Automatically blocks with sword", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(mode);
        this.settings.add(blockDuration);
        this.settings.add(lagDelay);
        this.settings.add(withKA);
        this.settings.add(weaponOnly);
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
        blocking = false;
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        stopBlocking();
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (blocking && System.currentTimeMillis() >= scheduledUnblock) {
            stopBlocking();
        }

        if (!canBlock()) {
            stopBlocking();
            return;
        }

        if (!withKA.getValue() && !blocking) {
            if (mc.options.attackKey.isPressed() && mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                startBlocking();
            }
        }
    }

    @Listener
    public void onAttack(AttackEvent event) {
        if (!canBlock()) return;

        if (withKA.getValue()) {
            KillAura ka = KillAura.INSTANCE;
            if (ka == null || !ka.getEnabled()) return;
        }

        startBlocking();
    }

    private boolean canBlock() {
        if (mc.player == null || mc.player.isDead()) return false;

        if (weaponOnly.getValue() && !AttackUtils.isHoldingWeapon()) return false;

        if (withKA.getValue()) {
            KillAura ka = KillAura.INSTANCE;
            if (ka == null || !ka.getEnabled() || ka.getTarget() == null) return false;
        }

        return true;
    }

    private void startBlocking() {
        if (blocking) return;

        if (mode.is("Normal")) {
            mc.options.useKey.setPressed(true);
        } else if (mode.is("Packet") || mode.is("Lag")) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }

        if (mode.is("Lag")) {
            LagUtils.requestLag(new LagRequest(LagDirection.ONLY_OUTBOUND, new TimedTimeout(lagDelay.getValue())));
        }

        blocking = true;
        scheduledUnblock = System.currentTimeMillis() + blockDuration.getValue();
    }

    private void stopBlocking() {
        if (!blocking) return;

        if (mode.is("Normal")) {
            mc.options.useKey.setPressed(false);
        }

        blocking = false;
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}
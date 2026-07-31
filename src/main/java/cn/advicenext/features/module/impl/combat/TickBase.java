package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.*;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class TickBase extends Module {

    public static TickBase INSTANCE;

    private final DoubleSetting tickRate = new DoubleSetting("TickRate", "Game tick rate multiplier", 1.5, 0.1, 10.0, 0.1);
    private final BooleanSetting boost = new BooleanSetting("Boost", "Boost next tick", false);
    private final IntSetting boostTicks = new IntSetting("BoostTicks", "Boost duration in ticks", 10, 0, 100, 1,
            () -> boost.getValue());
    private final DoubleSetting boostAmount = new DoubleSetting("BoostAmount", "Boost multiplier", 2.0, 0.1, 10.0, 0.1,
            () -> boost.getValue());
    private final BooleanSetting timer = new BooleanSetting("Timer", "Use timer speed", true);

    private final ModeSetting mode = new ModeSetting("Mode", "Tick skip mode", "Past",
            List.of("Past", "Future"));
    private final DoubleSetting rangeStart = new DoubleSetting("RangeStart", "Min attack range", 2.5, 8.0, 0.0, 0.1);
    private final DoubleSetting rangeEnd = new DoubleSetting("RangeEnd", "Max attack range", 4.0, 8.0, 0.0, 0.1);
    private final DoubleSetting balanceRecovery = new DoubleSetting("BalanceRecovery", "Balance recovery per tick", 1.0, 2.0, 0.0, 0.1);
    private final IntSetting balanceMax = new IntSetting("BalanceMax", "Max balance value", 20, 200, 0, 1);
    private final IntSetting maxTicks = new IntSetting("MaxTicks", "Max ticks to skip at once", 4, 20, 1, 1);
    private final BooleanSetting pauseOnFlag = new BooleanSetting("PauseOnFlag", "Reset balance on position flag", true);
    private final IntSetting pause = new IntSetting("Pause", "Extra pause ticks after skip", 0, 20, 0, 1);
    private final IntSetting cooldown = new IntSetting("Cooldown", "Cooldown after skip", 0, 100, 0, 1);
    private final BooleanSetting forceGround = new BooleanSetting("ForceGround", "Only skip to ground positions", false);
    private final BooleanSetting requiresKillAura = new BooleanSetting("RequiresKillAura", "Only work with KillAura", true);
    private final BooleanSetting showPath = new BooleanSetting("ShowPath", "Show predicted path", true);

    private int boostTickCounter = 0;
    private boolean boostActive = false;

    private float tickBalance = 0f;
    private boolean reachedLimit = false;
    private int ticksToSkip = 0;
    private int cooldownTicks = 0;
    private final List<TickSnapshot> tickBuffer = new ArrayList<>();

    public TickBase() {
        super("TickBase", "Manipulates client tick rate", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(tickRate);
        this.settings.add(boost);
        this.settings.add(boostTicks);
        this.settings.add(boostAmount);
        this.settings.add(timer);
        this.settings.add(mode);
        this.settings.add(rangeStart);
        this.settings.add(rangeEnd);
        this.settings.add(balanceRecovery);
        this.settings.add(balanceMax);
        this.settings.add(maxTicks);
        this.settings.add(pauseOnFlag);
        this.settings.add(pause);
        this.settings.add(cooldown);
        this.settings.add(forceGround);
        this.settings.add(requiresKillAura);
        this.settings.add(showPath);
    }

    @Override
    public void onEnable() {
        boostTickCounter = 0;
        boostActive = false;
        tickBalance = 0f;
        reachedLimit = false;
        ticksToSkip = 0;
        cooldownTicks = 0;
        tickBuffer.clear();
    }

    @Override
    public void onDisable() {
        boostTickCounter = 0;
        boostActive = false;
        tickBalance = 0f;
        reachedLimit = false;
        ticksToSkip = 0;
        cooldownTicks = 0;
        tickBuffer.clear();
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (ticksToSkip > 0) {
            ticksToSkip--;
            return;
        }

        if (!timer.getValue()) return;

        if (boost.getValue() && boostActive) {
            boostTickCounter++;
            if (boostTickCounter >= boostTicks.getValue()) {
                boostActive = false;
                boostTickCounter = 0;
            }
        }

        if (tickBuffer.isEmpty()) return;
        if (mc.player.getVehicle() != null) return;

        LivingEntity enemy = TargetUtils.getClosestPlayer(rangeEnd.getValue());
        if (enemy == null) return;

        double currentDist = mc.player.getEyePos().distanceTo(enemy.getEyePos());
        double minRange = rangeStart.getValue();
        double maxRange = rangeEnd.getValue();

        int bestTick = -1;
        for (int i = 0; i < Math.min(tickBuffer.size(), maxTicks.getValue()); i++) {
            TickSnapshot snap = tickBuffer.get(i);
            double distSq = snap.pos.distanceTo(enemy.getEyePos());
            if (distSq < currentDist && distSq >= minRange && distSq <= maxRange) {
                if (forceGround.getValue() && !snap.onGround) continue;
                bestTick = i;
                break;
            }
        }

        if (bestTick <= 0) return;

        if (requiresKillAura.getValue() && !KillAura.INSTANCE.getEnabled()) return;

        if (mode.is("Past")) {
            int totalSkip = bestTick + pause.getValue();
            try {
                for (int i = 0; i < bestTick; i++) {
                    mc.tick();
                    tickBalance -= 1;
                }
            } catch (Exception ignored) {}
            ticksToSkip = totalSkip;
        } else {
            int totalSkipped = 0;
            for (int i = 0; i < bestTick; i++) {
                try {
                    mc.tick();
                    tickBalance -= 1;
                    totalSkipped++;
                } catch (Exception ignored) {
                    break;
                }
                if (requiresKillAura.getValue() && !KillAura.INSTANCE.getEnabled()) break;
            }
            ticksToSkip = totalSkipped + pause.getValue();
        }

        cooldownTicks = cooldown.getValue();
    }

    @Override
    public void onMovement(MovementEvent event) {
        if (mc.player == null || mc.player.getVehicle() != null) return;

        tickBuffer.clear();

        if (tickBalance <= 0) {
            reachedLimit = true;
        }
        if (tickBalance * 2 > balanceMax.getValue()) {
            reachedLimit = false;
        }
        if (tickBalance <= balanceMax.getValue()) {
            tickBalance += balanceRecovery.getValue().floatValue();
        }

        if (reachedLimit) return;

        int simTicks = Math.min((int) tickBalance, maxTicks.getValue());
        if (simTicks <= 0) return;

        Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d vel = mc.player.getVelocity();
        boolean onGround = mc.player.isOnGround();

        for (int i = 0; i < simTicks; i++) {
            pos = pos.add(vel.x, vel.y, vel.z);
            vel = vel.multiply(0.98, 0.98, 0.98);
            if (onGround) {
                vel = vel.multiply(0.6, 1.0, 0.6);
            }
            vel = vel.add(0, -0.08, 0);

            tickBuffer.add(new TickSnapshot(new Vec3d(pos.x, pos.y, pos.z), onGround));
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket && pauseOnFlag.getValue()) {
            tickBalance = 0f;
            reachedLimit = true;
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!showPath.getValue() || tickBuffer.isEmpty()) return;

        VertexConsumer vc = event.getVertexConsumers().getBuffer(RenderLayers.lines());
        int color = 0xFFFFFFFF;

        for (int i = 0; i < tickBuffer.size() - 1; i++) {
            Vec3d from = tickBuffer.get(i).pos;
            Vec3d to = tickBuffer.get(i + 1).pos;
            Render3DEngine.drawLine3D(event.getMatrices(), vc, event.getCameraRenderState(), from, to, color, 1.5F);
        }
    }

    public float getTickRate() {
        if (!enabled || !timer.getValue()) return 1.0F;

        if (boost.getValue() && boostActive) {
            return boostAmount.getValue().floatValue();
        }

        return tickRate.getValue().floatValue();
    }

    public void startBoost() {
        if (boost.getValue()) {
            boostActive = true;
            boostTickCounter = 0;
        }
    }

    public float getTimerSpeed() {
        float rate = getTickRate();
        return MathHelper.clamp(rate, 0.1F, 10.0F);
    }

    private static class TickSnapshot {
        final Vec3d pos;
        final boolean onGround;

        TickSnapshot(Vec3d pos, boolean onGround) {
            this.pos = pos;
            this.onGround = onGround;
        }
    }
}
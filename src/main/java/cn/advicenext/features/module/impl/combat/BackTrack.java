package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.Listener;
import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.network.LagUtils;
import cn.advicenext.utility.minecraft.network.TrackedEntityPosition;
import cn.advicenext.utility.minecraft.network.lag.LagDirection;
import cn.advicenext.utility.minecraft.network.lag.LagRequest;
import cn.advicenext.utility.minecraft.network.lag.TimedTimeout;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BackTrack extends Module {

    public static BackTrack INSTANCE;

    private final IntSetting delay = new IntSetting("Delay", "Packet delay in ms", 100, 0, 1000, 10);
    private final IntSetting maxDelay = new IntSetting("MaxDelay", "Maximum packet delay in ms", 150, 0, 1000, 10);
    private final IntSetting range = new IntSetting("Range", "Target range", 3, 0, 10, 1);
    private final IntSetting nextDelay = new IntSetting("NextDelay", "Delay before next backtrack (ms)", 0, 0, 2000, 10);
    private final IntSetting buffer = new IntSetting("Buffer", "Extra tracking time (ms)", 500, 0, 2000, 10);
    private final IntSetting chance = new IntSetting("Chance", "Chance to backtrack", 100, 0, 100, 1);
    private final BooleanSetting pauseOnHurt = new BooleanSetting("PauseOnHurt", "Pause when target is hurt", false);
    private final IntSetting hurtTime = new IntSetting("HurtTime", "Hurt time threshold", 3, 0, 10, 1,
            () -> pauseOnHurt.getValue());
    private final BooleanSetting esp = new BooleanSetting("ESP", "Show tracked position", true);

    private final Random random = new Random();
    private Entity target = null;
    private final TrackedEntityPosition position = new TrackedEntityPosition();
    private int currentDelay = 0;
    private boolean isLagging = false;
    private long lastBacktrackTime = 0;
    private long trackingBufferStartTime = 0;
    private long lastAttackTime = 0;

    public BackTrack() {
        super("BackTrack", "Lag entities to hit their past positions", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(delay);
        this.settings.add(maxDelay);
        this.settings.add(range);
        this.settings.add(nextDelay);
        this.settings.add(buffer);
        this.settings.add(chance);
        this.settings.add(pauseOnHurt);
        this.settings.add(hurtTime);
        this.settings.add(esp);
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
        clear(false);
        currentDelay = delay.getValue() + random.nextInt(Math.max(1, maxDelay.getValue() - delay.getValue() + 1));
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        clear(true);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.world == null || mc.player == null) {
            clear(true);
            return;
        }

        updateTarget();

        if (isLagging && !shouldCancelPackets()) {
            flushIncoming();
            clear();
        }

        if (!isLagging) {
            currentDelay = delay.getValue() + random.nextInt(Math.max(1, maxDelay.getValue() - delay.getValue() + 1));
        }

        if (hasQueuedIncoming()) {
            flushExpired(System.currentTimeMillis() - currentDelay);
        }
    }

    private void updateTarget() {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == mc.player) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist <= range.getValue() && dist < bestDist) {
                best = entity;
                bestDist = dist;
            }
        }

        if (best != null) {
            processTarget(best);
        } else {
            clear();
        }
    }

    private void processTarget(Entity entity) {
        if (!shouldBacktrack(entity)) return;

        if (entity != target) {
            clear(false);
            position.setBaseFrom(entity);
        }

        target = entity;
    }

    private boolean shouldBacktrack(Entity target) {
        double dist = mc.player.distanceTo(target);
        boolean inRange = dist <= range.getValue();

        if (inRange) {
            trackingBufferStartTime = System.currentTimeMillis();
        }

        long bufferExpired = System.currentTimeMillis() - trackingBufferStartTime;
        boolean withinBuffer = inRange || bufferExpired < buffer.getValue();

        if (!(target instanceof LivingEntity living)) return false;

        if (pauseOnHurt.getValue() && living.hurtTime >= hurtTime.getValue()) return false;

        long timeSinceLastBacktrack = System.currentTimeMillis() - lastBacktrackTime;
        if (timeSinceLastBacktrack < nextDelay.getValue()) return false;

        long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
        if (timeSinceLastAttack > 1000) return false;

        return withinBuffer && random.nextInt(100) < chance.getValue() && mc.player.age > 10;
    }

    public boolean shouldCancelPackets() {
        return target != null && target.isAlive() && shouldBacktrack(target);
    }

    private boolean hasQueuedIncoming() {
        return isLagging;
    }

    private void flushIncoming() {
        isLagging = false;
    }

    private void flushExpired(long cutoffTime) {
        if (!shouldCancelPackets()) {
            flushIncoming();
        }
    }

    private void clear(boolean handlePackets) {
        if (handlePackets) {
            flushIncoming();
        }

        if (target != null) {
            lastBacktrackTime = System.currentTimeMillis();
        }

        target = null;
        position.reset();
    }

    private void clear() {
        clear(true);
    }

    @Listener
    public void onPacket(PacketEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (event.getOrigin() != PacketEvent.TransferOrigin.RECEIVE) return;

        net.minecraft.network.packet.Packet<?> packet = event.getPacket();
        if (packet == null) return;

        Entity target = this.target;
        if (target == null) return;

        if (packet instanceof PlaySoundS2CPacket sound) {
            if (sound.getSound().value() == SoundEvents.ENTITY_PLAYER_HURT
                    || sound.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE) {
                return;
            }
        }

        if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof DisconnectS2CPacket) {
            clear(true);
            return;
        }

        if (packet instanceof HealthUpdateS2CPacket health) {
            if (health.getHealth() <= 0) {
                clear(true);
                return;
            }
        }

        if (shouldCancelPackets()) {
            Vec3d pos = position.handlePacket(packet, mc.world, target);
            if (pos != null) {
                if (target.squaredDistanceTo(pos) < target.squaredDistanceTo(mc.player)) {
                    flushIncoming();
                    return;
                }
            }

            isLagging = true;
            LagUtils.INSTANCE.requestLag(new LagRequest(LagDirection.ONLY_INBOUND, new TimedTimeout(999999)));
            event.cancelled = true;
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!esp.getValue()) return;
        if (target == null) return;

        Vec3d trackedPos = position.getCurrentPosition();
        if (trackedPos == Vec3d.ZERO) return;

        Box box = target.getBoundingBox().offset(
                trackedPos.x - target.getX(),
                trackedPos.y - target.getY(),
                trackedPos.z - target.getZ()
        );

        VertexConsumer vertexConsumer = event.getVertexConsumers().getBuffer(RenderLayers.lines());
        int lineColor = 0xDC00FF00;

        Render3DEngine.drawBox3D(event.getMatrices(), vertexConsumer, event.getCameraRenderState(), box, lineColor, 1.5F);
    }

    @Override
    public String getDisplayValue() {
        return currentDelay + "ms";
    }
}
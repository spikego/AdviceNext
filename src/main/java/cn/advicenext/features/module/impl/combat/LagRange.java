package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.Listener;
import cn.advicenext.event.impl.AttackEvent;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ColorSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.combat.AttackUtils;
import cn.advicenext.utility.minecraft.network.LagUtils;
import cn.advicenext.utility.minecraft.network.lag.LagDirection;
import cn.advicenext.utility.minecraft.network.lag.LagRequest;
import cn.advicenext.utility.minecraft.network.lag.ModuleTimeout;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

public class LagRange extends Module {

    private static final double MINIMUM_DISTANCE_SQ = 3.0 * 3.0;

    private final DoubleSetting range = new DoubleSetting("Range", "Target range", 6.0, 3.0, 10.0, 0.1);
    private final IntSetting maxDelay = new IntSetting("MaxDelay", "Maximum delay (ms)", 200, 50, 1000, 10);
    private final BooleanSetting sprintReset = new BooleanSetting("SprintReset", "Flush on sprint", true);
    private final BooleanSetting blockSword = new BooleanSetting("BlockSword", "Flush on block", true);
    private final BooleanSetting splashPotion = new BooleanSetting("SplashPotion", "Flush on splash potion", true);
    private final BooleanSetting holdingWeapon = new BooleanSetting("HoldingWeapon", "Require holding weapon", true);
    private final BooleanSetting indicator = new BooleanSetting("Indicator", "Show real position indicator", true);
    private final ColorSetting indicatorColor = new ColorSetting("IndicatorColor", "Indicator color", new Color(255, 0, 0, 100).getRGB());
    private final BooleanSetting indicatorFilled = new BooleanSetting("IndicatorFilled", "Filled indicator", false);

    private Entity currentTarget;
    private double lastDistSq = -1;
    private boolean isLagging;
    private int lastSelfHurtTime;
    private int lastTargetHurtTime;
    private int hitMarkedEntityId;
    private boolean lastSprintState;
    private boolean lastBlockingState;
    private LagRequest outboundLag;

    public LagRange() {
        super("LagRange", "Delays packets to hit enemies from range", Category.COMBAT);
        this.settings.add(range);
        this.settings.add(maxDelay);
        this.settings.add(sprintReset);
        this.settings.add(blockSword);
        this.settings.add(splashPotion);
        this.settings.add(holdingWeapon);
        this.settings.add(indicator);
        this.settings.add(indicatorColor);
        this.settings.add(indicatorFilled);
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
        resetState();
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        flushLag();
        resetState();
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.player.isDead()) {
            if (isLagging) flushLag();
            resetState();
            return;
        }

        if (mc.currentScreen != null) {
            if (isLagging) flushLag();
            resetState();
            return;
        }

        double rangeSq = range.getValue() * range.getValue();
        boolean moving = isMoving();

        Entity nextTarget = findTarget(rangeSq);
        if (!sameTarget(nextTarget)) {
            if (isLagging) flushLag();
            lastDistSq = -1;
            hitMarkedEntityId = -1;
            lastTargetHurtTime = nextTarget != null ? ((LivingEntity) nextTarget).hurtTime : 0;
        }
        currentTarget = nextTarget;

        if (currentTarget != null) {
            double distSq = mc.player.squaredDistanceTo(currentTarget);

            if (isLagging) {
                if (distSq > rangeSq) {
                    flushLag();
                    lastDistSq = distSq;
                    hitMarkedEntityId = -1;
                    lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                    return;
                }

                if (lastDistSq >= 0 && distSq >= lastDistSq) {
                    boolean hitHold = hitMarkedEntityId == currentTarget.getId()
                        && distSq <= MINIMUM_DISTANCE_SQ
                        && mc.player.hurtTime == 0;
                    if (!hitHold) {
                        flushLag();
                        lastDistSq = distSq;
                        lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                        return;
                    }
                }

                int hurtTime = mc.player.hurtTime;
                if (hurtTime > lastSelfHurtTime) {
                    flushLag();
                    hitMarkedEntityId = -1;
                    lastSelfHurtTime = hurtTime;
                    lastDistSq = distSq;
                    lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                    return;
                }
                lastSelfHurtTime = hurtTime;

                if (holdingWeapon.getValue() && !AttackUtils.isHoldingWeapon()) {
                    flushLag();
                    lastDistSq = distSq;
                    lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                    return;
                }

                if (sprintReset.getValue()) {
                    boolean sprintingNow = mc.player.isSprinting();
                    if (sprintingNow && !lastSprintState) {
                        flushLag();
                        lastSprintState = sprintingNow;
                        lastDistSq = distSq;
                        lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                        return;
                    }
                    lastSprintState = sprintingNow;
                }

                if (blockSword.getValue()) {
                    boolean blockingNow = mc.player.isBlocking();
                    if (blockingNow && !lastBlockingState) {
                        flushLag();
                        lastBlockingState = blockingNow;
                        lastDistSq = distSq;
                        lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                        return;
                    }
                    lastBlockingState = blockingNow;
                }

                if (splashPotion.getValue() && mc.player.isUsingItem()) {
                    flushLag();
                    lastDistSq = distSq;
                    lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                    return;
                }

                lastDistSq = distSq;
                lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;
                return;
            }

            int hurtTime = mc.player.hurtTime;
            if (hurtTime > lastSelfHurtTime) {
                hitMarkedEntityId = -1;
            }
            lastSelfHurtTime = hurtTime;
            lastSprintState = mc.player.isSprinting();
            lastBlockingState = mc.player.isBlocking();

            if (hurtTime == 0
                && lastTargetHurtTime == 0
                && ((LivingEntity) currentTarget).hurtTime > 0) {
                hitMarkedEntityId = currentTarget.getId();
            }
            lastTargetHurtTime = ((LivingEntity) currentTarget).hurtTime;

            boolean closing = lastDistSq >= 0 && distSq < lastDistSq;
            boolean outsideMinDist = distSq > MINIMUM_DISTANCE_SQ;
            boolean weaponOk = !holdingWeapon.getValue() || AttackUtils.isHoldingWeapon();
            boolean hitMarkedHere = hitMarkedEntityId == currentTarget.getId();
            boolean hitStart = hitMarkedHere && distSq <= MINIMUM_DISTANCE_SQ && hurtTime == 0 && moving && weaponOk;

            lastDistSq = distSq;

            if (hurtTime == 0 && weaponOk && moving
                && ((closing && outsideMinDist) || hitStart)) {
                startLag();
            }
        } else {
            if (isLagging) flushLag();
            lastDistSq = -1;
            hitMarkedEntityId = -1;
            lastTargetHurtTime = 0;
        }
    }

    @Listener
    public void onAttack(AttackEvent e) {
        if (isLagging) {
            flushLag();
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!isLagging || !indicator.getValue()) return;
        if (mc.player == null) return;

        Vec3d delayedPos = LagUtils.getServerPosition();
        if (delayedPos == null) return;

        double halfW = mc.player.getWidth() / 2.0;
        double height = mc.player.getHeight();
        Box box = new Box(
            delayedPos.x - halfW, delayedPos.y, delayedPos.z - halfW,
            delayedPos.x + halfW, delayedPos.y + height, delayedPos.z + halfW
        );

        VertexConsumer vertexConsumer = event.getVertexConsumers().getBuffer(RenderLayers.lines());
        Color color = indicatorColor.getColor();
        int lineColor = ((color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue());

        Render3DEngine.drawBox3D(event.getMatrices(), vertexConsumer, event.getCameraRenderState(), box, lineColor, 2.0F);
    }

    private void startLag() {
        outboundLag = new LagRequest(LagDirection.ONLY_OUTBOUND, new ModuleTimeout(this));
        LagUtils.requestLag(outboundLag);
        isLagging = true;
    }

    private void flushLag() {
        if (!isLagging) return;
        if (outboundLag != null) {
            outboundLag.getTimeout().forceTimeOut();
            outboundLag = null;
        }
        isLagging = false;
    }

    private void resetState() {
        currentTarget = null;
        lastDistSq = -1;
        isLagging = false;
        lastSelfHurtTime = 0;
        lastTargetHurtTime = 0;
        hitMarkedEntityId = -1;
        lastSprintState = false;
        lastBlockingState = false;
        outboundLag = null;
    }

    private Entity findTarget(double rangeSq) {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == mc.player) continue;
            if (!entity.isAlive()) continue;
            double dist = mc.player.squaredDistanceTo(entity);
            if (dist < rangeSq && dist < bestDist) {
                best = entity;
                bestDist = dist;
            }
        }

        return best;
    }

    private boolean sameTarget(Entity nextTarget) {
        if (currentTarget == null || nextTarget == null) {
            return currentTarget == nextTarget;
        }
        return currentTarget.getId() == nextTarget.getId();
    }

    private boolean isMoving() {
        return mc.player.input.playerInput.forward() || mc.player.input.playerInput.backward()
            || mc.player.input.playerInput.left() || mc.player.input.playerInput.right();
    }

    @Override
    public String getDisplayValue() {
        return maxDelay.getValue() + "ms";
    }
}
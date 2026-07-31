package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.movement.Speed;
import cn.advicenext.features.module.impl.world.Scaffold;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.client.render.Render3DEngine;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TargetStrafe extends Module {

    public static TargetStrafe INSTANCE;

    private final DoubleSetting range = new DoubleSetting("Range", "Strafe radius", 1.0, 0.1, 6.0, 0.1);
    private final BooleanSetting holdJump = new BooleanSetting("HoldJump", "Only when holding jump", false);
    private final BooleanSetting render = new BooleanSetting("Render", "Show circle around target", true);
    private final BooleanSetting behind = new BooleanSetting("Behind", "Stay behind target", true);

    private float yaw;
    private boolean left, colliding;
    public boolean active;
    public LivingEntity target;

    public TargetStrafe() {
        super("TargetStrafe", "Strafe around your target", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(range);
        this.settings.add(holdJump);
        this.settings.add(render);
        this.settings.add(behind);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        Speed speed = findModule(Speed.class);

        boolean jumpHeld = mc.options.jumpKey.isPressed();
        boolean forwardHeld = mc.options.forwardKey.isPressed();

        if (holdJump.getValue() && !jumpHeld
                || !forwardHeld
                || !(speed != null && speed.getEnabled())
                || !isActive(KillAura.class)
                || isActive(Scaffold.class)) {
            active = false;
            target = null;
            return;
        }

        if (KillAura.INSTANCE != null && KillAura.INSTANCE.getEnabled()) {
            target = KillAura.INSTANCE.getTarget();
        }

        if (target == null) {
            active = false;
            return;
        }

        if (mc.player.horizontalCollision || !isBlockUnder(5)) {
            if (!colliding) {
                strafe();
                left = !left;
            }
            colliding = true;
        } else {
            colliding = false;
        }

        active = true;

        float targetYaw;
        if (behind.getValue()) {
            targetYaw = target.getYaw() + 180;
        } else {
            targetYaw = getYaw(mc.player, new Vec3d(target.getX(), target.getY(), target.getZ())) + (90 + 45) * (left ? -1 : 1);
        }

        double radius = range.getValue() + Math.random() / 100.0;
        double posX = -MathHelper.sin((float) Math.toRadians(targetYaw)) * radius + target.getX();
        double posZ = MathHelper.cos((float) Math.toRadians(targetYaw)) * radius + target.getZ();

        yaw = getYaw(mc.player, new Vec3d(posX, target.getY(), posZ));
    }

    private void strafe() {
        if (mc.player == null) return;
        double yaw = Math.toRadians(mc.player.getYaw());
        mc.player.setVelocity(-MathHelper.sin((float) yaw) * 0.2, mc.player.getVelocity().y, MathHelper.cos((float) yaw) * 0.2);
    }

    private boolean isBlockUnder(int blocks) {
        for (int i = 0; i < blocks; i++) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().down(i + 1)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private float getYaw(PlayerEntity from, Vec3d pos) {
        return from.getYaw() + MathHelper.wrapDegrees(
                (float) Math.toDegrees(Math.atan2(pos.z - from.getZ(), pos.x - from.getX())) - 90f - from.getYaw());
    }

    public float getStrafeYaw() {
        return yaw;
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue() || target == null) return;

        float tickDelta = event.getTickDelta();

        Vec3d targetPos = new Vec3d(
                MathHelper.lerp(tickDelta, target.lastRenderX, target.getX()),
                MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()),
                MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ())
        );

        VertexConsumer vertexConsumer = event.getVertexConsumers().getBuffer(RenderLayers.lines());

        int color;
        if (active) {
            color = 0x80FF80;
        } else {
            color = 0xFFFFFF;
        }

        Render3DEngine.drawCircle3D(
                event.getMatrices(),
                vertexConsumer,
                event.getCameraRenderState(),
                targetPos,
                range.getValue().floatValue(),
                color,
                56,
                1.5F
        );
    }

    private boolean isActive(Class<? extends Module> moduleClass) {
        for (Module m : ModuleManager.getModules()) {
            if (moduleClass.isInstance(m) && m.getEnabled()) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T extends Module> T findModule(Class<T> clazz) {
        for (Module m : ModuleManager.getModules()) {
            if (clazz.isInstance(m)) return (T) m;
        }
        return null;
    }
}
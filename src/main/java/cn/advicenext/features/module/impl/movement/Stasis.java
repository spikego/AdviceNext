package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec2f;

public class Stasis extends Module {

    public static Stasis INSTANCE;

    private final BooleanSetting disableOnHurt = new BooleanSetting("DisableOnHurt", "Disable when hurt", false);

    private double x, y, z;
    private boolean onGround;
    private Vec2f rotation;
    private boolean rotated;

    public Stasis() {
        super("Stasis", "Freeze in place while allowing head rotation", Category.MOVEMENT);
        INSTANCE = this;
        this.settings.add(disableOnHurt);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            toggle();
            return;
        }
        rotated = false;
        onGround = mc.player.isOnGround();
        x = mc.player.getX();
        y = mc.player.getY();
        z = mc.player.getZ();
        rotation = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        float f = (float) (mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2);
        float gcd = f * f * f * 1.2f;
        rotation = new Vec2f(rotation.x - rotation.x % gcd, rotation.y - rotation.y % gcd);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        mc.player.setVelocity(0, 0, 0);
        mc.player.setPosition(x, y, z);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket movePacket) {
            if (!(movePacket instanceof PlayerMoveC2SPacket.LookAndOnGround)) {
                event.cancelled = true;
            }
        }

        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket interactPacket) {
            Vec2f current = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            float f = (float) (mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2);
            float gcd = f * f * f * 1.2f;
            current = new Vec2f(current.x - current.x % gcd, current.y - current.y % gcd);
            if (rotation.equals(current)) return;
            rotation = current;
            event.cancelled = true;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    current.x, current.y, onGround, false));
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
                    mc.player.getActiveHand(), interactPacket.getBlockHitResult(), interactPacket.getSequence()));
        }

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            toggle();
        }

        if (disableOnHurt.getValue() && mc.player != null && mc.player.hurtTime == 1) {
            toggle();
        }
    }
}
package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class Phase extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", List.of("Vanilla", "Intave"));
    private boolean phasing;
    private boolean canClip;

    public Phase() {
        super("Phase", "Phase through blocks", Category.MOVEMENT);
        this.enabled = false;
        this.settings.add(mode);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Vanilla")) {
            phasing = false;
            double rotation = Math.toRadians(mc.player.getYaw());
            double x = Math.sin(rotation);
            double z = Math.cos(rotation);

            if (mc.player.horizontalCollision) {
                mc.player.setPosition(
                        mc.player.getX() - x * 0.005,
                        mc.player.getY(),
                        mc.player.getZ() + z * 0.005
                );
                phasing = true;
            } else if (mc.player.isInsideWall()) {
                mc.player.networkHandler.sendPacket(
                        new PlayerMoveC2SPacket.PositionAndOnGround(
                                new Vec3d(
                                        mc.player.getX() - x * 1.5,
                                        mc.player.getY(),
                                        mc.player.getZ() + z * 1.5
                                ),
                                mc.player.isOnGround(),
                                mc.player.horizontalCollision
                        )
                );
                mc.player.setVelocity(
                        mc.player.getVelocity().x * 0.3,
                        mc.player.getVelocity().y,
                        mc.player.getVelocity().z * 0.3
                );
                phasing = true;
            }
        }

        if (mode.is("Intave")) {
            if (canClip) {
                mc.player.setPosition(
                        mc.player.getX(),
                        mc.player.getY() - 0.0052,
                        mc.player.getZ()
                );
            }

            if (mc.player.isSneaking()) {
                double wdist = 0.00001;
                double sdist = -0.00001;
                double rotation = Math.toRadians(mc.player.getYaw());

                if (mc.options.forwardKey.isPressed()) {
                    double fx = Math.sin(rotation) * wdist;
                    double fz = Math.cos(rotation) * wdist;
                    mc.player.setPosition(
                            mc.player.getX() - fx,
                            mc.player.getY(),
                            mc.player.getZ() + fz
                    );
                }
                if (mc.options.backKey.isPressed()) {
                    double bx = Math.sin(rotation) * sdist;
                    double bz = Math.cos(rotation) * sdist;
                    mc.player.setPosition(
                            mc.player.getX() - bx,
                            mc.player.getY(),
                            mc.player.getZ() + bz
                    );
                }
                if (mc.options.leftKey.isPressed()) {
                    double lx = Math.sin(rotation - Math.PI / 2) * wdist;
                    double lz = Math.cos(rotation - Math.PI / 2) * wdist;
                    mc.player.setPosition(
                            mc.player.getX() - lx,
                            mc.player.getY(),
                            mc.player.getZ() + lz
                    );
                }
                if (mc.options.rightKey.isPressed()) {
                    double rx = Math.sin(rotation + Math.PI / 2) * wdist;
                    double rz = Math.cos(rotation + Math.PI / 2) * wdist;
                    mc.player.setPosition(
                            mc.player.getX() - rx,
                            mc.player.getY(),
                            mc.player.getZ() + rz
                    );
                }
            }
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;

        if (mode.is("Intave")) {
            canClip = mc.interactionManager.getBlockBreakingProgress() > 0.75f;
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getOrigin() != PacketEvent.TransferOrigin.RECEIVE) return;
        if (!(event.getPacket() instanceof GameMessageS2CPacket chatPacket)) return;

        String chat = chatPacket.content().getString();

        if (mode.is("Vanilla")) {
            if (chat.contains("Cages opened! FIGHT!")
                    || chat.contains("Cages opened!")
                    || chat.contains("The game starts in 3 seconds!")
                    || chat.contains("Cages open in:")
            ) {
                phasing = false;
            }
        }
    }
}
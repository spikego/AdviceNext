package cn.advicenext.utility.minecraft.network;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TrackedEntityPosition {

    public Vec3d base = Vec3d.ZERO;
    private Vec3d offset = Vec3d.ZERO;

    public void setBaseFrom(Entity entity) {
        this.base = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        this.offset = Vec3d.ZERO;
    }

    public Vec3d handlePacket(Packet<?> packet, World world, Entity target) {
        if (packet instanceof EntityPositionS2CPacket posPacket) {
            if (posPacket.entityId() != target.getId()) return null;
            Vec3d delta = posPacket.change().deltaMovement();
            offset = offset.add(delta);
            return base.add(offset);
        }
        if (packet instanceof EntityS2CPacket relMove) {
            if (relMove.getEntity(world).getId() != target.getId()) return null;
            double dx = relMove.getDeltaX() / 4096.0;
            double dy = relMove.getDeltaY() / 4096.0;
            double dz = relMove.getDeltaZ() / 4096.0;
            offset = offset.add(dx, dy, dz);
            return base.add(offset);
        }
        if (packet instanceof EntitySetHeadYawS2CPacket headYaw) {
            if (headYaw.getEntity(world).getId() != target.getId()) return null;
            return base.add(offset);
        }
        return null;
    }

    public Vec3d getCurrentPosition() {
        return base.add(offset);
    }

    public void reset() {
        base = Vec3d.ZERO;
        offset = Vec3d.ZERO;
    }
}
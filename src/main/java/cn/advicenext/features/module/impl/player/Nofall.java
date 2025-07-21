package cn.advicenext.features.module.impl.player;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.List;

public class Nofall extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "NoFall mode", "Packet", List.of("Packet", "NoGround"));

    public Nofall() {
        super("Nofall", "Prevents fall damage", Category.PLAYER);
        this.settings.add(mode);
    }

    public void onTick(TickEvent event){
        if (mc.player == null) return;

        if(mode.getValue().equals("NoGround")){
            if(mc.player.fallDistance > 3){
                mc.player.setOnGround(true);
            }
        }

        if(mode.getValue().equals("Packet")){
            if(mc.player.fallDistance > 3){
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true,mc.player.horizontalCollision));
            }
        }
    }
}

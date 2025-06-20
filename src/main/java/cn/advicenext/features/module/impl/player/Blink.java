package cn.advicenext.features.module.impl.player;

import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.concurrent.CopyOnWriteArrayList;

public class Blink extends Module {
    public static final Blink INSTANCE = new Blink();

    private final CopyOnWriteArrayList<PlayerMoveC2SPacket> packets = new CopyOnWriteArrayList<>();
    private Entity playerCopy = null;

    public Blink() {
        super("Blink", "Stores packets and sends them later", Category.PLAYER);
        this.enabled = false;
    }

    @Override
    public void onEnable() {
        packets.clear();
        // 不创建假身
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            for (PlayerMoveC2SPacket packet : packets) {
                mc.player.networkHandler.sendPacket(packet);
            }
        }
        packets.clear();
        // 不需要移除假身
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            packets.add((PlayerMoveC2SPacket) event.getPacket());
            event.cancelled = true;
        }
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        String text = " Packets size - §7[§f" + packets.size() + "§7]";
        event.getContext().drawText(mc.textRenderer, text, 50, 100, 0xFFFFFF, true);
    }
}
package cn.advicenext.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class Utility implements InstanceAccess {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static ClientPlayerEntity player = mc.player;
}
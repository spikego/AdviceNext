package cn.advicenext.utility;

import net.minecraft.client.MinecraftClient;

public interface InstanceAccess {
    MinecraftClient mc = MinecraftClient.getInstance();
}

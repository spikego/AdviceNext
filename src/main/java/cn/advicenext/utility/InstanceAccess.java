package cn.advicenext.utility;

import cn.advicenext.AdviceNext;
import net.minecraft.client.MinecraftClient;

public interface InstanceAccess {
    MinecraftClient mc = MinecraftClient.getInstance();

    AdviceNext adviceNext = new AdviceNext();
}

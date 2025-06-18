package cn.advicenext.features.module.impl.misc;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.minecraft.client.TimerUtils;

public class Timer extends Module{

    private final DoubleSetting timerSpeed = new DoubleSetting("TimerSpeed", "Set Minecraft timer", 1.0, 5.0, 0.0,0.1);
    public Timer(){
        super("Timer", "Allows you to change the speed of the game.", Category.MISC);
        this.enabled = false;
    }

    @Override
    public void onEnable() {
        TimerUtils.setTimerSpeed(timerSpeed.getValue());
    }

    @Override
    public void onDisable() {
        TimerUtils.setTimerSpeed(1.0);
    }

    @Override
    public void onTick(TickEvent event) {
        TimerUtils.setTimerSpeed(timerSpeed.getValue());
    }

}

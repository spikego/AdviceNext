package cn.advicenext.utility.minecraft.client;

import cn.advicenext.utility.Utility;

public class TimerUtils extends Utility {

    protected static float timerSpeed = 1.0F;
    Boolean screenTimer = false;
    Long lastTime = System.currentTimeMillis();


    public static void setTimerSpeed(Double speed) {
        timerSpeed = speed.floatValue();
    }

    public void resetTimer() {
       timerSpeed = 1.0F;
       screenTimer = false;
    }

    public static Float getTimerSpeed() {
        return timerSpeed;
    }
}

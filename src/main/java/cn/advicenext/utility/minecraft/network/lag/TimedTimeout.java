package cn.advicenext.utility.minecraft.network.lag;

public class TimedTimeout extends LagTimeout {
    private final long startTime;
    private final long delayMs;

    public TimedTimeout(long delayMs) {
        this.startTime = System.currentTimeMillis();
        this.delayMs = delayMs;
    }

    @Override
    protected boolean shouldHaveTimedOut() {
        return System.currentTimeMillis() - startTime >= delayMs;
    }
}
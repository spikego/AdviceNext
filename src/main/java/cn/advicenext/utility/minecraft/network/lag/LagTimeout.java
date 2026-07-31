package cn.advicenext.utility.minecraft.network.lag;

public abstract class LagTimeout {
    private volatile boolean forcefullyTimedOut = false;

    protected abstract boolean shouldHaveTimedOut();

    public final boolean isTimedOut() {
        return forcefullyTimedOut || shouldHaveTimedOut();
    }

    public final void forceTimeOut() {
        forcefullyTimedOut = true;
    }
}
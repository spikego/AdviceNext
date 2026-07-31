package cn.advicenext.utility.client.system;

import cn.advicenext.utility.InstanceAccess;

public class MetricsUtils implements InstanceAccess {
    public int getFps() {
        return mc.getCurrentFps();
    }

    //Memory
    public long getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    //cpu
    public double getCpuUsage() {
        return (double) Runtime.getRuntime().maxMemory() / Runtime.getRuntime().totalMemory();
    }

}

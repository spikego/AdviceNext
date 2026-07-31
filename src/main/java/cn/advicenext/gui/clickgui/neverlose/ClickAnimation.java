package cn.advicenext.gui.clickgui.neverlose;

public class ClickAnimation {
    private final int duration;
    private double endPoint;
    private boolean forwards;
    private long startTime;
    private double currentValue;

    public ClickAnimation(int ms, double endPoint) {
        this(ms, endPoint, false);
    }

    public ClickAnimation(int ms, double endPoint, boolean forwards) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.forwards = forwards;
        this.startTime = System.currentTimeMillis();
        this.currentValue = forwards ? 0 : endPoint;
    }

    public void setDirection(boolean forwards) {
        if (this.forwards != forwards) {
            this.forwards = forwards;
            this.startTime = System.currentTimeMillis() - (duration - Math.min(duration, System.currentTimeMillis() - startTime));
        }
    }

    public double getOutput() {
        long elapsed = System.currentTimeMillis() - startTime;
        double x = Math.min(1.0, Math.max(0.0, elapsed / (double) duration));

        if (forwards) {
            if (elapsed >= duration) return endPoint;
            return getDecelerateEquation(x) * endPoint;
        } else {
            if (elapsed >= duration) return 0;
            return (1 - getDecelerateEquation(x)) * endPoint;
        }
    }

    private double getDecelerateEquation(double x) {
        return 1 - ((x - 1) * (x - 1));
    }

    public double getSmoothOutput() {
        long elapsed = System.currentTimeMillis() - startTime;
        double x = Math.min(1.0, Math.max(0.0, elapsed / (double) duration));

        if (forwards) {
            if (elapsed >= duration) return endPoint;
            return getSmoothEquation(x) * endPoint;
        } else {
            if (elapsed >= duration) return 0;
            return (1 - getSmoothEquation(x)) * endPoint;
        }
    }

    private double getSmoothEquation(double x) {
        return -2 * Math.pow(x, 3) + 3 * Math.pow(x, 2);
    }

    public boolean isDone() {
        return System.currentTimeMillis() - startTime >= duration;
    }

    public void setEndPoint(double endPoint) {
        this.endPoint = endPoint;
    }
}
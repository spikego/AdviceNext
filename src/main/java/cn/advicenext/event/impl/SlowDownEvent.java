package cn.advicenext.event.impl;

import cn.advicenext.event.Event;

public class SlowDownEvent extends Event {
    private float forward = 1.0F;
    private float strafe = 1.0F;
    private boolean sprint = true;

    public float getForward() { return forward; }
    public void setForward(float forward) { this.forward = forward; }
    public float getStrafe() { return strafe; }
    public void setStrafe(float strafe) { this.strafe = strafe; }
    public boolean isSprint() { return sprint; }
    public void setSprint(boolean sprint) { this.sprint = sprint; }
}
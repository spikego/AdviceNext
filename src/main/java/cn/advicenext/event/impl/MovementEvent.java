package cn.advicenext.event.impl;

import cn.advicenext.event.Event;

public class MovementEvent extends Event {
    private float forward;
    private float strafe;
    private boolean sneak;
    private boolean jump;
    private boolean originalSneak;
    private boolean originalJump;

    public MovementEvent(float forward, float strafe) {
        this.forward = forward;
        this.strafe = strafe;
    }

    public MovementEvent(float forward, float strafe, boolean sneak, boolean jump) {
        this.forward = forward;
        this.strafe = strafe;
        this.sneak = sneak;
        this.jump = jump;
        this.originalSneak = sneak;
        this.originalJump = jump;
    }

    public float getForward() { return forward; }
    public float getStrafe() { return strafe; }
    public void setForward(float forward) { this.forward = forward; }
    public void setStrafe(float strafe) { this.strafe = strafe; }

    public boolean isSneak() { return sneak; }
    public void setSneak(boolean sneak) { this.sneak = sneak; }
    public boolean isJump() { return jump; }
    public void setJump(boolean jump) { this.jump = jump; }
    public boolean isOriginalSneak() { return originalSneak; }
    public boolean isOriginalJump() { return originalJump; }
}
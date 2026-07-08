package cn.advicenext.event.impl;

import cn.advicenext.event.Event;

public class MovementEvent extends Event {
    private float forward;
    private float strafe;
    
    public MovementEvent(float forward, float strafe) {
        this.forward = forward;
        this.strafe = strafe;
    }
    
    public float getForward() { return forward; }
    public float getStrafe() { return strafe; }
    public void setForward(float forward) { this.forward = forward; }
    public void setStrafe(float strafe) { this.strafe = strafe; }
}
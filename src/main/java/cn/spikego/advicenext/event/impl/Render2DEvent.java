package cn.spikego.advicenext.event.impl;

import cn.spikego.advicenext.event.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class Render2DEvent extends Event {
    private final DrawContext context;
    private final RenderTickCounter tickCounter;

    public Render2DEvent(DrawContext context, RenderTickCounter tickCounter) {
        this.context = context;
        this.tickCounter = tickCounter;
    }

    public DrawContext getContext() {
        return context;
    }

    public RenderTickCounter getTickCounter() {
        return tickCounter;
    }
}
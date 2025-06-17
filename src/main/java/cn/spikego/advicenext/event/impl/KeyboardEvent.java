package cn.spikego.advicenext.event.impl;

import cn.spikego.advicenext.event.Event;

public class KeyboardEvent extends Event {
    private int key;

    public KeyboardEvent(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}

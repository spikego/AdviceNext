package cn.advicenext.event.impl;

import cn.advicenext.event.Event;

public class KeyboardEvent extends Event {
    private int key;

    public KeyboardEvent(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}

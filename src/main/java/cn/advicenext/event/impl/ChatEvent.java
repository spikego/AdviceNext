package cn.advicenext.event.impl;

import cn.advicenext.event.Event;

public class ChatEvent extends Event {
    private String content;

    public ChatEvent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
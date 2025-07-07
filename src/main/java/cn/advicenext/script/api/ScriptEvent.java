package cn.advicenext.script.api;

import cn.advicenext.event.Event;

public abstract class ScriptEvent extends Event {
    private final String scriptName;
    
    public ScriptEvent(String scriptName) {
        this.scriptName = scriptName;
    }
    
    public String getScriptName() {
        return scriptName;
    }
}
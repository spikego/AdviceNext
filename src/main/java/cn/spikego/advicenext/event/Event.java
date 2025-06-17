package cn.spikego.advicenext.event;

public class Event {
    public boolean cancelled = false;

    public static class CancellableEvent extends Event {

        private boolean eventCancelled = false;


        public boolean isEventCancelled() {
            return eventCancelled;
        }

        public void cancelEvent() {
            this.eventCancelled = true;
        }
    }

    public boolean getCancelled() {
        return cancelled;
    }
}
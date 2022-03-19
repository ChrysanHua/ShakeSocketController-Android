package com.ssc.shakesocketcontroller.Models.events.signal;

public class CtrlStateChangedEvent {

    private final boolean isCtrlON;

    public CtrlStateChangedEvent(boolean isCtrlON) {
        this.isCtrlON = isCtrlON;
    }

    public boolean isCtrlON() {
        return isCtrlON;
    }
}

package com.ssc.shakesocketcontroller.Transaction.events.signal;

public class CtrlStateChangedEvent {

    private final boolean isCtrlON;

    public CtrlStateChangedEvent(boolean isCtrlON) {
        this.isCtrlON = isCtrlON;
    }

    public boolean isCtrlON() {
        return isCtrlON;
    }
}

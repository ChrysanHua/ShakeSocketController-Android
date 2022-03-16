package com.ssc.shakesocketcontroller.Transaction.events.signal;

public class TCPDisConnectEvent {

    private boolean isSendFailed;

    public TCPDisConnectEvent(boolean isSendFailed) {
        this.isSendFailed = isSendFailed;
    }

    public boolean isSendFailed() {
        return isSendFailed;
    }
}

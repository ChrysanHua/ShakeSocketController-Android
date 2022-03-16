package com.ssc.shakesocketcontroller.Transaction.events.signal;

import com.ssc.shakesocketcontroller.Transaction.pojo.ComputerInfo;

public class TCPConnectedEvent {

    private ComputerInfo targetInfo;

    public TCPConnectedEvent(ComputerInfo targetInfo) {
        this.targetInfo = targetInfo;
    }

    public ComputerInfo getTargetInfo() {
        return targetInfo;
    }
}

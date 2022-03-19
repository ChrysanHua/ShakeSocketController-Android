package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;

public class TCPConnectedEvent {

    private ComputerInfo targetInfo;

    public TCPConnectedEvent(ComputerInfo targetInfo) {
        this.targetInfo = targetInfo;
    }

    public ComputerInfo getTargetInfo() {
        return targetInfo;
    }
}

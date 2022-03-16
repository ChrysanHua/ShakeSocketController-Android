package com.ssc.shakesocketcontroller.Transaction.events.signal;


public class LaunchConnectEvent {

    private String mTargetMacStr;

    public LaunchConnectEvent(String targetMacStr) {
        this.mTargetMacStr = targetMacStr;
    }

    public String getTargetMacStr() {
        return mTargetMacStr;
    }

}

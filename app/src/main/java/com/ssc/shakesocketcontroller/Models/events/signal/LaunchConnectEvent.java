package com.ssc.shakesocketcontroller.Models.events.signal;


public class LaunchConnectEvent {

    private String mTargetMacStr;

    public LaunchConnectEvent(String targetMacStr) {
        this.mTargetMacStr = targetMacStr;
    }

    public String getTargetMacStr() {
        return mTargetMacStr;
    }

}

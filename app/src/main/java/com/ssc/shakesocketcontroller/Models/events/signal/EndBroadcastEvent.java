package com.ssc.shakesocketcontroller.Models.events.signal;

public class EndBroadcastEvent {

    private final boolean saveResult;           //是否需要保存结果

    public EndBroadcastEvent(boolean saveResult) {
        this.saveResult = saveResult;
    }

    public boolean shouldSaveResult() {
        return saveResult;
    }
}

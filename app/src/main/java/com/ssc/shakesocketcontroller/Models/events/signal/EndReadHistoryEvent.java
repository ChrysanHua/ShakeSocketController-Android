package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;

import java.util.List;

public class EndReadHistoryEvent {

    private final List<ComputerInfo> historyList;       //读取到的历史记录列表

    public EndReadHistoryEvent(List<ComputerInfo> historyList) {
        this.historyList = historyList;
    }

    public List<ComputerInfo> getHistoryList() {
        return historyList;
    }
}

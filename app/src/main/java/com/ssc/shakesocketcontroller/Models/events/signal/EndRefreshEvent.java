package com.ssc.shakesocketcontroller.Models.events.signal;

public class EndRefreshEvent {

    private final int newListCount;             //更新后列表的元素数量
    private final boolean isOnlineRefresh;      //当前更新的是什么类型（在线/历史）

    public EndRefreshEvent(int newListCount, boolean isOnlineRefresh) {
        this.newListCount = newListCount;
        this.isOnlineRefresh = isOnlineRefresh;
    }

    public int getNewListCount() {
        return newListCount;
    }

    public boolean isOnlineRefresh() {
        return isOnlineRefresh;
    }
}

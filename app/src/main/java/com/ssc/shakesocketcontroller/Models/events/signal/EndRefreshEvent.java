package com.ssc.shakesocketcontroller.Models.events.signal;

public class EndRefreshEvent {

    private final int oldListCount;             //更新前列表的元素数量
    private final boolean isOnlineRefresh;      //当前更新的是什么类型（在线/历史）
    private final boolean isUpdated;            //是否更新了列表

    public EndRefreshEvent(int oldListCount, boolean isOnlineRefresh, boolean isUpdated) {
        this.oldListCount = oldListCount;
        this.isOnlineRefresh = isOnlineRefresh;
        this.isUpdated = isUpdated;
    }

    public int getOldListCount() {
        return oldListCount;
    }

    public boolean isOnlineRefresh() {
        return isOnlineRefresh;
    }

    public boolean isUpdated() {
        return isUpdated;
    }
}

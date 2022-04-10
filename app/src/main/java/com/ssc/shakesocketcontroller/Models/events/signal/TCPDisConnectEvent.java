package com.ssc.shakesocketcontroller.Models.events.signal;

// TODO: 2022/4/10 TCPDisConnectEvent - 开发阶段保留，后续应删除

/**
 * @deprecated 已弃用以TCP方式进行通讯
 */
@Deprecated
public class TCPDisConnectEvent {

    private boolean isSendFailed;

    public TCPDisConnectEvent(boolean isSendFailed) {
        this.isSendFailed = isSendFailed;
    }

    public boolean isSendFailed() {
        return isSendFailed;
    }
}

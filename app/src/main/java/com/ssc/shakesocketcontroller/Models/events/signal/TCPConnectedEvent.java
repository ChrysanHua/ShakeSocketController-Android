package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;

// TODO: 2022/4/10 TCPConnectedEvent - 开发阶段保留，后续应删除

/**
 * @deprecated 已弃用以TCP方式进行通讯
 */
@Deprecated
public class TCPConnectedEvent {

    private ComputerInfo targetInfo;

    public TCPConnectedEvent(ComputerInfo targetInfo) {
        this.targetInfo = targetInfo;
    }

    public ComputerInfo getTargetInfo() {
        return targetInfo;
    }
}

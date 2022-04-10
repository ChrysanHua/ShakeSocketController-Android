package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Utils.ByteUtil;

// TODO: 2022/4/10 TCPReceiveEvent - 开发阶段保留，后续应删除

/**
 * @deprecated 已弃用以TCP方式进行通讯
 */
@Deprecated
public class TCPReceiveEvent {

    private byte[] recBuf;

    public TCPReceiveEvent(byte[] recBuf, int len) {
        this.recBuf = ByteUtil.subByte(recBuf, 0, len);
    }

    public byte[] getRecBuf() {
        return recBuf;
    }
}

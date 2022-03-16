package com.ssc.shakesocketcontroller.Transaction.events.signal;

import com.ssc.shakesocketcontroller.Utils.ByteUtil;

public class TCPReceiveEvent {

    private byte[] recBuf;

    public TCPReceiveEvent(byte[] recBuf, int len) {
        this.recBuf = ByteUtil.subByte(recBuf, 0, len);
    }

    public byte[] getRecBuf() {
        return recBuf;
    }
}

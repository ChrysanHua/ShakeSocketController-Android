package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Models.pojo.MsgPacket;

import java.net.InetAddress;

public class SendUDPEvent {

    private MsgPacket mMsgPacket;
    private final boolean asyncBlocking;

    public SendUDPEvent(MsgPacket msgPacket, boolean asyncBlocking) {
        this.mMsgPacket = msgPacket;
        this.asyncBlocking = asyncBlocking;
    }

    public byte[] getMsgData() {
        return mMsgPacket.getMsgData();
    }

    public InetAddress getAddress() {
        return mMsgPacket.getTargetInfo().address;
    }

    public MsgPacket getMsgPacket() {
        return mMsgPacket;
    }

    public boolean isAsyncBlocking() {
        return asyncBlocking;
    }
}

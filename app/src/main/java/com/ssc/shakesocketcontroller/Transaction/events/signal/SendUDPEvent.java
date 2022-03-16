package com.ssc.shakesocketcontroller.Transaction.events.signal;

import com.ssc.shakesocketcontroller.Transaction.pojo.MsgPacket;

import java.net.InetAddress;

public class SendUDPEvent {

    private MsgPacket mMsgPacket;

    public SendUDPEvent(MsgPacket msgPacket) {
        this.mMsgPacket = msgPacket;
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
}

package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Models.pojo.MsgPacket;

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

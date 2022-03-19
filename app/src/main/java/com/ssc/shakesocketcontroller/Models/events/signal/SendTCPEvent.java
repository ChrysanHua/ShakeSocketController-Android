package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Models.pojo.MsgPacket;

public class SendTCPEvent {

    private MsgPacket packet;

    public SendTCPEvent(MsgPacket packet) {
        this.packet = packet;
    }

    public byte[] getMsgData() {
        return packet.getMsgData();
    }

    public MsgPacket getPacket() {
        return packet;
    }
}

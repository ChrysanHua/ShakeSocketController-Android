package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Models.pojo.MsgPacket;

// TODO: 2022/4/10 SendTCPEvent - 开发阶段保留，后续应删除

/**
 * @deprecated 已弃用以TCP方式进行通讯
 */
@Deprecated
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

package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Utils.ByteUtil;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class BroadcastEvent {

    private final DatagramPacket mPacket;
    private final boolean initEvent;

    public BroadcastEvent(DatagramPacket packet) {
        mPacket = packet;
        initEvent = mPacket == null;
    }

    public DatagramPacket getPacket() {
        return mPacket;
    }

    public boolean isInitEvent() {
        return initEvent;
    }

    public byte[] getData() {
        // TODO: 2022/3/23 有待验证Packet接收回来的Buf是原始固定大小还是实际大小
        if (initEvent) {
            return null;
        } else {
            return ByteUtil.subByte(mPacket.getData(), mPacket.getOffset(), mPacket.getLength());
        }
    }

    public InetAddress getAddress() {
        if (initEvent) {
            return null;
        } else {
            return mPacket.getAddress();
        }
    }
}

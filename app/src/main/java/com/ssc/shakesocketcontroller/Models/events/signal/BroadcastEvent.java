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

    public byte[] getValidData() {
        if (initEvent) {
            return null;
        } else {
            //返回接收Buf中的有效部分（DatagramPacket.getLength()返回的才是实际接收的长度）
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

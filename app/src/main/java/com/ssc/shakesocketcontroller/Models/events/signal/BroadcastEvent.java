package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Utils.ByteUtil;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class BroadcastEvent {

    private DatagramPacket mPacket;

    public BroadcastEvent(DatagramPacket packet) {
        mPacket = packet;
    }

    public DatagramPacket getPacket() {
        return mPacket;
    }

    public byte[] getData() {
        return ByteUtil.subByte(mPacket.getData(), 0, mPacket.getLength());
    }

    public InetAddress getAddress() {
        return mPacket.getAddress();
    }

}

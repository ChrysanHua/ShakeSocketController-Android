package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Utils.ByteUtil;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class UDPReceiveEvent {

    private final DatagramPacket mPacket;
    private final long threadID;

    public UDPReceiveEvent(DatagramPacket packet, long threadID) {
        this.mPacket = packet;
        this.threadID = threadID;
    }

    public long getThreadID() {
        return threadID;
    }

    public DatagramPacket getPacket() {
        return mPacket;
    }

    public InetAddress getAddress() {
        return mPacket.getAddress();
    }

    public byte[] getValidData() {
        //返回接收Buf中的有效部分
        return ByteUtil.subByte(mPacket.getData(), mPacket.getOffset(), mPacket.getLength());
    }
}

package com.ssc.shakesocketcontroller.Models.pojo;

import com.ssc.shakesocketcontroller.Utils.ByteUtil;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import java.net.InetAddress;
import java.util.Objects;


public class MsgPacket {

    public static final int TYPE_LENGTH = 5;
    public static final String TYPE_CMD = "[cmd]";
    public static final String TYPE_IP = "[ipp]";
    public static final String TYPE_ANS = "[ans]";
    public static final String TYPE_GUN = "[gun]";

    private String typeStr;
    private String dataStr;
    private ComputerInfo targetInfo;


    private MsgPacket() {
    }

    public void setTargetInfo(ComputerInfo targetInfo) {
        this.targetInfo = Objects.requireNonNull(targetInfo);
    }

    public String getTypeStr() {
        return typeStr;
    }

    public String getDataStr() {
        return dataStr;
    }

    public ComputerInfo getTargetInfo() {
        return targetInfo;
    }

    public String getMsg() {
        return typeStr + dataStr;
    }

    public byte[] getMsgData() {
        return ByteUtil.concatByte(ByteUtil.fixLen(StrUtil.strToByte(typeStr),
                TYPE_LENGTH), StrUtil.strToByte(dataStr));
    }

    public boolean checkTypeStr(String TYPE_STR) {
        return typeStr.equals(TYPE_STR);
    }


    public static class Builder {
        private final String mTypeStr;
        private String mDataStr;
        private String mDeviceName;
        private String mNickName;
        private InetAddress mAddress;

        public Builder(String typeStr) {
            mTypeStr = Objects.requireNonNull(typeStr);
        }

        public Builder(byte[] msgData) {
            mTypeStr = StrUtil.byteToStr(ByteUtil.subByte(msgData, 0, TYPE_LENGTH)).trim();
            mDataStr = StrUtil.byteToStr(ByteUtil.subByte(msgData,
                    TYPE_LENGTH, msgData.length - TYPE_LENGTH));
        }

        public MsgPacket build() {
            MsgPacket msgPacket = new MsgPacket();
            msgPacket.typeStr = mTypeStr;
            msgPacket.dataStr = mDataStr;
            msgPacket.targetInfo = new ComputerInfo("", mDeviceName, mNickName, mAddress);
            return msgPacket;
        }

        public Builder dataStr(String dataStr) {
            mDataStr = dataStr;
            return this;
        }

        public Builder dataStr(byte[] dataStrBuf) {
            mDataStr = StrUtil.byteToStr(dataStrBuf);
            return this;
        }

        public Builder nickName(String nickName) {
            mNickName = nickName;
            return this;
        }

        public Builder deviceName(String deviceName) {
            mDeviceName = deviceName;
            return this;
        }

        public Builder address(InetAddress address) {
            mAddress = address;
            return this;
        }

    }

}

package com.ssc.shakesocketcontroller.Transaction.pojo;

import com.google.gson.annotations.SerializedName;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import java.net.InetAddress;

public class ComputerInfo {

    @SerializedName("ip")
    public final InetAddress address;           //地址
    public final String deviceName;             //设备名
    public final String userName;               //用户名
    public String nickName;                     //昵称（默认与用户名相同，可自定义修改）
    public boolean isChecked;                   //是否需要连接（是否被选中）

    private transient String macStr;            //MAC地址
    public transient boolean isConnected;       //是否已连接
    public transient boolean isSaved;           //是否历史连接（是否已保存）
    public transient boolean isOnline;          //是否在线


    public ComputerInfo(InetAddress address, String deviceName, String userName) {
        this.address = address;
        this.deviceName = deviceName;
        this.userName = userName;
        this.nickName = this.userName;
    }

    public ComputerInfo(InetAddress address, String deviceName, String userName,
                        boolean isConnected, boolean isSaved) {
        this.address = address;
        this.deviceName = deviceName;
        this.userName = userName;
        this.nickName = this.userName;
        this.isConnected = isConnected;
        this.isSaved = isSaved;
    }

    public String getIP() {
        return address.getHostAddress();
    }

    public byte[] getMacByte() {
        return DeviceUtil.getMacAddress(address);
    }

    public String getMacStr() {
        if (macStr == null) {
            macStr = StrUtil.MacToStr(address);
        }
        return macStr;
    }

    public boolean isIntact() {
        return !(address == null ||
                StrUtil.isNullOrEmpty(deviceName) ||
                StrUtil.isNullOrEmpty(userName));
    }

}

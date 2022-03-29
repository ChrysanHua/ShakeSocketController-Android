package com.ssc.shakesocketcontroller.Models.pojo;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import java.net.InetAddress;
import java.util.Objects;

/**
 * 设备连接信息数据类
 */
public class ComputerInfo {

    @SerializedName("ip")
    public final InetAddress address;           //地址
    public final String deviceName;             //设备名
    public final String userName;               //用户名
    public String nickName;                     //昵称（默认与用户名相同，可自定义修改）
    public boolean isChecked;                   //是否需要连接（是否被选中）

    private transient byte[] macByte;           //MAC地址
    public transient boolean isConnected;       //是否已连接
    public transient boolean isSaved;           //是否历史连接（是否已保存）
    public transient boolean isOnline;          //是否在线


    public ComputerInfo() {
        this(null, "", "");
    }

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
        if (macByte == null) {
            macByte = DeviceUtil.getMacAddress(address);
        }
        return macByte;
    }

    public String getMacStr() {
        return StrUtil.macByteToStr(getMacByte());
    }

    /**
     * 获取当前设备连接信息是否完整
     */
    public boolean isIntact() {
        return !(address == null ||
                StrUtil.isNullOrEmpty(deviceName) ||
                StrUtil.isNullOrEmpty(userName));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ComputerInfo info = (ComputerInfo) o;
        return Objects.equals(address, info.address) &&
                Objects.equals(deviceName, info.deviceName) &&
                Objects.equals(userName, info.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, deviceName, userName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("address", address)
                .add("deviceName", deviceName)
                .add("userName", userName)
                .add("nickName", nickName)
                .add("isChecked", isChecked)
                .add("isConnected", isConnected)
                .add("isSaved", isSaved)
                .add("isOnline", isOnline)
                .toString();
    }
}

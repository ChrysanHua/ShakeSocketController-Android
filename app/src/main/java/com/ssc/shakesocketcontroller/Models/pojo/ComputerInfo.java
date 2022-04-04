package com.ssc.shakesocketcontroller.Models.pojo;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import java.net.InetAddress;
import java.util.Objects;

import androidx.annotation.NonNull;

// TODO: 2022/4/2 电脑广播时只播关键3项，但手机接收时会另外包含address和nickName数据在里面；
//  首次连接时手机发送本机UUID等等所有本机数据，电脑先发回来一个收到，等电脑用户确认后发除IP外的所有项；
//  后续连接时则仅互发shakeMark；连接后直接发各种指令，对对方的身份有怀疑时再请求发送shakeMark确认身份；

/**
 * 设备连接信息数据类
 */
public class ComputerInfo {
    private static final String UNKNOWN_DEVICE_NAME = "未知设备";
    private static final String UNKNOWN_USER_NAME = "未知";

    public final String deviceID;               //1.设备的唯一标识符
    public final String deviceName;             //2.设备名
    public final String userName;               //3.用户名
    @SerializedName("ip")
    public InetAddress address;                 //地址
    public String nickName;                     //昵称（默认与用户名相同，可自定义修改）
    public String shakeMark;                    //通信互认标识符
    public String clientVersion;                //设备客户端的版本号
    public boolean isChecked;                   //是否需要连接（是否被选中）

    public transient boolean isConnected;       //是否已连接
    public transient boolean isSaved;           //是否历史连接（是否已保存）
    public transient boolean isOnline;          //是否在线


    public ComputerInfo() {
        this("", UNKNOWN_DEVICE_NAME, UNKNOWN_USER_NAME);
    }

    public ComputerInfo(String deviceID, String deviceName, String userName) {
        this.deviceID = deviceID;
        this.deviceName = deviceName;
        this.userName = userName;
        this.nickName = this.userName;
    }

    public ComputerInfo(String deviceID, String deviceName, String userName, InetAddress address) {
        this(deviceID, deviceName, userName);
        this.address = address;
    }

    public ComputerInfo(InetAddress address, String deviceName, String userName) {
        //临时
        this("", deviceName, userName);
        this.address = address;
        this.nickName = this.userName;
        this.clientVersion = null;
    }

    public ComputerInfo(InetAddress address, String deviceName, String userName,
                        boolean isConnected, boolean isSaved) {
        //临时
        this(address, deviceName, userName);
        this.isConnected = isConnected;
        this.isSaved = isSaved;
    }

    /**
     * 获取IP地址字符串
     */
    public String getIP() {
        return address.getHostAddress();
    }

    /**
     * 获取当前设备连接信息是否完整
     */
    public boolean isIntact() {
        return !(deviceID == null || address == null ||
                StrUtil.isNullOrEmpty(deviceName) ||
                StrUtil.isNullOrEmpty(userName) ||
                StrUtil.isNullOrEmpty(nickName));
    }

    /**
     * 获取当前设备连接信息是否可用
     */
    public boolean isUsable() {
        return isIntact() && !deviceID.isEmpty() &&
                !deviceName.equals(UNKNOWN_DEVICE_NAME) &&
                !userName.equals(UNKNOWN_USER_NAME);
    }

    /**
     * 判断与指定设备是否本质上相同
     */
    public boolean isSameDevice(@NonNull ComputerInfo info) {
        return deviceID.equals(info.deviceID);
    }

    /**
     * 获取当前设备是否正在等待用户确认连接
     */
    public boolean waitingForConfirm() {
        return isConnected && !isSaved &&
                shakeMark != null && shakeMark.isEmpty();
    }

    /**
     * 克隆指定目标对象的配置信息部分的数据
     */
    public void cloneConfig(ComputerInfo srcInfo) {
        this.nickName = srcInfo.nickName;
        this.shakeMark = srcInfo.shakeMark;
        this.clientVersion = srcInfo.clientVersion;
        this.isChecked = srcInfo.isChecked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ComputerInfo info = (ComputerInfo) o;
        return Objects.equals(deviceID, info.deviceID) &&
                Objects.equals(deviceName, info.deviceName) &&
                Objects.equals(userName, info.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceID, deviceName, userName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceID", deviceID)
                .add("deviceName", deviceName)
                .add("userName", userName)
                .add("address", address)
                .add("nickName", nickName)
                .add("shakeMark", shakeMark)
                .add("clientVersion", clientVersion)
                .add("isChecked", isChecked)
                .add("isConnected", isConnected)
                .add("isSaved", isSaved)
                .add("isOnline", isOnline)
                .toString();
    }
}

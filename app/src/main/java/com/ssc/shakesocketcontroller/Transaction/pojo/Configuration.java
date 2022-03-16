package com.ssc.shakesocketcontroller.Transaction.pojo;

import com.ssc.shakesocketcontroller.Utils.DeviceUtil;

public class Configuration {

    private String deviceName;
    private String nickName;


    private Configuration() {
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getNickName() {
        return nickName;
    }

    public ComputerInfo getLocalInfo() {
        return new ComputerInfo(DeviceUtil.getLocalAddress(),
                deviceName, nickName);
    }


    public static Configuration load() {
        Configuration config = new Configuration();

        config.deviceName = DeviceUtil.getDeviceName();
        config.nickName = config.deviceName;

        return config;
    }

}

package com.ssc.shakesocketcontroller.Models.events.signal;

import com.ssc.shakesocketcontroller.Utils.DeviceUtil;

import java.net.InetAddress;
import java.net.Socket;

public class ConnectConfirmEvent {

    private Socket mClient;

    public ConnectConfirmEvent(Socket client) {
        this.mClient = client;
    }

    public Socket getClient() {
        return mClient;
    }

    public InetAddress getTargetAddress() {
        return mClient.getInetAddress();
    }

    public String getTargetMacStr() {
        return DeviceUtil.getMacAddressStr(getTargetAddress());
    }
}

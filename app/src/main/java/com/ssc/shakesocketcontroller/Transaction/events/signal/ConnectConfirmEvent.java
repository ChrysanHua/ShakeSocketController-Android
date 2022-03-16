package com.ssc.shakesocketcontroller.Transaction.events.signal;

import com.ssc.shakesocketcontroller.Utils.StrUtil;

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
        return StrUtil.MacToStr(getTargetAddress());
    }
}

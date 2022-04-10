package com.ssc.shakesocketcontroller.Transaction.controller;

import org.greenrobot.eventbus.EventBus;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @deprecated 已弃用以TCP方式进行通讯
 */
@Deprecated
public class TCPEventListener {
    private static final String TAG = "TCPEventListener";

    private final ConcurrentHashMap<String, Socket> clientMap;

    public TCPEventListener(int port) {
        clientMap = new ConcurrentHashMap<>();

    }

    public void start() {
        EventBus.getDefault().register(this);

    }

    public void stop() {
        EventBus.getDefault().unregister(this);

    }

}

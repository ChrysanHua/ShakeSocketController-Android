package com.ssc.shakesocketcontroller.Transaction.threads;

import android.util.Log;

import com.ssc.shakesocketcontroller.Transaction.events.signal.ConnectConfirmEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPConnectThread extends Thread {
    private static final String TAG = "TCPConnectThread";

    private final ServerSocket serverSocket;


    public TCPConnectThread(int port) {
        setName("TCPConnectThread");
        ServerSocket tmp = null;
        try {
            tmp = new ServerSocket(port);
        } catch (IOException e) {
            Log.e(TAG, "TCPConnectThread: serverSocket tmp create() failed", e);
        }
        serverSocket = tmp;
        Log.i(TAG, "TCPConnectThread: Ready with port: " + port);
    }

    @Override
    public void run() {
        Log.i(TAG, "run: Begin TCPConnectThread " + this);
        while (true) {
            try {
                Socket client = serverSocket.accept();
                EventBus.getDefault().post(new ConnectConfirmEvent(client));
                Log.i(TAG, "run: serverSocket accept() success and post ConnectConfirmEvent");
            } catch (IOException e) {
                Log.e(TAG, "run: serverSocket accept() failed", e);
                break;
            }
        }
        Log.i(TAG, "run: End " + TAG);
    }

    public void cancel() {
        Log.i(TAG, "cancel: " + TAG);
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "cancel: serverSocket close() failed", e);
        }
    }

}

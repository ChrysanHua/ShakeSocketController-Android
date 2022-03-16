package com.ssc.shakesocketcontroller.Transaction.threads;

import android.util.Log;

import com.ssc.shakesocketcontroller.Transaction.events.signal.BroadcastEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class BroadcastListenThread extends Thread {
    private static final String TAG = "BroadcastListenThread";

    private static final int MAX_RECEIVE_SIZE = 4096;
    private final DatagramSocket bcSocket;


    public BroadcastListenThread(int port) {
        setName("BroadcastListenThread");
        DatagramSocket tmp = null;
        try {
            tmp = new DatagramSocket(port);
        } catch (SocketException e) {
            Log.e(TAG, "BroadcastListenThread: bcSocket tmp create() failed", e);
        }
        bcSocket = tmp;
        Log.i(TAG, "BroadcastListenThread: Ready with port: " + port);
    }

    @Override
    public void run() {
        Log.i(TAG, "run: Begin BroadcastListenThread " + this);
        DatagramPacket packet;
        while (true) {
            try {
                packet = new DatagramPacket(new byte[MAX_RECEIVE_SIZE], MAX_RECEIVE_SIZE);
                bcSocket.receive(packet);
                EventBus.getDefault().post(new BroadcastEvent(packet));
                Log.d(TAG, "run: Receive broadcast success and post BroadcastEvent");
            } catch (IOException e) {
                Log.e(TAG, "run: bcSocket receive() failed", e);
                break;
            }
        }
        Log.i(TAG, "run: End " + TAG);
    }

    public void cancel() {
        Log.i(TAG, "cancel: " + TAG);
        try {
            bcSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "cancel: bcSocket close() failed", e);
        }
    }

}

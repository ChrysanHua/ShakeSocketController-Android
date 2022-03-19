package com.ssc.shakesocketcontroller.Transaction.threads;

import android.util.Log;

import com.ssc.shakesocketcontroller.Models.events.signal.BroadcastEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

// TODO: 2022/3/17 目前优先需要考虑如何实现定时任务，用来实现一些超时处理等事务
// TODO: 2022/3/17 考虑广播监听是否需要将socket常驻在程序中，以避免重复创建Socket以及端口被占用等情况；
//  目前方案：广播在后台持续监听，最好加入超时异常，然后外部再利用定时任务，适时主动去停止监听，
//  因此socket要可复用，且可以随时停止监听（Receive）；

// TODO: 2022/3/17 正常UDP通讯端看看发送和接收是否需要分开Socket，双方均需常驻，特别是接收方；
//  接收方：持续接收数据，接收后交由MsgAdapter处理，而发送指令后回复超时则在外部使用定时任务来实现，
//      可以考虑一个指令仅触发一个定时任务（无论已连接几台设备，超时后逐个处理）；
//  发送方：可满足每次按需调用发送即可，如果单条指令需要发送给多台设备，可以在全部发送完毕之后再触发定时任务；

// TODO: 2022/3/18 暂时以关闭-重建socket的方式实现重复广播监听，后续考虑用DatagramChannel改进？


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

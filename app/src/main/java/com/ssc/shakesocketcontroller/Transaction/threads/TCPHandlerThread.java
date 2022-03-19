package com.ssc.shakesocketcontroller.Transaction.threads;

import android.util.Log;

import com.ssc.shakesocketcontroller.Models.events.signal.SendTCPEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPDisConnectEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPReceiveEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPHandlerThread extends Thread {
    private static final String TAG = "TCPHandlerThread";

    private final Socket tcpSocket;
    private final OutputStream writer;
    private final InputStream reader;

    private byte[] recBuf = new byte[4096];


    public TCPHandlerThread(Socket client) {
        setName("TCPHandlerThread");
        tcpSocket = client;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = tcpSocket.getOutputStream();
            inputStream = tcpSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "TCPHandlerThread: TCPHandler init failed", e);
            cancel();
            //throw e;
        }
        writer = outputStream;
        reader = inputStream;
        EventBus.getDefault().register(this);
        Log.i(TAG, "TCPHandlerThread: TCPHandler ready");
    }

    @Override
    public void run() {
        Log.i(TAG, "run: Begin TCPHandlerThread " + this);
        while (true) {
            try {
                int len = reader.read(recBuf);
                if (len > 0) {
                    EventBus.getDefault().post(new TCPReceiveEvent(recBuf, len));
                    Log.d(TAG, "run: Receive TCP success and post TCPReceiveEvent");
                } else {
                    //remote socket shutdown
                    cancel();
                    EventBus.getDefault().post(new TCPDisConnectEvent(false));
                }
            } catch (IOException e) {
                Log.e(TAG, "run: tcpHandler receive failed", e);
                break;
            }
        }
        Log.i(TAG, "run: End " + TAG);
    }

    public void cancel() {
        Log.i(TAG, "cancel: " + TAG);
        try {
            if (tcpSocket.isConnected()) {
                tcpSocket.shutdownInput();
                tcpSocket.shutdownOutput();
            }
            tcpSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "cancel: tcpSocket close() failed", e);
        }
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSendTCPEvent(SendTCPEvent event) {
        try {
            writer.write(event.getMsgData());
        } catch (IOException e) {
            Log.e(TAG, "onSendTCPEvent: TCPHandler send failed", e);
            EventBus.getDefault().post(new TCPDisConnectEvent(true));
        }
    }

}

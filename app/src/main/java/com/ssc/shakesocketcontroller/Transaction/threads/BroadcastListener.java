package com.ssc.shakesocketcontroller.Transaction.threads;

import android.util.Log;

import com.ssc.shakesocketcontroller.Models.events.signal.BroadcastEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.CtrlStateChangedEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndBroadcastEvent;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.util.AsyncExecutor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

// TODO: 2022/3/23 后续考虑用DatagramChannel或其它方式改进重复创建Socket的问题

/**
 * UDP广播监听接收器类
 */
public class BroadcastListener {
    private static final String TAG = "BroadcastListener";

    private static final int MAX_RECEIVE_BUF_SIZE = 4096;
    private final AsyncExecutor executor;           //异步执行器
    private final int port;                         //广播监听端口号
    private final int duration;                     //广播监听时长
    private DatagramSocket bcSocket;                //监听Socket
    private volatile boolean listening = false;     //监听状态
    private boolean closed = false;                 //监听器是否已关闭
    private ScheduledFuture<?> saveFuture;          //延时停止任务的操作计划


    public BroadcastListener(int port, int duration, Executor executor) {
        this.port = port;
        this.duration = duration;
        this.executor = AsyncExecutor.builder().threadPool(executor).build();
    }

    /**
     * 获取监听状态
     */
    public boolean isListening() {
        return listening;
    }

    /**
     * 获取监听器是否已关闭，已关闭则该监听器不可再启动
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 开始无限循环监听接收广播
     */
    private void beginListen() {
        //async exec
        executor.execute(() -> {
            try {
                //create socket
                bcSocket = new DatagramSocket(port);
                Log.d(TAG, "beginListen: Ready with port: " + port);
                //modify state
                listening = true;
                //begin receive
                while (listening) {
                    DatagramPacket packet = new DatagramPacket(
                            new byte[MAX_RECEIVE_BUF_SIZE], MAX_RECEIVE_BUF_SIZE);
                    bcSocket.receive(packet);
                    //post BroadcastEvent
                    EventBus.getDefault().post(new BroadcastEvent(packet));
                }
                Log.i(TAG, "beginListen: Socket reception stopped unexpectedly");
            } catch (SocketException e) {
                //expected closure, modify state
                Log.d(TAG, "beginListen: Socket closed", e);
                listening = false;
                return;
            } catch (Exception e) {
                //unexpected shutdown
                Log.e(TAG, "beginListen: Socket exception", e);
            }

            //because it is not actively stopped, it needs to be closed manually
            listening = false;
            stop(false);
        });
    }

    /**
     * 开始监听
     *
     * @throws Exception 如果监听器已关闭
     */
    public void start() throws Exception {
        if (closed) {
            throw new Exception(TAG + " has been closed!");
        }
        if (listening) {
            return;
        }

        EventBus.getDefault().register(this);
        //begin async listen
        beginListen();
        if (duration > 0) {
            //stop after the duration
            saveFuture = MyApplication.getController().schedule(() -> stop(true), duration);
        }
        Log.d(TAG, "start: Socket listening with port: " + port);
    }

    /**
     * 停止监听
     *
     * @param saveResult 指示监听器是否保存本次接收的数据，但实际是否保存将取决于具体状态
     */
    public void stop(boolean saveResult) {
        if (saveResult && MyApplication.getController().isCtrlON()) {
            //Ctrl-ON, this save command should be ignored
            return;
        }

        if (!saveResult && saveFuture != null) {
            //get not save command, stop the schedule task immediately anyway.
            //note that when you cancel it, it becomes isDone.
            saveFuture.cancel(false);
        }

        //real stop operation
        try {
            if (bcSocket != null) {
                bcSocket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "stop: Socket closing failed", e);
        }
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        //post EndBroadcastEvent
        boolean res = saveResult && saveFuture != null && !saveFuture.isDone();
        EventBus.getDefault().post(new EndBroadcastEvent(res));
    }

    /**
     * 彻底关闭广播监听器
     */
    public void close() {
        if (closed) {
            return;
        }

        stop(false);
        saveFuture = null;
        bcSocket = null;
        closed = true;
    }

    /**
     * 控制状态变更事件
     */
    @Subscribe(priority = 100)  //Make the priority large enough
    public void onCtrlStateChangedEvent(CtrlStateChangedEvent event) {
        if (event.isCtrlON() && listening) {
            //immediately stop listening and discard the previously collected info
            stop(false);
        }
    }
}
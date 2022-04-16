package com.ssc.shakesocketcontroller.Transaction.threads;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ssc.shakesocketcontroller.Models.pojo.AppConfig;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;

import org.greenrobot.eventbus.EventBus;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO: 2022/4/16 广播监听那边要处理listening赋值时机，以修复连续调用可能会导致启动多个线程同时监听的问题。
// TODO: 2022/4/16 前台服务考虑是否需要允许多个线程同时监听（先在隔壁项目测试连续启动服务会执行哪些回调）
//  （如果需要则增加一个配置项；同时仅当判断当连接数量超过一定比例后才开启？）

/**
 * SSC的Ctrl前台服务，负责发送和接收实际的Ctrl操作
 */
public class SSCService extends Service {
    private static final String TAG = "SSCService";
    /**
     * 前台服务通知渠道ID
     */
    private static final String FOREGROUND_SERVICE_CHANNEL_ID = "SSC_FOREGROUND_SERVICE_CHANNEL";

    private final int msgPort;                      //消息监听端口
    private final int msgMaxReceiveBufSize;         //接收消息数据包的最大Buf大小

    private ExecutorService executor;               //执行线程池
    private DatagramSocket udpSocket;               //UDPSocket，用于发送和接收

    private volatile boolean listening = false;     //监听状态

    public SSCService() {
        //通过APP配置初始化参数
        AppConfig config = MyApplication.getController().getCurrentConfig();
        this.msgPort = config.msgPort;
        this.msgMaxReceiveBufSize = config.msgMaxReceiveBufSize;
    }

    @Override
    public void onCreate() {
        //初始化执行线程池
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setNameFormat("pool-SSC-SERVICE-thread-%d")
                .setDaemon(true)
                .build());
        //初始化UDPSocket
        try {
            udpSocket = new DatagramSocket(msgPort);
            Log.d(TAG, "onCreate: UDPSocket ready with port: " + msgPort);
        } catch (Throwable e) {
            Log.e(TAG, "onCreate: UDPSocket creation failed.", e);
        }

        if (udpSocket != null) {
            //提升为前台服务
            //startForeground(1,buildForegroundNotification());
        }

        EventBus.getDefault().register(this);
        Log.i(TAG, "onCreate: SSC Service Instantiation OK.");
    }

    private Notification buildForegroundNotification() {
        return null;
    }

    private void beginListen() {
        //listening在哪里设置才合适？
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (MyApplication.getController().isStopped() || !MyApplication.getController().isCtrlON()) {
            //已处于非Ctrl-ON状态，直接停止服务
            stopSelf();
        } else if (udpSocket == null) {
            //UDPSocket初始化失败，停止服务，并post前台服务异常事件
            stopSelf();
            // TODO: 2022/4/16 前台服务异常事件 -> 监听过程的异常也使用该事件
            //EventBus.getDefault().post();
        } else if (!listening) {
            //开始执行异步监听
            //beginListen();
            //Log.i(TAG, "onCreate: UDPSocket starts listening with port: ");
        }
        Log.d(TAG, "onStartCommand: SSC Service running.");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        try {
            if (udpSocket != null) {
                udpSocket.close();
            }
        } catch (Throwable e) {
            Log.e(TAG, "onDestroy: UDPSocket closing exception.", e);
        }
        Log.i(TAG, "onDestroy: SSC Service Closed.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        //不提供绑定功能
        return null;
    }

    // TODO: 2022/4/16 接收send事件，里面要判断udpSocket非空，利用线程池执行

}
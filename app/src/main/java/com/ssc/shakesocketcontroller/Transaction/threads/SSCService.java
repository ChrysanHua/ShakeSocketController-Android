package com.ssc.shakesocketcontroller.Transaction.threads;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ssc.shakesocketcontroller.Models.events.signal.SSCServiceStateChangedEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.SendUDPEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.UDPReceiveEvent;
import com.ssc.shakesocketcontroller.Models.pojo.AppConfig;
import com.ssc.shakesocketcontroller.R;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import androidx.core.app.NotificationCompat;

// TODO: 2022/4/24 订阅连接成功事件，更新SSC前台服务通知

// TODO: 2022/4/20 后续涉及自动启动SSC服务的发开时，思考通过在启动Intent中再加入一个特殊键值；只需要单线程监听

/**
 * SSC的Ctrl前台服务，负责发送和接收实际的Ctrl操作
 */
public class SSCService extends Service {
    private static final String TAG = "SSCService";
    /**
     * 前台服务启动操作的有效性标志（作为启动服务时在Intent中指示该启动操作有效性的键）
     */
    public static final String STARTUP_VALID_FLAG =
            MyApplication.appContext.getPackageName() + ".SSC_SERVICE_STARTUP_VALID";
    /**
     * 前台服务通知渠道ID
     */
    public static final String FOREGROUND_SERVICE_CHANNEL_ID = "SSC_FOREGROUND_SERVICE_CHANNEL";
    /**
     * 前台服务通知渠道名
     */
    public static final String FOREGROUND_SERVICE_CHANNEL_NAME = "SSC服务";
    /**
     * 前台服务通知ID
     */
    public static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 1;

    private final int msgPort;                      //消息监听端口
    private final int msgMaxReceiveBufSize;         //接收消息数据包的最大Buf大小
    private final boolean allowMTListen;            //是否允许多线程监听

    private ExecutorService executor;               //执行线程池
    private DatagramSocket udpSocket;               //UDPSocket，用于发送和接收
    private NotificationCompat.Builder nBuilder;    //前台服务通知的Builder

    private volatile boolean listening = false;     //监听状态
    private volatile boolean isAutomaticOP = false; //是否正在执行自动启动/停止


    public SSCService() {
        //通过APP配置初始化参数
        final AppConfig config = MyApplication.getController().getCurrentConfig();
        this.msgPort = config.msgPort;
        this.msgMaxReceiveBufSize = config.msgMaxReceiveBufSize;
        this.allowMTListen = config.allowSSCMTListen;
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

        //初始化前台服务通知Builder
        nBuilder = MyApplication.getController().buildNotificationBuilder(
                FOREGROUND_SERVICE_CHANNEL_ID,
                FOREGROUND_SERVICE_CHANNEL_NAME,
                R.string.foreground_service_channel_desc);
        //提升为前台服务
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, buildForegroundNotification());

        EventBus.getDefault().register(this);
        Log.i(TAG, "onCreate: SSC Service Instantiation OK.");
    }

    /**
     * 构建该前台服务对应的通知
     */
    private Notification buildForegroundNotification() {
        //标题
        String title = "Listening…";
        final int ctrlCount = MyApplication.getController().getCtrlDevicesCount();
        if (ctrlCount > 0) {
            title = MyApplication.getController().getCtrlDevices().get(0).nickName;
            if (ctrlCount > 1) {
                title += " 等" + ctrlCount + "台设备";
            }
        }
        //内容
        final String content = MyApplication.getController().getCtrlConnectedDevicesCount()
                + "台设备已成功连接";

        //创建通知对象
        return nBuilder.setContentTitle(title).setContentText(content).build();
    }

    /**
     * 开始无限循环监听接收与Ctrl相关的UDP消息数据报
     */
    private void beginListen() {
        executor.submit(() -> {
            Log.i(TAG, "beginListen: UDPSocket starts listening in thread: "
                    + Thread.currentThread().getName());

            //获取当前线程ID
            final long threadID = Thread.currentThread().getId();
            boolean hasException = false;   //是否发生异常

            try {
                //开始持续监听
                while (listening) {
                    DatagramPacket packet = new DatagramPacket(
                            new byte[msgMaxReceiveBufSize], msgMaxReceiveBufSize);
                    udpSocket.receive(packet);
                    //post接收到UDP数据报事件
                    EventBus.getDefault().post(new UDPReceiveEvent(packet, threadID));
                }
                Log.w(TAG, "beginListen: Socket reception stopped unexpectedly!");
            } catch (SocketException e) {
                //预期内的异常，Socket被关闭
                //Log.d(TAG, "beginListen: Socket closed", e);
                Log.i(TAG, "beginListen: Socket closed.");
                listening = false;
                return;
            } catch (Throwable e) {
                //监听异常终止
                Log.e(TAG, "beginListen: Socket exception.", e);
                hasException = true;
            }

            //非主动的监听终止，确保停止服务，并post SSC服务状态变更事件
            final boolean needPostEvent = !isAutomaticOP && listening;
            listening = false;
            isAutomaticOP = true;
            stopSelf();
            if (needPostEvent) {
                //仅第一个进入的线程需要post SSC服务状态变更事件，发通知
                EventBus.getDefault().post(new SSCServiceStateChangedEvent(false, true,
                        hasException, threadID));
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (MyApplication.getController().isStopped() || !MyApplication.getController().isCtrlON()) {
            //已处于非Ctrl-ON状态，直接停止服务
            final boolean needPostEvent = !isAutomaticOP;
            isAutomaticOP = true;
            stopSelf();
            if (needPostEvent) {
                //仅系统自行重启服务时，才需要post SSC服务状态变更事件，发通知
                EventBus.getDefault().post(new SSCServiceStateChangedEvent(false, true, false));
            }
        } else if (udpSocket == null) {
            //UDPSocket初始化失败，自动停止服务
            isAutomaticOP = true;
            stopSelf();
            //post SSC服务状态变更事件，发SnackBar
            EventBus.getDefault().post(new SSCServiceStateChangedEvent(false, true, true));
        } else if (!listening) {
            //设置状态
            listening = true;
            //开始执行异步监听
            beginListen();
            //post SSC服务状态变更事件，发SnackBar
            EventBus.getDefault().post(new SSCServiceStateChangedEvent(true));
        } else if ( //忽略重复调用启动（当不允许多线程监听，或当前是无效启动，或启动异常需要自动停止服务时）
                allowMTListen && !isAutomaticOP
                        && intent != null && intent.getBooleanExtra(STARTUP_VALID_FLAG, false)) {
            //每次调用（启动）都开启一个新线程进行同步多线程监听
            beginListen();
        }
        Log.i(TAG, "onStartCommand: SSC Service running.");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        try {
            if (udpSocket != null) {
                //关闭Socket
                udpSocket.close();
            }

            //关闭执行线程池
            executor.shutdown();
            //清空通知Builder
            nBuilder = null;
        } catch (Throwable e) {
            Log.e(TAG, "onDestroy: SSC Service closing exception.", e);
            //post SSC服务状态变更事件，发Toast
            EventBus.getDefault().post(new SSCServiceStateChangedEvent(false, false, true));
        }

        if (!isAutomaticOP) {
            //只有正常停止服务时，才需要这个post SSC服务状态变更事件，发SnackBar
            EventBus.getDefault().post(new SSCServiceStateChangedEvent(false));
        }
        Log.i(TAG, "onDestroy: SSC Service Closed.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        //不提供绑定功能
        return null;
    }

    /**
     * 发送Ctrl相关消息事件
     */
    @Subscribe(sticky = true, priority = 1)
    public void onSendUDPEvent(SendUDPEvent event) {
        //无论如何，先移除sticky事件
        EventBus.getDefault().removeStickyEvent(event);

        if (udpSocket == null || udpSocket.isClosed() || !listening) {
            return;
        }

        //开始执行异步发送
        Future<?> future = executor.submit(() -> {
            //获取当前线程ID
            final long threadID = Thread.currentThread().getId();

            try {
                // TODO: 2022/4/18 从event中构造出数据报，完善发送代码；要注意单次调用可能需要发送多条数据
                DatagramPacket packet = null;   //伪代码
                udpSocket.send(packet);
                Log.d(TAG, "onSendUDPEvent: send packet successfully");
            } catch (Throwable e) {
                Log.e(TAG, "onSendUDPEvent: send packet failed.", e);
                //发送失败，post SSC服务状态变更事件，检查各项状态然后视情况告知用户
                // TODO: 2022/4/19 根据异常状态在controller订阅中作出相应处理（告知用户还是停止服务还是忽略）
                EventBus.getDefault().post(new SSCServiceStateChangedEvent(listening, isAutomaticOP,
                        event, threadID));
            }
        });

        if (event.isAsyncBlocking()) {
            try {
                //阻塞等待异步发送完成
                future.get();
            } catch (Throwable e) {
                Log.e(TAG, "onSendUDPEvent: Exception during blocking.", e);
                //同步等待过程中出现异常，无法确定发送是否已完成，post SSC服务状态变更事件，发Toast
                EventBus.getDefault().post(new SSCServiceStateChangedEvent(listening, isAutomaticOP,
                        event, -1));
            }
        }
    }
}

package com.ssc.shakesocketcontroller.Transaction.threads;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ssc.shakesocketcontroller.Models.events.signal.SSCServiceExceptionEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.SendUDPEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.UDPReceiveEvent;
import com.ssc.shakesocketcontroller.Models.pojo.AppConfig;
import com.ssc.shakesocketcontroller.R;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;
import com.ssc.shakesocketcontroller.UI.activities.MainActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.core.app.NotificationCompat;

// TODO: 2022/4/16 前台服务允许多个线程同时监听，启用策略在controller中实现↓
//  （当配置开启时，根据连接数量计算线程数，每超10个多开一条线程，但最多不超过【核心数-1】）

// TODO: 2022/4/18 思考前台服务通知更新问题，在这里更新还是在controller中更新？

/**
 * SSC的Ctrl前台服务，负责发送和接收实际的Ctrl操作
 */
public class SSCService extends Service {
    private static final String TAG = "SSCService";
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

    private volatile boolean listening = false;     //监听状态

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

        if (udpSocket != null) {
            //提升为前台服务
            startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, buildForegroundNotification());
        }

        EventBus.getDefault().register(this);
        Log.i(TAG, "onCreate: SSC Service Instantiation OK.");
    }

    /**
     * 构建该前台服务对应的通知
     */
    private Notification buildForegroundNotification() {
        final NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //创建通知渠道（如果需要）
            notificationManager.createNotificationChannel(new NotificationChannel(
                    FOREGROUND_SERVICE_CHANNEL_ID,
                    FOREGROUND_SERVICE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW));
        }

        //创建点击通知时的行为
        final PendingIntent pi = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), 0);
        //标题
        String title = "Listening…";
        int ctrlCount = MyApplication.getController().getCtrlDevicesCount();
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
        return new NotificationCompat.Builder(this, FOREGROUND_SERVICE_CHANNEL_ID)
                .setContentIntent(pi)
                .setSmallIcon(R.drawable.ic_ssc_switch)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
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
                Log.d(TAG, "beginListen: Socket closed", e);
                //Log.i(TAG, "beginListen: Socket closed.");
                listening = false;
                return;
            } catch (Throwable e) {
                //监听异常终止
                Log.e(TAG, "beginListen: Socket exception.", e);
                hasException = true;
            }

            //非主动终止监听，停止服务，并post前台服务异常事件
            listening = false;
            stopSelf();
            EventBus.getDefault().post(new SSCServiceExceptionEvent(hasException, threadID));
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (MyApplication.getController().isStopped() || !MyApplication.getController().isCtrlON()) {
            //已处于非Ctrl-ON状态，直接停止服务
            stopSelf();
        } else if (udpSocket == null) {
            //UDPSocket初始化失败，停止服务，并post前台服务异常事件
            stopSelf();
            EventBus.getDefault().post(new SSCServiceExceptionEvent());
        } else if (!listening) {
            //设置状态
            listening = true;
            //开始执行异步监听
            beginListen();
        } else if (allowMTListen) {     //如果不允许多线程监听则忽略其重复启动的调用
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
        } catch (Throwable e) {
            Log.e(TAG, "onDestroy: SSC Service closing exception.", e);
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
    @Subscribe(priority = 1)
    public void onSendUDPEvent(SendUDPEvent event) {
        if (udpSocket == null || udpSocket.isClosed() || !listening) {
            return;
        }

        //开始执行异步发送
        executor.submit(() -> {
            //获取当前线程ID
            final long threadID = Thread.currentThread().getId();

            try {
                // TODO: 2022/4/18 从event中构造出数据报，完善发送代码；要注意单次调用可能需要发送多条数据
                DatagramPacket packet = null;   //伪代码
                udpSocket.send(packet);
                Log.d(TAG, "onSendUDPEvent: send packet successfully");
            } catch (Throwable e) {
                //发送失败，post前台服务异常事件
                Log.e(TAG, "onSendUDPEvent: send packet failed.", e);
                EventBus.getDefault().post(new SSCServiceExceptionEvent(event, threadID));
            }
        });
    }
}

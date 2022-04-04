package com.ssc.shakesocketcontroller.Transaction.controller;

import android.util.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ssc.shakesocketcontroller.Models.events.signal.BroadcastEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndBroadcastEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndReadHistoryEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndRefreshEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.SendUDPEvent;
import com.ssc.shakesocketcontroller.Models.pojo.AppConfig;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Transaction.threads.BroadcastListener;
import com.ssc.shakesocketcontroller.Transaction.threads.HistoryWorker;
import com.ssc.shakesocketcontroller.Transaction.threads.TCPConnectThread;
import com.ssc.shakesocketcontroller.Transaction.threads.TCPHandlerThread;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.SubscriberExceptionEvent;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.eventbus.util.ThrowableFailureEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

// TODO: 2022/3/17 正常UDP通讯端看看发送和接收是否需要分开Socket，双方均需常驻，特别是接收方；
//  接收方：持续接收数据，接收后交由MsgAdapter处理，而发送指令后回复超时则在外部使用定时任务来实现，
//      可以考虑一个指令仅触发一个定时任务（无论已连接几台设备，超时后逐个处理）；
//  发送方：可满足每次按需调用发送即可，如果单条指令需要发送给多台设备，可以在全部发送完毕之后再触发定时任务；

// TODO: 2022/3/24 合理处理EventBus的两个全局异常接收事件。

/**
 * 全局控制器类，使用单例模式
 */
public final class TransactionController {
    private static final String TAG = "TransactionController";

    private static final int BC_PORT = 19019;
    private static final int MSG_PORT = 10019;
    private static final int TCP_PORT = 11019;

    private static final int BC_LISTEN_DURATION = 3000;
    private static final int ASYNC_THREAD_COUNT = 3;

    /**
     * 自引用，用于单例模式的对象
     */
    private static final TransactionController controllerInstance = new TransactionController();

    private boolean stopped = true;                         //启动状态
    private volatile boolean ctrlON = false;                //控制（Ctrl）状态
    private int lastDestinationID = -1;                     //最后一个导航目的地ID

    private final List<ComputerInfo> onlineDeviceList;
    private final List<ComputerInfo> historyDeviceList;
    private final List<ComputerInfo> ctrlDeviceList;

    private ScheduledExecutorService scheduledExecutor;     //异步延时任务执行器
    private BroadcastListener bcListener;                   //广播监听器
    private HistoryWorker historyWorker;                    //历史连接信息记录器
    private AppConfig config;                               //APP配置

    private TCPConnectThread tcpListener;
    private TCPHandlerThread tcpHandler;
    private MessageAdapter msgAdapter;
    private DatagramSocket udpSender;


    static TransactionController getInstance() {
        return controllerInstance;
    }

    private TransactionController() {
        onlineDeviceList = MyApplication.createTestCPInfoList(10, true);
        historyDeviceList = MyApplication.createTestCPInfoList(3, false);
        ctrlDeviceList = MyApplication.createTestCPInfoList(0, true);
        try {
            config = AppConfig.load();
        } catch (Exception e) {
            //应注意这个异常是致命的
            Log.e(TAG, "TransactionController: ", e);
        }
        //msgAdapter = new MessageAdapter(this);
        Log.i(TAG, "TransactionController: instantiation OK");
    }

    public List<ComputerInfo> getCurrentDevices(boolean isOnline) {
        return isOnline ? onlineDeviceList : historyDeviceList;
    }

    public List<ComputerInfo> getCtrlDevices() {
        return ctrlDeviceList;
    }

    public int getCurrentDevicesCount(boolean isOnline) {
        return getCurrentDevices(isOnline).size();
    }

    public int getCtrlDevicesCount() {
        return getCtrlDevices().size();
    }

    public int getLastDestinationID() {
        return lastDestinationID;
    }

    public void setLastDestinationID(int lastDestinationID) {
        this.lastDestinationID = lastDestinationID;
    }

    /**
     * 获取控制器的Ctrl（控制）状态
     */
    public boolean isCtrlON() {
        return ctrlON;
    }

    /**
     * 设置控制器的Ctrl（控制）状态
     */
    public void setCtrlON(boolean enabled) {
        ctrlON = enabled;

        if (ctrlON) {
            //Ctrl-ON，先清空旧的控制列表
            ctrlDeviceList.clear();
        }

        //获取CPInfo列表选中项，改变其成员状态
        for (int i = 0; i < onlineDeviceList.size(); i++) {
            ComputerInfo info = onlineDeviceList.get(i);
            if (info.isChecked) {
                if (ctrlON) {
                    //拉到控制列表里
                    ctrlDeviceList.add(info);
                    //连接成功才保存为历史
                    info.isSaved = false;
                }
                info.isConnected = ctrlON;
            }
        }
    }

    /**
     * 替换新设备列表（在线/历史）
     */
    public void setNewDevices(List<ComputerInfo> newList, boolean isOnline) {
        if (isOnline) {
            onlineDeviceList.clear();
            onlineDeviceList.addAll(newList);
        } else {
            historyDeviceList.clear();
            historyDeviceList.addAll(newList);
        }
    }

    /**
     * 获取控制器的关闭/启动状态
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * 获取当前已加载的APP配置
     */
    public AppConfig getCurrentConfig() {
        return config;
    }

    /**
     * 启动全局控制器
     */
    public void start() {
        //初始化后台异步延时任务执行器，其线程池中的线程将被设置为守护线程
        scheduledExecutor = Executors.newScheduledThreadPool(ASYNC_THREAD_COUNT,
                new ThreadFactoryBuilder()
                        .setNameFormat("pool-SSC-ASYNC-thread-%d")
                        .setDaemon(true)
                        .build());
        //初始化广播监听器
        bcListener = new BroadcastListener(BC_PORT, BC_LISTEN_DURATION, scheduledExecutor);
        //初始化历史连接记录器
        historyWorker = new HistoryWorker(scheduledExecutor);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        stopped = false;
    }

    /**
     * 关闭全局控制器
     */
    public void stop() {
        // TODO: 2022/3/21 stop方法需要找到合适的时机来调用，目前考虑在离开Activity的时候如果Ctrl没开就调用；
        //  这样就需要同步考虑再次进入Activity（App未中止）的时候进行reload
        bcListener.close();
        scheduledExecutor.shutdown();

        //msgAdapter.stop();
        //StopBroadcastListener();
        //StopTCPListener();
        //StopTCPHandler();
        //if (udpSender != null) {
        //    udpSender.close();
        //    udpSender = null;
        //}

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        stopped = true;
    }

    protected void reload() {
        stop();
        try {
            udpSender = new DatagramSocket();
        } catch (SocketException e) {
            udpSender = null;
            Log.e(TAG, "reload: udpSender create() failed", e);
        }
        EventBus.getDefault().register(this);
        //msgAdapter.start();
        stopped = false;
    }

    /**
     * 从在线列表和历史列表中检索是否已存在指定的设备连接信息，并视情况决定返回哪一个对象
     *
     * @return 如果已存在则视情况合并信息然后返回对应的对象，否则返回参数原对象
     */
    //该方法用于广播监听
    public ComputerInfo filterInfoFromCurDevices(final ComputerInfo info) {
        // TODO: 2022/4/3 历史读取那边要设置isSave，广播这边记得设置isOnline
        ComputerInfo resInfo = info;
        if (info != null) {
            if (onlineDeviceList.contains(info)) {
                //已存在于在线列表，使用列表值，但需复制IP
                resInfo = onlineDeviceList.get(onlineDeviceList.indexOf(info));
                resInfo.address = info.address;
            } else if (historyDeviceList.contains(info)) {
                //仅存在于历史列表，使用列表值，但需复制IP
                resInfo = historyDeviceList.get(historyDeviceList.indexOf(info));
                resInfo.address = info.address;
            } else if (!MyApplication.getController().getCurrentConfig().ignoredSameHistory) {
                ComputerInfo sameHistory = lastInfoSameInHistory(info);
                if (sameHistory != null) {
                    //仅存在于历史列表中，但信息已发生变化，使用新值，但需复制设置
                    resInfo.cloneConfig(sameHistory);
                }
            }
        }
        return resInfo;
    }

    /**
     * 获取历史设备列表中最后一个与指定目标相似的设备信息
     *
     * @return 返回最后一个相似对象，如果没有则返回null
     */
    public ComputerInfo lastInfoSameInHistory(@NonNull final ComputerInfo info) {
        ComputerInfo resInfo = null;
        for (int i = historyDeviceList.size() - 1; i >= 0; i--) {
            if (historyDeviceList.get(i).isSameDevice(info)) {
                resInfo = historyDeviceList.get(i);
                break;
            }
        }
        return resInfo;
    }


    public void StartBroadcastListener() {
        try {
            bcListener.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void StopBroadcastListener() {
        bcListener.stop(false);
    }

    public void StartTCPListener() {
        if (tcpListener == null) {
            tcpListener = new TCPConnectThread(TCP_PORT);
        }
        tcpListener.start();
    }

    public void StopTCPListener() {
        if (tcpListener == null)
            return;
        tcpListener.cancel();
        tcpListener = null;
    }

    public void StartTCPHandler(Socket client) {
        if (tcpHandler != null) {
            //only allow one TCPHandler exist
            StopTCPHandler();
        }
        tcpHandler = new TCPHandlerThread(client);
        tcpHandler.start();
    }

    public void StopTCPHandler() {
        if (tcpHandler == null)
            return;
        tcpHandler.cancel();
        tcpHandler = null;
    }

    /**
     * 执行一次广播监听，监听将持续一段时间然后自动结束
     */
    public void observeBCOnce() {
        //如果正在监听则忽略本次调用
        if (!bcListener.isListening()) {
            //post空广播事件，通知准备开始广播监听
            EventBus.getDefault().post(new BroadcastEvent(null));
            //开始监听
            try {
                bcListener.start();
            } catch (Exception e) {
                Log.e(TAG, "observeBCOnce: ", e);
            }
        }
    }

    /**
     * 执行一次历史读取
     */
    public void readHistoryOnce() {
        // TODO: 2022/4/1 进入APP时应当先读取一次历史记录
        //如果正在读取则忽略本次调用
        if (!historyWorker.isReading()) {
            if (bcListener.isListening()) {
                //如果正在监听广播则停止它
                bcListener.stop(false);
            }
            //开始读取
            historyWorker.readStart();
        }
    }

    /**
     * 发起一个异步延时任务
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (!scheduledExecutor.isShutdown()) {
            return scheduledExecutor.schedule(command, delay, unit);
        }
        return null;
    }

    /**
     * 发起一个异步延时任务（以毫秒为延时单位）
     */
    public ScheduledFuture<?> schedule(Runnable command, long millisecondDelay) {
        return schedule(command, millisecondDelay, TimeUnit.MILLISECONDS);
    }


    @Subscribe
    public void onEndBroadcastEvent(EndBroadcastEvent event) {
        //测试方法，该方法应该出现在MsgAdapter里
        Log.i(TAG, "onEndBroadcastEvent: shouldSaveResult? -> " + event.shouldSaveResult());
        if (event.shouldSaveResult()) {
            //模拟BC改变在线列表
            Random random = new Random();
            List<ComputerInfo> newList = MyApplication.createTestCPInfoList(random.nextInt(10), true);
            onlineDeviceList.clear();
            onlineDeviceList.addAll(newList);
            //post刷新完成事件
            EventBus.getDefault().postSticky(new EndRefreshEvent(onlineDeviceList.size(), true, true));
            Log.i(TAG, "onEndBroadcastEvent: after post EndRefreshEvent");
        }
    }

    @Subscribe
    public void onEndReadHistoryEvent(EndReadHistoryEvent event) {
        //测试方法，该方法应该出现在MsgAdapter里
        Log.i(TAG, "onEndReadHistoryEvent: " + event.getHistoryList().size());
        //post刷新完成事件
        EventBus.getDefault().postSticky(new EndRefreshEvent(historyDeviceList.size(), false, false));
        Log.i(TAG, "onEndReadHistoryEvent: after post EndRefreshEvent");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSendUDPEvent(SendUDPEvent event) {
        if (udpSender == null)
            return;
        try {
            DatagramPacket packet = new DatagramPacket(event.getMsgData(),
                    event.getMsgData().length, event.getAddress(), MSG_PORT);
            udpSender.send(packet);
            Log.d(TAG, "onSendUDPEvent: send packet success");
        } catch (IOException e) {
            Log.e(TAG, "onSendUDPEvent: udpSender send() failed", e);
        }
    }

    /**
     * 全局Event（EventBus）执行异常事件
     */
    @Subscribe
    public void onSubscriberExceptionEvent(SubscriberExceptionEvent event) {
        Log.e(TAG, "onSubscriberExceptionEvent: ", event.throwable);
    }

    /**
     * 全局AsyncExecutor（EventBus）执行异常事件
     */
    @Subscribe
    public void onThrowableFailureEvent(ThrowableFailureEvent event) {
        Log.e(TAG, "onThrowableFailureEvent: ", event.getThrowable());
    }

}

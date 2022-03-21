package com.ssc.shakesocketcontroller.Transaction.controller;

import android.os.Handler;
import android.util.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ssc.shakesocketcontroller.Models.events.signal.EndRefreshEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.SendUDPEvent;
import com.ssc.shakesocketcontroller.Transaction.threads.BroadcastListenThread;
import com.ssc.shakesocketcontroller.Transaction.threads.TCPConnectThread;
import com.ssc.shakesocketcontroller.Transaction.threads.TCPHandlerThread;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Models.pojo.Configuration;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 全局控制器类，使用单例模式
 */
public final class TransactionController {
    private static final String TAG = "TransactionController";

    private static final int BC_PORT = 19019;
    private static final int MSG_PORT = 10019;
    private static final int TCP_PORT = 11019;

    private static final int BC_LISTEN_DURATION = 3000;
    private static final int ASYNC_THREAD_COUNT = 2;

    private static final TransactionController controllerInstance = new TransactionController();

    private boolean stopped = true;                         //启动状态
    private boolean ctrlON = false;                         //控制（Ctrl）状态
    private int lastDestinationID = -1;                     //最后一个导航目的地ID

    private final List<ComputerInfo> onlineDeviceList;
    private final List<ComputerInfo> historyDeviceList;
    private final List<ComputerInfo> ctrlDeviceList;

    private ScheduledExecutorService scheduledExecutor;     //异步任务执行器

    private BroadcastListenThread bcListener;
    private TCPConnectThread tcpListener;
    private TCPHandlerThread tcpHandler;
    private MessageAdapter msgAdapter;
    private DatagramSocket udpSender;
    private Configuration config;

    private TransactionController() {
        onlineDeviceList = BuildTestCPInfos(10, true);
        historyDeviceList = BuildTestCPInfos(3, false);
        ctrlDeviceList = BuildTestCPInfos(0, true);
        config = Configuration.load();
        //msgAdapter = new MessageAdapter(this);
        Log.i(TAG, "TransactionController: instantiation OK");
    }

    static TransactionController getInstance() {
        return controllerInstance;
    }

    private List<ComputerInfo> BuildTestCPInfos(int count, boolean online) {
        //测试用，生成临时测试数据
        List<ComputerInfo> infos = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            ComputerInfo cp = new ComputerInfo(DeviceUtil.getLocalAddress(),
                    "CP" + i, "NickName" + i + online);
            cp.isOnline = online || i % 2 == 0;
            cp.isSaved = (i <= 2);
            //cp.isConnected = (i == 2 || i == 11);
            cp.isChecked = (i <= 2);
            infos.add(cp);
        }
        return infos;
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
     * 获取控制器的关闭/启动状态
     */
    public boolean isStopped() {
        return stopped;
    }

    public Configuration getCurrentConfig() {
        return config;
    }

    /**
     * 启动全局控制器
     */
    public void start() {
        //初始化后台异步执行器，其线程池中的线程将被设置为守护线程
        scheduledExecutor = Executors.newScheduledThreadPool(ASYNC_THREAD_COUNT,
                new ThreadFactoryBuilder()
                        .setNameFormat("pool-SSC-ASYNC-thread-%d")
                        .setDaemon(true)
                        .build());

        stopped = false;
    }

    /**
     * 关闭全局控制器
     */
    public void stop() {
        scheduledExecutor.shutdown();

        //if (EventBus.getDefault().isRegistered(this)) {
        //    EventBus.getDefault().unregister(this);
        //}
        //msgAdapter.stop();
        //StopBroadcastListener();
        //StopTCPListener();
        //StopTCPHandler();
        //if (udpSender != null) {
        //    udpSender.close();
        //    udpSender = null;
        //}

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
        msgAdapter.start();
        StartBroadcastListener();
        stopped = false;
    }

    public void StartBroadcastListener() {
        if (bcListener == null) {
            bcListener = new BroadcastListenThread(BC_PORT);
        }
        bcListener.start();
    }

    public void StopBroadcastListener() {
        if (bcListener == null)
            return;
        bcListener.cancel();
        bcListener = null;
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
        // TODO: 2022/3/9 先获取是否正在监听状态（上一次监听尚未结束），不是的话才执行监听；
        //  从配置中获取监听间隔，调用一次广播监听；
        //  BCListener每收到一次广播就应该通知MsgAdapter缓存起来，这时应后历史列表对比，复用其元素，
        //  最后监听自动停止时MsgAdapter再整体处理，然后通知列表Adapter更新并停止刷新UI；
        // TODO: 2022/3/11 广播监听内部的具体实现应该订阅CtrlStateChangedEvent事件，
        //  如果处于Ctrl-ON状态，应当立即停止当前的刷新行为；
        Log.i(TAG, "observeBCOnce: " + new Handler().postDelayed(() -> {
            //模拟BC监听的这个耗时过程
            Random random = new Random();
            List<ComputerInfo> newList = BuildTestCPInfos(random.nextInt(10), true);
            onlineDeviceList.clear();
            onlineDeviceList.addAll(newList);
            EventBus.getDefault().post(new EndRefreshEvent(onlineDeviceList.size(), true));
        }, BC_LISTEN_DURATION));
    }

    /**
     * 执行一次历史读取
     */
    public void readHistoryOnce() {
        // TODO: 2022/3/10 先获取是否正在读取历史（上一次读取尚未结束），不是的话才执行读取；
        //  从本地读取历史设备列表，这时应该和在线列表逐一对比，复用其中的元素；
        // TODO: 2022/3/11 历史读取内部实现应订阅CtrlStateChangedEvent事件，
        //  如果处于Ctrl-ON状态，应当立即停止当前的刷新行为；
        Log.i(TAG, "readHistoryOnce: " + new Handler().postDelayed(() -> {
            //模拟本地读取的耗时过程
            EventBus.getDefault().post(new EndRefreshEvent(historyDeviceList.size(), false));
        }, 1500));
    }

    /**
     * 设定一个延时异步执行的任务
     */
    public void schedule(Runnable command, long delay, TimeUnit unit) {
        if (!scheduledExecutor.isShutdown()) {
            scheduledExecutor.schedule(command, delay, unit);
        }
    }

    /**
     * 设定一个延时异步执行的任务（以毫秒为延时单位）
     */
    public void schedule(Runnable command, long millisecondDelay) {
        schedule(command, millisecondDelay, TimeUnit.MILLISECONDS);
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

}

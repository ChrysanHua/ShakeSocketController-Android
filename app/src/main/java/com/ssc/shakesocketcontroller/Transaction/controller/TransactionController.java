package com.ssc.shakesocketcontroller.Transaction.controller;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ssc.shakesocketcontroller.Models.events.signal.BroadcastEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.CtrlStateChangedEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndRefreshEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.SSCServiceStateChangedEvent;
import com.ssc.shakesocketcontroller.Models.pojo.AppConfig;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.R;
import com.ssc.shakesocketcontroller.Transaction.threads.BroadcastListener;
import com.ssc.shakesocketcontroller.Transaction.threads.HistoryWorker;
import com.ssc.shakesocketcontroller.Transaction.threads.SSCService;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.SubscriberExceptionEvent;
import org.greenrobot.eventbus.util.ThrowableFailureEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

// TODO: 2022/4/20 现在：
//  2.处理全局发通知的逻辑；
//  3.先搞定SSC服务里与发送相关的逻辑功能；

// TODO: 2022/3/17 SSC前台服务：
//  接收方：持续接收数据，接收后交由MsgAdapter处理；
//  发送方：实现单条指令只post一次事件，回复超时监测可以在全部设备发送完毕之后再在外部触发定时任务；
//      单条指令仅触发一个定时任务（无论已连接几台设备，超时后逐个处理）；

// TODO: 2022/4/8 有必要将主列表换为线程安全的Vector，冲突主要体现在setNewDevices时的各读取访问操作，该工作暂时延后！
// TODO: 2022/3/24 合理处理EventBus的两个全局异常接收事件。

/**
 * 全局控制器类，使用单例模式
 */
public final class TransactionController {
    private static final String TAG = "TransactionController";

    /**
     * 自引用，用于单例模式的对象
     */
    private static final TransactionController controllerInstance = new TransactionController();

    private static final int ASYNC_THREAD_COUNT = 3;        //异步延时任务执行器的最大线程数

    private volatile boolean stopped = true;                //启动状态
    private volatile boolean ctrlON = false;                //控制（Ctrl）状态
    private volatile boolean sscServiceStateFlag = false;   //SSC服务状态标志（不能保证准确反映实际运行状态）
    private int lastDestinationID = -1;                     //最后一个导航目的地ID

    private final List<ComputerInfo> onlineDeviceList;      //在线设备连接列表
    private final List<ComputerInfo> historyDeviceList;     //历史设备连接列表
    private final List<ComputerInfo> ctrlDeviceList;        //当前要进行SSC控制的设备连接列表

    private final MessageAdapter msgAdapter;                //消息适配器
    private ScheduledExecutorService scheduledExecutor;     //异步延时任务执行器
    private BroadcastListener bcListener;                   //广播监听器
    private HistoryWorker historyWorker;                    //历史连接信息记录器
    private AppConfig config;                               //APP配置


    /**
     * @return 返回单例对象
     */
    static TransactionController getInstance() {
        return controllerInstance;
    }

    private TransactionController() {
        //初始化各设备连接列表
        onlineDeviceList = new ArrayList<>();
        historyDeviceList = new ArrayList<>();
        ctrlDeviceList = new ArrayList<>();
        //加载APP配置
        config = loadLatestConfig();
        //初始化消息适配器
        msgAdapter = new MessageAdapter();
        Log.i(TAG, "TransactionController: Instantiation OK");
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

    public int getCtrlConnectedDevicesCount() {
        int count = 0;
        for (int i = 0; i < ctrlDeviceList.size(); i++) {
            ComputerInfo info = ctrlDeviceList.get(i);
            if (info.isConnected && info.isSaved) {
                count++;
            }
        }
        return count;
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
    public void setNewDevices(@NonNull final List<ComputerInfo> newList, boolean isOnline) {
        //该方法线程不安全！
        if (stopped) {
            return;
        }

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
     * 从本地加载最新的APP配置，如果加载失败将返回默认值配置
     */
    public AppConfig loadLatestConfig() {
        try {
            return AppConfig.load();
        } catch (Exception e) {
            // TODO: 2022/4/10 告知用户APP配置加载异常
            //无法从本地加载配置，这是十分致命的异常，必须告知用户
            Log.e(TAG, "loadLatestConfig: ", e);
            //返回默认值配置，这只是保持程序正常运行的备用方案
            return new AppConfig();
        }
    }

    /**
     * 启动全局控制器
     */
    protected void start() {
        //执行启动
        reload();
        //加载历史设备连接列表
        List<ComputerInfo> historyList = historyWorker.read();
        for (int i = 0; i < historyList.size(); i++) {
            //标志其为历史设备
            historyList.get(i).isSaved = true;
        }
        setNewDevices(historyList, false);
    }

    /**
     * 重新启动全局控制器
     */
    public void reload() {
        if (!stopped) {
            stop();
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        //启动消息适配器
        msgAdapter.start();
        //初始化后台异步延时任务执行器，其线程池中的线程将被设置为守护线程
        scheduledExecutor = Executors.newScheduledThreadPool(ASYNC_THREAD_COUNT,
                new ThreadFactoryBuilder()
                        .setNameFormat("pool-SSC-ASYNC-thread-%d")
                        .setDaemon(true)
                        .build());
        //初始化广播监听器
        bcListener = new BroadcastListener(scheduledExecutor);
        //初始化历史连接记录器
        historyWorker = new HistoryWorker(scheduledExecutor);

        stopped = false;
        Log.i(TAG, "reload: SSC controller started.");
    }

    /**
     * 关闭全局控制器
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;

        //先停止消息的适配处理
        msgAdapter.stop();
        //停止SSC服务（如果需要）
        toggleSSCServiceState(false);
        //关闭广播监听、历史读取以及异步延时执行器等
        bcListener.close();
        scheduledExecutor.shutdown();
        bcListener = null;
        historyWorker = null;
        scheduledExecutor = null;
        //重置状态记录等
        setLastDestinationID(-1);
        if (ctrlON) {
            setCtrlON(false);
        }

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        Log.i(TAG, "stop: SSC controller stopped.");
    }

    /**
     * 切换SSC前台服务的状态（启动/停止）
     */
    public void toggleSSCServiceState(boolean enabled) {
        final Intent intent = new Intent(MyApplication.appContext, SSCService.class);
        try {
            if (enabled) {      //启动前台服务
                //设置启动有效性标志，如果已经执行过启动则当前为无效操作，否则为有效操作
                intent.putExtra(SSCService.STARTUP_VALID_FLAG, !sscServiceStateFlag);
                //计算SSC监听线程数（如果需要）：每10台设备至少需要1个线程，线程数最多不超过【可用核心数-1】
                final int threadCount = !config.allowSSCMTListen ? 1 : Math.max(1, Math.min(
                        DeviceUtil.getAvailableProcessors() - 1, (getCtrlDevicesCount() / 10) + 1));
                Log.d(TAG, "toggleSSCServiceState: threadCount -> " + threadCount);

                //执行启动服务操作
                for (int i = 0; i < threadCount; i++) {
                    //需要多少个线程就执行多少次启动操作
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MyApplication.appContext.startForegroundService(intent);
                    } else {
                        MyApplication.appContext.startService(intent);
                    }
                }
            } else {            //停止前台服务
                //执行停止服务操作
                MyApplication.appContext.stopService(intent);
                // TODO: 2022/4/19 利用异步执行器来提供一种异步执行，用于停止服务前阻塞等待给设备发送断连信号，
                //  具体调用那个停止方法根据Ctrl等状态自动判断（届时controller的stop方法要检查）
            }

            //设置标志
            sscServiceStateFlag = enabled;
        } catch (Throwable e) {
            Log.e(TAG, "toggleSSCServiceState: Toggle SSC Service state failed.", e);
            //post SSC服务状态变更事件，发SnackBar
            EventBus.getDefault().post(
                    new SSCServiceStateChangedEvent(sscServiceStateFlag, true, true));
        }
    }

    /**
     * 从在线列表和历史列表中检索是否已存在指定的设备连接信息，并视情况决定返回哪一个对象
     *
     * @return 如果已存在则视情况合并信息然后返回对应的对象，否则返回参数原对象
     */
    //该方法用于广播监听
    public ComputerInfo filterInfoFromCurDevices(final ComputerInfo info) {
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
            } else if (!config.ignoredSameHistory) {
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
     * 根据指定的新在线列表来设置旧列表中的元素下线
     */
    public void offlineAccordingToNew(@NonNull List<ComputerInfo> newOnlineList) {
        List<ComputerInfo> oldOnlineList = new ArrayList<>(onlineDeviceList);
        //筛选出已下线的设备连接
        oldOnlineList.removeAll(newOnlineList);
        for (int i = 0; i < oldOnlineList.size(); i++) {
            //标志其为已下线
            oldOnlineList.get(i).isOnline = false;
        }
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

    /**
     * 将指定的新历史设备列表与现有历史设备列表进行合并
     *
     * @return 合并后的新列表（两个源列表不会发生变化）
     */
    //此方法用于历史读取
    public List<ComputerInfo> mergeHistoryDevices(@NonNull final List<ComputerInfo> newList) {
        //移除没有出现在新列表中的旧列表元素（因为应当包含什么元素要以新列表为准）
        List<ComputerInfo> resList = new ArrayList<>(historyDeviceList);
        resList.retainAll(newList);
        //移除出现在旧列表中的新列表元素（因为已存在的元素以旧列表中的值为准）
        List<ComputerInfo> newListTemp = new ArrayList<>(newList);
        newListTemp.removeAll(resList);
        //标志新元素为历史设备
        for (int i = 0; i < newListTemp.size(); i++) {
            newListTemp.get(i).isSaved = true;
        }
        //合并新旧列表
        resList.addAll(newListTemp);
        return resList;
    }

    /**
     * 执行一次广播监听，监听将持续一段时间然后自动结束
     */
    public void observeBCOnce() {
        //如果正在监听则忽略本次调用
        if (!bcListener.isListening()) {
            if (historyWorker.isReading()) {
                //如果正在读取历史则直接取消本次调用（这种情况几乎不可能出现，因为历史读取过程耗时极短）
                //post刷新完成事件
                EventBus.getDefault().postSticky(new EndRefreshEvent(
                        getCurrentDevicesCount(true), true, false));
                Log.i(TAG, "observeBCOnce: This BC listening operation is cancelled!");
            } else {
                //post空广播事件，通知准备开始广播监听
                EventBus.getDefault().post(new BroadcastEvent(null));
                //开始监听
                bcListener.start();
            }
        }
    }

    /**
     * 执行一次历史读取
     */
    public void readHistoryOnce() {
        //如果正在读取则忽略本次调用
        if (!historyWorker.isReading()) {
            if (bcListener.isListening()) {
                //如果正在监听广播则停止它
                bcListener.stop(false);
                Log.i(TAG, "readHistoryOnce: The BC listening operation was stopped!");
            }
            //开始读取
            historyWorker.readStart();
        }
    }

    public void pubNotification(@StringRes int resID) {
        //发布SSC服务通知
        // TODO: 2022/4/22 发通知，通知ID：2，
        //  渠道ID：SSC_SERVICE_NOTIFY_CHANNEL，渠道名：SSC服务通知，标题：SSC服务提醒
        Log.d(TAG, "pubNotification: " + MyApplication.appContext.getString(resID));
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

    /**
     * SSC前台服务状态变更事件
     */
    @Subscribe(priority = 1)
    public void onSSCServiceStateChangedEvent(SSCServiceStateChangedEvent event) {
        //根据事件状态确定执行什么操作（变更Ctrl状态、重置服务状态标志、发通知、处理发送失败；发送连接握手信息？）
        Log.d(TAG, "onSSCServiceStateChangedEvent: " + event);

        if (!event.isSendFailed() && event.isAutoOperation()
                && (event.isHasError() || event.isRunningException())) {
            //设置变更后的Ctrl状态
            setCtrlON(event.getFinalState());
            //post控制状态变更事件
            EventBus.getDefault().post(new CtrlStateChangedEvent(event.getFinalState()));
        }

        if (!event.isSendFailed() && event.isAutoOperation() && !event.getFinalState()) {
            //重置服务状态标志
            sscServiceStateFlag = false;
        }

        if (!event.isSendFailed() && (event.isRunningException() ||
                (event.isAutoOperation() && !event.isHasError()))) {
            //发通知
            if (!event.getFinalState()) {
                pubNotification(event.isHasError() ?
                        R.string.tip_notify_ctrl_exp_off : R.string.tip_notify_ctrl_auto_off);
            }
        }

        if (event.isSendFailed()) {
            // TODO: 2022/4/22 处理发送失败事件

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
        //Toast "好像出了点问题…"
    }

    //测试方法，保存历史设备列表
    public void saveHistoryDevices(List<ComputerInfo> historyList) {
        historyWorker.write(historyList);
    }

}

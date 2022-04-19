package com.ssc.shakesocketcontroller.Transaction.controller;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ssc.shakesocketcontroller.Models.events.command.BaseCmdEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.AnswerEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.BroadcastEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.ConnectConfirmEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndBroadcastEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndReadHistoryEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.EndRefreshEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.LaunchConnectEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.UDPReceiveEvent;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Models.pojo.MsgPacket;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息适配器，负责处理UI与后台交互的全局的事件、指令、信号消息等
 */
public class MessageAdapter {
    private static final String TAG = "MessageAdapter";

    private final List<ComputerInfo> bcInfoList;        //广播监听接收的设备连接缓存列表
    private final Gson gson;                            //用于Json字符串转换

    private volatile boolean hadDiscardedBC = false;    //指示某轮广播监听是否发生过丢弃
    private volatile boolean stopped = true;            //启动状态


    public MessageAdapter() {
        bcInfoList = new ArrayList<>();
        gson = new GsonBuilder().serializeNulls().create();
    }

    /**
     * 获取适配器停止/开始状态
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * 开始消息适配处理
     */
    public void start() {
        if (!stopped) {
            return;
        }

        EventBus.getDefault().register(this);
        //设置状态
        stopped = false;
        Log.i(TAG, "start: Adapter started.");
    }

    /**
     * 停止消息适配处理
     */
    public void stop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        //重置状态
        stopped = true;
        //清空缓存列表
        bcInfoList.clear();
        Log.i(TAG, "stop: Adapter stopped.");
    }

    /**
     * 广播监听事件，即收到一个广播数据报
     */
    @Subscribe(priority = 3)
    public void onBroadcastEvent(BroadcastEvent event) {
        if (event.isInitEvent()) {
            //开始广播监听，清空旧列表，并重置状态
            bcInfoList.clear();
            hadDiscardedBC = false;
        } else {
            //解析event里的数据报，将其转换为一个可用的CPInfo对象
            // TODO: 2022/4/7 先用伪代码实现逻辑↓，反序列化json记得处理异常
            Log.d(TAG, "onBroadcastEvent: get one BC.");
            String json = StrUtil.byteToStr(event.getValidData());
            ComputerInfo bcInfo = gson.fromJson(json, ComputerInfo.class);
            if (bcInfo != null) {
                //设置IP信息
                bcInfo.address = event.getAddress();
                //如果设备信息不完整则直接忽略
                if (!stopped && bcInfo.isIntact()) {
                    if (bcInfoList.contains(bcInfo)) {
                        //如果本轮监听已接收过该设备的广播则直接更新IP即可
                        bcInfoList.get(bcInfoList.indexOf(bcInfo)).address = bcInfo.address;
                    } else {
                        //从在线列表或者历史列表中检索相对应的对象（如果有）
                        bcInfo = MyApplication.getController().filterInfoFromCurDevices(bcInfo);
                        //标志其为在线
                        bcInfo.isOnline = true;
                        //加入广播缓存列表
                        bcInfoList.add(bcInfo);
                    }
                }
            }
        }
    }

    /**
     * 广播监听结束事件
     */
    @Subscribe(priority = 2)
    public void onEndBroadcastEvent(EndBroadcastEvent event) {
        Log.d(TAG, "onEndBroadcastEvent: shouldSaveResult? -> " + event.shouldSaveResult());
        //如果已经Ctrl-ON，则不应再去更新在线列表
        if (event.shouldSaveResult() && !MyApplication.getController().isCtrlON()) {
            int oldListCount = MyApplication.getController().getCurrentDevicesCount(true);
            boolean isUpdated = !hadDiscardedBC && !stopped;    //根据丢弃状态判断是否需要执行替换操作
            if (isUpdated) {
                //本轮监听没有发生过丢弃，即本轮监听结果可靠，设置未收到广播的设备下线
                MyApplication.getController().offlineAccordingToNew(bcInfoList);
                //替换新的在线列表
                MyApplication.getController().setNewDevices(bcInfoList, true);
                Log.d(TAG, "onEndBroadcastEvent: Replaced list");
            }
            //post刷新完成事件
            EventBus.getDefault().postSticky(new EndRefreshEvent(oldListCount, true, isUpdated));
            Log.d(TAG, "onEndBroadcastEvent: after post EndRefreshEvent");
        } else {
            //丢弃已缓存的广播设备信息，并标志状态
            bcInfoList.clear();
            hadDiscardedBC = true;
        }
    }

    /**
     * 历史读取结束事件
     */
    @Subscribe(priority = 4)
    public void onEndReadHistoryEvent(EndReadHistoryEvent event) {
        Log.d(TAG, "onEndReadHistoryEvent: List Size -> " + event.getHistoryList().size());
        //如果已经Ctrl-ON，则不应再去更新历史列表
        if (!MyApplication.getController().isCtrlON()) {
            boolean isUpdated = false;
            int oldListCount = MyApplication.getController().getCurrentDevicesCount(false);

            //如果历史记录没有变化则无须执行替换操作
            if (oldListCount != event.getHistoryList().size() || !event.getHistoryList().equals(
                    MyApplication.getController().getCurrentDevices(false))) {
                //合并新旧历史记录
                List<ComputerInfo> newHistoryList = MyApplication.getController()
                        .mergeHistoryDevices(event.getHistoryList());
                //二次检查
                if (!MyApplication.getController().isCtrlON() && !stopped) {
                    //替换新的历史连接列表，并标志状态
                    MyApplication.getController().setNewDevices(newHistoryList, false);
                    isUpdated = true;
                    Log.d(TAG, "onEndReadHistoryEvent: Replaced list");
                }
            }

            //post刷新完成事件
            EventBus.getDefault().postSticky(new EndRefreshEvent(oldListCount, false, isUpdated));
            Log.d(TAG, "onEndReadHistoryEvent: after post EndRefreshEvent");
        }
    }

    /**
     * SSC监听事件，即收到一个UDP数据报
     */
    @Subscribe
    public void onUDPReceiveEvent(UDPReceiveEvent event) {
        Log.d(TAG, "onUDPReceiveEvent: get one Msg.");
        // TODO: 2022/4/19 判断数据包是否合法，如果合法就打包，然后处理执行
    }

    //伪方法
    private void analyzeDGPacket(BroadcastEvent event) {
        // TODO: 2022/4/1 所有的数据报包是否都应该使用统一的接口，像MsgPacket.Builder那样传入数据报，
        //  然后执行IP端口判断、Byte解析等等，根据数据里包含的类型信息创建对应的处理类对象，
        //  最后外部调用其统一的接口方法来实现多态。
        /*这是旧的广播监听数据分析代码，应该由单独的方法（或类）来解析数据报，最终广播里只要Json字符串
        try {
            MsgPacket packet = new MsgPacket.Builder(event.getData()).build();
            //check legality
            if (packet.checkTypeStr(MsgPacket.TYPE_IP)) {
                //analyze targetInfo
                ComputerInfo targetInfo = gson.fromJson(packet.getDataStr(), ComputerInfo.class);
                if (targetInfo.isIntact()) {
                    packet.setTargetInfo(targetInfo);
                    //save targetInfo
                    //targetMap.put(packet.getTargetInfo().getMacStr(), packet.getTargetInfo());
                    //send localInfo back
                    //MsgPacket bcBackPacket = new MsgPacket.Builder(MsgPacket.TYPE_IP)
                    //        .dataStr(gson.toJson(config.getLocalInfo())).build();
                    //bcBackPacket.setTargetInfo(packet.getTargetInfo());
                    //EventBus.getDefault().post(new SendUDPEvent(bcBackPacket));
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onBroadcastEvent: Broadcast analyze failed", e);
        }
        EventBus.getDefault().cancelEventDelivery(event);*/
    }

    @Subscribe
    public void onLaunchConnectEvent(LaunchConnectEvent event) {
        //if (targetMap.containsKey(event.getTargetMacStr())) {
        //    curTargetMacStr = event.getTargetMacStr();
        //    //start TCPListener to wait for connecting
        //    controller.StartTCPListener();
        //    //stop BroadcastListener
        //    controller.StopBroadcastListener();
        //    //send UDP localInfo packet to target
        //    MsgPacket packet = new MsgPacket.Builder(MsgPacket.TYPE_GUN)
        //            .dataStr(gson.toJson(config.getLocalInfo())).build();
        //    packet.setTargetInfo(targetMap.get(curTargetMacStr));
        //    EventBus.getDefault().post(new SendUDPEvent(packet));
        //}
    }

    @Subscribe
    public void onConnectConfirmEvent(ConnectConfirmEvent event) {
        //check legality
        //if (curTargetMacStr.equals(event.getTargetMacStr())) {
        //    //stop BroadcastListener
        //    controller.StopBroadcastListener();
        //    //stop TCPListener(OPT)
        //    controller.StopTCPListener();
        //    //start TCPHandler
        //    controller.StartTCPHandler(event.getClient());
        //    //notify UI that connect successfully connect
        //    EventBus.getDefault().post(new TCPConnectedEvent(targetMap.get(curTargetMacStr)));
        //} else {
        //    try {
        //        event.getClient().close();
        //        Log.i(TAG, "onConnectConfirmEvent: illegal connection close() success");
        //    } catch (IOException e) {
        //        Log.d(TAG, "onConnectConfirmEvent: illegal connection close() failed");
        //    }
        //}
    }

    @Subscribe
    public void onCmdEvent(BaseCmdEvent event) {
        try {
            MsgPacket cmdPacket = new MsgPacket.Builder(MsgPacket.TYPE_CMD)
                    .dataStr(gson.toJson(event)).build();
            //EventBus.getDefault().post(new SendTCPEvent(cmdPacket));
        } catch (Exception e) {
            Log.e(TAG, "onCmdEvent: " + event.getCmdType() + " cmd control failed"
                    , e);
            EventBus.getDefault().post(new AnswerEvent(event.getCmdType(), false));
        }
    }

    /* 收到设备发过来的信息（供参考的旧代码）
    @Subscribe
    public void onTCPReceiveEvent(TCPReceiveEvent event) {
        try {
            MsgPacket tcpPacket = new MsgPacket.Builder(event.getRecBuf()).build();
            //check msgType
            if (tcpPacket.checkTypeStr(MsgPacket.TYPE_ANS)) {
                EventBus.getDefault().post(gson.fromJson(tcpPacket.getDataStr(),
                        AnswerEvent.class));
            }
        } catch (Exception e) {
            Log.e(TAG, "onTCPReceiveEvent: TCPReceive analyze failed", e);
        }
    }*/

    /* 发送信息给设备（供参考的旧代码）
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
    }*/

}

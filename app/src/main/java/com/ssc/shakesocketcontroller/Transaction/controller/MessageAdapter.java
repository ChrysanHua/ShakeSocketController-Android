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
import com.ssc.shakesocketcontroller.Models.events.signal.SendTCPEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPConnectedEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPReceiveEvent;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Models.pojo.MsgPacket;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// TODO: 2022/3/24 MsgAdapter每收到一次BCEvent就应该缓存起来，这时应和历史列表对比，复用其元素，
//  最后收到EndBCEvent再整体处理，然后通知列表Adapter更新并停止刷新UI；√
//  MsgAdapter需要根据Ctrl状态判断是否处理数据（包括广播和历史）;
//  以及考虑中止后还是收到“处理”指令（即全空广播数据的处理）；
//  MsgAdapter在收到空广播之后、广播完成之前，
//  如果有历史读取完成事件发生，应当优先保证执行历史读取行为（事实上任何时候广播都应该等历史）；

/**
 * 消息适配器，负责处理UI与后台交互的全局的事件、指令、信号消息等
 */
public class MessageAdapter {
    private static final String TAG = "MessageAdapter";

    private final List<ComputerInfo> bcInfoList;        //广播监听接收的设备连接缓存列表
    private final Gson gson;                            //用于Json字符串转换

    private volatile boolean hadDiscardedBC = false;    //指示某轮广播监听是否发生过丢弃

    private HashMap<String, ComputerInfo> targetMap;
    private String curTargetMacStr;


    public MessageAdapter() {
        bcInfoList = new ArrayList<>();
        gson = new GsonBuilder().serializeNulls().create();

        targetMap = new HashMap<>();
    }

    public void start() {
        EventBus.getDefault().register(this);
        targetMap.clear();
        curTargetMacStr = null;
    }

    public void stop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
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
            //先用伪代码实现逻辑↓，反序列化json记得处理异常
            String json = StrUtil.byteToStr(event.getData());
            ComputerInfo bcInfo = gson.fromJson(json, ComputerInfo.class);
            if (bcInfo != null) {
                //设置IP信息
                bcInfo.address = event.getAddress();
                //如果设备信息不完整则直接忽略
                if (bcInfo.isIntact()) {
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
        //如果已经Ctrl-ON，则不应再去更新在线列表
        if (event.shouldSaveResult() && !MyApplication.getController().isCtrlON()) {
            int oldListCount = MyApplication.getController().getCurrentDevicesCount(true);
            if (!hadDiscardedBC) {
                //本轮监听没有发生过丢弃，替换新的在线列表
                MyApplication.getController().setNewDevices(bcInfoList, true);
            }
            //post刷新完成事件
            EventBus.getDefault().postSticky(new EndRefreshEvent(oldListCount, true, !hadDiscardedBC));
            Log.i(TAG, "onEndBroadcastEvent: after post EndRefreshEvent");
        } else {
            //丢弃已缓存的广播设备信息，并标志状态
            bcInfoList.clear();
            hadDiscardedBC = true;
        }
    }

    @Subscribe()
    public void onEndReadHistoryEvent(EndReadHistoryEvent event) {
        //要考虑读取历史与广播监听之间的冲突，以及Ctrl-ON的问题等等
    }

    //伪方法
    private void analyzeDGPacket(BroadcastEvent event) {
        // TODO: 2022/4/1 所有的数据报包是否都应该使用同一的接口，像MsgPacket.Builder那样传入数据报，
        //  然后执行IP判断、Byte解析等等，根据数据里包含的类型信息创建对应的处理类对象，
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
        if (targetMap.containsKey(event.getTargetMacStr())) {
            curTargetMacStr = event.getTargetMacStr();
            //start TCPListener to wait for connecting
            //controller.StartTCPListener();
            //stop BroadcastListener
            //controller.StopBroadcastListener();
            //send UDP localInfo packet to target
            //MsgPacket packet = new MsgPacket.Builder(MsgPacket.TYPE_GUN)
            //        .dataStr(gson.toJson(config.getLocalInfo())).build();
            //packet.setTargetInfo(targetMap.get(curTargetMacStr));
            //EventBus.getDefault().post(new SendUDPEvent(packet));
        }
    }

    @Subscribe
    public void onConnectConfirmEvent(ConnectConfirmEvent event) {
        //check legality
        if (curTargetMacStr.equals(event.getTargetMacStr())) {
            //stop BroadcastListener
            //controller.StopBroadcastListener();
            //stop TCPListener(OPT)
            //controller.StopTCPListener();
            //start TCPHandler
            //controller.StartTCPHandler(event.getClient());
            //notify UI that connect successfully connect
            EventBus.getDefault().post(new TCPConnectedEvent(targetMap.get(curTargetMacStr)));
        } else {
            try {
                event.getClient().close();
                Log.i(TAG, "onConnectConfirmEvent: illegal connection close() success");
            } catch (IOException e) {
                Log.d(TAG, "onConnectConfirmEvent: illegal connection close() failed");
            }
        }
    }

    @Subscribe
    public void onCmdEvent(BaseCmdEvent event) {
        try {
            MsgPacket cmdPacket = new MsgPacket.Builder(MsgPacket.TYPE_CMD)
                    .dataStr(gson.toJson(event)).build();
            EventBus.getDefault().post(new SendTCPEvent(cmdPacket));
        } catch (Exception e) {
            Log.e(TAG, "onCmdEvent: " + event.getCmdType() + " cmd control failed"
                    , e);
            EventBus.getDefault().post(new AnswerEvent(event.getCmdType(), false));
        }
    }

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
    }

}

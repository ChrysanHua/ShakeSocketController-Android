package com.ssc.shakesocketcontroller.Transaction.controller;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ssc.shakesocketcontroller.Models.events.command.BaseCmdEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.AnswerEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.BroadcastEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.ConnectConfirmEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.LaunchConnectEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.SendTCPEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.SendUDPEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPConnectedEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPReceiveEvent;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Models.pojo.Configuration;
import com.ssc.shakesocketcontroller.Models.pojo.MsgPacket;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class MessageAdapter {
    private static final String TAG = "MessageAdapter";

    private TransactionController controller;
    private Configuration config;

    private HashMap<String, ComputerInfo> targetMap;
    private String curTargetMacStr;
    private Gson gson;

    public Collection<ComputerInfo> getTargetCollection() {
        return targetMap.values();
    }

    public MessageAdapter(TransactionController controller) {
        this.controller = controller;
        this.config = this.controller.getCurrentConfig();
        targetMap = new HashMap<>();
        gson = new GsonBuilder().serializeNulls().create();
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

    @Subscribe(priority = 1)
    public void onBroadcastEvent(BroadcastEvent event) {
        try {
            MsgPacket packet = new MsgPacket.Builder(event.getData()).build();
            //check legality
            if (packet.checkTypeStr(MsgPacket.TYPE_IP)) {
                //analyze targetInfo
                ComputerInfo targetInfo = gson.fromJson(packet.getDataStr(), ComputerInfo.class);
                if (targetInfo.isIntact()) {
                    packet.setTargetInfo(targetInfo);
                    //save targetInfo
                    targetMap.put(packet.getTargetInfo().getMacStr(), packet.getTargetInfo());
                    //send localInfo back
                    MsgPacket bcBackPacket = new MsgPacket.Builder(MsgPacket.TYPE_IP)
                            .dataStr(gson.toJson(config.getLocalInfo())).build();
                    bcBackPacket.setTargetInfo(packet.getTargetInfo());
                    EventBus.getDefault().post(new SendUDPEvent(bcBackPacket));
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onBroadcastEvent: Broadcast analyze failed", e);
        }
        EventBus.getDefault().cancelEventDelivery(event);
    }

    @Subscribe
    public void onLaunchConnectEvent(LaunchConnectEvent event) {
        if (targetMap.containsKey(event.getTargetMacStr())) {
            curTargetMacStr = event.getTargetMacStr();
            //start TCPListener to wait for connecting
            controller.StartTCPListener();
            //stop BroadcastListener
            controller.StopBroadcastListener();
            //send UDP localInfo packet to target
            MsgPacket packet = new MsgPacket.Builder(MsgPacket.TYPE_GUN)
                    .dataStr(gson.toJson(config.getLocalInfo())).build();
            packet.setTargetInfo(targetMap.get(curTargetMacStr));
            EventBus.getDefault().post(new SendUDPEvent(packet));
        }
    }

    @Subscribe
    public void onConnectConfirmEvent(ConnectConfirmEvent event) {
        //check legality
        if (curTargetMacStr.equals(event.getTargetMacStr())) {
            //stop BroadcastListener
            controller.StopBroadcastListener();
            //stop TCPListener(OPT)
            //controller.StopTCPListener();
            //start TCPHandler
            controller.StartTCPHandler(event.getClient());
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

package com.ssc.shakesocketcontroller.Transaction.controller;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context appContext;       //应用程序上下文

    @Override
    public void onCreate() {
        super.onCreate();
        //获取全局应用程序上下文
        appContext = getApplicationContext();
        //启动Controller
        getController().start();
    }

    public static TransactionController getController() {
        //返回Controller实例对象
        return TransactionController.getInstance();
    }

    public static List<ComputerInfo> createTestCPInfoList(int count, boolean online) {
        //测试用，生成临时测试数据
        InetAddress address = DeviceUtil.getLocalAddress();
        List<ComputerInfo> infos = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            ComputerInfo cp = new ComputerInfo(address, "CP" + i, "NickName" + i + online);
            cp.isOnline = online || i % 2 == 0;
            cp.isSaved = (i <= 2);
            //cp.isConnected = (i == 2 || i == 11);
            cp.isChecked = (i <= 2);
            infos.add(cp);
        }
        return infos;
    }
}

package com.ssc.shakesocketcontroller.Transaction.controller;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MyApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context appContext;       //应用程序上下文

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        //设置Controller静态变量并在此获取？
    }

    public static TransactionController getController() {
        //返回Controller实例对象
        return TransactionController.getInstance();
    }
}

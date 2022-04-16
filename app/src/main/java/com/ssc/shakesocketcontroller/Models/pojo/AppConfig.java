package com.ssc.shakesocketcontroller.Models.pojo;

import android.content.Context;
import android.util.Log;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

// TODO: 2022/4/4 将APP配置项替换到各个对应地方，去除原先的常量
// TODO: 2022/4/13 【本机设备名】考虑如何初始化并替换MainActivity中对应代码

/**
 * APP设置数据类
 */
public class AppConfig {
    private static final String TAG = "AppConfig";
    public static final String CONFIG_FILE_NAME = "SSC_CONFIG";

    public final String uuid;                   //本设备唯一标识符

    public String nickName;                     //昵称（默认与设备名相同，可自定义修改）
    public int bcPort;                          //广播监听端口
    public int bcListenDuration;                //单次广播监听时长
    public int bcMaxReceiveBufSize;             //接收广播数据包的Buf大小
    public int msgPort;                         //消息监听端口
    public int msgResponseTimeout;              //消息响应超时
    public int msgMaxReceiveBufSize;            //接收消息数据包的Buf大小

    public boolean ignoredSameHistory;          //广播监听时忽略相似的历史连接
    public boolean autoSwipeRefresh;            //进入列表时自动下拉刷新

    public final transient String deviceName;   //本机设备名


    /**
     * <h3>注意：不要直接使用此构造函数，使用load()方法加载配置！</h3>
     * 初始化带默认值的APP设置，每次从Json反序列化时都会被调用
     */
    public AppConfig() {
        //在此设定默认值
        this.uuid = DeviceUtil.generateUUID();
        this.deviceName = DeviceUtil.getDeviceName();
        this.nickName = this.deviceName;
        this.bcPort = 19019;
        this.bcListenDuration = 3000;
        this.bcMaxReceiveBufSize = 4096;
        this.msgPort = 10019;
        this.msgResponseTimeout = 3000;
        this.msgMaxReceiveBufSize = 4096;

        this.ignoredSameHistory = false;
        this.autoSwipeRefresh = true;
    }


    /**
     * 从本地加载设置
     *
     * @return 返回从本地加载的APP设置，本地没有则返回默认设置（并自动保存该默认设置到本地）
     * @throws IOException 方法内只捕捉了FileNotFoundException异常
     */
    public static AppConfig load() throws IOException {
        AppConfig config = null;

        try (final FileInputStream fileInputStream
                     = MyApplication.appContext.openFileInput(CONFIG_FILE_NAME);
             final BufferedReader reader = new BufferedReader(
                     new InputStreamReader(fileInputStream, StrUtil.DEFAULT_CHARSET))) {

            //从文件中读取
            final StringBuilder stringBuilder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = reader.readLine();
            }
            //反序列化Json字符串
            final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
            config = gson.fromJson(stringBuilder.toString(), AppConfig.class);
        } catch (FileNotFoundException ignored) {
            //本地没有设置文件，直接忽略该异常
        }

        if (config == null) {
            //本地文件读取不到APP设置数据，初始化一个默认设置
            config = new AppConfig();
            //将默认设置保存到本地
            save(config);
            Log.i(TAG, "load: Init AppConfig.");
        }

        return config;
    }

    /**
     * 保存设置到本地
     *
     * @param config 要保存的APP设置，传入null等效于清空原文件
     * @throws IOException 方法内没有捕捉任何异常
     */
    public static void save(final AppConfig config) throws IOException {
        try (final FileOutputStream fileOutputStream = MyApplication.appContext
                .openFileOutput(CONFIG_FILE_NAME, Context.MODE_PRIVATE)) {
            //序列化为Json字符串
            final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
            final String json = (config == null ? "" : gson.toJson(config));
            //保存到文件中
            fileOutputStream.write(StrUtil.strToByte(json));
            fileOutputStream.flush();
        }
    }

    // TODO: 2022/3/31 重构equals,hashCode,toString方法

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", uuid)
                .add("nickName", nickName)
                .add("bcPort", bcPort)
                .add("bcListenDuration", bcListenDuration)
                .add("bcMaxReceiveBufSize", bcMaxReceiveBufSize)
                .add("deviceName", deviceName)
                .toString();
    }
}

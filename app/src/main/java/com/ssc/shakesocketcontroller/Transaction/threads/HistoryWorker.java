package com.ssc.shakesocketcontroller.Transaction.threads;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ssc.shakesocketcontroller.Models.events.signal.EndReadHistoryEvent;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.util.AsyncExecutor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 历史连接信息记录器
 */
public class HistoryWorker {
    private static final String TAG = "HistoryWorker";
    public static final String HISTORY_FILE_NAME = "HISTORY_DEVICES";
    public static final String HISTORY_DATA_KEY = "DEVICES_DATA";

    private final AsyncExecutor executor;               //异步执行器
    private final SharedPreferences preferences;        //用于存储与读取历史连接设备信息
    private final Gson gson;                            //用于Json字符串转换
    private final Type listType;                        //用于Json字符串反序列化

    private volatile boolean reading = false;           //读取状态


    public HistoryWorker(Executor executor) {
        this.executor = AsyncExecutor.builder().threadPool(executor).build();
        this.preferences = MyApplication.appContext
                .getSharedPreferences(HISTORY_FILE_NAME, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
        this.listType = new TypeToken<List<ComputerInfo>>() {
        }.getType();
    }

    /**
     * 获取读取状态
     */
    public boolean isReading() {
        return reading;
    }

    /**
     * 执行读取历史操作，历史数据为空时必然返回空列表(non-null)
     */
    public List<ComputerInfo> read() {
        List<ComputerInfo> infoList = null;

        try {
            //从XML中读取Json字符串
            final String json = preferences.getString(HISTORY_DATA_KEY, "");
            //反序列化Json字符串
            infoList = gson.fromJson(json, listType);
        } catch (Exception e) {
            Log.e(TAG, "read: failed", e);
        }

        if (infoList == null) {
            //读取不到历史，初始化一个空列表
            infoList = new ArrayList<>();
        }

        return infoList;
    }

    /**
     * 开始异步执行历史数据读取操作，一旦执行将无法显式取消读取行为
     */
    public void readStart() {
        if (reading) {
            return;
        }

        //设置状态，确保reading状态表示完整的读取行为（因为实际的read()操作耗时极短）
        reading = true;
        //开始异步执行读取
        executor.execute(() -> {
            //读取历史后，post历史读取完成事件
            EventBus.getDefault().post(new EndReadHistoryEvent(read()));
            //最后再重置状态
            reading = false;
        });
    }

    /**
     * 执行保存历史操作，传入null等效于清空历史数据
     *
     * @param list 要进行覆盖保存的新历史数据列表
     */
    public void write(List<ComputerInfo> list) {
        // TODO: 2022/4/10 保存历史的时候要确保设置isSave属性
        //序列化为Json字符串
        final String json = (list == null ? "" : gson.toJson(list));
        //将Json字符串写入XML
        preferences.edit().putString(HISTORY_DATA_KEY, json).apply();
    }

}

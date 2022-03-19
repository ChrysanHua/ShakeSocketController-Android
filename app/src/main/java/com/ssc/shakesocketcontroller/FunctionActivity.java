package com.ssc.shakesocketcontroller;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;

import com.ssc.shakesocketcontroller.Models.pojo.FunctionStr;

public class FunctionActivity extends AppCompatActivity {

    private FunctionStr[] funs = {
            new FunctionStr("远程控制", "远程控制电脑执行关机、锁屏", PowerControlActivity.class),
            new FunctionStr("PPT助手", "控制电脑PPT的幻灯片播放", PPTControlActivity.class),
            new FunctionStr("音乐控制", "控制电脑播放音乐", MusicControlActivity.class),
            new FunctionStr("音量调节", "调节电脑音量大小", VolumeControlActivity.class),
            new FunctionStr("文件传输", "获取电脑文件", VolumeControlActivity.class)};

    private FunctionAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function);

        TextView emptyTV = findViewById(R.id.tips_text_view);
        emptyTV.setVisibility(View.GONE);

        //RecyclerView recyclerView = findViewById(R.id.recycler_view);
        //recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //adapters = new FunctionAdapter(funs);
        //recyclerView.setAdapter(adapters);

    }

}

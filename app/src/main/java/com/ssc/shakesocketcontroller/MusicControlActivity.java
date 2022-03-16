package com.ssc.shakesocketcontroller;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ssc.shakesocketcontroller.Transaction.events.command.BaseCmdEvent;
import com.ssc.shakesocketcontroller.Transaction.events.command.MusicCmd;
import com.ssc.shakesocketcontroller.Transaction.events.signal.AnswerEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MusicControlActivity extends AppCompatActivity {

    Button playBtn;
    Button nextBtn;
    Button frontBtn;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnswerEvent(AnswerEvent event) {
        if (event.checkAnsType(BaseCmdEvent.CMD_MUSIC)) {
            if (event.getAnsValue().equals("next")) {
                nextBtn.setClickable(event.isAnsStatus());
            } else if (event.getAnsValue().equals("front")) {
                frontBtn.setClickable(event.isAnsStatus());
            } else if (event.getAnsValue().equals("play")) {
                playBtn.setClickable(event.isAnsStatus());
            }
            Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_control);

        playBtn = findViewById(R.id.music_play_btn);
        nextBtn = findViewById(R.id.music_next_btn);
        frontBtn = findViewById(R.id.music_front_btn);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new MusicCmd("next"));
            }
        });

        frontBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new MusicCmd("front"));
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new MusicCmd("play"));
            }
        });

    }

    @Override
    protected void onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

}

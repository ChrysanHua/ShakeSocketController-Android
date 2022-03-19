package com.ssc.shakesocketcontroller;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ssc.shakesocketcontroller.Models.events.command.BaseCmdEvent;
import com.ssc.shakesocketcontroller.Models.events.command.PPTCmd;
import com.ssc.shakesocketcontroller.Models.events.signal.AnswerEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PPTControlActivity extends AppCompatActivity {

    Button nextBtn;
    Button frontBtn;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnswerEvent(AnswerEvent event) {
        if (event.checkAnsType(BaseCmdEvent.CMD_PPT)) {
            if (event.getAnsValue().equals("down")) {
                nextBtn.setClickable(event.isAnsStatus());
            } else if (event.getAnsValue().equals("up")) {
                frontBtn.setClickable(event.isAnsStatus());
            }
            Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pptcontrol);

        nextBtn = findViewById(R.id.ppt_next_btn);
        frontBtn = findViewById(R.id.ppt_front_btn);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new PPTCmd("down"));
            }
        });

        frontBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new PPTCmd("up"));
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

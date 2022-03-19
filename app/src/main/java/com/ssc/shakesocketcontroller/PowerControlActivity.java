package com.ssc.shakesocketcontroller;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ssc.shakesocketcontroller.Models.events.command.BaseCmdEvent;
import com.ssc.shakesocketcontroller.Models.events.command.ScreenLockCmd;
import com.ssc.shakesocketcontroller.Models.events.command.ShutdownCmd;
import com.ssc.shakesocketcontroller.Models.events.signal.AnswerEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PowerControlActivity extends AppCompatActivity {

    Button shutdownBtn;
    Button screenLockBtn;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnswerEvent(AnswerEvent event) {
        if (event.checkAnsType(BaseCmdEvent.CMD_SHUTDOWN)) {
            shutdownBtn.setClickable(event.isAnsStatus());
        } else if (event.checkAnsType(BaseCmdEvent.CMD_SCREENLOCK)) {
            screenLockBtn.setClickable(event.isAnsStatus());
        }
        Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_control);

        shutdownBtn = findViewById(R.id.shutdown_button);
        screenLockBtn = findViewById(R.id.screen_lock_button);

        shutdownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new ShutdownCmd(15));
            }
        });

        screenLockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new ScreenLockCmd());
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

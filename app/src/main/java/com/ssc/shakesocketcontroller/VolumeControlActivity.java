package com.ssc.shakesocketcontroller;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ssc.shakesocketcontroller.Models.events.command.BaseCmdEvent;
import com.ssc.shakesocketcontroller.Models.events.command.VolumeCmd;
import com.ssc.shakesocketcontroller.Models.events.signal.AnswerEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class VolumeControlActivity extends AppCompatActivity {

    TextView valueTextView;
    Button enterBtn;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnswerEvent(AnswerEvent event) {
        if (event.checkAnsType(BaseCmdEvent.CMD_VOLUME)) {
            valueTextView.setText(event.getAnsValue());
            enterBtn.setClickable(event.isAnsStatus());
            Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volume_control);

        valueTextView = findViewById(R.id.volume_value_edit_view);
        enterBtn = findViewById(R.id.volume_enter_btn);

        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EventBus.getDefault().isRegistered(v.getContext())) {
                    EventBus.getDefault().register(v.getContext());
                }
                v.setClickable(false);
                EventBus.getDefault().post(new VolumeCmd(valueTextView.getText().toString()));
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

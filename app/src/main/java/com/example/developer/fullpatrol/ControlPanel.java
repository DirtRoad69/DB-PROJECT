package com.example.developer.fullpatrol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ControlPanel extends LockableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_control_panel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.Lock();
        this.setLockBackButton(false);
        Interpol.getInstance().setOutOfMainActivity(true);
    }

    @Override
    protected void onPause() {
        this.Unlock();
        Interpol.getInstance().setOutOfMainActivity(false);
        super.onPause();
    }
}

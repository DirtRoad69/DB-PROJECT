package com.example.developer.fullpatrol;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;

public class TestSignal extends Service {
    public TestSignal() {
    }
    private FirebaseManager firebaseManager;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        firebaseManager = FirebaseManager.getInstance();
        Toast.makeText(this, "time", Toast.LENGTH_SHORT).show();
        firebaseManager.sendEventType(MainActivity.eventsCollection, "test signal", 19, "");
        Log.i("WSX", "onStartCommand: test signal");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw null;
    }
}

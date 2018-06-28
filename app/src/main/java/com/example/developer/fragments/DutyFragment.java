package com.example.developer.fragments;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.fullpatrol.AlarmReceiver;
import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.Interpol;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;
import com.example.developer.fullpatrol.SiteDataManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DutyFragment extends KioskFragment implements View.OnClickListener {
    private static final String TAG = "dfc";

    public static final String TITLE = "Duty Fragment";

    private SiteDataManager siteDataManager;
    private BroadcastReceiver updateUIReciver;

    public static String DutyStatus = "ON DUTY";
    private TextView txtCountDown, txtDutyStatus;
    private CountDownTimer mCountDownTimer; //added

    public DutyFragment(){
        this.siteDataManager = SiteDataManager.getInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmReceiver.ACTION_REST_ALARM);
        filter.addAction(AlarmReceiver.ACTION_REST_COUNTER);
        filter.addAction(AlarmReceiver.ACTION_UNLOCK);
        filter.addAction(AlarmReceiver.ACTION_START_PATROL);

        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //UI update here
                if (intent != null){
                    switch(intent.getAction()){
                        case AlarmReceiver.ACTION_REST_ALARM:
                            if(MainActivity.alarmMgr != null){
                                MainActivity.alarmMgr.cancel(MainActivity.alarmIntent);
                            }
                            //MainActivity.this.setFirstTimeAlarm();
                            Log.i("RFC", "Received");
                            break;
                        case AlarmReceiver.ACTION_REST_COUNTER:
                            startTimer(intent.getDoubleExtra("Duration", 0));
                            break;
                        case AlarmReceiver.ACTION_UNLOCK:
                            //MainActivity.this.Unlock();
                            break;
                        case AlarmReceiver.ACTION_START_PATROL:
                            DutyFragment.this.startFragment(PatrolFragment.TITLE);
                            break;
                    }

                }
            }
        };
        getActivity().registerReceiver(this.updateUIReciver, filter);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.activity_main,container, false);

        Button btnPanic = parentView.findViewById(R.id.btn_panic);
        this.txtCountDown = parentView.findViewById(R.id.ttv_time);
        this.txtDutyStatus = parentView.findViewById(R.id.ttv_duty_status);

        btnPanic.setOnClickListener(this);

        setupTimer();
        return parentView;
    }


    private void setupTimer(){
        int startHour, startMin, intervalTimer;
        startHour = siteDataManager.getInt("startHour");
        startMin = siteDataManager.getInt("startMin");
        intervalTimer = siteDataManager.getLong("intervalTimer").intValue();



        // Set the alarm to start at 8:30 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, startMin);
        calendar.set(Calendar.SECOND, 0);


        offDuty(DutyStatus.equals("ON DUTY"), calendar);




        long nxtTime =  Interpol.getNextTimePatrol(calendar.getTimeInMillis(), 1000 * 60 * intervalTimer);
        if(nxtTime < 0){
            long  remainingTime = calendar.getTimeInMillis() - System.currentTimeMillis();
            Interpol.getInstance().setNextTime(calendar.getTimeInMillis());
            startTimer(remainingTime / 60000.0);
            DutyStatus = "OFF DUTY";
        }else{
            Interpol.getInstance().setNextTime(nxtTime);
            long diff = nxtTime - System.currentTimeMillis();
            startTimer((diff) / 60000.0);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_panic:
                break;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    private void offDuty(Boolean isOnDuty, Calendar c){
        if(!isOnDuty)
            c.add(Calendar.DATE, 1);
    }


    private void startTimer(double duration) {

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

        Log.i("RFC", duration + "=dur");
        mCountDownTimer = new CountDownTimer((long) (duration * 60 * 1000), 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountDownText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                Intent intent = new Intent("com.example.intent.restart");
                intent.putExtra(AlarmReceiver.ACTION_CALLER, AlarmReceiver.CALLER_TIMER);
                
                getContext().sendBroadcast(intent);
                Toast.makeText(getActivity(), "TIMER ACTIVITY 1", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }



    private void updateCountDownText(long millisUntilFinished) {
        int hours = (int) (((millisUntilFinished / 1000.0) / 60) / 60);
        int minutes = (int) ((millisUntilFinished / 1000.0) / 60) % 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d",hours, minutes, seconds);

        txtCountDown.setText(timeLeftFormatted);
        txtDutyStatus.setText(DutyStatus);

        txtDutyStatus.setBackgroundResource( DutyStatus.equals("OFF DUTY") ? R.color.colorPrimary :  R.color.colorAccent);

    }


    @Override
    protected void proccessCommand(String command) {

    }

    @Override
    public String getTitle() {
        return TITLE;
    }


}

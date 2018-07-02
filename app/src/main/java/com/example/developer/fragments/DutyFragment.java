package com.example.developer.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.fullpatrol.AlarmReceiver;
import com.example.developer.fullpatrol.ControlPanel;
import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.InputCollector;
import com.example.developer.fullpatrol.Interpol;
import com.example.developer.fullpatrol.LinkDeviceActivity;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;
import com.example.developer.fullpatrol.SiteDataManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DutyFragment extends KioskFragment implements View.OnClickListener {
    public static final String TAG = "DutyFragment"
            , TITLE = "Duty Fragment"
            , EXTRA_DURATION = "Duration";
    public static final int REQUEST_CONTROL = 10
            , REQUEST_EXIT = 11;

    public static String DutyStatus = "ON DUTY";

    private FirebaseManager firebaseManager;
    private SiteDataManager siteDataManager;


    private BroadcastReceiver updateUIReceiver;


    private TextView txtCountDown, txtDutyStatus;
    private CountDownTimer mCountDownTimer;
    private Toolbar mTopToolbar;

    private int panicCount;
    private Toast panicToast;


    public DutyFragment(){
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmReceiver.ACTION_REST_ALARM);
        filter.addAction(AlarmReceiver.ACTION_REST_COUNTER);
        filter.addAction(AlarmReceiver.ACTION_START_PATROL);

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //UI update here
                if (intent != null){
                    switch(intent.getAction()){
                        case AlarmReceiver.ACTION_REST_ALARM:
                            if(MainActivity.alarmMgr != null){
                                MainActivity.alarmMgr.cancel(MainActivity.alarmIntent);
                            }
                            Log.i("RFC", "Received");
                            break;
                        case AlarmReceiver.ACTION_REST_COUNTER:
                            startTimer(intent.getDoubleExtra(EXTRA_DURATION, 0));
                            break;
                        case AlarmReceiver.ACTION_START_PATROL:
                            DutyFragment.this.startFragment(new PatrolFragment());
                            break;
                    }

                }
            }
        };
        getActivity().registerReceiver(this.updateUIReceiver, filter);
        this.panicCount = MainActivity.MAX_PANIC_TAPS;
        this.panicToast = Toast.makeText(this.getContext(), String.format("press panic %d more times", panicCount), Toast.LENGTH_SHORT);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.activity_main,container, false);

        Button btnPanic = parentView.findViewById(R.id.btn_panic);
        this.txtCountDown = parentView.findViewById(R.id.ttv_time);
        this.txtDutyStatus = parentView.findViewById(R.id.ttv_duty_status);

        btnPanic.setOnClickListener(this);

        this.mTopToolbar =  parentView.findViewById(R.id.my_toolbar);
        ((MainActivity)this.getActivity()).setSupportActionBar(mTopToolbar);

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
                onPanic();
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

                if(millisUntilFinished<=3000){

                    if(!MainActivity.wakeActive){
                        MainActivity.wakeActive = true;
                        ((MainActivity)getActivity()).wakeUpDevice();
                    }

                    Log.i(TAG, "onTick: is on");
                }
            }

            @Override
            public void onFinish() {
                Intent intent = new Intent("com.example.intent.restart");
                intent.putExtra(AlarmReceiver.ACTION_CALLER, AlarmReceiver.CALLER_TIMER);

                getContext().sendBroadcast(intent);
                Toast.makeText(getActivity(), "TIMER ACTIVITY 1", Toast.LENGTH_SHORT).show();
                //release wakelock
                if(MainActivity.wakeActive){
                    MainActivity.wakeActive = false;
                    ((MainActivity)getActivity()).setDeviceSleep();
                }
            }
        }.start();
    }

    public void onPanic(){
        if(panicCount == MainActivity.MAX_PANIC_TAPS){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    panicCount = MainActivity.MAX_PANIC_TAPS;
                }
            }, MainActivity.PANIC_REST_DURATION);
        }

        panicCount--;

        if(panicCount == 0){
            String siteId = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getString(LinkDeviceActivity.PREF_LINKED_SITE, null);
            if(siteId != null){
                this.firebaseManager.sendEventType(MainActivity.eventsCollection, MainActivity.PANIC_EVENT_DESCRIPTION, MainActivity.PANIC_EVENT_ID, siteId);
                panicCount = MainActivity.MAX_PANIC_TAPS;
                panicToast.setText("Panic Message Sent.");
                panicToast.show();
            }
        }else{
            panicToast.setText(String.format("press panic %d more times", panicCount));
            panicToast.show();
        }
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
    protected void onFragmentResult(int requestCode, int resultCode, Bundle extraData) {
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == MainActivity.REQUEST_CONTROL) {
                boolean loggedIn = extraData.getBoolean(AuthenticationFragment.EXTRA_LOGGED_IN, false);
                if(loggedIn){
//                    this.Unlock();
//                    Intent controlPanelIntent = new Intent(this, ControlPanel.class);
//                    this.startActivity(controlPanelIntent);
                }
            } else if (requestCode == REQUEST_EXIT) {
                boolean loggedIn = extraData.getBoolean(AuthenticationFragment.EXTRA_LOGGED_IN, false);
                if(loggedIn){
                    mCountDownTimer.cancel();
                    ((MainActivity)this.getActivity()).exitKiosk();
                }
            }
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_control_panel:
                sendIntent(AuthenticationFragment.ACCESS_TYPE_CONTROL, REQUEST_CONTROL);
                break;
            case R.id.action_exit_kiosk:
                sendIntent(AuthenticationFragment.ACCESS_TYPE_ADMIN, REQUEST_EXIT);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sendIntent(String accessType, int code){

        Bundle extraData = new Bundle();
        extraData.putString(AuthenticationFragment.ACCESS_TYPE, accessType);
        AuthenticationFragment authenticationFragment = new AuthenticationFragment();
        authenticationFragment.setArguments(extraData);

        this.startFragmentForResult(authenticationFragment, code);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }


}

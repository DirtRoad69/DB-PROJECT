package com.example.developer.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.fragments.controlPanelFragments.fragments.ControlPanelFragment;
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


    public static BroadcastReceiver updateUIReceiver;


    private TextView txtCountDown, txtDutyStatus;
    private CountDownTimer mCountDownTimer;
    private Toolbar mTopToolbar;

    private int panicCount;
    private int supervisorReqCount;
    private Toast panicToast;
    boolean isPlayed;

    public DutyFragment(){
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        ((MainActivity)getActivity()).wakeUpDevice();
        IntentFilter filter = new IntentFilter();

        filter.addAction(AlarmReceiver.ACTION_REST_ALARM);
        filter.addAction(AlarmReceiver.ACTION_REST_COUNTER);
        filter.addAction(AlarmReceiver.ACTION_START_PATROL);
        filter.addAction(AlarmReceiver.ACTION_END_TIME);
        filter.addAction(AlarmReceiver.ACTION_NOW_OFFDUTY);

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
                            Log.i("WSX", "onReceive: ACTION_REST_COUNTER "+Math.abs(intent.getDoubleExtra(EXTRA_DURATION, 0)));
                            startTimer(Math.abs(intent.getDoubleExtra(EXTRA_DURATION, 0)));
                            break;
                        case AlarmReceiver.ACTION_NOW_OFFDUTY:
                            Log.i("WSX", "onReceive: ACTION_NOW_OFFDUTY "+Math.abs(intent.getDoubleExtra(EXTRA_DURATION, 0)));
                            startTimer(Math.abs(intent.getDoubleExtra(EXTRA_DURATION, 0)));
                        case AlarmReceiver.ACTION_START_PATROL:
                            DutyFragment.this.startFragment(new PatrolFragment());
                            break;
                        case AlarmReceiver.ACTION_END_TIME:
                            Log.i("WSX", "onReceive: REFRESH");
                            if(DutyStatus.equals("OFF DUTY")){
                                Log.i("WSX", "onReceive: REFRESH removeSelf");

                                ((MainActivity) getActivity()).resetIfOffDuty();



                            }

                            break;

                    }

                }
            }
        };
        getActivity().registerReceiver(this.updateUIReceiver, filter);
        this.panicCount = MainActivity.MAX_PANIC_TAPS;
        supervisorReqCount = MainActivity.MAX_PANIC_TAPS;
        this.panicToast = Toast.makeText(this.getContext(), String.format("press panic %d more times", panicCount), Toast.LENGTH_SHORT);

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.activity_main,container, false);


        Button btnSupervisorRequest = parentView.findViewById(R.id.btn_supervisor_request);
        Button btnPanic = parentView.findViewById(R.id.btn_panic);
        this.txtCountDown = parentView.findViewById(R.id.ttv_time);
        this.txtDutyStatus = parentView.findViewById(R.id.ttv_duty_status);

        ((MainActivity)getActivity()).setScreenSleep();

        btnPanic.setOnClickListener(this);
        btnSupervisorRequest.setOnClickListener(this);

        this.mTopToolbar =  parentView.findViewById(R.id.my_toolbar);
        ((MainActivity)this.getActivity()).setSupportActionBar(mTopToolbar);

        setupTimer();
        return parentView;
    }


    private void setupTimer(){
        int startHour, startMin, intervalTimer;
        startHour = siteDataManager.getInt("startHour");
        startMin = siteDataManager.getInt("startMin");


        // Set the alarm to start at 8:30 a.m.
        Log.i("WSX", "setupTimer: set starttime");
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(Calendar.getInstance().getTime());
        startDate.setTimeInMillis(System.currentTimeMillis());
        startDate.set(Calendar.HOUR_OF_DAY, startHour);
        startDate.set(Calendar.MINUTE, startMin);
        startDate.set(Calendar.SECOND, 0);



        //added

        int endHour =  siteDataManager.getInt("endHour") , endMin =siteDataManager.getInt("endMin");
        Calendar endTime = Calendar.getInstance();
        endTime.setTimeInMillis(System.currentTimeMillis());
        endTime.set(endTime.HOUR_OF_DAY, endHour);
        endTime.set(endTime.MINUTE, endMin);
        endTime.set(endTime.SECOND, 0);





        Calendar nowTime = Calendar.getInstance();


        Log.i("WSX", "setupTimer: 1");


        detectDutyStatus(startDate, nowTime, endTime, firebaseManager, siteDataManager);
        Log.i("WSX", "setupTimer: 2 ");

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_panic:
                onPanic();
                break;
            case R.id.btn_supervisor_request:
                onSupervisorRequest();
                break;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    private void offDuty(Boolean isOnDuty, Calendar c){
        if(!isOnDuty){
            Log.i("WSX", "offDuty: ");
            c.add(Calendar.DATE, 1);
        }

    }

    public boolean isPM(int hour){
        boolean isPM = false;
        if(hour >= 12){
            isPM = true;
        }
        return isPM;
    }

    public void compareDates(Calendar startDate,Calendar nowDate, Calendar endDate, FirebaseManager firebaseManager, SiteDataManager siteDataManager){

        if((endDate.getTimeInMillis() >= nowDate.getTimeInMillis())
                && (nowDate.getTimeInMillis() >= startDate.getTimeInMillis())){





            long nxtTime =  Interpol.getNextTimePatrol(startDate.getTimeInMillis(), 1000*60* siteDataManager.getLong("intervalTimer").intValue());
            Date resultdate = new Date(nxtTime);
            Log.i("RFC", resultdate.toString());
            if(nxtTime < 0){
                //MainActivity.dutyStatus = "OFF DUTY";
                startDate.add(startDate.DATE, 1);
                long  remainingTime = startDate.getTimeInMillis() - System.currentTimeMillis();
                Interpol.getInstance().setNextTime(startDate.getTimeInMillis());

                startTimer(remainingTime / 60000.0);
                DutyStatus = "OFF DUTY";

            }else{
                if(DutyFragment.DutyStatus.equals("OFF DUTY")){
                    firebaseManager.sendEventType("events",  "ON DUTY" , 2, "site");
                }
                DutyFragment.DutyStatus = "ON DUTY";
                Interpol.getInstance().setNextTime(nxtTime);
                long diff = nxtTime - System.currentTimeMillis();
                startTimer((diff) / 60000.0);
            }


            Log.i("WSX", "compareDates: ON DUTY DUTY FRAG");
            Log.i("WSX", "compareDates: UPDATE UI DUTY FRAG");
            Log.i("WSX", "compareDates: SET NEXT TIME. . .DUTY FRAG\n" +nowDate.getTime() +" \nendTime "+ endDate.getTime() +"\nrStartTime "+ startDate.getTime() );
        }else{

            Log.i("WSX", "compareDates: OFF DUTY ELSE StartDate: "+ startDate.getTime() +" NOW: "+Calendar.getInstance().getTime());

//            if(startDate.getTimeInMillis() >= Calendar.getInstance().getTimeInMillis())
//                //startDate.add(startDate.DATE, 1);
            Log.i("WSX", "compareDates: OFF DUTY");
            Log.i("WSX", "compareDates: OFF DUTY SENT. . .DUTY FRAG");
            Log.i("WSX", "compareDates: SET NEXT TIME. . .DUTY FRAG\n" +nowDate.getTime() +" \nendTime "+ endDate.getTime() +"\nStartTime "+ startDate.getTime() );

            long nxtTime = startDate.getTimeInMillis() - System.currentTimeMillis();
            if(nxtTime < 0){
                startDate.add(startDate.DATE, 1);
                DutyStatus = "OFF DUTY";
                Log.i("WSX", "compareDates: OFF DUTY LESS THAN ZERO StartDate: "+ startDate.getTime());
                long  remainingTime = startDate.getTimeInMillis() - System.currentTimeMillis();
                Interpol.getInstance().setNextTime(startDate.getTimeInMillis());
                startTimer(remainingTime / 60000.0);
            }else {
                Log.i("WSX", "compareDates: OFF DUTY ELSE StartDate: "+ startDate.getTime());
                DutyFragment.DutyStatus = "OFF DUTY";
                firebaseManager.sendEventType("events", DutyFragment.DutyStatus, 10, "site");
                long remainingTime = startDate.getTimeInMillis() - System.currentTimeMillis();
                Interpol.getInstance().setNextTime(startDate.getTimeInMillis());
                startTimer(remainingTime / 60000.0);

            }

            Log.i("WSX", "OFF DUTY on DUTY FRAGMENT DUTY FRAG");


        }

    }
    public void detectDutyStatus(Calendar startDate,Calendar nowDate, Calendar endDate, FirebaseManager firebaseManager,SiteDataManager siteDataManager){
        //check if on different day
        if(isPM(startDate.get(startDate.HOUR_OF_DAY)) && !isPM(endDate.get(endDate.HOUR_OF_DAY))){
            //skip day

            endDate.add(endDate.DATE, 1);

            Log.i("WSX", "compareDates: Day Skipped DUTY FRAG");
            compareDates(startDate, nowDate, endDate, firebaseManager, siteDataManager);

        }else{
            Log.i("WSX", "compareDates: Day not skipped we are on the current day. . .DUTY FRAG");
            compareDates(startDate, nowDate, endDate, firebaseManager, siteDataManager);
        }

    }


    private void startTimer(double duration) {

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

        Log.i("RFC", duration + "=dur");
        isPlayed = false;
        mCountDownTimer = new CountDownTimer((long) (duration * 60 * 1000), 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if(millisUntilFinished < 5000){

                    if(!MainActivity.wakeActive)
                    {
                        ((MainActivity)getActivity()).wakeUpDevice(1);
                        MainActivity.wakeActive = true;
                    }

                    Log.i(TAG, "onTick: is on");
                }

                //((MainActivity)getActivity()).wakeUpDevice();
                updateCountDownText(millisUntilFinished);
                if(millisUntilFinished <= 30000){
                    ((MainActivity)getActivity()).wakeUpScreen();
                    try {
//                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                        Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
//                        r.play();
                        if(millisUntilFinished <= 15000)
                        {
                            if(!isPlayed){
                                ((MainActivity)getActivity()).textToSpeech(getString(R.string.start_patrol), getContext());
                                isPlayed = true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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

                //((MainActivity)getActivity()).setScreenSleep();

            }
        }.start();
    }


    public void onSupervisorRequest(){
        if(supervisorReqCount == MainActivity.MAX_PANIC_TAPS){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    supervisorReqCount = MainActivity.MAX_PANIC_TAPS;
                }
            }, MainActivity.PANIC_REST_DURATION);
        }

        supervisorReqCount--;

        if(supervisorReqCount == 0){
            String siteId = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getString(LinkDeviceActivity.PREF_LINKED_SITE, null);
            if(siteId != null){
                firebaseManager.sendEventType(MainActivity.eventsCollection, "Supervisor Request", 9, "");
                supervisorReqCount = MainActivity.MAX_PANIC_TAPS;
                panicToast.setText("Request Message Sent.");
                panicToast.show();
            }
        }else{
            panicToast.setText(String.format("press request %d more times", supervisorReqCount));
            panicToast.show();
        }
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
        //added try catch
        try{
            if(resultCode == Activity.RESULT_OK){
                if(requestCode == MainActivity.REQUEST_CONTROL) {
                    boolean loggedIn = extraData.getBoolean(AuthenticationFragment.EXTRA_LOGGED_IN, false);
                    if(loggedIn){

                        DutyFragment.this.startFragment(new ControlPanelFragment());
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
        }catch (Exception e){}
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
            case R.id.action_report_voice:
                //start voice fragment
                DutyFragment.this.startFragment(new TakeAudioFragment());
                break;
            case R.id.action_report_image:
                //start image fragment
                DutyFragment.this.startFragment(new NewLocalCamera());
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

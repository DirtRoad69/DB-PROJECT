package com.example.developer.fullpatrol;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.fragments.AuthenticationFragment;
import com.example.developer.fragments.ControlPanelFragment;
import com.example.developer.fragments.DutyFragment;
import com.example.developer.fragments.KioskFragment;
import com.example.developer.fragments.PatrolFragment;
import com.example.developer.fragments.ScanFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends LockableActivity {

    public static final String TAG = "PatrolFragment"
            ,eventsCollection = "patrolDataDummy"
            ,PANIC_EVENT_DESCRIPTION = "Panic";

    //setup data
    public static final int REQUEST_CONTROL = 10, REQUEST_EXIT = 11, MAX_PANIC_TAPS = 5
            , PANIC_REST_DURATION = 5000
            , PANIC_EVENT_ID = 8;
    public static final String SITES_COLLECTION = "site";

    //firebase stuff
    FirebaseManager firebaseManager;
    SiteDataManager siteDataManager;

    private KioskFragment[] kioskFragments = {new DutyFragment(), new PatrolFragment(), new ScanFragment(), new AuthenticationFragment(), new ControlPanelFragment()};


    private Toolbar mTopToolbar;
    //timer values


    //alarm
    public static AlarmManager alarmMgr;
    public static PendingIntent alarmIntent;
    public static boolean isKioskActive = false;


    public static String _dutyStatus = "ON DUTY";
    private FragmentManager fragmentManager;

    PowerManager.WakeLock wakelock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.Lock();


        Toast.makeText(this, "On MainActivity Created. . .", Toast.LENGTH_SHORT).show();

        setContentView(R.layout.main_layout);
        fragmentManager = this.getSupportFragmentManager();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        String siteId = this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).getString(LinkDeviceActivity.PREF_LINKED_SITE, null);
        if(siteId != null && !isKioskActive){
            Toast.makeText(this, "On MainActivity Created. . .IsActive", Toast.LENGTH_SHORT).show();
            this.startKioskSession(SITES_COLLECTION, siteId);
        }else{
            //error
            Log.i(TAG, "onCreate: null site id");
        }
        isKioskActive = true;
    }

    public void wakeUpDevice() {

        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "wake up");
        wakelock.acquire();
    }
    public void setDeviceSleep() {
        wakelock.release();
    }

    private void startKioskSession(String collection, String siteId) {
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();
        this.firebaseManager.init(collection, siteId);




        this.firebaseManager.getPatrolData(new FirebaseManager.DataCallback() {
            @Override
            public void onDataReceived(Map<String, Object> data) {
                MainActivity.this.siteDataManager.setData(data);
                MainActivity.this.addFragment(new DutyFragment());
                MainActivity.this.setFirstTimeAlarm();
            }

            @Override
            public void onDataReceived(List<Map<String, Object>> data) {

            }
        });
    }





    @Override
    public boolean onSupportNavigateUp() {
        this.notifyFragment();
        return super.onSupportNavigateUp();
    }

    public void sendIntent(String accessType, int code){
        Bundle extraData = new Bundle();
        extraData.putString(AuthenticationFragment.ACCESS_TYPE, accessType);
        AuthenticationFragment authenticationFragment = new AuthenticationFragment();
        authenticationFragment.setArguments(extraData);

        this.addFragment(authenticationFragment);
    }

    private void offDuty(Boolean isOnDuty, Calendar c){
        if(!isOnDuty)
            c.add(Calendar.DATE, 1);
    }

    public void exitKiosk(){

        if(this.alarmMgr != null && this.alarmIntent != null)
            alarmMgr.cancel(alarmIntent);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.ACTION_CALLER, AlarmReceiver.CALLER_ALARM);
        PendingIntent repeatingAlarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        this.alarmMgr.cancel(repeatingAlarmIntent);
        isKioskActive = false;
        Log.i("RFC", "Exiting Kiosk");
        this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).edit().putBoolean(HomeActivity.SHARE_KIOSK_ENABLED, false).apply();
        this.Unlock();
        this.finish();
    }

    private void setFirstTimeAlarm() {
        alarmMgr = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        if(alarmIntent != null){
            alarmMgr.cancel(alarmIntent);
        }

        Intent intent = new Intent(this, CountDownStarter.class);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

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


        offDuty(_dutyStatus.equals("ON DUTY"), calendar);




        long nxtTime =  Interpol.getNextTimePatrol(calendar.getTimeInMillis(), 1000 * 60 * intervalTimer);
        if(nxtTime < 0){
            long  remainingTime = calendar.getTimeInMillis() - System.currentTimeMillis();
            Interpol.getInstance().setNextTime(calendar.getTimeInMillis());
            //startTimer(remainingTime / 60000.0);
            alarmMgr.set(AlarmManager.RTC_WAKEUP, remainingTime, alarmIntent);
            _dutyStatus = "OFF DUTY";
        }else{
            Interpol.getInstance().setNextTime(nxtTime);

            // setRepeating() lets you specify a precise custom interval--in this case,
            // 20 minutes.
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
            Date resultdate = new Date(nxtTime);
            Date rd = new Date(calendar.getTimeInMillis());

            alarmMgr.set(AlarmManager.RTC_WAKEUP, nxtTime, alarmIntent);
            long diff = nxtTime - System.currentTimeMillis();
            Log.i("RFC",  diff + "|" + calendar.getTimeInMillis() +  "|" + System.currentTimeMillis() + "|" + nxtTime + "|" + resultdate + "|" + rd);
            //startTimer((diff) / 60000.0);
            Log.i("RFC",  "|" + resultdate.toString());
        }

    }





    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        this.Lock();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == REQUEST_CONTROL) {
                boolean loggedIn = data.getBooleanExtra(InputCollector.EXTRA_LOGGED_IN, false);
                if(loggedIn){
                    this.Unlock();
                    Intent controlPanelIntent = new Intent(this, ControlPanel.class);
                    this.startActivity(controlPanelIntent);
                }
            } else if (requestCode == REQUEST_EXIT) {
                boolean loggedIn = data.getBooleanExtra(InputCollector.EXTRA_LOGGED_IN, false);
                if(loggedIn){
                    this.exitKiosk();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onInterceptBackPress() {
        this.notifyFragment();
        super.onInterceptBackPress();
    }

    private void notifyFragment() {
        int indexTopFragment = fragmentManager.getBackStackEntryCount() - 1;
        if(indexTopFragment >= 0){
            String title = fragmentManager.getBackStackEntryAt(indexTopFragment).getName();
            Fragment fragment = fragmentManager.findFragmentByTag(title);
            if(fragment != null){
                ((KioskFragment)fragment).onBackPressed();
            }
        }
    }

    public void addFragment(KioskFragment fragment){
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.frame_container, fragment, fragment.getTitle());
        transaction.addToBackStack(fragment.getTitle());
        transaction.commit();
    }

    public void removeFragment(String title) {
        fragmentManager.popBackStack(title, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: called");
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void removeFragment(String title, int resultCode, Bundle extraData){
        int secondLast = fragmentManager.getBackStackEntryCount() - 2;
        if(secondLast >= 0){
            String secondTitle = fragmentManager.getBackStackEntryAt(secondLast).getName();
            fragmentManager.popBackStack(title, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Fragment nextFragment = fragmentManager.findFragmentByTag(secondTitle);
            if(nextFragment != null)
                ((KioskFragment)nextFragment).onResult(resultCode, extraData);
        }
    }

}

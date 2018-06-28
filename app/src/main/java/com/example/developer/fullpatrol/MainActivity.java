package com.example.developer.fullpatrol;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

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

    private static final String TAG = "PatrolFragment";

    //setup data
    private static final int REQUEST_CONTROL = 10, REQUEST_EXIT = 11;
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

    public static String _dutyStatus = "ON DUTY";
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.Lock();

        setContentView(R.layout.main_layout);
        fragmentManager = this.getSupportFragmentManager();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        String siteId = this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).getString(LinkDeviceActivity.PREF_LINKED_SITE, null);
        if(siteId != null){
            this.startKioskSession(SITES_COLLECTION, siteId);
        }else{
            //error
            Log.i(TAG, "onCreate: null site id");
        }
    }



    private void startKioskSession(String collection, String siteId) {
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();
        this.firebaseManager.init(collection, siteId);

        this.mTopToolbar =  findViewById(R.id.my_toolbar);
        setSupportActionBar(mTopToolbar);


        this.firebaseManager.getPatrolData(new FirebaseManager.DataCallback() {
            @Override
            public void onDataReceived(Map<String, Object> data) {
                MainActivity.this.siteDataManager.setData(data);
                MainActivity.this.startFragment(DutyFragment.TITLE);
                MainActivity.this.setFirstTimeAlarm();
            }

            @Override
            public void onDataReceived(List<Map<String, Object>> data) {

            }
        });
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int code = -1;
        String accessType = null;
        switch (item.getItemId()){
            case R.id.action_control_panel:
                code = REQUEST_CONTROL;
                accessType = InputCollector.ACCESS_TYPE_CONTROL;
                sendIntent(accessType, code);
                break;
            case R.id.action_exit_kiosk:
                code = REQUEST_EXIT;
                accessType = InputCollector.ACCESS_TYPE_ADMIN;
                sendIntent(accessType, code);

                break;
        }


        return super.onOptionsItemSelected(item);
    }

    public void sendIntent(String accessType, int code){


//        Intent inputIntent = new Intent(this, InputCollector.class);
//        inputIntent.putExtra(InputCollector.ACCESS_TYPE, accessType);
//        this.startActivityForResult(inputIntent, code);
//

        startFragment(AuthenticationFragment.TITLE);

    }

    private void offDuty(Boolean isOnDuty, Calendar c){
        if(!isOnDuty)
            c.add(Calendar.DATE, 1);
    }

    private void exitKiosk(){
//        if(this.mCountDownTimer != null)
//            this.mCountDownTimer.cancel();
//
//        if(this.alarmMgr != null && this.alarmIntent != null)
//            alarmMgr.cancel(alarmIntent);
//
//        Intent intent = new Intent(context, AlarmReceiver.class);
//        intent.putExtra(AlarmReceiver.ACTION_CALLER, AlarmReceiver.CALLER_ALARM);
//        PendingIntent repeatingAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
//        this.alarmMgr.cancel(repeatingAlarmIntent);
//
//        Log.i("RFC", "Exiting Kiosk");
//        this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).edit().putBoolean(HomeActivity.SHARE_KIOSK_ENABLED, false).apply();
//        Interpol.getInstance().setOutOfMainActivity(false);
//        this.Unlock();
//        this.finish();
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

//        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
//                1000*60*patrolTime
//                , alarmIntent);

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

    public void startFragment(String fragmentId) {
        int fragmentIndex = this.getFragmentIndex(fragmentId);
        if(fragmentIndex >= 0){
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.frag_main, kioskFragments[fragmentIndex]);
            kioskFragments[fragmentIndex].requestCode = -1;
            fragmentTransaction.addToBackStack(fragmentId);
            fragmentTransaction.commit();
        }else{
            Log.i(TAG, "startFragment: couldn't find fragment:" + fragmentId);
        }
    }

    public void startFragment(String fragmentId, int requestCode) {
        int fragmentIndex = this.getFragmentIndex(fragmentId);
        if(fragmentIndex >= 0){
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.frag_main, kioskFragments[fragmentIndex]);
            kioskFragments[fragmentIndex].requestCode = requestCode;
            fragmentTransaction.addToBackStack(fragmentId);
            fragmentTransaction.commit();
        }else{
            Log.i(TAG, "startFragment: couldn't find fragment:" + fragmentId);
        }
    }

    public void returnToFragment(String fragmentId, int requestCode, int resultCode, Intent intent) {
        int fragmentIndex = this.getFragmentIndex(fragmentId);
        if (fragmentIndex >= 0) {
            fragmentManager.popBackStack();
            kioskFragments[fragmentIndex].onFragmentReturn(requestCode, resultCode, intent);
        } else {
            Log.i(TAG, "startFragment: couldn't find fragment:" + fragmentId);
        }
    }

    public void returnToFragment(){
        fragmentManager.popBackStack();
    }

    public int getFragmentIndex(String title){
        for(int pos = 0; pos < this.kioskFragments.length; pos++){
            Log.i(TAG, "getFragmentIndex: " + kioskFragments[pos].getTitle());
            if(kioskFragments[pos].getTitle().equals(title)){
                return pos;
            }
        }
        return -1;
    }


    public void closeFragment(String title) {
        int index= getFragmentIndex(title);
        if(index >= 0){
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(kioskFragments[index]);
            transaction.commit();
        }
    }
}

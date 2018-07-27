package com.example.developer.fullpatrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.developer.ServerSide.AppleProjectDB;
import com.example.developer.ServerSide.FirebaseClientManager;
import com.example.developer.fragments.AuthenticationFragment;
import com.example.developer.fragments.controlPanelFragments.fragments.ControlPanelFragment;
import com.example.developer.fragments.DutyFragment;
import com.example.developer.fragments.KioskFragment;
import com.example.developer.fragments.PatrolFragment;
import com.example.developer.fragments.ScanFragment;
import com.example.developer.services.subroutines.ApplicationMiddleware;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MainActivity extends LockableActivity {

    public static final String TAG = "PatrolFragment"
            ,eventsCollection = "events"
            ,PANIC_EVENT_DESCRIPTION = "Panic";

    //setup data
    public static final int REQUEST_CONTROL = 10, REQUEST_EXIT = 11, MAX_PANIC_TAPS = 5
            , PANIC_REST_DURATION = 5000
            , PANIC_EVENT_ID = 8;
    public static final String SITES_COLLECTION = "site";
    public static String siteId;

    //firebase stuff
    FirebaseManager firebaseManager;
    SiteDataManager siteDataManager;

    //wakelock stuff
    PowerManager.WakeLock wakelock;

    private KioskFragment[] kioskFragments = {new DutyFragment(), new PatrolFragment(), new ScanFragment(), new AuthenticationFragment(), new ControlPanelFragment()};


    private Toolbar mTopToolbar;
    //timer values


    //alarm
    public static AlarmManager alarmMgr;
    public static PendingIntent alarmIntent;

    public static String _dutyStatus = "ON DUTY";
    public static String deviceId;


    private FragmentManager fragmentManager;
    public static boolean wakeActive;
    private PowerManager.WakeLock wakelock2;



    //just added

    public static final int  SERVER = 124, LOCAL_DB = 123;

    private String DATABASE_NAME = "AppleProject";

    private SQLiteDatabase appleDB;
    public static AppleProjectDB appleProjectDBServer;

    ApplicationMiddleware middleware;
    FirebaseClientManager firebaseClientManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("ZAQ@", "onCreate: cresr");
        this.Lock();
        Log.i("ZAQ@", "onCreate: cresr222");
        deviceId = this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).getString(LinkDeviceActivity.PREF_UID, null);
        wakeActive = false;
        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake up");
        wakelock.setReferenceCounted(false);

        wakelock2 = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "wake up");
        wakelock2.setReferenceCounted(false);


        setContentView(R.layout.main_layout);

        Toast.makeText(this, "On MainActivity Created. . .", Toast.LENGTH_SHORT).show();
        Log.i("ZAQ@", "onCreate: \"On MainActivity Created. . .\"");

        fragmentManager = this.getSupportFragmentManager();
        //this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        siteId = this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).getString(LinkDeviceActivity.PREF_LINKED_SITE, null);

        //added



        appleDB = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);

        appleProjectDBServer = new AppleProjectDB(appleDB);
        appleProjectDBServer.createSitesTable();
        appleProjectDBServer.createPointsTable();

        middleware = new ApplicationMiddleware(appleProjectDBServer);
        firebaseClientManager = FirebaseClientManager.getFirebaseClientManagerInstance();
        firebaseClientManager.init(SITES_COLLECTION, MainActivity.siteId);
        Log.i("ZAQ@", "onCreate: initialization completed");




        this.firebaseClientManager.getPatrolDataInSync();





        ContentValues contentValues = new ContentValues();


        //MainActivity.this.addFragment(new TimePickerFragment());


        //end added



        if(siteId != null){
            this.startKioskSession(SITES_COLLECTION, siteId);
        }else{
            //error
            Log.i(TAG, "onCreate: null site id");
        }
    }





    public void startKioskSession(String collection, String siteId) {
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();
        this.firebaseManager.init(collection, siteId);
        Log.i("ZAQ@", "startKioskSession: initialization starting");

        this.firebaseManager.getPatrolDataLocally(new FirebaseManager.DataCallback() {
            @Override
            public void onDataUpdated(Map<String, Object> data) {

            }

            @Override
            public void onDataReceived(Map<String, Object> data) {
                MainActivity.this.siteDataManager.setData(data);
                MainActivity.this.addFragment(new DutyFragment());
                MainActivity.this.setFirstTimeAlarm();
                Log.i("ZAQ@", "startKioskSession: initialization complete");
            }

            @Override
            public void onDataReceived(List<Map<String, Object>> data) {

            }
        });

//        //to do
//        this.firebaseManager.getPatrolDataInSync(new FirebaseManager.DataCallback() {
//
//            @Override
//            public void onDataUpdated(Map<String, Object> data) {
//                MainActivity.this.siteDataManager.setData(data);
//            }
//            @Override
//            public void onDataReceived(Map<String, Object> data) {
//
//            }
//
//            @Override
//            public void onDataReceived(List<Map<String, Object>> data) {
//
//            }
//        });
    }


    public void wakeUpScreen() {

        if(!wakelock2.isHeld()){
            wakelock2.acquire();
        }
    }

    public void setScreenSleep() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (wakelock2.isHeld())
            wakelock2.release();

    }
    public void wakeUpDevice() {

        if(!wakelock.isHeld()){
            wakelock.acquire();
        }
    }

    public void setDeviceSleep() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (wakelock.isHeld())
            wakelock.release();

    }

    @Override
    public boolean onSupportNavigateUp() {
        this.notifyFragment();
        return super.onSupportNavigateUp();
    }

    public void sendIntent(String accessType, int code){


//        Intent inputIntent = new Intent(this, InputCollector.class);
//        inputIntent.putExtra(InputCollector.ACCESS_TYPE, accessType);
//        this.startActivityForResult(inputIntent, code);
//
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

        Log.i("ZAQ@", "Exiting Kiosk");
        this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).edit().putBoolean(HomeActivity.SHARE_KIOSK_ENABLED, false).apply();
        this.Unlock();
        this.finish();
    }
    public void pauseKiosk(){

        if(this.alarmMgr != null && this.alarmIntent != null)
            alarmMgr.cancel(alarmIntent);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.ACTION_CALLER, AlarmReceiver.CALLER_ALARM);
        PendingIntent repeatingAlarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        this.alarmMgr.cancel(repeatingAlarmIntent);

        Log.i("ZAQ@", "pausing Kiosk");
        //this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE).edit().putBoolean(HomeActivity.SHARE_KIOSK_ENABLED, false).apply();


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
            transaction.commitAllowingStateLoss();

    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //
    }

    public void removeFragment(String title) {
        fragmentManager.popBackStack(title, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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


    //added


    public void removePointFragmentLearningMode(String title, int resultCode, Bundle extraData){

        Fragment nextFragment = ControlPanelFragment.getDurationInfoFragment();
        if(ControlPanelFragment.getPointInfoFragment() != null){
            ((KioskFragment)nextFragment).onResult(resultCode, extraData);
            Log.i("ZAQ!", "removePointFragmentLearningMode: " + nextFragment);

        }

        int secondLast = fragmentManager.getBackStackEntryCount() - 2;
        if(secondLast >= 0){
            fragmentManager.popBackStack(title, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

    }
    public static AppleProjectDB getAppleProjectDBServer() {
        return appleProjectDBServer;
    }


    public void removeFragmentLearningMode(String title, int resultCode, Bundle extraData){
        Fragment nextFragment = ControlPanelFragment.getPointInfoFragment();
        if(ControlPanelFragment.getPointInfoFragment() != null){
            ((KioskFragment)nextFragment).onResult(resultCode, extraData);
            Log.i("ZAQ!", "removeFragmentLearningMode: " + nextFragment);
        }

        int secondLast = fragmentManager.getBackStackEntryCount() - 2;
        if(secondLast >= 0){
            fragmentManager.popBackStack(title, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }



    public static class MyAdapter extends FragmentStatePagerAdapter {

        private final List<Fragment> mFragmnetList = new ArrayList<>();
        private final List<String> mFragmnetTitleList = new ArrayList<>();

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title){
            this.mFragmnetList.add(fragment);
            this.mFragmnetTitleList.add(title);

        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return super.instantiateItem(container, position);
        }

        @Override
        public int getCount() {
            return mFragmnetList.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmnetList.get(position);
        }
    }
}

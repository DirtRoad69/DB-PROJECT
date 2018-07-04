package com.example.developer.fullpatrol;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.fragments.DutyFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver{
    public static final String ACTION_KILL_ALARM = "broadcast.kill.Alarm";
    public static final String ACTION_REST_COUNTER = "broadcast.reset.counter";
    public static final String ACTION_REST_ALARM = "broadcast.reset.alarm";
    public static final String ACTION_START_PATROL = "broadcast.start.patrol";
    public static final String ACTION_UNLOCK = "broadcast.reset.unlock";
    public static final String ACTION_CALLER = "CALLER", CALLER_ALARM = "alarm", CALLER_TIMER = "timer";
    public static final String ACTION_END_TIME = "end time";
    //timer values

    Interpol interpol;
    public static long ReceiveTime = 0;


    @Override
    public void onReceive(final Context context, Intent intent){
        Log.i("RFC", "" + intent.getStringExtra(ACTION_CALLER));

        final FirebaseManager firebaseManager = FirebaseManager.getInstance();
        final SiteDataManager siteDataManager = SiteDataManager.getInstance();
        this.interpol = Interpol.getInstance();





            if(!interpol.execute(System.currentTimeMillis()))
                return;

            if(interpol.isExecuting())
                return;

            interpol.setExecuting(true);

        this.ReceiveTime = System.currentTimeMillis();

        firebaseManager.getPatrolData(new FirebaseManager.DataCallback() {
            @Override
            public void onDataUpdated(Map<String, Object> data) {

            }

            @Override
            public void onDataReceived(Map<String, Object> data) {
                siteDataManager.compareAndUpdate(data, new SiteDataManager.CompareCallback() {
                    @Override
                    public void onCompareFinished() {
                        String  pointCol = "site/"+ context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).getString(LinkDeviceActivity.PREF_LINKED_SITE, null) +"/patrolPoints";
                        Log.i("RFV", pointCol);
                        firebaseManager.getPatrolPoints(pointCol, new FirebaseManager.DataCallback() {
                            @Override
                            public void onDataUpdated(Map<String, Object> data) {

                            }

                            @Override
                            public void onDataReceived(Map<String, Object> empty) {




                                Calendar calendar = Calendar.getInstance();
                                calendar.setTimeInMillis(System.currentTimeMillis());
                                calendar.set(Calendar.HOUR_OF_DAY, siteDataManager.getInt("startHour"));
                                calendar.set(Calendar.MINUTE, siteDataManager.getInt("startMin"));
                                calendar.set(Calendar.SECOND, 0);
                                Calendar calendar2 = Calendar.getInstance();
                                int hour = calendar2.getTime().getHours(), min = calendar2.getTime().getMinutes();
                                int endHour =  siteDataManager.getInt("endHour") , endMin =siteDataManager.getInt("endMin");

                                List<PatrolPoint> points = (List<PatrolPoint>)siteDataManager.get("patrolPoints");
                                String startPoint = siteDataManager.get("startEndPoint").toString();
                                for(int pos = 0 ; pos < points.size(); pos++){
                                    if(points.get(pos).pointId.contains(startPoint)){
                                        points.get(pos).isStarting = true;
                                        break;
                                    }
                                }

                                if(hour >= endHour && min >= endMin){
                                    //Session Over
                                    DutyFragment.DutyStatus = "OFF DUTY";
                                    firebaseManager.sendEventType("events",  DutyFragment.DutyStatus , 10, "site");
                                    resetAlarm(context);
                                    calendar.add(Calendar.DATE, 1);
                                    interpol.setNextTime(calendar.getTimeInMillis());
                                    Log.i("RFC", "Ignore 2");
                                }else{


                                    long nxtTime =  Interpol.getNextTimePatrol(calendar.getTimeInMillis(), 1000*60* siteDataManager.getLong("intervalTimer").intValue());
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                                    Date resultdate = new Date(nxtTime);
                                    Log.i("RFC", resultdate.toString());

                                    if(nxtTime < 0){
                                        //MainActivity.dutyStatus = "OFF DUTY";
                                        long  remainingTime = calendar.getTimeInMillis() - System.currentTimeMillis();
                                        interpol.setNextTime(calendar.getTimeInMillis());
                                        updateTime(context, (remainingTime / 60000.0));
                                        Log.i("RFC", "Ignore");

                                    }else{
                                        if(DutyFragment.DutyStatus.equals("OFF DUTY")){
                                            firebaseManager.sendEventType("events",  "ON DUTY" , 2, "site");
                                        }
                                        DutyFragment.DutyStatus = "ON DUTY";
                                        interpol.setNextTime(nxtTime);
                                        updateTime(context, (nxtTime - System.currentTimeMillis()) / 60000.0);
                                        Log.i("RFC", "Execute");
                                        //unlockMain(context);
                                        if(interpol.started())
                                            return;
                                        interpol.setStarted(true);
                                        startPatrol(context);
                                    }


//                        if(execute){
//
//
//
//                        }else{
//                            Log.i("RFC", "Ignore");
//                            if(intent.getStringExtra(ACTION_CALLER).equals(CALLER_TIMER)){
//                                Intent resetCounter = new Intent();
//                                resetCounter.setAction(ACTION_REST_COUNTER);
//                                context.sendBroadcast(resetCounter);
//                            }
//                        }



                                }
                                interpol.setStarted(false);
                                interpol.setExecuting(false);
                            }

                            @Override
                            public void onDataReceived(List<Map<String, Object>> data) {

                            }
                        });

                    }

                    @Override
                    public void valueChanged(String key) {
                        Log.i("RFC", "value changed:" + key);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(System.currentTimeMillis());
                        calendar.set(Calendar.HOUR_OF_DAY, siteDataManager.getInt("startHour"));
                        calendar.set(Calendar.MINUTE, siteDataManager.getInt("startMin"));
                        calendar.set(Calendar.SECOND, 0);
                        switch (key){
                            case "startPatrolTime":

                                long nxtTime =  Interpol.getNextTimePatrol(calendar.getTimeInMillis(), 1000*60* siteDataManager.getLong("intervalTimer").intValue());
                                if(nxtTime < 0){
                                    DutyFragment.DutyStatus = "OFF DUTY";
                                    firebaseManager.sendEventType("events",  DutyFragment.DutyStatus , 10, "site");
                                    resetAlarm(context);
                                }
                                break;
                        }
                    }
                });
            }

            @Override
            public void onDataReceived(List<Map<String, Object>> data) {

            }
        });





        //timer





//        Calendar calendar2 = Calendar.getInstance();
//        int hour = calendar2.getTime().getHours(), min = calendar2.getTime().getMinutes();
//        int endHour =  MainActivity.endHour , endMin = MainActivity.endMin;
//        if(hour >= endHour && min >= endMin){
//            //Session Over
//            Intent alarmKiller = new Intent();
//            alarmKiller.setAction(ACTION_KILL_ALARM);
//            context.sendBroadcast(alarmKiller);
//        }else{
//            //startPatrol(context);
//            Log.i("RFC", "Act new");
//            Intent resetCounter = new Intent();
//            resetCounter.setAction(ACTION_REST_COUNTER);
//            context.sendBroadcast(resetCounter);
//        }


    }

    private void unlockMain(Context context) {
        Intent unlockMain = new Intent();
        unlockMain.setAction(ACTION_UNLOCK);
        context.sendBroadcast(unlockMain);
    }

    private void resetAlarm(Context context) {
        Intent resetAlarm = new Intent();
        resetAlarm.setAction(ACTION_REST_ALARM);
        context.sendBroadcast(resetAlarm);
    }



    private void updateTime(Context context, double duration){
        Intent resetCounter = new Intent();
        resetCounter.setAction(ACTION_REST_COUNTER);
        resetCounter.putExtra("Duration", duration);
        context.sendBroadcast(resetCounter);
    }

    public void startPatrol(Context context){



        Intent i = new Intent(ACTION_START_PATROL);
        context.sendBroadcast(i);
    }
    //timer code


    private long getNextTimePatrol(long pastTime, long interval){
        long current = System.currentTimeMillis();
        long diff = current - pastTime;
        long  div = diff % interval;
        long nextTime = (current - div) + interval;
        return  nextTime;
    }
}

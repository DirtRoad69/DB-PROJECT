package com.example.developer.fullpatrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.developer.fragments.DutyFragment;
import com.example.developer.objects.PatrolPoint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver{
    public static final String ACTION_KILL_ALARM = "broadcast.kill.Alarm";
    public static final String ACTION_REST_COUNTER = "broadcast.reset.counter";
    public static final String ACTION_REST_ALARM = "broadcast.reset.alarm";
    public static final String ACTION_START_PATROL = "broadcast.start.patrol";
    public static final String ACTION_UNLOCK = "broadcast.reset.unlock";
    public static final String ACTION_CALLER = "CALLER", CALLER_ALARM = "alarm", CALLER_TIMER = "timer";
    public static final String ACTION_END_TIME = "end time";
    public static final String ACTION_NOW_OFFDUTY = "action.now.offduty";
    //timer values

    Interpol interpol;
    public static long ReceiveTime = 0;
    private boolean daySkipped = false;


    @Override
    public void onReceive(final Context context, Intent intent){
        try {
            Log.i("RFC", "" + intent.getStringExtra(ACTION_CALLER));

            final FirebaseManager firebaseManager = FirebaseManager.getInstance();
            final SiteDataManager siteDataManager = SiteDataManager.getInstance();
            this.interpol = Interpol.getInstance();


            Log.i("RFC", "onReceive: " + !interpol.execute(System.currentTimeMillis()));


            if(!interpol.execute(System.currentTimeMillis()))
                return;

            if(interpol.isExecuting())
                return;

            interpol.setExecuting(true);

            this.ReceiveTime = System.currentTimeMillis();

            firebaseManager.getPatrolDataLocally(new FirebaseManager.DataCallback() {
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
                            firebaseManager.getPatrolPointsLocally(pointCol, new FirebaseManager.DataCallback() {
                                @Override
                                public void onDataUpdated(Map<String, Object> data) {

                                }

                                @Override
                                public void onDataReceived(Map<String, Object> empty) {

                                    try{


                                        Calendar startTime = Calendar.getInstance();
                                        startTime.setTimeInMillis(System.currentTimeMillis());
                                        startTime.set(Calendar.HOUR_OF_DAY, siteDataManager.getInt("startHour"));
                                        startTime.set(Calendar.MINUTE, siteDataManager.getInt("startMin"));
                                        startTime.set(Calendar.SECOND, 0);

                                        int endHour =  siteDataManager.getInt("endHour") , endMin =siteDataManager.getInt("endMin");
                                        Calendar endTime = Calendar.getInstance();
                                        endTime.setTimeInMillis(System.currentTimeMillis());
                                        endTime.set(endTime.HOUR_OF_DAY, endHour);
                                        endTime.set(endTime.MINUTE, endMin);
                                        endTime.set(endTime.SECOND, 0);




                                        Calendar nowTime = Calendar.getInstance();



                                        List<PatrolPoint> points = (List<PatrolPoint>)siteDataManager.get("patrolPoints");
                                        String startPoint = siteDataManager.get("startEndPoint").toString();
                                        for(int pos = 0 ; pos < points.size(); pos++){
                                            if(points.get(pos).pointId.contains(startPoint)){
                                                points.get(pos).isStarting = true;
                                                break;
                                            }
                                        }

                                        detectDutyStatus(startTime, nowTime, endTime, firebaseManager, context, siteDataManager);
                                        interpol.setStarted(false);
                                        interpol.setExecuting(false);
                                    }catch(Exception e){}
                                }

                                @Override
                                public void onDataReceived(List<Map<String, Object>> data) {

                                }
                            });

                        }

//                    @Override
//                    public void valueChanged(String key) {
//                        Log.i("RFC", "value changed:" + key);
//                        Calendar calendar = Calendar.getInstance();
//                        calendar.setTimeInMillis(System.currentTimeMillis());
//                        calendar.set(Calendar.HOUR_OF_DAY, siteDataManager.getInt("startHour"));
//                        calendar.set(Calendar.MINUTE, siteDataManager.getInt("startMin"));
//                        calendar.set(Calendar.SECOND, 0);
//                        switch (key){
//                            case "startPatrolTime":
//
//                                long nxtTime =  Interpol.getNextTimePatrol(calendar.getTimeInMillis(), 1000*60* siteDataManager.getLong("intervalTimer").intValue());
//                                if(nxtTime < 0){
//                                    DutyFragment.DutyStatus = "OFF DUTY";
//                                    firebaseManager.sendEventType("events",  DutyFragment.DutyStatus , 10, "site");
//                                    resetAlarm(context);
//                                }
//                                break;
//                        }
//                    }
                    });
                }

                @Override
                public void onDataReceived(List<Map<String, Object>> data) {

                }
            });

        }catch (Exception exception){
            Log.i("WSX", "onReceive: ERRo" + exception.getMessage());

            //restart the app
        }


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

    public boolean isPM(int hour){
        boolean isPM = false;
        if(hour >= 12){
            isPM = true;
        }
        return isPM;
    }

    public void compareDates(Calendar startDate,Calendar nowDate, Calendar endDate, FirebaseManager firebaseManager, Context context, SiteDataManager siteDataManager){

        if((endDate.getTimeInMillis() >= nowDate.getTimeInMillis())
                && (nowDate.getTimeInMillis() >= startDate.getTimeInMillis())){





            long nxtTime =  Interpol.getNextTimePatrol(startDate.getTimeInMillis(), 1000*60* siteDataManager.getLong("intervalTimer").intValue());
            Date resultdate = new Date(nxtTime);
            Log.i("RFC", resultdate.toString());
            if(nxtTime < 0){
                //MainActivity.dutyStatus = "OFF DUTY";
                long  remainingTime = startDate.getTimeInMillis() - System.currentTimeMillis();
                interpol.setNextTime(startDate.getTimeInMillis());
                updateTime(context, (remainingTime / 60000.0));
                Log.i("RFC", "Ignore");

            }else{
                if(DutyFragment.DutyStatus.equals("OFF DUTY")){
                    firebaseManager.sendEventType("events",  "ON DUTY" , 2, "site");
                }
                DutyFragment.DutyStatus = "ON DUTY";
                interpol.setNextTime(nxtTime);
                updateTime(context, (nxtTime - System.currentTimeMillis()) / 60000.0);
                Log.i("WSX", "Execute "+(nxtTime - System.currentTimeMillis()) / 60000.0);
                //unlockMain(context);
                if(interpol.started())
                    return;
                interpol.setStarted(true);
                startPatrol(context);
            }


            Log.i("WSX", "compareDates: ON DUTY");
            Log.i("WSX", "compareDates: UPDATE UI");
            Log.i("WSX", "compareDates: SET NEXT TIME. . .\n" +nowDate.getTime() +" \nendTime "+ endDate.getTime() +"\nrStartTime "+ startDate.getTime() );
        }else{

            Log.i("WSX", "compareDates: OFF DUTY");
            Log.i("WSX", "compareDates: OFF DUTY SENT. . .");
            Log.i("WSX", "compareDates: SET NEXT TIME. . .\n" +nowDate.getTime() +" \nendTime "+ endDate.getTime() +"\nStartTime "+ startDate.getTime() );

//            if(startDate.getTimeInMillis() >= Calendar.getInstance().getTimeInMillis())
//                startDate.add(startDate.DATE, 1);

            long nxtTime = startDate.getTimeInMillis() - System.currentTimeMillis();
            if(nxtTime < 0){
                startDate.add(startDate.DATE, 1);
                DutyFragment.DutyStatus = "OFF DUTY";
                firebaseManager.sendEventType("events",  DutyFragment.DutyStatus , 10, "site");
                resetAlarm(context);
                interpol.setNextTime(startDate.getTimeInMillis());
                Log.i("RFC", "Ignore 2");
                long  remainingTime = startDate.getTimeInMillis() - System.currentTimeMillis();
                updateTime(context, (remainingTime / 60000.0));
            }else {
                DutyFragment.DutyStatus = "OFF DUTY";
                firebaseManager.sendEventType("events", DutyFragment.DutyStatus, 10, "site");
                resetAlarm(context);
                interpol.setNextTime(startDate.getTimeInMillis());
                Log.i("RFC", "Ignore 2");
                long remainingTime = startDate.getTimeInMillis() - System.currentTimeMillis();
                updateTime(context, (remainingTime / 60000.0));
            }

        }

    }
    public void detectDutyStatus(Calendar startDate,Calendar nowDate, Calendar endDate, FirebaseManager firebaseManager, Context context,SiteDataManager siteDataManager){
        //check if on different day
        if(isPM(startDate.get(startDate.HOUR_OF_DAY)) && !isPM(endDate.get(endDate.HOUR_OF_DAY))){
            //skip day

            endDate.add(endDate.DATE, 1);

            Log.i("WSX", "compareDates: Day Skipped");
            compareDates(startDate, nowDate, endDate, firebaseManager, context, siteDataManager);

        }else{
            Log.i("WSX", "compareDates: Day not skipped we are on the current day. . .");
            compareDates(startDate, nowDate, endDate, firebaseManager, context, siteDataManager);
        }

    }
    private boolean checkDay(Calendar endDate, Calendar nowDate){

        if(endDate.getTimeInMillis() <= nowDate.getTimeInMillis()){
            Log.i("WSX",endDate.getTime()+ " endDate.getTimeInMillis()"+endDate.getTimeInMillis());
            Log.i("WSX",nowDate.getTime()+ " nowDate.getTimeInMillis()"+nowDate.getTimeInMillis());
            return true;

        }else{
            return false;
        }
    }


    private void updateTime(Context context, double duration){
        Intent resetCounter = new Intent();
        resetCounter.setAction(ACTION_REST_COUNTER);
        resetCounter.putExtra("Duration", duration);
        context.sendBroadcast(resetCounter);
    }
    private void updateTimeOffDuty(Context context, double duration){
        Intent resetCounter = new Intent();
        resetCounter.setAction(ACTION_NOW_OFFDUTY);
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

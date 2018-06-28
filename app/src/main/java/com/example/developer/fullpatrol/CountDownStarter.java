package com.example.developer.fullpatrol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CountDownStarter extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent i) {
            AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            SiteDataManager siteDataManager = SiteDataManager.getInstance();

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(AlarmReceiver.ACTION_CALLER, AlarmReceiver.CALLER_ALARM);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);


            int startHour = siteDataManager.getInt("startHour"), startMin = siteDataManager.getInt("startMin"), intervalTimer = siteDataManager.getLong("intervalTimer").intValue();
            // Set the alarm to start at 8:30 a.m.
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, startHour);
            calendar.set(Calendar.MINUTE, startMin);
            calendar.set(Calendar.SECOND, 0);

            long nxtTime =  Interpol.getNextTimePatrol(calendar.getTimeInMillis(), 1000 * 60 * intervalTimer);

            // setRepeating() lets you specify a precise custom interval--in this case,
            // 20 minutes.
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
            Date resultdate = new Date(nxtTime);
            Log.i("RFC",  "2|" + resultdate);
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 1000 * 60 * intervalTimer, alarmIntent);

//            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System
//                    .currentTimeMillis(),///calendar.getTimeInMillis(),
//                1000*60* 2
//                , alarmIntent);
        }


}


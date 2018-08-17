package com.example.developer.services.subroutines;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.developer.fragments.DutyFragment;
import com.example.developer.fullpatrol.AlarmReceiver;
import com.example.developer.fullpatrol.MainActivity;


public class ApplicationMiddleware extends Observer {

    private Context context;
    public ApplicationMiddleware(Subject subject , Context context){

        this.subject = subject;
        this.subject.attach(this);
        this.context = context;

    }

    @Override
    public void update(int location) {
        //update server
        if(location == MainActivity.LOCAL_DB){
            Log.i("WSX", "update: Local DB changed");
            //push to server


        }
        else if(location == MainActivity.SERVER){
            Log.i("WSX", "update: server DB changed");
            if(DutyFragment.DutyStatus.equals("OFF DUTY")){
                Intent i = new Intent(AlarmReceiver.ACTION_END_TIME);
                context.sendBroadcast(i);
            }
            Log.i("WSX", "update: server DB changed 2");

        }

    }
}

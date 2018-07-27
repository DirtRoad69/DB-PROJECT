package com.example.developer.services.subroutines;

import android.util.Log;

import com.example.developer.fullpatrol.MainActivity;


public class ApplicationMiddleware extends Observer {

    public ApplicationMiddleware(Subject subject){

        this.subject = subject;
        this.subject.attach(this);

    }

    @Override
    public void update(int location) {
        //update server
        if(location == MainActivity.LOCAL_DB){
            Log.i("ZAQ@", "update: Local DB changed");
            //push to server


        }
        else if(location == MainActivity.SERVER){
            Log.i("ZAQ@", "update: server DB changed");
            //push to local db

        }

    }
}

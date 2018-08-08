package com.example.developer.fullpatrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnbootListener extends BroadcastReceiver {
    private static final String TAG = "RFC";

    /**This the method the System will call when an event will filtered occurs*/
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, intent.getAction());

        /**Here we check which event occurred and do the appropriate task.*/
        switch (intent.getAction()){
            case Intent.ACTION_SCREEN_ON:
                showKiosk(context);
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                //this.onBoot(context);
                Log.i("WSX", "onReceive: BOOTCOMPLETE");
                onBootDemo(context);
                break;
            case "android.intent.action.QUICKBOOT_POWERON":
                //this.onBoot(context);
                Log.i("WSX", "onReceive: QUICKBOOT_POWERON");
                onBootDemo(context);

                break;
                default:
                    showKiosk(context);
                    Log.i("Sgj", "H");
        }
    }

    private void onBootDemo(Context context){
        /**Show the BaseKiosk Activity*/
        Intent i = new Intent(context, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        Log.i("WSX", "onBoot: tried");
        /**Since the Service was killed, we restart it.*/

        Log.i("WSX", "onBoot: failed");
    }
    private void onBoot(Context context){
        /**Show the BaseKiosk Activity*/
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        Log.i("WSX", "onBoot: tried");
        /**Since the Service was killed, we restart it.*/
        Intent intent = new Intent(context, DispatcherService.class);
        context.startService(intent);
        Log.i("WSX", "onBoot: failed");
    }

    private void showKiosk(Context context){
        Log.i(TAG, "showKiosk: we are off");
        /**the process of starting the Kiosk Activty.*/
        if(Interpol.getInstance().isOutOfMainActivity())
            return;

        Log.i(TAG, "showKiosk: we are on");
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        /**At this point the Kiosk Activity is Running.*/
    }
}

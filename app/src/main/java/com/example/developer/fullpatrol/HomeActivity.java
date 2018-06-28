package com.example.developer.fullpatrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public static final String SHARE_KIOSK_ENABLED = "SHARE_KIOSK_ENABLED";
    public static final int REQUEST_SETUP = 231;
    private static final int REQUEST_LINK = 421;
    private static final int REQUEST_UNLINK = 521;
    private SharedPreferences sharedPreferences;
    private TextView ttvMessage, ttvUID, ttvLSName, ttvLsArea;
    private Button btnLink;
    private SwitchCompat spnEnable;
    private Intent dispatcherIntent;
    private ProgressDialog progressDialog;
    private FirebaseManager firebaseManager;
    private Thread looper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        this.firebaseManager = FirebaseManager.getInstance();
        this.firebaseManager.init();




        this.sharedPreferences = this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE);
        String deviceID, siteID;
        deviceID = this.sharedPreferences.getString(LinkDeviceActivity.PREF_UID, null);
        siteID = this.sharedPreferences.getString(LinkDeviceActivity.PREF_LINKED_SITE, null);

        if(deviceID == null || siteID == null){
            Intent setupIntent = new Intent(this, LinkDeviceActivity.class);
            this.startActivityForResult(setupIntent, REQUEST_SETUP);
        }else{


        }

        this.spnEnable = this.findViewById(R.id.spn_enable_kiosk);
        this.ttvMessage = this.findViewById(R.id.ttv_msg);
        this.ttvUID = this.findViewById(R.id.ttv_device_uid);
        this.ttvLSName = this.findViewById(R.id.ttv_linked_site_name);
        this.ttvLsArea = this.findViewById(R.id.ttv_linked_site_area);
        this.btnLink = this.findViewById(R.id.btn_link);

        btnLink.setOnClickListener(this);
        spnEnable.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        int requestCode = (getSITE() == null) ? REQUEST_LINK : REQUEST_UNLINK;
        Intent accessIntent = new Intent(this, InputCollector.class);;
        accessIntent.putExtra(InputCollector.ACCESS_TYPE,  InputCollector.ACCESS_TYPE_ADMIN);
        this.startActivityForResult(accessIntent, requestCode);
    }

    private void enableKioskMode() {
        updateActiveStatus(true);
        this.sharedPreferences.edit().putBoolean(SHARE_KIOSK_ENABLED, true).apply();

        dispatcherIntent = new Intent(this, DispatcherService.class);
        this.startService(dispatcherIntent);
    }

    private void updateActiveStatus(boolean isActive) {
        String uid = sharedPreferences.getString(LinkDeviceActivity.PREF_UID, null);
        if(uid == null)
            return;

        Map<String, Object> data = new HashMap<>();
        data.put("isActive", isActive);
        showProgress("Kiosk Mode", "Please Wait. " + (isActive ? "Enabling" : "Disabling") +" Kiosk Mode.");
        firebaseManager.updateField("machineCodes", uid, data, new FirebaseManager.DataPushCallack() {
            @Override
            public void onPushed() {
                dismissProgress();
            }
        });
    }

    private void showProgress(String title, String msg) {
        if(progressDialog == null){
            View parent = LayoutInflater.from(this).inflate(R.layout.progress_layout, null, false);
            TextView ttvTitle = parent.findViewById(R.id.ttv_title), ttvMsg = parent.findViewById(R.id.ttv_msg);
            ttvTitle.setText(title);
            ttvMsg.setText(msg);
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            progressDialog.setContentView(parent);

        }
    }

    private void dismissProgress(){
        if(progressDialog != null){
            progressDialog.dismiss();
            progressDialog  = null;
        }
    }

    private void disableKioskMode() {
        updateActiveStatus(false);
        this.sharedPreferences.edit().putBoolean(SHARE_KIOSK_ENABLED, false).apply();
        if(dispatcherIntent == null)
            dispatcherIntent = new Intent(this, DispatcherService.class);

        this.stopService(dispatcherIntent);


    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked){
            Log.i("RFC", "Enter C");

            if(getSITE() == null){
                Log.i("RFC", "Enter C2");

                showError();
                spnEnable.setChecked(false);
            }else{
                this.enableKioskMode();
                ttvMessage.setVisibility(View.VISIBLE);
                Log.i("RFC", "Enter C3");

            }
        }else{
            this.disableKioskMode();
            ttvMessage.setVisibility(View.GONE);
        }
    }

    private void showError() {
        Log.i("RFC", "Enter dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Unlinked Device.")
                .setMessage("Cannot Start Kiosk When Device Is Not Linked With A Site. Do You Want To Link The Device?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent linkIntent = new Intent(HomeActivity.this, LinkDeviceActivity.class);
                        HomeActivity.this.startActivity(linkIntent);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
//        errorDialog.show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(spnEnable != null)
            spnEnable.setChecked(this.sharedPreferences.getBoolean(SHARE_KIOSK_ENABLED, false));

        refresh();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       if(resultCode == Activity.RESULT_OK) {
           if (requestCode == REQUEST_LINK) {
               boolean authenticated = data.getBooleanExtra(InputCollector.EXTRA_LOGGED_IN, false);
               if(authenticated){
                   Intent setupIntent = new Intent(this, LinkDeviceActivity.class);
                   this.startActivityForResult(setupIntent, REQUEST_SETUP);
               }
           } else if (requestCode == REQUEST_UNLINK) {
               boolean authenticated = data.getBooleanExtra(InputCollector.EXTRA_LOGGED_IN, false);
               if(authenticated){
                   String uid = getUID();
                   showProgress("Unlink", "Unlinking Device [ " + uid + " ] From Site [ " + getSiteName() + " ]");
                   unlink(uid);
               }
           }
       }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void unlink(String uid) {

        firebaseManager.unlinkDevice(uid, "machineCodes", "siteMachines", new FirebaseManager.DataPushCallack() {
            @Override
            public void onPushed() {
                spnEnable.setChecked(false);
                sharedPreferences.edit().putString(LinkDeviceActivity.PREF_LINKED_SITE, null)
                        .putString(LinkDeviceActivity.PREF_LINKED_SITE_NAME, null)
                        .putString(LinkDeviceActivity.PREF_LINKED_SITE_AREA, null)
                        .apply();

                sharedPreferences.edit().putBoolean(SHARE_KIOSK_ENABLED, false).apply();
                if(dispatcherIntent == null)
                    dispatcherIntent = new Intent(HomeActivity.this, DispatcherService.class);

                stopService(dispatcherIntent);
                refresh();
                dismissProgress();
            }
        });
    }

    private void refresh() {
        String uid = getUID(), lsName = getSiteName(), lsArea = getSiteArea();
        boolean isSiteSet = !(lsName == null && lsArea == null);
        btnLink.setText((!isSiteSet ? "Link" : "Unlink") + " Device");
        ttvUID.setText((uid == null) ? "[ Error: not set ]" : uid);
        ttvLSName.setText( !isSiteSet? "[ not set ]" : lsName);
        ttvLsArea.setText( !isSiteSet? "[ not set ]" : lsArea);
    }

    private String getUID(){
        return this.sharedPreferences.getString(LinkDeviceActivity.PREF_UID, null);
    }


    private String getSITE(){
        return this.sharedPreferences.getString(LinkDeviceActivity.PREF_LINKED_SITE, null);
    }

    private String getSiteName(){
        return this.sharedPreferences.getString(LinkDeviceActivity.PREF_LINKED_SITE_NAME, null);
    }
    private String getSiteArea(){
        return this.sharedPreferences.getString(LinkDeviceActivity.PREF_LINKED_SITE_AREA, null);
    }
}